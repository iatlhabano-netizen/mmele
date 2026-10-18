package bw.mmele.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import bw.mmele.AppLanguage
import bw.mmele.academy.AcademyScreen
import bw.mmele.academy.LessonRoute
import bw.mmele.academy.LessonViewModel
import bw.mmele.academy.Lessons
import bw.mmele.data.AppStore
import bw.mmele.data.Outcome
import bw.mmele.data.StatKey
import bw.mmele.data.StatsRules
import bw.mmele.data.Tally
import bw.mmele.engine.Player
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Difficulty
import bw.mmele.feedback.FeedbackController
import bw.mmele.online.OnlineGameRepository
import bw.mmele.online.OnlineSession
import bw.mmele.ui.about.AboutScreen
import bw.mmele.ui.common.HowToPlayDialog
import bw.mmele.ui.common.HowToPlayOffer
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.game.GameRecord
import bw.mmele.ui.game.GameRoute
import bw.mmele.ui.game.GameViewModel
import bw.mmele.ui.home.HomeScreen
import bw.mmele.ui.online.OnlineLobbyScreen
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.scenes.Scene
import bw.mmele.ui.stats.StatsScreen
import bw.mmele.ui.theme.MmeleTheme

private enum class Screen { LEADERBOARD, TOURNAMENTS, HOME, GAME, LOBBY, ABOUT, ACADEMY, LESSON, STATS, MORUBA_SETUP, MORUBA }

/** Callbacks that persist app data (DataStore in the app, no-ops in previews/tests). */
interface AppDataSink {
    fun saveGame(encoded: String?) {}
    fun addResult(key: StatKey, outcome: Outcome) {}
    fun resetStats() {}
    fun setLessonsDone(n: Int) {}
    fun setHowToPlaySeen() {}
}

/**
 * Top-level app: a tiny navigator. [onlineRepo] makes a fresh transport per online visit, or
 * returns null when online play isn't configured in this build. [data] is what was stored on the
 * phone at launch; changes go to [sink] and are mirrored in memory.
 */
