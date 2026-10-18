/**
 * Emulator tests for chat, blocking, reports, ranked settlement and tournaments.
 * Run: firebase emulators:exec --only database,auth,functions --project demo-mmele "node chat_ranked.test.mjs"
 * (the function-backed checks are skipped with a notice if the functions emulator is not running).
 */
import { initializeApp } from "firebase/app";
import { getAuth, signInAnonymously, connectAuthEmulator } from "firebase/auth";
import { getDatabase, ref, set, update, get, remove, serverTimestamp, connectDatabaseEmulator } from "firebase/database";
import { initializeApp as adminApp } from "firebase-admin/app";
import { getDatabase as adminDb } from "firebase-admin/database";
import { settleMatch, decideWinner, createBracket, reportBracket, championOf, prizes, eloUpdate } from "./functions/ranked.js";

const URL = "http://127.0.0.1:9000/?ns=demo-mmele-default-rtdb";
const fails = []; let passed = 0;
async function expect(name, ok, p) { let threw = false; try { await p; } catch { threw = true; } if (ok !== threw) passed++; else fails.push(`${name}: expected ${ok ? "ALLOW" : "DENY"}`); }
const check = (name, c) => { if (c) passed++; else fails.push(name); };
async function client(name) {
  const app = initializeApp({ apiKey: "demo", authDomain: "demo-mmele.firebaseapp.com", databaseURL: URL, projectId: "demo-mmele" }, name);
  const auth = getAuth(app); connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  const db = getDatabase(app); connectDatabaseEmulator(db, "127.0.0.1", 9000);
  const user = (await signInAnonymously(auth)).user;
  return { db, uid: user.uid, r: (p) => ref(db, p) };
}
const admin = adminDb(adminApp({ projectId: "demo-mmele", databaseURL: `http://${process.env.FIREBASE_DATABASE_EMULATOR_HOST ?? "127.0.0.1:9000"}/?ns=demo-mmele-default-rtdb` }, "cr-admin"));
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const base = (uid) => ({ hostUid: uid, createdAt: Date.now(), updatedAt: Date.now(), phase: "lobby", variant: "classic", clockMinutes: 0, styleOne: "obsidian", styleTwo: "quartz", moveCount: 0 });
const msg = (c, seat, k, b) => ({ [`chats/CHT234/m${Math.random().toString(36).slice(2, 9)}`]: { uid: c.uid, seat, k, b, at: serverTimestamp() }, [`users/${c.uid}/lastChatAt`]: serverTimestamp() });

async function pure() {
  const s = settleMatch({ rating: 1200, points: 1000, games: 0, wins: 0 }, { rating: 1200, points: 5, games: 40, wins: 3 }, "one");
  check("pure: equal players, new A wins +20, B -12", s.one.rating === 1220 && s.two.rating === 1188);
  check("pure: stake capped at loser's balance", s.moved === 5 && s.two.points === 0 && s.one.points === 1005);
  check("pure: draw moves no points", settleMatch({ rating: 1400, points: 10, games: 50, wins: 0 }, { rating: 1200, points: 10, games: 50, wins: 0 }, "draw").moved === 0);
  check("pure: vector 1500 v 1300 B wins", JSON.stringify(eloUpdate(1500, 1300, 0)) === "[1482,1318]");
  check("pure: resignation decides alone", decideWinner({ resigned: "two" }, null) === "one");
  check("pure: disagreeing reports are disputed", decideWinner({}, { one: "one", two: "two" }) === "disputed");
  check("pure: one report waits", decideWinner({}, { one: "one" }) === null);
  let b = createBracket(["a", "b", "c", "d"]);
  b = reportBracket(b, 4, "r1m0", "a"); b = reportBracket(b, 4, "r1m1", "d");
  check("pure: winners meet in the final", b.r2m0.one === "a" && b.r2m0.two === "d");
  b = reportBracket(b, 4, "r2m0", "d");
  check("pure: champion", championOf(b, 4) === "d");
  check("pure: prizes 70/30 of 16x20", JSON.stringify(prizes(16)) === "[224,96]");
}

