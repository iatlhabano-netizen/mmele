#!/usr/bin/env node
/**
 * Mmele clean-up: deletes online rooms idle for more than N days (default 30), drops room-index
 * entries that point at deleted rooms, and deletes anonymous Firebase users inactive for N days.
 * No Cloud Functions or paid plan needed: run it by hand (e.g. monthly) from any computer.
 *
 *   npm install                       # once, in firebase/
 *   export GOOGLE_APPLICATION_CREDENTIALS=~/mmele-keys/firebase-admin.json   # service-account key, NEVER commit
 *   node purge.mjs --project YOUR_PROJECT_ID --database-url https://YOUR-DB.europe-west1.firebasedatabase.app --dry-run
 *   node purge.mjs --project YOUR_PROJECT_ID --database-url https://YOUR-DB.europe-west1.firebasedatabase.app
 *
 * Options: --days N (default 30) · --dry-run (report only) · --rooms-only · --users-only
 * Under `firebase emulators:exec` the emulator hosts are picked up automatically.
 */
import { initializeApp, applicationDefault } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getDatabase } from "firebase-admin/database";
import { pathToFileURL } from "node:url";

const DAY = 24 * 3600 * 1000;

/** Core logic, exported for the emulator test. Returns counts. */
export async function purge({ db, auth, days = 30, dryRun = false, now = Date.now(), rooms = true, users = true, log = console.log }) {
  const cutoff = now - days * DAY;
  const out = { roomsDeleted: 0, roomsKept: 0, indexEntriesDropped: 0, usersDeleted: 0, usersKept: 0 };

  if (rooms) {
    const snap = await db.ref("rooms").get();
    const updates = {};
    snap.forEach((child) => {
      const v = child.val() || {};
      const last = typeof v.updatedAt === "number" ? v.updatedAt : typeof v.createdAt === "number" ? v.createdAt : 0;
      if (last < cutoff) { updates[child.key] = null; out.roomsDeleted++; } else out.roomsKept++;
    });
    if (!dryRun && out.roomsDeleted) await db.ref("rooms").update(updates);
  }

  // Index entries whose room is gone (after the step above).
  const live = new Set(Object.keys((await db.ref("rooms").get()).val() || {}));
  const idx = (await db.ref("users").get()).val() || {};
  const idxUpdates = {};
  for (const [uid, u] of Object.entries(idx)) {
    for (const code of Object.keys((u && u.rooms) || {})) {
      if (!live.has(code)) { idxUpdates[`${uid}/rooms/${code}`] = null; out.indexEntriesDropped++; }
    }
  }

  if (users) {
    const stale = [];
    let token;
    do {
      const page = await auth.listUsers(1000, token);
      for (const u of page.users) {
        const anonymous = (u.providerData || []).length === 0 && !u.email && !u.phoneNumber;
        const last = Date.parse(u.metadata.lastRefreshTime || u.metadata.lastSignInTime || u.metadata.creationTime) || 0;
        if (anonymous && last < cutoff) stale.push(u.uid); else out.usersKept++;
      }
      token = page.pageToken;
    } while (token);
    out.usersDeleted = stale.length;
    for (const uid of stale) { idxUpdates[uid] = null; }
    if (!dryRun) {
      for (let i = 0; i < stale.length; i += 1000) await auth.deleteUsers(stale.slice(i, i + 1000));
    }
  }

  if (!dryRun && Object.keys(idxUpdates).length) await db.ref("users").update(idxUpdates);
  log(`${dryRun ? "[dry run] would delete" : "deleted"}: ${out.roomsDeleted} rooms idle > ${days} days (kept ${out.roomsKept}), ` +
      `${out.indexEntriesDropped} stale index entries, ${out.usersDeleted} anonymous users inactive > ${days} days (kept ${out.usersKept})`);
  return out;
}

function arg(name, fallback) {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const emulated = !!process.env.FIREBASE_DATABASE_EMULATOR_HOST;
  const projectId = arg("--project", process.env.GCLOUD_PROJECT || (emulated ? "demo-mmele" : undefined));
  const databaseURL = arg("--database-url", emulated ? `http://${process.env.FIREBASE_DATABASE_EMULATOR_HOST}/?ns=${projectId}-default-rtdb` : undefined);
  if (!projectId || !databaseURL) {
    console.error("Usage: node purge.mjs --project ID --database-url URL [--days 30] [--dry-run] [--rooms-only|--users-only]");
    process.exit(2);
  }
  const app = initializeApp(emulated ? { projectId, databaseURL } : { credential: applicationDefault(), projectId, databaseURL });
  const days = Number(arg("--days", "30"));
  const only = process.argv.includes("--rooms-only") ? "rooms" : process.argv.includes("--users-only") ? "users" : null;
  purge({ db: getDatabase(app), auth: getAuth(app), days, dryRun: process.argv.includes("--dry-run"), rooms: only !== "users", users: only !== "rooms" })
    .then(() => process.exit(0))
    .catch((e) => { console.error(e.message || e); process.exit(1); });
}
