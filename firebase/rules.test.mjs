/**
 * Security-rule tests against the RTDB emulator (firebase/database.rules.json).
 * Run with the emulator: `firebase emulators:exec --only database,auth "node rules.test.mjs"`
 * from the firebase/ directory. No cloud project is used (demo-mmele).
 */
import { initializeApp } from "firebase/app";
import { getAuth, signInAnonymously, connectAuthEmulator } from "firebase/auth";
import { getDatabase, ref, set, update, get, serverTimestamp, connectDatabaseEmulator } from "firebase/database";
import { initializeApp as adminApp } from "firebase-admin/app";
import { getDatabase as adminDb } from "firebase-admin/database";

const app = initializeApp({ apiKey: "demo", authDomain: "demo-mmele.firebaseapp.com", databaseURL: "http://127.0.0.1:9000/?ns=demo-mmele-default-rtdb", projectId: "demo-mmele" });
const auth = getAuth(app);
connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
const db = getDatabase(app);
connectDatabaseEmulator(db, "127.0.0.1", 9000);

const fails = [];
let passed = 0;
async function expect(name, ok, promise) {
  let threw = false;
  try { await promise; } catch (e) { threw = true; }
  const good = ok ? !threw : threw;
  if (good) passed++; else fails.push(`${name}: expected ${ok ? "ALLOW" : "DENY"}, got ${threw ? "DENY" : "ALLOW"}`);
}
const room = (code, path = "") => ref(db, `rooms/${code}${path ? "/" + path : ""}`);

const base = (uid) => ({ hostUid: uid, createdAt: Date.now(), updatedAt: Date.now(), phase: "lobby", variant: "classic", clockMinutes: 0, styleOne: "obsidian", styleTwo: "quartz", moveCount: 0, connected: { one: true } });