async function rules() {
  const h = await client("h"), g = await client("g"), x = await client("x");
  await admin.ref("rooms/CHT234").set({ ...base(h.uid), guestUid: g.uid, phase: "playing" });
  await expect("host sends a quick phrase", true, update(h.r("/"), msg(h, "one", "p", "good_game")));
  await expect("rate limit: second message within 1.5 s refused", false, update(h.r("/"), msg(h, "one", "e", "👍")));
  await expect("guest sends free text", true, update(g.r("/"), msg(g, "two", "t", "Ke a leboga, well played")));
  await sleep(1600);
  await expect("after the gap the host may send again", true, update(h.r("/"), msg(h, "one", "e", "👍")));
  await sleep(1600);
  await expect("text over 120 characters refused", false, update(h.r("/"), msg(h, "one", "t", "x".repeat(121))));
  await expect("unknown phrase id refused", false, update(h.r("/"), msg(h, "one", "p", "buy_coins")));
  await expect("cannot send as the other seat", false, update(h.r("/"), msg(h, "two", "t", "hi")));
  await expect("cannot skip the rate-limit stamp", false, set(h.r("chats/CHT234/zz1"), { uid: h.uid, seat: "one", k: "t", b: "hi", at: serverTimestamp() }));
  await expect("outsider cannot write chat", false, update(x.r("/"), msg(x, "one", "t", "hi")));
  await expect("players read chat", true, get(g.r("chats/CHT234")));
  await expect("outsider cannot read chat", false, get(x.r("chats/CHT234")));
  const ids = Object.keys((await admin.ref("chats/CHT234").get()).val());
  const mine = ids[0];
  await expect("report written by reporter", true, set(g.r("reports/r1"), { reporterUid: g.uid, reportedUid: h.uid, room: "CHT234", messageId: mine, body: "good_game", at: serverTimestamp() }));
  await expect("reports are not readable by players", false, get(g.r("reports")));
  await expect("cannot report as someone else", false, set(x.r("reports/r2"), { reporterUid: g.uid, reportedUid: h.uid, room: "CHT234", messageId: "m", body: "b", at: serverTimestamp() }));
  await expect("player deletes chat with the room", true, remove(h.r("chats/CHT234")));

  // blocking
  await expect("host blocks x", true, set(h.r(`users/${h.uid}/blocked/${x.uid}`), true));
  await expect("cannot block yourself", false, set(h.r(`users/${h.uid}/blocked/${h.uid}`), true));
  await expect("host creates lobby", true, set(h.r("rooms/BKK234"), base(h.uid)));
  await expect("blocked player cannot join the host's room", false, update(x.r("rooms/BKK234"), { guestUid: x.uid, "connected/two": true, phase: "playing", updatedAt: Date.now() }));
  await expect("other player can", true, update(g.r("rooms/BKK234"), { guestUid: g.uid, "connected/two": true, phase: "playing", updatedAt: Date.now() }));

  // ranked: no client may write ratings / ledger / settled / its own result before the end
  await expect("player cannot write own rating", false, set(h.r(`ratings/${h.uid}`), { rating: 3000, points: 999999 }));
  await expect("player cannot write ledger", false, set(h.r(`ledger/${h.uid}/x`), { delta: 100 }));
  await expect("player cannot mark settled", false, set(h.r("settled/RNK234"), { winner: "one" }));
  await expect("ranked room created", true, set(h.r("rooms/RNK234"), { ...base(h.uid), ranked: true }));
  await expect("cannot create a room with an invite (functions only)", false, set(h.r("rooms/NVT234"), { ...base(h.uid), invite: g.uid }));
  await update(g.r("rooms/RNK234"), { guestUid: g.uid, "connected/two": true, phase: "playing", updatedAt: Date.now() });
  await expect("ranked flag is immutable", false, set(h.r("rooms/RNK234/ranked"), false));
  await expect("no result while playing", false, set(h.r("results/RNK234/one"), "one"));
  await admin.ref("rooms/RNK234/phase").set("finished");
  await expect("host reports result", true, set(h.r("results/RNK234/one"), "one"));
  await expect("host cannot report for the guest", false, set(h.r("results/RNK234/two"), "one"));
  await expect("result is write-once", false, set(h.r("results/RNK234/one"), "two"));
  await expect("everyone can read ratings (leaderboard)", true, get(x.r("ratings")));

  // tournaments
  await expect("create a 4-player tournament", true, set(h.r("tournaments/t1"), { name: "Gabs Cup", size: 4, entry: 20, createdBy: h.uid, createdAt: serverTimestamp(), status: "open" }));
  await expect("size 5 refused", false, set(h.r("tournaments/t2"), { name: "x", size: 5, entry: 20, createdBy: h.uid, createdAt: serverTimestamp(), status: "open" }));
  await expect("entry fee must be the fixed virtual 20", false, set(h.r("tournaments/t3"), { name: "x", size: 4, entry: 0, createdBy: h.uid, createdAt: serverTimestamp(), status: "open" }));
  await expect("cannot create already running", false, set(h.r("tournaments/t4"), { name: "x", size: 4, entry: 20, createdBy: h.uid, createdAt: serverTimestamp(), status: "running" }));
  await expect("join yourself", true, set(g.r(`tournaments/t1/entrants/${g.uid}`), serverTimestamp()));
  await expect("cannot join for someone else", false, set(g.r(`tournaments/t1/entrants/${x.uid}`), serverTimestamp()));
  await expect("cannot write the bracket", false, set(g.r("tournaments/t1/matches"), { r1m0: { winner: g.uid } }));
  await expect("cannot change status", false, set(h.r("tournaments/t1/status"), "finished"));
  return { h, g, x };
}

