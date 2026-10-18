// Mmele Cloud Functions (Realtime Database triggers, 2nd gen). Authoritative settlement of ranked
// matches and tournaments: clients can never write ratings/, ledger/, settled/ or tournament
// brackets (database.rules.json). Virtual points only. Deploy: see docs/RANKED.md.
import { onValueWritten, onValueCreated, onValueDeleted } from "firebase-functions/v2/database";
import { initializeApp } from "firebase-admin/app";
import { getDatabase, ServerValue } from "firebase-admin/database";
import { settleMatch, decideWinner, fresh, createBracket, reportBracket, championOf, prizes, shuffle, ENTRY } from "./ranked.js";

initializeApp();
// Every trigger works on the database instance that fired it (in the emulator the default
// FIREBASE_CONFIG namespace differs from the RTDB instance, so getDatabase() alone would miss it).
let ROOT = null;
const db = () => ({ ref: (p) => (p ? (ROOT ?? getDatabase().ref()).child(p) : (ROOT ?? getDatabase().ref())) });
const on = (fn) => (e) => { ROOT = (e.data.after ?? e.data).ref.root; return fn(e); };
const CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const code = () => Array.from({ length: 6 }, () => CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)]).join("");

async function profile(uid) { const s = await db().ref(`ratings/${uid}`).get(); return s.exists() ? { ...fresh(uid), ...s.val(), uid } : fresh(uid); }
const stored = (p) => ({ rating: p.rating, points: p.points, games: p.games, wins: p.wins, name: p.name ?? "" });
const ledger = (uid, delta, reason, ref) => db().ref(`ledger/${uid}`).push({ delta, reason, ref, at: ServerValue.TIMESTAMP });

/** Settle one finished ranked room exactly once (settled/{code} is claimed in a transaction). */
export async function settleRoom(roomCode) {
  const room = (await db().ref(`rooms/${roomCode}`).get()).val();
  if (!room || room.ranked !== true || room.phase !== "finished" || !room.guestUid) return "skip";
  const results = (await db().ref(`results/${roomCode}`).get()).val();
  const winner = decideWinner(room, results);
  if (!winner) return "waiting";
  const claim = await db().ref(`settled/${roomCode}`).transaction((cur) => (cur ? undefined : { winner, at: Date.now() }));
  if (!claim.committed) return "already";
  if (winner === "disputed") return "disputed"; // recorded, nothing moves; see docs/RANKED.md
  const [a, b] = await Promise.all([profile(room.hostUid), profile(room.guestUid)]);
  const s = settleMatch(a, b, winner);
  await db().ref().update({
    [`ratings/${room.hostUid}`]: stored(s.one),
    [`ratings/${room.guestUid}`]: stored(s.two),
  });
  if (s.moved) {
    const [w, l] = winner === "one" ? [room.hostUid, room.guestUid] : [room.guestUid, room.hostUid];
    await Promise.all([ledger(w, s.moved, "ranked_win", roomCode), ledger(l, -s.moved, "ranked_loss", roomCode)]);
  }
  if (room.tournament?.id) await advanceTournament(room.tournament.id, room.tournament.match, winner === "one" ? room.hostUid : winner === "two" ? room.guestUid : null);
  return "settled";
}

async function openMatchRooms(tid, matches) {
  const up = {};
  for (const [id, m] of Object.entries(matches)) {
    if (!m.one || !m.two || m.winner || m.room) continue;
    const c = code();
    up[`tournaments/${tid}/matches/${id}/room`] = c;
    up[`rooms/${c}`] = { hostUid: m.one, invite: m.two, phase: "lobby", variant: "classic", clockMinutes: 0, styleOne: "obsidian", styleTwo: "quartz",
      moveCount: 0, ranked: true, tournament: { id: tid, match: id }, createdAt: ServerValue.TIMESTAMP, updatedAt: ServerValue.TIMESTAMP };
  }
  if (Object.keys(up).length) await db().ref().update(up);
}

