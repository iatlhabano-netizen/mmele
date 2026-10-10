# Google Play store listing (draft)

> Limits: title 30, short description 80, full description 4,000 characters (counts below were measured).
> Policy reminders ([Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842)):
> no "best", "#1", "free" or ranking claims in the title, no emoji or ALL CAPS gimmicks in the title, no keyword
> stuffing, no fake testimonials, and don't use other apps' names. Don't imply endorsement by Botswana Tourism, MSSA
> or any government body.

## App name (title)
**Chosen: `Mmele: Morabaraba of Botswana`** (29 of 30 chars, verified with `len()`). The launcher label stays `Mmele`.
Alternatives: `Mmele – Morabaraba Board Game` (29), `Mmele: Morabaraba & Triangle` (28).
Name-clash note: Play already has **"MHELE: Morabaraba & 12 Morris"** (`com.kemlow.mhele`) and several "Morabaraba" apps,
and a separate web game was promoted as "mmele" at www.mmele.app (March 2025). Keeping "of Botswana" in the
title and using your own icon makes Mmele easier to tell apart. See the audit report.

## Short description
Recommended (80): `Morabaraba and Khutlotharo from Botswana: play the computer, a friend or online.`
Alternative (77): `The Setswana cattle game: play Morabaraba vs computer, online or pass & play.`
If v1 ships **without** online play, use: `Morabaraba and Khutlotharo from Botswana. Play the computer or pass & play.`

## Full description (1668 chars)
If v1 ships without online play, delete the "Play online" bullet and the word "online" in the short description.

```
Mmele is Morabaraba, the cattle game played across Botswana and Southern Africa, made with care for your phone.

Two players, twelve cows each, one board of three squares. Place your cows, slide them along the lines, and form a row of three (a mill) to shoot one of your opponent's cows. Bring them down to two and the herd is yours.

THREE WAYS TO PLAY
• Play vs Computer: Easy, Medium or Hard. Hard thinks ahead and punishes mistakes.
• Play online: create a room and send the 6-letter code to a friend. No sign-up, no names, no chat.
• Pass & Play: two players, one phone. The top player's panel turns to face them.

TWO TRADITIONAL BOARDS
• Classic Morabaraba: 24 points and 12 cows each. Mills shoot, and you fly when you're down to three. Follows the generally accepted tournament rules.
• Khutlotharo: the small Setswana triangle. Seven points, three cows each, and the first to make three in a line wins. Quick, sharp and surprisingly deep.

A GAME OF BOTSWANA
Play against scenes of Botswana's great places, all drawn in the app: the Okavango Delta, the Chobe riverfront, the Makgadikgadi salt pans, the Central Kalahari and the Tsodilo Hills. Each one comes with a short note about the place.

Choose your pieces: smooth pebbles, rough diamonds, or eggs of Botswana's birds: kori bustard, ostrich, crowned lapwing, helmeted guineafowl and lilac-breasted roller.

MADE TO FEEL GOOD
• Drag or tap to move, with legal moves shown as you play
• Undo, an optional chess clock, sound and haptics
• No ads, no in-app purchases, no account
• vs Computer and Pass & Play work fully offline

Learn a game your grandparents played, or teach it to someone new. Tshameka!
```

## Category, tags, contact
- App or game: **Game** · Category: **Board**
- Tags (choose up to 5 from Play's list in Store settings → Manage tags): *Board*, *Abstract strategy*, *Classic*,
  *Offline*, *Strategy* (pick whatever exact labels Play offers that match)
- Email (shown publicly): **CONTACT_EMAIL_PLACEHOLDER** (use a dedicated address; set it everywhere with `site/set-contact.sh`)
- Website (optional): **https://YOUR-VERCEL-DOMAIN** (the `site/` landing page)
- Privacy policy URL (required): **https://YOUR-VERCEL-DOMAIN/privacy** (from `site/`; set with `site/set-domain.sh`)

## Graphic assets needed ([specs](https://support.google.com/googleplay/android-developer/answer/9866151), [icon spec](https://developer.android.com/distribute/google-play/resources/icon-design-specifications))
| Asset | Spec | Status |
|---|---|---|
| App icon | 512×512 PNG, 32-bit with alpha, ≤1 MB, full square (Play rounds the corners), no added shadow | **Done**: `/workspace/mmele-store/icon-512.png` (30 KB), rendered from the adaptive launcher icon layers |
| Feature graphic | 1024×500, JPEG or 24-bit PNG (no alpha) | **Done**: `feature-graphic-1024x500.png` (24-bit, Okavango sunset + board + wordmark) |
| Phone screenshots | 2–8; JPEG or 24-bit PNG; 320–3840 px per side; long side ≤ 2× short side. For promotion eligibility: ≥4 at ≥1080 px (e.g. 1080×1920) | **Done**: 6 × 1080×1920 24-bit PNG in `/workspace/mmele-store/` (`phone-1-home` … `phone-6-online-room-code`), rendered from the real screens by `StoreAssetsTest`; re-export with `python3 tools/export_store_assets.py` |
| 7" / 10" tablet screenshots | Optional, but recommended because the app runs on tablets | Optional |

Upload order: (1) home, (2) classic board vs Computer (Chobe), (3) Khutlotharo triangle (Kalahari), (4) bird-egg
piece set, Pass & Play "must shoot" (Okavango), (5) Tsodilo Hills scene + fact on the win card, (6) online room code.

## App access (Play Console → App content → App access)
Choose **"All or some functionality is restricted"** only if online play is enabled. Suggested text:
> No login is needed. vs Computer and Pass & Play work offline immediately. To test Play online you need two
> devices: on device 1, Home → "Play online" → Tshameka · Find a room → Create room. That shows a 6-letter code. On
> device 2, choose "Play online" → Tshameka · Find a room → "Join a friend", enter the code and tap "Join game". There is no account, chat or purchase.
If online play is not in the build, choose "All functionality is available without special access".

## Other App content answers (expected)
- Ads: **No ads**
- Content rating (IARC): board game, no violence, gambling, user chat or shared location. Answer "No" to user
  interaction / UGC / sharing location. Expected result: **ESRB Everyone, PEGI 3, USK 0, IARC 3+** (to be confirmed
  when you submit the questionnaire)
- Target audience: **13–15, 16–17, 18+** (see the audit report on Families/COPPA)
- News app: No · Government app: No · Financial features: None · Health: None
- Data safety: see DATA_SAFETY_ANSWERS.md
