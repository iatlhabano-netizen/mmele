# Mmele — Privacy Policy

> **DRAFT. Not legal advice.** Fill in every `[PLACEHOLDER]` before publishing. Host this at a public URL (see
> RELEASE_CHECKLIST.md) and paste that URL into Play Console *and* into the app's About screen.
> Keep it accurate: if the app changes (new SDK, analytics, ads, chat), update this page **before** releasing.

**Effective date:** [DATE]
**App:** Mmele (Android), package `[APPLICATION ID, e.g. com.tshipiworks.mmele]`
**Who we are (the "controller"):** [LEGAL NAME], trading as [Tshipiworks / DEVELOPER NAME ON GOOGLE PLAY],
[POSTAL ADDRESS], Gabane, Botswana. Contact: **[CONTACT EMAIL]**.

## 1. The short version

- Mmele is a free board game. There are **no ads, no in-app purchases, no analytics, no crash reporting and no
  sign-up**. We never ask for your name, email, phone number, contacts, photos or location.
- **Play vs Computer** and **Pass & Play** work fully offline. Nothing about those games leaves your phone.
- **Play online** uses Google Firebase to connect two phones. To do that, Firebase gives your app a random
  anonymous ID, stores the game room you play in, and (like any internet server) sees your IP address.
- We do not sell or share your data, and we do not use it for advertising or profiling.

## 2. What is stored only on your phone

| Data | Why | Where |
|---|---|---|
| Your last chosen mode and difficulty, sound/haptics settings | To remember your choices | App storage on your device |
| Games in progress | So you can continue playing | Device memory |

This data stays on your device. If you have turned on Android's own device backup, Android may include the app's
settings in your Google backup; that is controlled by you in Android settings and covered by Google's privacy
policy. Uninstalling the app deletes this data.

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

## 4. Service provider (data processor)

Online play is hosted on **Google Firebase** (Firebase Authentication and Firebase Realtime Database), provided by
Google LLC / Google Ireland Limited, which processes this data on our behalf under the
[Firebase Data Processing and Security Terms](https://firebase.google.com/terms/data-processing-terms).
Game data is stored in the **[DATABASE REGION, e.g. europe-west1 (Belgium)]** region. Google may process data in
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
- <a id="delete"></a>**Delete your online data in the app:** Home → *About · privacy · licences* → **Delete my online
  data**. This deletes every room you created or joined from that phone and then your anonymous ID, straight away.
- Or email **[CONTACT EMAIL]** with the subject "Mmele data deletion" and, if you can, the room code(s) you used.
  Because we hold no name or email, we may ask for the room code so we can find the data. We will reply within
  **30 days**.
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
for people aged **13 and over** [ADJUST to the target age you choose in Play Console]. Online play collects no
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

[LEGAL NAME], [POSTAL ADDRESS], Gabane, Botswana. Email: **[CONTACT EMAIL]**.
