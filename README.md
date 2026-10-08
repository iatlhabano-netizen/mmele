# Mmele — Morabaraba for Android

A premium Kotlin + Jetpack Compose implementation of **Mmele / Morabaraba**, the Southern African
twelve-cow mill game, plus **Khutlotharo**, a small sudden-death triangle variant. Backdrops celebrate
Botswana destinations (Okavango, Chobe, Makgadikgadi, Central Kalahari, Tsodilo), all drawn in code.

**Status: milestone 1.** Pass & Play, Solo vs AI (Easy / Medium / Hard), both variants, haptics, sound,
undo, optional chess clock, scenes. The Academy (tutorial puzzles) is milestone 2 and shows as "coming soon".

## Modules

| Module | What | Android deps |
|---|---|---|
| `:engine` | Board graphs (adjacency + mills) for both variants, immutable `GameState`, sealed `Action` (`Place` / `Move` / `Shoot`), rules & phase state machine (Placement → Movement → Flying → Game over, with a *must shoot* sub-state), draw rules, AI (`EasyAi`, `SearchAi` = negamax/alpha-beta, iterative deepening, Zobrist TT) | **none** (pure Kotlin/JVM) |
| `:app` | MVVM + UDF: `GameViewModel` exposes `StateFlow<GameUiState>`, takes `GameIntent`s, emits one-shot `FeedbackEvent`s; Compose UI (`BoardCanvas`, HUDs, home, game-over), haptics (`VibrationEffect`), `SoundPool` audio, Botswana `Scene`s | Compose BOM 2024.12.01, Material 3 |

Key files: `engine/.../Rules.kt` (state machine), `engine/.../ai/SearchAi.kt`, `app/.../ui/board/BoardCanvas.kt`,
`app/.../ui/game/GameViewModel.kt`, `app/.../ui/scenes/SceneArt.kt`.

## Rules

See **[docs/RULES.md](docs/RULES.md)** — MSSA "Generally Accepted Rules" with sources, every edge-case
decision (double mill = one shot, full board, re-mill ban, draw rules) and the triangle-variant
reconstruction (verified a draw with perfect play by `tools/solve_triangle.py`).
Scene facts and sources: **[docs/SCENES.md](docs/SCENES.md)**.

## Build

Requirements: JDK 17, Android SDK (platform 35, build-tools 35). Toolchain: Gradle 8.11.1 (wrapper),
AGP 8.7.3, Kotlin 2.0.21. `minSdk 26`, `targetSdk 35`.

```bash
./gradlew :engine:test            # engine unit + fuzz + AI tests
./gradlew :app:testDebugUnitTest  # ViewModel tests + Paparazzi screenshot verification
./gradlew assembleDebug           # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:recordPaparazziDebug   # re-render screenshots into app/src/test/snapshots/images
python3 tools/gen_sounds.py       # regenerate the procedural sounds (numpy + ffmpeg)
python3 tools/solve_triangle.py 4 # exhaustive solve of the triangle variant
```

## Design notes

* **Board** — one `Canvas`, every coordinate derived from normalised node positions, so it fits any
  aspect ratio. Pebbles are radial-gradient bodies with rim occlusion, specular highlight and an
  elevation-aware soft shadow; flying cows hover; tokens have stable ids and spring-animate between
  nodes; placement drops in with overshoot; shot cows fade with an ochre burst; mills trigger a gold
  line trace. Tap-to-move and drag-and-drop (the dragged cow renders 35 dp above the finger; the
  landing node is the one under the cow, not the finger).
* **AI** runs on `Dispatchers.Default` with a hard time cap (Medium 0.9 s, Hard 1.6 s) and cooperative
  cancellation (undo / leaving the game cancels it). Search depth counts *turns*: the shot after a mill
  is part of the same turn.
  * Easy: random, but usually completes a mill and often blocks one.
  * Medium: alpha-beta to depth 3 (iterative 1→3 under the time cap), material/mills/open-twos/mobility eval.
  * Hard: iterative deepening alpha-beta + Zobrist transposition table + move ordering, eval adds
    positional weights (intersection degree), open/"running" (double) mill detection and double threats.
* **Haptics** — predefined effects on API 29+, custom amplitude waveforms where supported,
  timings-only fallbacks on API 26–28; failures are swallowed.
* **Sounds** — synthesised by `tools/gen_sounds.py` (no samples, no licences), ~60 KB total OGG.
* **Scenes** — random at launch, selectable on the home screen; full strength with ambient motion on
  home and game-over; static, blurred (Android 12+) and scrimmed to `#121417` behind the board.

## Tests

* `:engine` — 43 JUnit tests: adjacency symmetry/degrees, the exact 20 mills (exhaustive collinear
  enumeration), protected-mill rule + all-in-mills exception, double mill = one shot, phase transitions,
  flying, win by 2 cows, blockade, full board at end of placement, both draw rules, MSSA re-mill ban,
  triangle win conditions, 8,000 random self-play games (termination + "never zero legal actions"),
  AI legality on sampled positions for all levels, AI time cap / cancellation, strength sanity.
* `:app` — 5 ViewModel tests (tap/place/undo, mill → shoot, token tracking, solo AI reply + undo,
  AI-first, drag & drop) and 8 Paparazzi screenshot tests.

## Known limitations

* Not yet run on a physical device or emulator (none available in the build environment): haptics,
  sound timing, 120 Hz smoothness and real-device AI speed are unverified.
* Paparazzi renders do not show the RenderEffect blur or animations (static frames).
* Medium/Hard are time-capped, so on slow phones Medium may finish depth 2 instead of 3.
* Undo does not rewind the chess clock; no game persistence across process death yet.
* Strings are hard-coded English with Setswana phase names; no localisation resources yet.
