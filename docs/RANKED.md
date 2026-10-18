# Ranked play and tournaments (virtual points only)

Points are **virtual**. They have no cash value and cannot be bought, sold, transferred or cashed out. The
app says so on every screen that shows points (`R.string.points_disclaimer`). `REAL_MONEY_ENABLED=false`
(`BuildConfig` and `FeatureFlags`) and there is no payment code. See `REAL_MONEY.md`.

## Model
- Every player starts with rating **1200** and **1000 points** (`Elo.START_*`).
- Ranked match: fixed stake **10 points**; the winner takes up to 10 from the loser (never below 0). Draw: no
  points move. Elo K = 40 for a player's first 30 ranked games, then 24.
- Tournaments: single elimination, 4/8/16 players, entry **20 points**. Pot split 70 % / 30 % to winner / runner-up.
  A drawn tournament match is replayed in a fresh room. Leaving while the tournament is still open refunds the entry.

## How settlement works (server-authoritative)
Clients can never write `ratings/`, `ledger/`, `settled/`, room `invite`/`tournament` fields, or tournament
`paid/`/`bracket/` — the rules deny it (`firebase/database.rules.json`). Only Cloud Functions
(`firebase/functions/index.js`, admin SDK) change them:

| Trigger | What it does |
| --- | --- |
| `onRankedResult` (`results/{code}/{seat}`) / `onRankedFinish` (room status → finished) | decides the winner, claims `settled/{code}` in a transaction (exactly once), writes both ratings and a ledger line each, advances the tournament |
| `onTournamentEntry` | checks capacity and balance, takes the entry, writes `paid/` + ledger; the last payer builds the bracket and opens rooms (`invite` = the opponent) |
| `onTournamentLeave` | refunds an unpaid-out entry while open |

Winner decision (`decideWinner` in `ranked.js`): a resignation/timeout claim that the room rules already
validated decides alone; otherwise **both** players' reports must agree. Disagreement = `disputed`: recorded in
`settled/`, nothing moves.

## Honest limitations
- **Collusion:** two players who agree on a fake result (or one who resigns on purpose) can move points/rating
  between them. The server does not replay the moves. Mitigations if it matters: replay moves in the function
  with the engine (port to JS) and rate-limit ranked games between the same pair.
- A ranked **rematch in the same room** is not settled again (one settlement per room code).
- Host-blocked guests are refused by the rules but the app shows "room is full"; a guest blocking the host is
  refused client-side only.
- Cloud Functions need the **Blaze (pay-as-you-go) plan**. Without them ranked results are stored but never
  settled and tournaments never start. Usage at this scale is well inside the free tier quotas.

## Deploy
```bash
cd firebase
npx firebase login
npx firebase use <your-project-id>
npx firebase deploy --only database          # rules
(cd functions && npm ci) && npx firebase deploy --only functions   # needs Blaze
```
Give yourself the admin claim to read `reports/` (one-off, admin SDK):
`admin.auth().setCustomUserClaims('<your-uid>', { admin: true })`.

## Tests
`cd firebase && npm run test:rules` (needs JDK 21+): rules, chat, blocks, reports, ranked rules and a full
4-player tournament through the functions emulator. Kotlin logic: `ChatRankedTest.kt` in `:online`.
