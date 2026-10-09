# Online play: setup guide

Mmele works fully **offline** (Pass & Play, Solo vs AI). Online play is optional and needs a free
Firebase project that **you** own. Until you add one, the "Play online" screen says
*"Online play not configured yet"* and nothing crashes.

This takes about 15 minutes. You don't need a credit card: the free **Spark** plan covers it.

---

## 1. Create a Firebase project

1. Go to https://console.firebase.google.com and sign in with a Google account.
2. Click **Create a project** (or **Add project**). Name it, for example `mmele`.
3. Google Analytics isn't needed, so you can switch it off. Click **Create project**.

## 2. Add the Android app

1. On the project overview page, click the **Android** icon ("Add app").
2. **Android package name**: `com.tshipiworks.mmele`. This must match exactly (it is the app's permanent Play ID).
3. App nickname: `Mmele`. The SHA-1 field is optional: Anonymous sign-in and Realtime Database don't need it.
   If you add it anyway (or later add Google sign-in / App Check), register **both** certificates:
   - upload key (this box): SHA-1 `54:2D:D7:52:09:E6:B3:B8:A3:A5:47:31:C2:AC:43:C0:EE:B4:8C:AB`,
     SHA-256 `F4:8F:43:11:19:4B:32:33:8C:3D:01:F1:6E:C5:1A:8B:DC:92:94:7E:AF:BA:16:8B:53:81:BA:5D:AD:79:39:17`
   - Play's **app signing key**: copy it from Play Console → *Test and release → App integrity* after the first upload.
     Phones that install from Play see that key, not the upload key.
4. Click **Register app**, then **Download google-services.json**.
5. Put that file at **`mmele/app/google-services.json`**, next to `app/build.gradle.kts`.
   - It identifies your project. It is not a password, but don't post it publicly.
     `.gitignore` already excludes it.
6. Skip the remaining "add SDK" steps in the console. The project is already wired for it.

## 3. Turn on Anonymous sign-in

1. In the left menu: **Build → Authentication → Get started**.
2. Open the **Sign-in method** tab, choose **Anonymous**, switch it **on**, and **Save**.

Players never see a login screen. Each phone gets an invisible anonymous id, which the security
rules use to know who is the host and who is the guest.

## 4. Create the Realtime Database

1. Left menu: **Build → Realtime Database → Create database**.
2. Pick a location close to your players. For Botswana, **europe-west1 (Belgium)** is usually the
   lowest-latency option on the list.
3. Choose **Start in locked mode** and click **Enable**.

> Why Realtime Database and not Firestore? RTDB has built-in *presence* (`onDisconnect()`), which
> powers the "Opponent disconnected" indicator and the abandon timer without any server code.
> It's also faster for many tiny writes, and the free tier is plenty for a board game.

## 5. Publish the security rules

1. In **Realtime Database**, open the **Rules** tab.
2. Delete what's there. Paste the full contents of **`mmele/firebase/database.rules.json`**.
3. Click **Publish**.

Or, with the Firebase CLI (`npm i -g firebase-tools`): run
`cd firebase && firebase login && firebase use --add` (pick your project), then
`firebase deploy --only database`.

What the rules enforce:

- You must be signed in (anonymously) to read or write anything.
- A room can only be **created** once, by its host, as an empty lobby with a valid 6-character code.
- A room can be **deleted** by its host or guest at any time, and by any signed-in player once it has been idle
  (`updatedAt`) for more than **30 days**. The host's app deletes a finished room when the host leaves it.
- `users/{uid}/rooms/{code}` is a private index of your own rooms (only you can read/write it). The in-app
  **About → Delete my online data** uses it to delete your rooms, then deletes your anonymous user.
- Only one guest can join, and only while the room is in the lobby.
- Moves are **append-only**, at exactly the next index, only while playing, and only for **your
  own seat**. Turn order is checked too: no two placements or moves in a row, and a shot must
  directly follow your own move.
- You can only change your own presence, offers and resignation. You can claim an
  **abandoned** game only after the opponent has been away 90 seconds, checked against server time.
- Moves can be wiped (for a rematch) only after the game ended and the opponent asked for one.

What the rules **cannot** check: whether a move is legal Morabaraba (for example, whether a
mill really formed before a shot). That is checked by both phones, which replay every move
through the shared `:engine` rules. If one phone sends an illegal move, the other stops the game
with "The other phone sent a move the rules don't allow". Full server-side legality would need
Cloud Functions, which requires the paid Blaze plan. See *Known limits* below.

## 6. Build and run

```bash
cd mmele
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The build turns on the Google Services plugin automatically when `app/google-services.json`
exists. Without the file, the build still succeeds and online play is simply hidden behind the
"not configured" message.

## 7. Play

1. Phone A: **Play online → Create room**. A 6-character code appears, like `K7M 3PQ`.
   Tap **Share code** to send it by WhatsApp, SMS and so on.
2. Phone B: **Play online**, type the code, **Join game**.
3. The host plays the darker set and moves first. Variant and pieces follow the host's choices,
   and each phone keeps its own scene.

During a game:

- The green or red dot at the top shows whether your opponent is connected.
- **Offer draw**: if your opponent also offers (they'll see "Accept"), the game is drawn. Making a
  move withdraws a pending offer.
- **Resign** asks you to tap twice to confirm.
- If your opponent disappears, a 90-second countdown starts. After it ends you can **Claim win**.
- After a game, **Rematch** starts a new game in the same room once both players tap it.
- If your app restarts mid-game, rejoining with the same code puts you back in your seat, as long
  as Firebase still remembers your anonymous id on that phone.

## Testing locally without a Firebase project

The security rules ship with emulator tests. You need Node 18+ and Java 21+:

```bash
cd firebase
npm install                              # installs the firebase JS SDK used by the tests
npx firebase-tools emulators:exec --only database,auth --project demo-mmele \
  "node rules.test.mjs && node payloads.test.mjs"
```

- `rules.test.mjs` runs 40 allow/deny checks: cheating writes, turn order, abandon timing,
  rematch, and so on.
- `payloads.test.mjs` replays the **exact** writes the Android client makes. They are exported
  by the JVM test `PayloadFixtureTest` to `fixtures/kotlin-payloads.json`, and the test confirms
  the rules accept every one.

The pure-Kotlin tests (`./gradlew :online:test :app:testDebugUnitTest`) also run two complete
clients against an in-memory room store, with no network at all.

## Known limits

- **No server-side move legality** (see above). The rules make cheating awkward and detectable,
  not impossible.
- **No chess clock online yet.** Online games are untimed. A fair clock needs server timestamps
  per move, which is planned.
- Anonymous ids belong to one install. Clearing app data or reinstalling means you can't
  rejoin a game in progress as the same seat.
- Rooms are not deleted automatically. For housekeeping, you can delete old `rooms/*` in the
  console, or add a scheduled Cloud Function later (Blaze plan).

## Monthly clean-up (no Cloud Functions, free plan)

Rooms whose players never came back, and anonymous users nobody uses any more, are removed by
`firebase/purge.mjs`. Run it about **once a month** from any computer with Node 18+:

1. Firebase console → ⚙ **Project settings → Service accounts → Generate new private key**. Save the JSON
   **outside** the repo, e.g. `~/mmele-keys/firebase-admin.json`. It is a full-admin credential: never commit,
   email or upload it.
2. ```
   cd mmele/firebase && npm install
   export GOOGLE_APPLICATION_CREDENTIALS=~/mmele-keys/firebase-admin.json
   node purge.mjs --project YOUR_PROJECT_ID --database-url https://YOUR-DB.europe-west1.firebasedatabase.app --dry-run
   node purge.mjs --project YOUR_PROJECT_ID --database-url https://YOUR-DB.europe-west1.firebasedatabase.app
   ```
   It deletes rooms idle > 30 days, index entries pointing at deleted rooms, and **anonymous** users inactive
   > 30 days (users with an email/phone/provider are never touched). `--days N` changes the cut-off.

The privacy policy promises: finished rooms deleted when the host leaves; everything else within **60 days** of
last activity (30 days idle + up to a month until the next run). If you stop running the script, change the policy.

Tests: `cd firebase && npm run test:rules` runs the rule tests, the Kotlin-payload replay and the purge test
against the emulator (needs JDK 21 and firebase-tools).

