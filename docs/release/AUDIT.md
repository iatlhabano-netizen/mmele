> **SUPERSEDED (10 Oct 2026).** This is the original 9 Oct audit, kept for the record. Current status lives in
> [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md). Fixed since: targetSdk/compileSdk 36 (blocker 1); signed AAB +
> Play App Signing-ready upload key and R8 release build (2); privacy policy and terms written, site in `site/`
> ready for Vercel (3, hosting still yours); online play has a friendly "coming soon" state and full
> delete-my-data / 30-day clean-up (4); application ID `com.tshipiworks.mmele` (5); store assets in
> `/workspace/mmele-store/` (7); About screen with licences and sources; TalkBack board, contrast, leave-game
> dialog, backup off. In 1.1.0: monotonic, background-paused clocks; async start-up with SplashScreen; online
> idle claim enforced by rules; transposition-table draw-counter fix; solved Khutlotharo; Academy, stats,
> replay, hints, saved games; English + draft Setswana. Still open: blocker 6 (12 testers × 14 days) and
> everything account-side.

# Mmele: Play release-readiness and legal audit (9 Oct 2026)

> Research summary, **not legal advice**. Checked against commit `86805b6` and official sources on 9 Oct 2026.
> ⚠ = could not fully verify.

## Verdict: not ready yet. Top blockers
1. **targetSdk 35 → must be 36.** Since 31 Aug 2026, new apps must target Android 16 (API 36)
   ([Play help](https://support.google.com/googleplay/android-developer/answer/11926878),
   [Android docs](https://developer.android.com/google/play/requirements/target-sdk)). Needs an AGP upgrade
   (8.7.3 tops out at API 35; ⚠ exact minimum AGP for 36 to confirm when upgrading, likely ≥ 8.9).
2. **No release signing / AAB.** New apps must be uploaded as an Android App Bundle and use Play App Signing
   ([upload](https://developer.android.com/studio/publish/upload-bundle),
   [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)).
   The R8 release build has never been run or tested.
3. **No privacy policy URL** (required for every app, and especially with Firebase).
4. **Online play shows "not configured"** without a Firebase project. Either configure it or hide the mode, or
   reviewers see broken functionality.
5. **Application ID `bw.mmele` is permanent once uploaded.** Decide now (recommend `com.tshipiworks.mmele`).
   Package names must be registered for Android developer verification; unregistered Play apps are removed after
   30 Sep 2026 ([source](https://support.google.com/googleplay/android-developer/answer/16984799)).
6. **Account path:** personal accounts created after 13 Nov 2023 need a closed test with **≥12 testers opted in for
   14 consecutive days** before production ([source](https://support.google.com/googleplay/android-developer/answer/14151465)).
   This is the longest lead-time item.
7. **No store assets** (512 px icon, 1024×500 feature graphic, ≥2 screenshots in a valid aspect ratio; the
   current Paparazzi PNGs are 1:2.22, above the 1:2 limit).

## 1. App audit (what the code really does)

| Item | Finding |
|---|---|
| `versionCode`/`versionName` | 2 / 0.2.0. Fine, but reset to 1 / 1.0.0 for the first public release (any value ≥1 works) |
| `minSdk` | 26 (Android 8). Fine |
| Minify | `isMinifyEnabled = true` for release, default rules only. **Untested.** Firebase ships its own consumer rules, so it should be fine; confirm by testing |
| Permissions (merged manifest) | `INTERNET`, `ACCESS_NETWORK_STATE` (Firebase), `VIBRATE`, `com.google.android.providers.gsf.permission.READ_GSERVICES` (Play services), `bw.mmele.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (AndroidX, internal). All are **normal/install-time**: no runtime prompts and no Play "sensitive permission" declarations needed. No `AD_ID`, location, contacts, camera, mic or storage |
| SDKs | `firebase-auth` 23.1.0, `firebase-database` 21.0.0 (BoM 33.7.0), AndroidX, DataStore 1.1.1. **No Analytics, Crashlytics, Installations, Messaging or Ads** (checked `releaseRuntimeClasspath`). firebase-auth transitively adds play-services-auth, Credential Manager, reCAPTCHA and Play Integrity. These are only used by phone/Google sign-in, which the app doesn't call |
| Data leaving the device | Offline modes: **none**. Online: anonymous Firebase UID, room data (code, UIDs, variant, piece ids, moves, phase, connected flags, timestamps, offers), plus IP/user agent seen by Firebase. Without `google-services.json`: nothing (FirebaseApp never initialises) |
| Retention | **Rooms are never deleted** and anonymous users are never cleaned up. Fix before publishing a retention period |
| Room privacy | Rules allow any signed-in user to read a room **only if they know its code**: no listing, no names, no chat. Codes are 6 characters from a 31-character alphabet (~887 million combinations) |
| `allowBackup="true"` | Settings (and possibly the Firebase auth token) can be restored on a new device. Harmless, but consider `dataExtractionRules` or false |
| Back handling | Back in a game returns to Home **immediately, losing the game** (no confirm). Online Back leaves the lobby. Worth a confirm dialog |
| Accessibility | The board is a single Canvas with one `contentDescription`, so **TalkBack users cannot play**. The pre-launch report will flag small/unlabelled targets. Add per-point semantics |
| Contrast | Gold/Bone on Slate look fine. `BoneDim` subtitles on dark/translucent panels over scenes may fall below 4.5:1 ⚠ (measure) |
| Android 16 behaviour at target 36 | Edge-to-edge is enforced (the app already calls `enableEdgeToEdge`). Predictive back is on by default (Compose `BackHandler` is compatible). Large-screen orientation/resizability restrictions are ignored. Test on a tablet emulator |

### Play-specific items
- **Data safety:** see `DATA_SAFETY_ANSWERS.md`. Declare **Device or other IDs** and **App interactions** as collected,
  not shared, optional, encrypted in transit. Not Location (IP isn't used to infer location).
  Basis: [Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469),
  [Firebase disclosure](https://firebase.google.com/docs/android/play-data-disclosure).
- **Content rating (IARC):** expect **Everyone / PEGI 3 / IARC 3+**. There's no violence (shooting a cow is just
  removing a piece; describe it as "capture"), no gambling and no chat. ⚠ Only the real questionnaire decides
  ([Play help](https://support.google.com/googleplay/android-developer/answer/9859655)).
- **Target audience:** recommend **13+**. If you include under-13 groups, the **Families Policy** applies: stricter
  review, a policy commitment in Data safety, any SDK must be Families-compliant, and online play with unknown people
  is scrutinised ([Families](https://support.google.com/googleplay/android-developer/answer/9893335),
  [target audience](https://support.google.com/googleplay/android-developer/answer/9867159)). Choosing 13+ avoids
  this, and also keeps you clearly outside COPPA's "directed to children" test. Don't use child-oriented marketing
  (cartoons, "for kids").
- **Account deletion:** Play's definition of an "app account" is a user-facing identity with verification. An
  invisible anonymous UID arguably isn't one, so the requirement likely **doesn't apply** ⚠ (judgement call).
  Provide a web deletion request path anyway (cheap), and ideally an in-app "Delete my online data"
  ([source](https://support.google.com/googleplay/android-developer/answer/13327111)).
- **App access:** no login. Give reviewers the two-device room-code steps (in `PLAY_LISTING.md`).
- **Store assets:** icon 512×512 32-bit PNG ≤1 MB; feature graphic 1024×500 JPEG/24-bit PNG; 2–8 screenshots,
  320–3840 px, aspect ≤ 2:1 ([assets](https://support.google.com/googleplay/android-developer/answer/9866151),
  [icon](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)).
- **Developer account:** US$25 one-time; identity verification for all; organisations need a D-U-N-S number (free,
  up to 30 days). Personal accounts show your legal name, country and developer email publicly, and your full
  address only if you monetise. Organisations show their legal address and phone
  ([required info](https://support.google.com/googleplay/android-developer/answer/13628312),
  [get started](https://support.google.com/googleplay/android-developer/answer/6112435),
  [identity](https://support.google.com/googleplay/android-developer/answer/10841920)).

## 2. Legal picture by jurisdiction (plain words)

**What actually gets indie developers into trouble** (in order of real-world frequency): (1) **Play enforcement**,
not lawsuits: a Data safety form that doesn't match the SDKs, a missing or broken privacy policy, metadata
violations (keyword stuffing, other apps' names, "#1"), fake or incentivised reviews, tester farms; (2) **copying**
someone's art, sounds, fonts, music or brand/logo; (3) **kids' data** with ads or analytics SDKs (the big US FTC
COPPA fines are almost all ad-SDK cases); (4) selling or sharing data with ad networks without consent. Mmele does
none of 2–4. Its main risk is (1), and the drafts address it.

| Jurisdiction | Applies? | What to do | Realistic risk |
|---|---|---|---|
| **EU/EEA GDPR** | Yes, if offered there and online play is used: the UID and IP are personal data (online identifiers). Offline modes process nothing | Privacy policy (drafted). Lawful basis: **contract** (the game you asked for) plus **legitimate interest** (security). Firebase = Google as **processor** under the [Data Processing and Security Terms](https://firebase.google.com/terms/data-processing-terms). Storing in **europe-west1** keeps data at rest in the EU (Google may still process elsewhere under SCCs, [locations](https://firebase.google.com/docs/database/locations)). An **Art. 27 EU representative** is normally required for non-EU controllers, unless processing is occasional and low-risk. ⚠ A pseudonymous game with no profiling arguably fits the exemption, but this is debatable ([Art. 27](https://www.legislation.gov.uk/eur/2016/679/article/27/adopted)) | **Low.** Regulators prioritise large and sensitive processing. Worst plausible case: a complaint letter that you answer by deleting data |
| **EU Digital Services Act (trader status)** | Traders must show name, address, phone and email to EU users. ⚠ I couldn't find a current official Play page with a separate trader form. Play's account pages say non-monetised personal accounts show legal name, country and email, and the full address only when monetising | If Play Console asks you to declare trader status: a free, non-monetised hobby app is arguably **non-trader**. If you publish under the Tshipiworks business identity, you're more likely a trader: then use a business address/PO Box and a dedicated phone/email | Low. It's a Play gating issue (EU distribution), not a lawsuit risk |
| **UK GDPR + Children's Code** | Same as the EU. The ICO Age Appropriate Design Code covers services "likely to be accessed" by under-18s, which a board game is | Already aligned: high-privacy defaults, no profiling, no location, no nudging, no chat. Mention UK rights (done) | Low |
| **US COPPA** | Only if the app is **directed to under-13s** or you have actual knowledge of under-13 users. The amended Rule has been in force since 22 Apr 2026 ([FR](https://www.federalregister.gov/documents/2025/04/22/2025-05904/childrens-online-privacy-protection-rule)). Persistent identifiers used only for "internal operations" (e.g. running the game, security) can be collected without parental consent, with notice | Target 13+, avoid kid-marketing, delete on request. If you later target kids: offline only, or a neutral age gate plus no online for under-13s | Low with a 13+ target and no ads |
| **US CCPA/CPRA and other states** | **No.** Thresholds: >US$26.625M revenue, ≥100,000 consumers' data bought/sold/shared, or ≥50% of revenue from selling data ([CPPA](https://cppa.ca.gov/regulations/cpi_adjustment.html)). Other state laws have similar 100k-consumer thresholds | Nothing beyond the policy's "we don't sell/share" statement | Negligible |
| **Botswana DPA 2024** (Act 18 of 2024, **in force since 14 Jan 2025**, [S.I. 4/2025](https://botswanalaws.com/bulletin/principal-legislation/bulletin-2024/act-18-of-2024---data-protection-act)) | **Yes**: you're a controller established in Botswana | GDPR-style duties: lawful basis, transparency, security, a **record of processing activities** (s.60; a one-page internal doc), breach notice to the IDPC within 72 h (s.63). Children: **parental consent for under-16s where processing relies on consent** for information-society services (s.29). Using contract/legitimate interest avoids that, but it's another reason not to market to children. **Cross-border transfer** (Firebase is outside Botswana) needs adequacy, safeguards (Google's SCCs) or a derogation (s.74–78), and s.74 says a copy should remain in Botswana ⚠ (unclear how this is applied to cloud services; pseudonymous data minimises the issue). No registration fee or filing according to secondary sources ⚠ ([analysis](https://research.lawlab.africa/botswana/)) | Low-moderate *in theory* (new law, active regulator). In practice low for pseudonymous game data. Keep the record of processing and the policy. ⚠ Consider asking the IDPC or a local lawyer about s.74 if online play takes off |
| **South Africa POPIA** | Probably **not**: POPIA covers responsible parties domiciled in SA, or using means in SA ⚠ (my reading of s.3; Firebase isn't hosted in SA for RTDB). Marketing to SA users doesn't by itself change that | The privacy policy mentions the Information Regulator as a courtesy | Negligible |

### IP and names
- **The games** (Morabaraba/Mmele, Khutlotharo/Tsoro Yematatu) are traditional, and **game rules aren't
  copyrightable**. Free to implement.
- **"Morabaraba" trademark:** I found no evidence of a registration by Mind Sports South Africa or anyone else ⚠
  (I didn't search CIPC (SA) or the Botswana CIPA registries directly; a quick search there is worth it). Don't
  copy MSSA's rule text verbatim or use their logo. Mmele's rules doc paraphrases and cites them, which is fine.
- **"Mmele" name:** Play already has *"MHELE: Morabaraba & 12 Morris"* (`com.kemlow.mhele`) and many "Morabaraba"
  apps (`com.motebotech.morabaraba`, `com.mzansimostwanted.mrb`, `com.masilo.morabarabacollection`,
  `com.SisandaTech.Morabaraba3D`). **A web game called "mmele" was promoted at www.mmele.app** (LinkedIn, Brendan
  Mosweu, 31 Mar 2025). ⚠ The site didn't load from the box, so its current status is unverified. Generic
  Setswana game names are weak trademarks, but to avoid confusion use a distinctive title (e.g. "Mmele: Morabaraba
  of Botswana") and your own icon, and check that no-one has registered "Mmele" in Botswana (CIPA) ⚠.
- **Art and sounds:** all scenes, pieces and icons are drawn in code. Sounds are synthesised by `tools/gen_sounds.py`.
  Fonts are system fonts. So everything is original, with no third-party assets. Scene facts and egg descriptions
  are short paraphrased facts with sources (`docs/SCENES.md`, `docs/PIECES.md`). Fine; credit them on an About screen.
- **Open-source licences:** AndroidX, Kotlin, coroutines, Okio, DataStore and Firebase client SDKs are mostly
  **Apache 2.0**, which requires keeping notices. Add an in-app "Open-source licences" screen using Google's
  `com.google.android.gms:oss-licenses-plugin` + `play-services-oss-licenses`, or **AboutLibraries**
  (mikepenz), both generated automatically from Gradle. Some Play services artefacts use the Android SDK licence.
  The generators handle that.
- **Botswana tourism angle:** facts and place names are fine. Don't use the Botswana Tourism Organisation logo or
  say "official", and don't use photos you didn't make.

## 3. Recommended app changes (not made)
1. targetSdk/compileSdk 36 (+ AGP/Kotlin/Compose upgrade); test on Android 16 and on a tablet.
2. Final applicationId (e.g. `com.tshipiworks.mmele`); versionCode 1 / versionName 1.0.0.
3. Release signing config (upload key) + `bundleRelease`; test the minified build.
4. About screen: version, privacy policy, terms, contact, open-source licences, credits.
5. Hide Online when not configured, **or** configure Firebase (europe-west1).
6. If online ships: "Delete my online data" (delete own rooms + `currentUser.delete()`), room TTL clean-up, and
   optionally Firebase App Check (Play Integrity) against abuse. That would add Play Integrity to Data safety:
   re-check before adding.
7. Confirm-before-leave on Back/Home mid-game.
8. TalkBack semantics for board points and controls; contrast check of `BoneDim` text.
9. `allowBackup`: add `dataExtractionRules` or set false.
10. Store assets at Play sizes.