@Composable
fun MmeleApp(
    feedback: FeedbackController?,
    onlineRepo: () -> OnlineGameRepository? = { null },
    onShareCode: (String) -> Unit = {},
    initialMode: GameMode = GameMode.SOLO,
    initialDifficulty: Difficulty = Difficulty.MEDIUM,
    onModeChoice: (GameMode, Difficulty) -> Unit = { _, _ -> },
    onlineConfigured: Boolean = false,
    versionName: String = "",
    versionCode: Int = 0,
    data: AppStore.Snapshot = AppStore.Snapshot(null, emptyMap(), 0, howToPlaySeen = true),
    sink: AppDataSink = object : AppDataSink {},
    language: AppLanguage = AppLanguage.SYSTEM,
    onLanguage: (AppLanguage) -> Unit = {},
    /** Next non-repeating Botswana fact (+ photo) for a finished game; null in previews/tests. */
    drawFactCard: () -> bw.mmele.data.FactCard? = { null },
    settings: bw.mmele.SettingsPrefs.Settings = bw.mmele.SettingsPrefs.Settings(),
    onSettings: (bw.mmele.SettingsPrefs.Settings) -> Unit = {},
    chatRepo: () -> bw.mmele.online.chat.ChatRepository? = { null },
    rankedRepo: () -> bw.mmele.online.ranked.RankedRepository? = { null },
) {
    MmeleTheme {
        var variant by rememberSaveable { mutableStateOf(Variant.CLASSIC) }
        var mode by rememberSaveable { mutableStateOf(initialMode) }
        var difficulty by rememberSaveable { mutableStateOf(initialDifficulty) }
        var human by rememberSaveable { mutableStateOf(Player.ONE) }
        var clock by rememberSaveable { mutableIntStateOf(0) }
        // A random Botswana destination by default; the player can pick another on the home screen.
        var scene by rememberSaveable { mutableStateOf(Scene.entries.random()) }
        var sound by rememberSaveable { mutableStateOf(true) }
        var haptics by rememberSaveable { mutableStateOf(true) }
        var session by rememberSaveable { mutableIntStateOf(0) }
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        var styleOneId by rememberSaveable { mutableStateOf(PieceStyle.Obsidian.id) }
        var styleTwoId by rememberSaveable { mutableStateOf(PieceStyle.Quartz.id) }
        var lessonIndex by rememberSaveable { mutableIntStateOf(0) }
        var morubaHoles by rememberSaveable { mutableIntStateOf(8) }
        var morubaStones by rememberSaveable { mutableIntStateOf(2) }
        var morubaVsComputer by rememberSaveable { mutableStateOf(true) }
        var morubaLevel by rememberSaveable { mutableStateOf(Difficulty.MEDIUM) }
        var howTo by rememberSaveable { mutableStateOf(if (data.howToPlaySeen) 0 else 1) } // 0 none, 1 offer, 2 dialog
        // Mirrors of stored data.
        var savedGame by remember { mutableStateOf(data.savedGame) }
        var stats by remember { mutableStateOf(data.stats) }
        var lessonsDone by remember { mutableIntStateOf(data.lessonsDone) }
        /** Set when "Continue game" was tapped; consumed by the next GameViewModel. */
        var resume by remember { mutableStateOf<GameRecord?>(null) }
        // Online sessions hold live connections, so they are not saveable; a rotation keeps them.
        var onlineSession by remember { mutableStateOf<OnlineSession?>(null) }
        var lobbyRepo by remember { mutableStateOf<OnlineGameRepository?>(null) }
        feedback?.soundEnabled = sound
        feedback?.hapticsEnabled = haptics
        val config = GameConfig(
            variant, mode, difficulty, human, clock, scene,
            styleOne = PieceStyle.fromId(styleOneId) ?: PieceStyle.Obsidian,
            styleTwo = PieceStyle.fromId(styleTwoId) ?: PieceStyle.Quartz,
        )
        fun applyConfig(c: GameConfig) {
            variant = c.variant; mode = c.mode; difficulty = c.difficulty; human = c.human; clock = c.clockMinutes; scene = c.scene
            styleOneId = c.styleOne.id; styleTwoId = c.styleTwo.id
        }
        val saveGame: (GameRecord?) -> Unit = { rec ->
            val enc = rec?.encode()
            if (enc != savedGame) { savedGame = enc; sink.saveGame(enc) }
        }
        /** The fact card for the game that just finished (one draw per finished game). */
        var factCard by remember { mutableStateOf<bw.mmele.data.FactCard?>(null) }
        val drawFact: () -> bw.mmele.data.FactCard? = {
            drawFactCard().also { c -> if (c != null) bw.mmele.analytics.Tracker.log(bw.mmele.analytics.Events.FACT_SHOWN, "category" to c.fact.category.toString()) }
        }
        var lobbyCode by rememberSaveable { mutableStateOf("") }
        val onResult: (GameConfig, bw.mmele.engine.GameResult) -> Unit = { c, r ->
            factCard = drawFact()
            val key = StatKey.of(c)
            val o = StatsRules.outcomeOf(c, r)
            stats = stats + (key to (stats[key] ?: Tally()).plus(o))
            sink.addResult(key, o)
        }
        if (screen == Screen.GAME && config.mode == GameMode.ONLINE && onlineSession == null) screen = Screen.HOME

        AnimatedContent(targetState = screen, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "nav") { target ->
            when (target) {
                Screen.GAME -> {
                    val s = onlineSession
                    val gameConfig = if (s != null) s.configFor(scene) else config
                    val vm: GameViewModel = viewModel(key = "game-$session") {
                        if (s != null) GameViewModel(gameConfig, online = s, onResult = onResult)
                        else GameViewModel.create(gameConfig, createSavedStateHandle(), resume, saveGame, onResult).also { resume = null }
                    }
                    val chat = remember(onlineSession) { if (onlineSession != null) chatRepo() else null }
                    GameRoute(vm, feedback, onHome = { screen = Screen.HOME; onlineSession = null }, onShareCode = onShareCode, factCard = factCard,
                        chatRepo = chat, chatEnabled = settings.chatEnabled)
                }
                Screen.MORUBA_SETUP -> bw.mmele.ui.moruba.MorubaSetupScreen(
                    setup = bw.mmele.ui.moruba.MorubaSetup(morubaHoles, morubaStones, morubaVsComputer, morubaLevel),
                    onChange = { morubaHoles = it.holes; morubaStones = it.stones; morubaVsComputer = it.vsComputer; morubaLevel = it.difficulty },
                    onStart = { session++; screen = Screen.MORUBA },
                    onBack = { screen = Screen.HOME },
                )
                Screen.MORUBA -> {
                    val setup = bw.mmele.ui.moruba.MorubaSetup(morubaHoles, morubaStones, morubaVsComputer, morubaLevel)
                    val vm: bw.mmele.ui.moruba.MorubaViewModel = viewModel(key = "moruba-$session") {
                        bw.mmele.analytics.Tracker.log(bw.mmele.analytics.Events.MATCH_STARTED, "game" to "moruba", "mode" to if (setup.vsComputer) "solo" else "pass_and_play")
                        bw.mmele.ui.moruba.MorubaViewModel(setup, onGameOver = { w ->
                            factCard = drawFact()
                            bw.mmele.analytics.Tracker.log(bw.mmele.analytics.Events.MATCH_COMPLETED, "game" to "moruba", "mode" to if (setup.vsComputer) "solo" else "pass_and_play",
                                "result" to if (!setup.vsComputer) "finished" else if (w == Player.ONE) "win" else "loss")
                        })
                    }
                    bw.mmele.ui.moruba.MorubaRoute(vm, feedback, onHome = { screen = Screen.HOME }, factCard = factCard)
                }
                Screen.LOBBY -> OnlineLobbyScreen(
                    config = config,
                    repo = lobbyRepo,
                    onSession = { onlineSession = it; session++; lobbyCode = ""; screen = Screen.GAME },
                    onBack = { lobbyCode = ""; screen = Screen.HOME },
                    initialCode = lobbyCode,
                )
                Screen.LEADERBOARD -> {
                    val repo = remember { rankedRepo() }
                    bw.mmele.ui.ranked.LeaderboardScreen(repo, onBack = { screen = Screen.HOME })
                }
                Screen.TOURNAMENTS -> {
                    val repo = remember { rankedRepo() }
                    bw.mmele.ui.ranked.TournamentsScreen(repo, onBack = { screen = Screen.HOME }, onPlayRoom = { code ->
                        applyConfig(config.copy(mode = GameMode.ONLINE)); lobbyCode = code; screen = Screen.LOBBY
                    })
                }
                Screen.ABOUT -> {
                    val repo = remember { onlineRepo() }
                    AboutScreen(
                        versionName = versionName,
                        versionCode = versionCode,
                        onBack = { screen = Screen.HOME },
                        onDeleteOnlineData = repo?.let { r -> { r.deleteMyData() } },
                    )
                }
                Screen.ACADEMY -> AcademyScreen(
                    lessonsDone = lessonsDone,
                    onOpen = { lessonIndex = it; session++; screen = Screen.LESSON },
                    onBack = { screen = Screen.HOME },
                )
                Screen.LESSON -> {
                    val idx = lessonIndex.coerceIn(0, Lessons.ALL.lastIndex)
                    val vm: LessonViewModel = viewModel(key = "lesson-$session-$idx") {
                        bw.mmele.analytics.Tracker.log(bw.mmele.analytics.Events.TUTORIAL_STEP, "lesson" to idx + 1, "step" to "start")
                        LessonViewModel(Lessons.ALL[idx], onSolved = {
                            bw.mmele.analytics.Tracker.log(bw.mmele.analytics.Events.TUTORIAL_STEP, "lesson" to idx + 1, "step" to "solved")
                            if (idx + 1 > lessonsDone) { lessonsDone = idx + 1; sink.setLessonsDone(idx + 1) }
                        })
                    }
                    LessonRoute(
                        vm, feedback,
                        hasNext = idx < Lessons.ALL.lastIndex,
                        onNext = { lessonIndex = idx + 1; session++ },
                        onBack = { screen = Screen.ACADEMY },
                    )
                }
                Screen.STATS -> StatsScreen(
                    stats = stats,
                    onBack = { screen = Screen.HOME },
                    onReset = { stats = emptyMap(); sink.resetStats() },
                )
                Screen.HOME -> {
                    HomeScreen(
                        config = config,
                        onConfigChange = {
                            if (it.mode != mode || it.difficulty != difficulty) onModeChoice(it.mode, it.difficulty)
                            applyConfig(it)
                        },
                        soundOn = sound,
                        hapticsOn = haptics,
                        onSoundChange = { sound = it },
                        onHapticsChange = { haptics = it },
                        onPlay = { onlineSession = null; resume = null; session++; screen = Screen.GAME },
                        onPlayOnline = { lobbyRepo = onlineRepo(); screen = Screen.LOBBY },
                        onAbout = { screen = Screen.ABOUT },
                        onMoruba = { screen = Screen.MORUBA_SETUP },
                        onlineConfigured = onlineConfigured,
                        savedGame = remember(savedGame) { GameRecord.decode(savedGame) },
                        onContinue = {
                            GameRecord.decode(savedGame)?.let { rec ->
                                applyConfig(rec.config)
                                onlineSession = null
                                resume = rec
                                session++
                                screen = Screen.GAME
                            }
                        },
                        lessonsDone = lessonsDone,
                        lessonsTotal = Lessons.ALL.size,
                        onAcademy = { screen = Screen.ACADEMY },
                        onStats = { screen = Screen.STATS },
                        onHowToPlay = { howTo = 2 },
                        language = language,
                        onLanguage = onLanguage,
                        settings = settings,
                        onSettings = onSettings,
                        onLeaderboard = { screen = Screen.LEADERBOARD },
                        onTournaments = { screen = Screen.TOURNAMENTS },
                    )
                }
            }
        }
        when (howTo) {
            1 -> HowToPlayOffer(onShow = { howTo = 2; sink.setHowToPlaySeen() }, onSkip = { howTo = 0; sink.setHowToPlaySeen() })
            2 -> HowToPlayDialog(onDone = { howTo = 0 })
        }
    }
}
