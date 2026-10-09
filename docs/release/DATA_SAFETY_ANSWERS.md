# Play Console: Data safety form, draft answers

> **Draft, checked against the code on 9 Oct 2026 (commit 86805b6).** You (the developer) are legally responsible for
> these answers. Re-check after **any** dependency change. Google says the form must describe the sum of
> all versions currently on Play.
> Sources: [Play Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469),
> [Firebase data disclosure for Play](https://firebase.google.com/docs/android/play-data-disclosure).

## What the code actually sends (audit)

| Source | Off-device? | What |
|---|---|---|
| vs Computer, Pass & Play, settings (DataStore `home` prefs) | **No** | Stored locally only |
| `firebase-auth` 23.1.0 (anonymous sign-in only) | Yes, online play only | Firebase user ID (random), IP address, user-agent strings, Firebase App ID, "Firebase user agent" (device model/OS/SDK versions, installer) |
| `firebase-database` 21.0.0 | Yes, online play only | Room data written by the app (room code, `hostUid`/`guestUid`, variant, piece-style ids, moves, phase, connected flags, `leftAt`/`createdAt`/`updatedAt` timestamps, resign/draw/rematch offers); IP address and user agent (profiler); Auth UID with each request |
| Analytics / Crashlytics / Installations / Messaging / Ads | **Not present.** Verified in `releaseRuntimeClasspath`: no `firebase-analytics`, `firebase-crashlytics`, `firebase-installations`, `play-services-ads`, no `AD_ID` permission | n/a |
| `play-services-auth`, `recaptcha`, `integrity`, `googleid`, `credentials` | Pulled in transitively by firebase-auth. They are only used for phone/Google sign-in, which the app **does not call** | Nothing, as far as the app's usage goes |

**Important:** if you ship a build **without** `google-services.json` (online play shows "not configured"), Firebase
never initialises and **nothing leaves the device**. Use answer set A. With online play enabled, use answer set B.

---

> **Decision (2026-10-09): v1 ships with online play, so submit answer set B** (once `google-services.json` is in
> the build). Set A is kept only for a hypothetical offline-only build.

## Answer set A: build without Firebase configured

- Does your app collect or share any of the required user data types? → **No**
- (Play then shows "No data collected" and "No data shared".)
- Account deletion questions: "Does your app allow users to create an account?" → **No**

## Answer set B: online play enabled (recommended final answers)

### Overview
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (Firebase uses HTTPS/TLS) |
| Which methods of account creation does your app support? | **My app does not allow users to create an account.** Anonymous Firebase sign-in is invisible: no username, password, email or OTP, no cross-device identity. See "Account deletion" below. |
| Do you provide a way for users to request that their data is deleted? | **Yes**. In-app (About → Delete my online data: deletes the user's rooms, then the anonymous Firebase user) and by email/web (privacy policy `#delete`). Delete-data URL: `https://www.tshipiworks.com/mmele/privacy#delete` (`AppInfo.DELETE_DATA_URL`) |

### Data types: tick ONLY these

**1. Device or other IDs** (Firebase anonymous user ID; Google's own examples for this type include "Firebase installation ID")
- Collected: **Yes**. Shared: **No** (Google/Firebase is a *service provider*, which Play says is not "sharing")
- Processed ephemerally? **No** (stored in the room)
- Required or optional? **Optional** (only if the user chooses "Play online"; the app works fully without it)
- Purposes: **App functionality**; **Fraud prevention, security, and compliance**

**2. App activity → App interactions** (the moves and actions you make in an online game)
- Collected: **Yes**. Shared: **No**
- Ephemeral: **No**. **Optional**
- Purposes: **App functionality**

### Do NOT tick
- **Location (approximate/precise):** Play only requires this if location is *inferred* from IP. Mmele does not
  infer location, and Firebase does not do it for the app. Firebase does receive the IP address; mention that in the
  privacy policy, which the draft does.
- Personal info (name, email, user IDs that identify a person), Financial, Health, Messages, Photos, Audio,
  Files, Calendar, Contacts, Web browsing, Installed apps, Search history.
- **App info and performance** (crash logs, diagnostics): not collected; there's no Crashlytics. If you add it later,
  update this.
- **"Other user-generated content":** there is no chat or free text. Moves are covered by App interactions.

### Security practices
- Data encrypted in transit: **Yes**
- Users can request data deletion: **Yes**
- Committed to Play Families Policy: **No** (unless you choose a children's target audience; not recommended, see
  the audit report)
- Independent security review: **No**

### Account deletion (User Data policy)
Google defines an "app account" as *"a unique user identity that developers provide as a user-facing feature to
serve the user across applications and/or devices… generally include a mechanism to verify an identity"*
([source](https://support.google.com/googleplay/android-developer/answer/13327111)). Mmele's anonymous Firebase UID is
none of those things: it's not user-facing, has no credentials, and doesn't work across devices. So in our reading
the in-app plus web account-deletion requirement **does not apply**. **This is a judgement call, not a confirmed
ruling.** The cheap, safe course is still to provide:
1. a web deletion path: the privacy policy section 6, an anchor link such as `…/privacy#delete`, naming "Mmele" and
   the developer name exactly as on the Play listing; and
2. an in-app **About → "Delete my online data"** action that deletes the user's rooms and calls
   `FirebaseAuth.currentUser.delete()` (**implemented in 1.0.0**).

### Retention (as implemented in 1.0.0)
- Finished rooms: the host's app deletes the room when the host leaves the finished game.
- Any room idle > 30 days: deletable by any signed-in client (security rules) and deleted by `firebase/purge.mjs`.
- Anonymous users inactive > 30 days: deleted by `firebase/purge.mjs`.
- Run the script monthly → the published bound is **60 days** after last activity. The Data safety form has no
  retention field; it only needs "data can be deleted: Yes".
If Play review disagrees, both are already in place.
