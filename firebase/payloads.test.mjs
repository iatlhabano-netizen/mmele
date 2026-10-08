/**
 * Replays firebase/fixtures/kotlin-payloads.json (written by the :online JVM test
 * PayloadFixtureTest) against the RTDB emulator, as two anonymous users. Every write the
 * Android client would make must be accepted by database.rules.json.
 */
import { readFileSync } from "node:fs";
import { initializeApp } from "firebase/app";
import { getAuth, signInAnonymously, connectAuthEmulator } from "firebase/auth";
import { getDatabase, ref, set, update, get, serverTimestamp, connectDatabaseEmulator } from "firebase/database";

const cfg = { apiKey: "demo", authDomain: "demo-mmele.firebaseapp.com", databaseURL: "http://127.0.0.1:9000/?ns=demo-mmele-default-rtdb", projectId: "demo-mmele" };
function client(name) {
  const app = initializeApp(cfg, name);
  const auth = getAuth(app);
  connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  const db = getDatabase(app);
  connectDatabaseEmulator(db, "127.0.0.1", 9000);
  return { auth, db };
}
const fx = JSON.parse(readFileSync(new URL("./fixtures/kotlin-payloads.json", import.meta.url)));
const host = client("host"), guest = client("guest");
const hostUid = (await signInAnonymously(host.auth)).user.uid;
const guestUid = (await signInAnonymously(guest.auth)).user.uid;

function prep(v) {
  if (v === "__SERVER_TIME__") return serverTimestamp();
  if (v === "$HOST") return hostUid;
  if (v === "$GUEST") return guestUid;
  if (v && typeof v === "object") return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, prep(x)]));
  return v;
}

let ok = 0;
try {
  try { await set(ref(host.db, `rooms/${fx.code}`), prep(fx.create)); ok++; }
  catch (e) { console.log("payloads: CREATE denied"); process.exit(1); }
  for (const [i, s] of fx.steps.entries()) {
    const who = s.actor === "host" ? host : guest;
    try { await update(ref(who.db, `rooms/${fx.code}`), prep(s.update)); ok++; }
    catch (e) { console.log(`payloads: step ${i} (${s.actor}) DENIED: ${JSON.stringify(s.update)}`); process.exit(1); }
  }
  const final = (await get(ref(host.db, `rooms/${fx.code}`))).val();
  for (const [k, v] of Object.entries(fx.expectFinal)) {
    if (final[k] !== v) { console.log(`payloads: final ${k}=${final[k]} expected ${v}`); process.exit(1); }
  }
  console.log(`payloads: ${ok} Kotlin writes accepted by the rules, final state ok`);
  process.exit(0);
} catch (e) { console.error(e); process.exit(1); }
