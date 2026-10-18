# Mmele — Privacy Policy

> **Source for the website page `site/privacy.html`** (regenerate with `python3 site/build_pages.py`; lines
> starting with `>` and `[NOTE TO DEVELOPER …]` items are left out of the web page). Not legal advice.
> `CONTACT_EMAIL_PLACEHOLDER` / `POSTAL_ADDRESS_PLACEHOLDER` are filled by `site/set-contact.sh`.
> Keep it accurate: if the app changes (new SDK, analytics, ads, chat), update this page **before** releasing.

**Effective date:** 10 October 2026
**App:** Mmele (Android), package `com.tshipiworks.mmele`
**Who we are (the "controller"):** Artful Tenant, trading as Tshipiworks,
POSTAL_ADDRESS_PLACEHOLDER, Gabane, Botswana. Contact: **CONTACT_EMAIL_PLACEHOLDER**.

## 1. The short version

- Mmele is a free board game. There are **no ads, no in-app purchases and no sign-up**. Ranked play uses **virtual
  points only**: they have no cash value and cannot be bought, sold or cashed out. We never ask for your name, email, phone number, contacts, photos or location.
- **Play vs Computer** and **Pass & Play** work fully offline. Nothing about those games leaves your phone.
- **Play online** uses Google Firebase to connect two phones. To do that, Firebase gives your app a random
  anonymous ID, stores the game room you play in, and (like any internet server) sees your IP address.
- **Usage and crash data** (Firebase Analytics and Crashlytics) is sent only while "Share usage & crash data" is
  on (home screen). Events are anonymous; the advertising ID is not collected.
- **Chat** in online games is visible only to the two players and is deleted with the room. Mute, block and
  report are available; a report keeps a copy of the reported message for moderation (up to 90 days).
- We do not sell or share your data, and we do not use it for advertising or profiling.

## 2. What is stored only on your phone

| Data | Why | Where |
|---|---|---|
| Your last chosen mode and difficulty, sound/haptics settings | To remember your choices | App storage on your device |
| Games in progress | So you can continue playing | Device memory |

This data stays on your device. The app opts out of Android cloud backup and device-to-device transfer, so these
settings are not copied to Google's servers or to a new phone. Uninstalling the app deletes this data.

## 3. What is processed when you use "Play online"

Online play only happens if you choose it. When you create or join a room:

| Data | Details | Purpose | Legal basis (EU/UK, Botswana) |
|---|---|---|---|
| Anonymous user ID | A random identifier created by Firebase Authentication ("anonymous sign-in"). It is not linked to your name, email, phone number or Google account. | To know which player is which in a room and to stop other people making moves for you | Performance of the service you asked for (contract); legitimate interest in security |
| Game room data | The 6-character room code, the board variant, the piece sets chosen, the moves played, whose turn it is, connection status (online/offline), resign/draw/rematch offers, and timestamps | To run the game between the two players | Performance of the service (contract) |
| IP address and technical data | Your IP address and "user agent" information (Android version, device model, Firebase SDK versions), which Firebase receives automatically with every connection | To deliver the connection, protect the service from abuse, and for Google to maintain Firebase | Legitimate interest in running a secure service |

We do **not** use your IP address to work out your location.

**Who can see room data:** the other player in your room, and anyone else who uses the app and enters the same room
code while it exists (there is no public list of rooms). Rooms contain no names or messages — there is no chat.

## 3a. Chat, ranked play and tournaments

- Chat messages (quick phrases, emoji, or up to 120 characters of filtered text) live under the room, readable only
  by its two players, and are deleted with the room (within 24 hours). Chat can be switched off.
- Blocks are stored under your anonymous ID. Reports (room code, both anonymous IDs, the message) are admin-only and
  kept up to 90 days.
- Ranked play stores rating, virtual points, games played and a points ledger under your anonymous ID. Points have
  no cash value.

## 3b. Usage and crash data