async function main() {
  const host = (await signInAnonymously(auth)).user;
  const hostUid = host.uid;

  await expect("host creates lobby", true, set(room("ABC234"), base(hostUid)));
  await expect("host cannot create twice", false, set(room("ABC234"), base(hostUid)));
  await expect("bad code rejected", false, set(room("ABC230"), base(hostUid))); // contains 0
  await expect("cannot create with moves", false, set(room("ABC235"), { ...base(hostUid), moves: { 0: "one|P|1" } }));
  await expect("cannot claim someone else's uid", false, set(room("ABC236"), { ...base(hostUid), hostUid: "someone-else" }));

  // guest joins
  const guestApp = initializeApp({ apiKey: "demo", authDomain: "demo-mmele.firebaseapp.com", databaseURL: "http://127.0.0.1:9000/?ns=demo-mmele-default-rtdb", projectId: "demo-mmele" }, "g");
  const gauth = getAuth(guestApp);
  connectAuthEmulator(gauth, "http://127.0.0.1:9099", { disableWarnings: true });
  const gdb = getDatabase(guestApp);
  connectDatabaseEmulator(gdb, "127.0.0.1", 9000);
  const guest = (await signInAnonymously(gauth)).user;
  const groom = (path = "") => ref(gdb, `rooms/ABC234${path ? "/" + path : ""}`);

  // By design any signed-in player may read a room: knowing the 6-char code is what lets you join.
  await expect("signed-in player can look up a room by code", true, get(groom()));
  await expect("guest joins", true, update(groom(), { guestUid: guest.uid, "connected/two": true, phase: "playing", updatedAt: Date.now() }));
  await expect("second guest cannot overwrite", false, update(room("ABC234"), { guestUid: hostUid }));

  // moves
  await expect("guest cannot move first", false, update(groom(), { "moves/0": { by: "two", m: "P|1" }, moveCount: 1 }));
  await expect("first move must be a placement, not a shot", false, update(room("ABC234"), { "moves/0": { by: "one", m: "S|1" }, moveCount: 1 }));
  await expect("host places", true, update(room("ABC234"), { "moves/0": { by: "one", m: "P|3" }, moveCount: 1, updatedAt: Date.now() }));
  await expect("host cannot move twice in a row (index)", false, update(room("ABC234"), { "moves/1": { by: "one", m: "P|4" }, moveCount: 2 }));
  await expect("guest moves at index 1", true, update(groom(), { "moves/1": { by: "two", m: "P|5" }, moveCount: 2, updatedAt: Date.now() }));
  await expect("host cannot shoot right after the guest's move", false, update(room("ABC234"), { "moves/2": { by: "one", m: "S|5" }, moveCount: 3 }));
  await expect("guest cannot place twice in a row", false, update(groom(), { "moves/2": { by: "two", m: "P|6" }, moveCount: 3 }));
  await expect("guest may shoot after own placement (mill claimed)", true, update(groom(), { "moves/2": { by: "two", m: "S|3" }, moveCount: 3 }));
  await expect("guest cannot shoot twice", false, update(groom(), { "moves/3": { by: "two", m: "S|4" }, moveCount: 4 }));
  await expect("guest cannot move again after the shot", false, update(groom(), { "moves/3": { by: "two", m: "P|4" }, moveCount: 4 }));
  await expect("host replies after the shot", true, update(room("ABC234"), { "moves/3": { by: "one", m: "P|3" }, moveCount: 4 }));
  await expect("cannot rewrite history", false, update(room("ABC234"), { "moves/0": { by: "one", m: "P|9" } }));
  await expect("cannot skip an index", false, update(room("ABC234"), { "moves/5": { by: "one", m: "P|6" }, moveCount: 6 }));
  await expect("malformed move rejected", false, update(room("ABC234"), { "moves/4": { by: "one", m: "HACK|0" }, moveCount: 5 }));
  await expect("guest cannot write the host's seat", false, update(groom(), { "moves/4": { by: "one", m: "P|6" }, moveCount: 5 }));
  await expect("host cannot move out of turn", false, update(room("ABC234"), { "moves/4": { by: "one", m: "P|6" }, moveCount: 5 }));
  await expect("host cannot mark guest connected", false, update(room("ABC234"), { "connected/two": false }));
  await expect("guest marks self away", true, update(groom(), { "connected/two": false, "leftAt/two": Date.now() - 1000 }));

  // abandonment: too early, then after 90 s
  await expect("host cannot claim too early", false, update(room("ABC234"), { resigned: "two", phase: "abandoned" }));
  await expect("guest cannot resign for the host", false, update(groom(), { resigned: "one" }));
  await expect("guest's leftAt can't be in the future", false, update(groom(), { "leftAt/two": Date.now() + 600000 }));
  await expect("guest stays away (leftAt 100 s ago)", true, update(groom(), { "leftAt/two": Date.now() - 100000 }));
  await expect("host claims abandoned win", true, update(room("ABC234"), { resigned: "two", phase: "abandoned" }));
  await expect("move after the game ended rejected", false, update(room("ABC234"), { "moves/4": { by: "one", m: "P|6" }, moveCount: 5 }));

  // rematch: needs the opponent's offer first
  await expect("host cannot wipe moves without the guest's rematch offer", false, update(room("ABC234"), { moves: null, moveCount: 0, resigned: null, phase: "playing" }));
  await expect("guest offers rematch", true, update(groom(), { "offers/rematch/two": true, "connected/two": true }));
  await expect("host cannot set the guest's offer", false, update(room("ABC234"), { "offers/rematch/two": false }));
  await expect("host accepts rematch", true, update(room("ABC234"), { moves: null, moveCount: 0, resigned: null, offers: null, phase: "playing", updatedAt: Date.now() }));
  const after = (await get(room("ABC234"))).val();
  if (after.moveCount !== 0 || after.moves || after.resigned) fails.push("rematch did not reset the room"); else passed++;

  // a fresh game in the same room works, and resigning yourself is allowed
  await expect("host first move after rematch", true, update(room("ABC234"), { "moves/0": { by: "one", m: "P|7" }, moveCount: 1 }));
  await expect("guest resigns", true, update(groom(), { resigned: "two", phase: "finished" }));
  // ---- per-user room index (used by "Delete my online data") ----
  const uref = (d, uid, path) => ref(d, `users/${uid}${path ? "/" + path : ""}`);
  await expect("host indexes own room", true, set(uref(db, hostUid, "rooms/ABC234"), true));
  await expect("host cannot write the guest's index", false, set(uref(db, guest.uid, "rooms/ABC234"), true));
  await expect("index keys must be room codes", false, set(uref(db, hostUid, "rooms/abc"), true));
  await expect("index holds nothing else", false, set(uref(db, hostUid, "name"), "Neo"));
  await expect("guest cannot read the host's index", false, get(uref(gdb, hostUid)));
  await expect("guest indexes own room", true, set(uref(gdb, guest.uid, "rooms/ABC234"), true));

  // ---- deletion and clean-up ----
  const strangerApp = initializeApp({ apiKey: "demo", authDomain: "demo-mmele.firebaseapp.com", databaseURL: "http://127.0.0.1:9000/?ns=demo-mmele-default-rtdb", projectId: "demo-mmele" }, "s");
  const sauth = getAuth(strangerApp);
  connectAuthEmulator(sauth, "http://127.0.0.1:9099", { disableWarnings: true });
  const sdb = getDatabase(strangerApp);
  connectDatabaseEmulator(sdb, "127.0.0.1", 9000);
  await signInAnonymously(sauth);
  const sroom = (code) => ref(sdb, `rooms/${code}`);
  await expect("stranger cannot delete an active room", false, set(sroom("ABC234"), null));
  await expect("host deletes the finished room", true, set(room("ABC234"), null));
  await expect("host clears own index", true, set(uref(db, hostUid), null));

  const DAY = 24 * 3600 * 1000;
  await expect("host creates a room (later stale)", true, set(room("ABC237"), { ...base(hostUid), createdAt: Date.now() - 40 * DAY, updatedAt: Date.now() - 31 * DAY }));
  await expect("anyone signed in may delete a room idle > 30 days", true, set(sroom("ABC237"), null));
  await expect("host creates a fresh room", true, set(room("ABC238"), base(hostUid)));
  await expect("stranger cannot delete a fresh room", false, set(sroom("ABC238"), null));
  await expect("room idle 29 days is not yet purgeable", true, set(room("ABC239"), { ...base(hostUid), updatedAt: Date.now() - 29 * DAY }));
  await expect("stranger cannot delete a 29-day room", false, set(sroom("ABC239"), null));
  await expect("guest joins ABC238", true, update(ref(gdb, "rooms/ABC238"), { guestUid: guest.uid, "connected/two": true, phase: "playing", updatedAt: Date.now() }));
  await expect("guest may delete a room it plays in", true, set(ref(gdb, "rooms/ABC238"), null));
  await expect("delete is not a back door to overwrite", false, set(sroom("ABC239"), { ...base("x"), phase: "finished" }));

  // ---- idle claim: the player to move sits for 5 minutes without moving ----
  // (lastMoveAt must be server "now" when written, so the admin SDK backdates it, as time passing would.)
  const admin = adminDb(adminApp({ projectId: "demo-mmele", databaseURL: `http://${process.env.FIREBASE_DATABASE_EMULATOR_HOST ?? "127.0.0.1:9000"}/?ns=demo-mmele-default-rtdb` }, "rules-admin"));
  await expect("host creates idle-test room", true, set(room("WAT234"), base(hostUid)));
  const iroom = (path = "") => ref(gdb, `rooms/WAT234${path ? "/" + path : ""}`);
  await expect("guest joins with the idle clock", true, update(iroom(), { guestUid: guest.uid, "connected/two": true, phase: "playing", updatedAt: serverTimestamp(), lastMoveAt: serverTimestamp(), toMove: "one" }));
  await expect("lastMoveAt must be server time", false, update(room("WAT234"), { "moves/0": { by: "one", m: "P|3" }, moveCount: 1, lastMoveAt: Date.now() - 400000, toMove: "two" }));
  await expect("host moves and hands the turn over", true, update(room("WAT234"), { "moves/0": { by: "one", m: "P|3" }, moveCount: 1, lastMoveAt: serverTimestamp(), toMove: "two" }));
  await expect("guest cannot reset the idle clock without moving", false, update(iroom(), { lastMoveAt: serverTimestamp() }));
  await expect("guest cannot take the turn back without moving", false, update(iroom(), { toMove: "one" }));
  await expect("host cannot claim before 5 minutes", false, update(room("WAT234"), { resigned: "two", phase: "abandoned" }));
  await admin.ref("rooms/WAT234/lastMoveAt").set(Date.now() - 301000);
  await expect("guest (to move) cannot claim on the idle clock", false, update(iroom(), { resigned: "one", phase: "abandoned" }));
  await expect("host claims after 5 idle minutes, guest still connected", true, update(room("WAT234"), { resigned: "two", phase: "abandoned" }));
  // The idle player can't dodge by flipping connection: the claim doesn't depend on presence.
  await admin.ref("rooms/WAT235").set({ ...base(hostUid), guestUid: guest.uid, phase: "playing", connected: { one: true, two: true }, moveCount: 1, moves: { 0: { by: "one", m: "P|1" } }, toMove: "one", lastMoveAt: Date.now() - 400000 });
  await expect("guest claims when the host is the idle one (toMove one)", true, update(ref(gdb, "rooms/WAT235"), { resigned: "one", phase: "abandoned" }));

  await expect("unauthenticated write denied", false, (async () => { await auth.signOut(); await update(room("ABC234"), { phase: "playing" }); })());

  console.log(`rules: ${passed} passed, ${fails.length} failed`);
  for (const f of fails) console.log("  FAIL " + f);
  process.exit(fails.length === 0 ? 0 : 1);
}
main().catch((e) => { console.error(e); process.exit(1); });