async function functionsFlow({ h, g }) {

  // Wait for the functions emulator to settle RNK234 after the guest's agreeing report.
  await set(g.r("results/RNK234/two"), "one");
  let s = null;
  for (let i = 0; i < 40 && !(s = (await admin.ref(`ratings/${h.uid}`).get()).val()); i++) await sleep(250);
  if (!s) { console.log("NOTICE: functions emulator not running; settlement checks skipped"); return; }
  const rh = (await admin.ref(`ratings/${h.uid}`).get()).val(), rg = (await admin.ref(`ratings/${g.uid}`).get()).val();
  check("fn: winner gains rating and the stake", rh.rating === 1220 && rh.points === 1010);
  for (let i = 0; i < 20 && !Object.values((await admin.ref(`ledger/${g.uid}`).get()).val() ?? {}).some((l) => l.reason === "ranked_loss"); i++) await sleep(250);
  const lg = Object.values((await admin.ref(`ledger/${g.uid}`).get()).val() ?? {});
  check("fn: loser loses rating and the stake (ledgered)", rg.rating === 1180 && lg.some((l) => l.reason === "ranked_loss" && l.delta === -10));
  await expect("player reads own ledger", true, get(g.r(`ledger/${g.uid}`)));
  await expect("player cannot read another ledger", false, get(g.r(`ledger/${h.uid}`)));
  // tournament: 4 players join, bracket starts, rooms open, two results + final produce a champion
  const p = [g, await client("p3"), await client("p4"), await client("p5")];
  for (const c of p.slice(1)) await set(c.r(`tournaments/t1/entrants/${c.uid}`), serverTimestamp());
  let t = null;
  for (let i = 0; i < 40; i++) { t = (await admin.ref("tournaments/t1").get()).val(); if (t?.status === "running" && t.matches?.r1m1?.room) break; await sleep(250); }
  check("fn: full tournament starts", t?.status === "running"); if (t?.status !== "running") console.log(JSON.stringify(t));
  check("fn: entry fee taken", (await admin.ref(`ratings/${p[1].uid}/points`).get()).val() === 980);
  const byUid = Object.fromEntries(p.map((c) => [c.uid, c]));
  async function play(mid) {
    const m = (await admin.ref(`tournaments/t1/matches/${mid}`).get()).val();
    const room = (await admin.ref(`rooms/${m.room}`).get()).val();
    check(`fn: ${mid} room invites the right guest`, room.hostUid === m.one && room.invite === m.two && room.ranked === true);
    await update(byUid[m.two].r(`rooms/${m.room}`), { guestUid: m.two, "connected/two": true, phase: "playing", updatedAt: Date.now() });
    await admin.ref(`rooms/${m.room}/phase`).set("finished");
    await set(byUid[m.one].r(`results/${m.room}/one`), "one");
    await set(byUid[m.two].r(`results/${m.room}/two`), "one");
    return m.one;
  }
  await play("r1m0"); await play("r1m1");
  for (let i = 0; i < 40; i++) { t = (await admin.ref("tournaments/t1").get()).val(); if (t.matches.r2m0?.room) break; await sleep(250); }
  const champ = await play("r2m0");
  for (let i = 0; i < 40; i++) { t = (await admin.ref("tournaments/t1").get()).val(); if (t.status === "finished") break; await sleep(250); }
  check("fn: tournament finishes with the final's winner", t.status === "finished" && t.champion === champ);
}

async function main() {
  await pure();
  const c = await rules();
  await functionsFlow(c);
  console.log(`chat+ranked: ${passed} passed, ${fails.length} failed`);
  for (const f of fails) console.log("  FAIL " + f);
  process.exit(fails.length === 0 ? 0 : 1);
}
main().catch((e) => { console.error(e); process.exit(1); });