With the toggle on, Firebase Analytics receives anonymous events (match started/completed with game, mode and result;
tutorial steps; online room created/joined; chat sent — type only, never text; ranked match; tournament joined; fact
shown) and Crashlytics receives crash reports. No advertising ID; ad personalisation disabled. Analytics retention
2 months, crash data 90 days.

## 4. Service provider (data processor)

Online play is hosted on **Google Firebase** (Firebase Authentication and Firebase Realtime Database), provided by
Google LLC / Google Ireland Limited, which processes this data on our behalf under the
[Firebase Data Processing and Security Terms](https://firebase.google.com/terms/data-processing-terms).
Game data is stored in the **europe-west1 (Belgium)** region [NOTE TO DEVELOPER: confirm this matches the region you pick when creating the database]. Google may process data in
other countries where it or its sub-processors operate, using the safeguards in those terms (including the EU
Standard Contractual Clauses). See also [Privacy and Security in Firebase](https://firebase.google.com/support/privacy).

Google Play itself (downloads, updates, reviews) is run by Google under
[Google's Privacy Policy](https://policies.google.com/privacy); we receive only aggregated install statistics.

## 5. How long we keep it

- Finished game rooms: deleted by the host's app as soon as the host leaves the finished game.
- Unfinished or abandoned rooms: kept so players can reconnect, then deleted within **60 days** of the last
  activity (rooms idle for 30 days are removed by a clean-up we run at least monthly).
- Anonymous user IDs: deleted within **60 days** of last use (same monthly clean-up), or immediately when you use
  "Delete my online data".
- [NOTE TO DEVELOPER: these periods depend on you running `firebase/purge.mjs` monthly (docs/ONLINE_SETUP.md).
  Do not publish them if you won't.]
- Firebase/Google technical logs: kept by Google for its standard periods under the terms above.

## 6. Your choices and rights

- You can use Mmele without ever going online. Simply don't choose "Play online".
- Uninstalling the app (or clearing its storage) removes the anonymous ID from your phone; your old rooms can no
  longer be linked to you.

### Delete your online data {#delete}

- **In the app:** Home → *About · privacy · licences* → **Delete my online data**. This deletes every room you
  created or joined from that phone and then your anonymous ID, straight away.
- **By email:** write to **CONTACT_EMAIL_PLACEHOLDER** with the subject "Mmele data deletion" and, if you can, the room
  code(s) you used. Because we hold no name or email, we may ask for the room code so we can find the data. We will
  reply within **30 days**.
- If you never used "Play online", there is no online data to delete.

### Your rights

- Depending on where you live, you may have rights to access, correct, delete, restrict or object to processing of
  your data, and to data portability:
  - **EU / EEA and UK (GDPR / UK GDPR):** you can also complain to your local data protection authority (in the UK,
    the Information Commissioner's Office, ico.org.uk).
  - **Botswana (Data Protection Act, 2024):** you can complain to the Information and Data Protection Commission
    (IDPC), Botswana.
  - **United States:** we do not sell or "share" personal information for cross-context behavioural advertising,
    and we do not use it for targeted ads or profiling.
  - **South Africa (POPIA):** you may contact the Information Regulator of South Africa.

## 7. Children

Mmele is a family-friendly board game, but it is **not directed at children under 13**, and online play is meant
for people aged **13 and over**. Online play collects no
names, no chat and no contact details, only the anonymous technical data above. If you believe a child under 13 has
used online play, contact us and we will delete the related data.

## 8. Security

Connections to Firebase are encrypted (HTTPS/TLS). Database security rules only let the two players in a room
change that room, and only in the ways the game allows. No system is perfectly secure, but we keep the data we
hold to the minimum needed to play.

## 9. Changes

If we change how the app uses data (for example, adding a new feature), we will update this page and change the
effective date before releasing that version. Significant changes will also be noted in the app's release notes.

## 10. Contact

Artful Tenant, trading as Tshipiworks, POSTAL_ADDRESS_PLACEHOLDER, Gabane, Botswana.
Email: **CONTACT_EMAIL_PLACEHOLDER**.
