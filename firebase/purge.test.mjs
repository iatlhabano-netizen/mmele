/** Emulator test for purge.mjs. Run inside `firebase emulators:exec --only database,auth`. */
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getDatabase } from "firebase-admin/database";
import { purge } from "./purge.mjs";

const app = initializeApp({ projectId: "demo-mmele", databaseURL: `http://${process.env.FIREBASE_DATABASE_EMULATOR_HOST}/?ns=demo-mmele-default-rtdb` }, "purge-test");
const db = getDatabase(app);
const auth = getAuth(app);
const DAY = 24 * 3600 * 1000;
const now = Date.now();
const fails = [];
const check = (name, ok) => { if (!ok) fails.push(name); };

await db.ref().update({
  "rooms/OLD222": { hostUid: "a", phase: "finished", variant: "classic", moveCount: 0, updatedAt: now - 45 * DAY },
  "rooms/NEW333": { hostUid: "a", phase: "playing", variant: "classic", moveCount: 0, updatedAt: now - 2 * DAY },
  "users/a/rooms/OLD222": true,
  "users/a/rooms/NEW333": true,
});
const anon = await auth.createUser({});
const withEmail = await auth.createUser({ email: "keep@example.com" });

const dry = await purge({ db, auth, days: 30, dryRun: true, now, log: () => {} });
check("dry run counts the stale room", dry.roomsDeleted === 1);
check("dry run deletes nothing", (await db.ref("rooms/OLD222").get()).exists());

const r = await purge({ db, auth, days: 30, now, users: false, log: () => {} });
check("stale room deleted", !(await db.ref("rooms/OLD222").get()).exists());
check("fresh room kept", (await db.ref("rooms/NEW333").get()).exists());
check("index entry for deleted room dropped", !(await db.ref("users/a/rooms/OLD222").get()).exists() && r.indexEntriesDropped >= 1);
check("index entry for live room kept", (await db.ref("users/a/rooms/NEW333").get()).exists());

// Users: with a cutoff in the future every anonymous user is "inactive"; email users are never touched.
await db.ref(`users/${anon.uid}/rooms/NEW333`).set(true);
const u = await purge({ db, auth, days: 30, now: now + 31 * DAY, rooms: false, log: () => {} });
check("inactive anonymous user deleted", u.usersDeleted >= 1 && !(await auth.getUser(anon.uid).then(() => true, () => false)));
check("their index removed", !(await db.ref(`users/${anon.uid}`).get()).exists());
check("non-anonymous user kept", await auth.getUser(withEmail.uid).then(() => true, () => false));

console.log(`purge: ${9 - fails.length} passed, ${fails.length} failed`);
for (const f of fails) console.log("  FAIL " + f);
process.exit(fails.length ? 1 : 0);
