# Analytics and Crashlytics

Active only when `app/google-services.json` exists (the Crashlytics Gradle plugin is applied only then) **and**
the user leaves "Share usage & crash data" on (home screen; stored in DataStore, default on). Collection is off
in the manifest until the app reads that setting at start. `AD_ID` permission removed; ad personalisation off.

| Event | Params |
| --- | --- |
| `first_open` | automatic (reserved by Firebase, not logged manually) |
| `match_started` | game (mmele/moruba), mode |
| `match_completed` | game, mode, result (win/loss/draw/finished) |
| `tutorial_step` | lesson, step (start/solved) |
| `online_room_created`, `online_room_joined` | ranked |
| `chat_sent` | kind (p/e/t) — never the text |
| `ranked_match` | result |
| `tournament_joined` | size |
| `fact_shown` | category |

Code: `app/src/main/kotlin/bw/mmele/analytics/Analytics.kt` (`Tracker.log`; no-op without Firebase).

## Console steps (you)
1. Firebase console → Analytics: enable Google Analytics for the project (link or create a GA4 property).
2. Crashlytics → Enable. Build and run the release once; force a test crash if you want to see it appear.
3. Upload mapping files: automatic with the Crashlytics plugin on release builds.
4. Set data retention (GA4 → Admin → Data retention: 2 months) to match the privacy policy.
