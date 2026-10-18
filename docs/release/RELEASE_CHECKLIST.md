# Mmele: Google Play release checklist

Ordered. **[BOT]** = the agent can do it on the box (code/docs/builds, local commits only). **[YOU]** = only you
can do it (accounts, identity, money, legal choices, public hosting, people). Nothing here is pushed or published
by the bot.

## Phase 0: decisions (YOU, ~1 hour, before any code changes)
1. **[YOU] Final application ID.** Recommended: `com.tshipiworks.mmele` (a domain you own). `bw.mmele` is
   technically allowed (Android doesn't check domain ownership), but reverse-DNS of a domain you don't control is
   bad practice. **It can never change after the first upload**, and Android developer verification ties it to
   your identity.
2. **[YOU] Personal or organisation account.** Personal: ID verification, then the **12 testers × 14 days** closed
   test before production. Organisation (Tshipiworks): needs a **D-U-N-S number** (free, up to 30 days) and shows
   your business address/phone publicly, but skips the 12-tester rule. For a hobby app, personal is simpler.
3. **[YOU] Ship v1 with or without online play?** Without: no data collected, the simplest Data safety form, and no
   Firebase costs. But you **must hide the Online mode** in that build, or reviewers will see a "not configured"
   screen. With: you create a Firebase project (Spark free plan) in europe-west1.
4. **[YOU] Target age:** recommended 13+ (13–15, 16–17, 18+). See the audit for why not "children".
5. **[YOU] Contact email** to publish (e.g. mmele@tshipiworks.com) and a postal address for the privacy policy.

## Phase 1: app changes (BOT, after you approve)

> **Status 2026-10-09: done (commits a5feed2…), except on-device testing.** targetSdk/compileSdk 36; app ID
> `com.tshipiworks.mmele` 1.0.0 (1); upload key `~/mmele-keys/mmele-upload.jks` (SHA-256
> `F4:8F:43:11:19:4B:32:33:8C:3D:01:F1:6E:C5:1A:8B:DC:92:94:7E:AF:BA:16:8B:53:81:BA:5D:AD:79:39:17`); signed R8
> AAB/APK at `/workspace/mmele-release-1.0.0.{aab,apk}` (built **without** `google-services.json`, so online is
> off in these two files: rebuild after Firebase setup); About screen; delete-my-data + 30-day rules + purge script;
> leave-game dialog, TalkBack board, contrast, no backup; store assets in `/workspace/mmele-store/`.
> **Not verified:** running the release APK on a device/emulator (the box's emulator never started its VM), and
> the Firebase path against a real project. Item 12 is moot (online ships). Item 15 waits for your details.
> **Before upload:** set the contact email and postal address with `site/set-contact.sh`, deploy `site/` to
> Vercel (see `site/README.md`), then run `site/set-domain.sh <domain>` and rebuild the AAB.
6. **[BOT]** Raise `compileSdk`/`targetSdk` to **36**. Since 31 Aug 2026 this is **required for new apps**.
   Needs an AGP/Kotlin bump. Then re-test edge-to-edge and predictive back.
7. **[BOT]** Change `applicationId` (and namespace) to your choice; set `versionCode = 1`/`versionName = "1.0.0"`.
8. **[BOT]** Release signing: generate an **upload key** keystore on the box, wire `signingConfigs.release`
   (passwords read from `~/.gradle/gradle.properties`, never committed), build a signed **AAB**
   (`bundleRelease`). **[YOU]** keep an offline backup of the keystore and passwords (e.g. on a USB stick plus a
   password manager). Play App Signing holds the real app key, and a lost upload key can be reset via Play support.
9. **[BOT]** Test the **R8-minified release build** on the emulator (never tested yet). Add keep rules if Firebase
   needs them.
10. **[BOT]** Add an **About** screen: version, privacy policy link, terms link, contact email, *Open-source
    licences* (Google `oss-licenses-plugin` or AboutLibraries), credits/sources for scene facts and egg
    descriptions.
11. **[BOT]** If online play ships: a "Delete my online data" action (delete own rooms + `currentUser.delete()`), and
    room clean-up. Options: client deletes finished rooms; or a security rule allowing deletion of rooms older than N
    days, plus a small cleanup script you run monthly; or a Cloud Function (needs the paid Blaze plan).
12. **[BOT]** If online play doesn't ship: hide the "Play online" mode when `FIREBASE_CONFIGURED=false`.
13. **[BOT]** Quick quality fixes: confirm before leaving a game in progress with Back/Home; TalkBack labels for
    board points and buttons; check BoneDim-on-slate text contrast (4.5:1); turn `allowBackup` off or add
    `dataExtractionRules`.
14. **[BOT]** Store assets: 512×512 icon PNG, 1024×500 feature graphic, 4–8 phone screenshots at 1080×1920 rendered
    from Paparazzi.
15. **[BOT]** Fill in placeholders in `PRIVACY_POLICY.md`/`TERMS.md` once you give the details, and export them as
    simple HTML pages.

## Phase 1c: version 1.2.0 (BOT, 18 Oct 2026)

> `versionCode 3` / `versionName 1.2.0`, signed AAB/APK at `/workspace/mmele-release-1.2.0.{aab,apk}` (+ mapping).
- [x] 670+ Botswana facts (`res/raw/botswana_facts.json`, English only) on the game-over card, never repeating
      until all are shown (persisted shuffled cycle in DataStore).
- [x] 57 free-licence Wikimedia Commons photos on the game-over card; credits in About and `docs/PHOTO_CREDITS.md`.
- [x] New game: Moruba (4-row mancala), vs Computer and Pass & Play.
- [ ] **[YOU]** Review Moruba strings in `docs/SETSWANA_REVIEW.md`; spot-check facts and photo captions.

## Phase 1b: version 1.1.0 (BOT, done 10 Oct 2026)

> `versionCode 2` / `versionName 1.1.0`, signed AAB/APK at `/workspace/mmele-release-1.1.0.{aab,apk}`, pushed
> to `github.com/iatlhabano-netizen/mmele` (main). Built without `google-services.json` (online off).

- [x] Academy: 5 lessons, each with a practice position, unlocking in order; first-run How to play.
- [x] Saved games (Continue game), process-death safe; clock-aware undo.
- [x] English + Setswana draft with in-app language picker. **[YOU]** review `docs/SETSWANA_REVIEW.md`.
- [x] Stats with win streaks; hint button; replay viewer; illegal-move explanations.
- [x] AI: node budgets (same strength on every phone), Expert level, solved Khutlotharo, TT draw-counter fix.
- [x] Review fixes: monotonic clock paused in background; no `runBlocking` at start-up (SplashScreen);
      online idle claim (5 min) in the rules with emulator tests.
- [ ] **[YOU]** Try 1.1.0 on a real phone (the box emulator still can't boot), especially TalkBack and Setswana.

## Phase 2: accounts and hosting (YOU)
16. **[YOU]** Create the Play Console developer account (play.google.com/console), pay the **US$25** one-time fee,
    and verify identity (government ID; for organisations, the D-U-N-S number). Verify your email and phone.
17. **[YOU]** If online: create the Firebase project, choose Realtime Database location **europe-west1**, enable
    Anonymous Auth, deploy `firebase/database.rules.json`, download `google-services.json` into `app/`, and accept the
    Firebase Data Processing and Security Terms in the console (Project settings → Privacy). See
    `docs/ONLINE_SETUP.md`.
18. **[YOU] Host the privacy policy at a public URL** (it must be a live, non-PDF, non-editable web page). Options:
    - a page on **www.tshipiworks.com/mmele-privacy** (best: matches your developer identity);
    - **GitHub Pages** (free; needs a public repo with only the HTML, separate from the app code);
    - **Google Sites** (free, no code).
    Put the deletion section at an anchor (`#delete`) for the Data safety deletion link.

## Phase 3: Play Console setup (YOU, with drafts from BOT)
19. **[YOU]** Create the app (name, default language English (UK/US), Game, Free). Note that **free can never become
    paid**.
20. **[YOU]** App content: privacy policy URL, Ads = No, App access, Content rating questionnaire (IARC), Target
    audience 13+, Data safety (paste from `DATA_SAFETY_ANSWERS.md`), News = No, Government = No, Financial = none,
    Health = none.
21. **[YOU]** Store listing: paste from `PLAY_LISTING.md` and upload the assets.
22. **[YOU]** Register the package name for Android developer verification if Play prompts you (Play Console →
    Android developer verification).

## Phase 4: testing (YOU)
23. **[YOU]** Upload the AAB to **Internal testing** first (instant, up to 100 testers). Check the **pre-launch
    report** for crashes and accessibility warnings. **[BOT]** fixes anything it finds.
24. **[YOU] Closed test:** recruit **at least 12 real testers** (they need Google accounts and must **opt in and stay
    opted in for 14 consecutive days**). Good sources: Gabane/Gaborone friends, family, Tshipiworks customers, a
    local board-game or Morabaraba club, university CS students. Ask them to actually play (Google asks about
    engagement). **Don't use paid "tester farms" or fake accounts**; that's a known way to get the account banned.
25. **[BOT]** Ship fixes from tester feedback as new versionCodes to the closed track.

## Phase 5: production (YOU)
26. **[YOU]** Apply for production access (Dashboard). Answer the questionnaire honestly about the test, feedback
    and changes.
27. **[YOU]** Choose countries (start with Botswana, South Africa, Namibia, Zimbabwe, plus anywhere else). Use a
    staged rollout (e.g. 20%) and publish.
28. **[YOU]** Afterwards: reply to reviews yourself, never buy or incentivise reviews, keep the contact email working,
    and re-check Data safety after every SDK change. Next target-API deadline: expect API 37 around Aug 2027.