/** Records a tournament match result; a draw is replayed (new room). Pays virtual prizes at the end. */
export async function advanceTournament(tid, matchId, winnerUid) {
  const t = (await db().ref(`tournaments/${tid}`).get()).val();
  if (!t || t.status !== "running") return;
  if (!winnerUid) { await db().ref(`tournaments/${tid}/matches/${matchId}/room`).remove(); return openMatchRooms(tid, { ...t.matches, [matchId]: { ...t.matches[matchId], room: null } }); }
  const matches = reportBracket(t.matches, t.size, matchId, winnerUid);
  await db().ref(`tournaments/${tid}/matches`).set(matches);
  const champ = championOf(matches, t.size);
  if (!champ) return openMatchRooms(tid, matches);
  const final = matches[`r${Math.log2(t.size)}m0`];
  const runner = final.one === champ ? final.two : final.one;
  const [p1, p2] = prizes(t.size, t.entry);
  for (const [uid, prize] of [[champ, p1], [runner, p2]]) {
    await db().ref(`ratings/${uid}`).transaction((cur) => { const b = stored({ ...fresh(uid), ...(cur ?? {}) }); return { ...b, points: b.points + prize }; });
    await ledger(uid, prize, "tournament_prize", tid);
  }
  await db().ref(`tournaments/${tid}`).update({ status: "finished", champion: champ, runnerUp: runner, finishedAt: ServerValue.TIMESTAMP });
}

/** Virtual entry: checks capacity and balance, takes the entry, starts the bracket when full. */
export async function handleEntry(tid, uid) {
  const tRef = db().ref(`tournaments/${tid}`);
  const t = (await tRef.get()).val();
  const p = await profile(uid);
  const count = Object.keys(t?.entrants ?? {}).length;
  if (!t || t.status !== "open" || count > t.size || p.points < (t.entry ?? ENTRY)) {
    await db().ref().update({ [`tournaments/${tid}/entrants/${uid}`]: null, [`tournaments/${tid}/rejected/${uid}`]: !t || t.status !== "open" ? "closed" : count > t.size ? "full" : "points" });
    return "rejected";
  }
  await db().ref(`ratings/${uid}`).transaction((cur) => { const b = stored({ ...fresh(uid), ...(cur ?? {}) }); return { ...b, points: b.points - t.entry }; });
  await db().ref(`tournaments/${tid}/paid/${uid}`).set(true);
  await ledger(uid, -t.entry, "tournament_entry", tid);
  // Re-read: entries arrive concurrently, so whoever pays last starts the bracket.
  const paid = Object.keys((await tRef.child("paid").get()).val() ?? {});
  if (paid.length === t.size) {
    const started = await tRef.child("status").transaction((s) => (s === "open" || s === null ? "running" : undefined)) // null = not cached yet; RTDB retries with the real value;
    if (!started.committed) return "joined";
    const players = shuffle(paid);
    const matches = createBracket(players);
    await tRef.update({ matches, startedAt: ServerValue.TIMESTAMP });
    await openMatchRooms(tid, matches);
  }
  return "joined";
}

export async function handleLeave(tid, uid) {
  const t = (await db().ref(`tournaments/${tid}`).get()).val();
  if (!t?.paid?.[uid] || t.status !== "open") return;
  await db().ref(`ratings/${uid}`).transaction((cur) => cur ? { ...cur, points: (cur.points ?? 0) + t.entry } : cur);
  await db().ref(`tournaments/${tid}/paid/${uid}`).remove();
  await ledger(uid, t.entry, "tournament_refund", tid);
}

export const onRankedResult = onValueWritten("/results/{code}/{seat}", on((e) => settleRoom(e.params.code)));
export const onRankedFinish = onValueWritten("/rooms/{code}/phase", on((e) => (e.data.after.val() === "finished" ? settleRoom(e.params.code) : null)));
export const onTournamentEntry = onValueCreated("/tournaments/{tid}/entrants/{uid}", on((e) => handleEntry(e.params.tid, e.params.uid)));
export const onTournamentLeave = onValueDeleted("/tournaments/{tid}/entrants/{uid}", on((e) => handleLeave(e.params.tid, e.params.uid)));
