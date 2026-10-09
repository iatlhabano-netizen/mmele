package bw.mmele

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import bw.mmele.engine.Boards
import bw.mmele.engine.GameState
import bw.mmele.engine.Player
import bw.mmele.engine.RuleSet
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.game.GameScreenContent
import bw.mmele.ui.game.GameUiState
import bw.mmele.ui.game.Interaction
import bw.mmele.ui.game.LastMove
import bw.mmele.ui.game.TokenUi
import bw.mmele.ui.home.HomeScreen
import bw.mmele.ui.game.GameOverCard
import bw.mmele.ui.scenes.Scene
import bw.mmele.ui.scenes.SceneBackdrop
import bw.mmele.ui.scenes.SceneMode
import bw.mmele.engine.GameResult
import bw.mmele.engine.WinReason
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bw.mmele.ui.theme.MmeleTheme
import org.junit.Rule
import bw.mmele.ui.pieces.EggKind
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.pieces.PieceFamily
import bw.mmele.ui.game.OnlineUi
import bw.mmele.ui.game.PieceIcon
import bw.mmele.ui.online.OnlineLobbyScreen
import bw.mmele.online.InMemoryOnlineGameRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import bw.mmele.ui.theme.MmeleColors
import org.junit.Test

/** JVM screenshot renders (Paparazzi / layoutlib). Run `./gradlew :app:recordPaparazziDebug`. */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.NoActionBar",
        maxPercentDifference = 0.5,
    )

    private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)
    private fun t(l: String) = Boards.TRIANGLE.labels.indexOf(l)

    private fun tokensOf(g: GameState): List<TokenUi> =
        (0 until g.board.size).mapNotNull { n -> g.ownerAt(n)?.let { TokenUi(n + 1, it, n) } }

    private fun ui(config: GameConfig, g: GameState, selected: Int? = null, lastMove: LastMove? = null, clock: Long = 0) = GameUiState(
        config = config,
        game = g,
        tokens = tokensOf(g),
        selected = selected,
        targets = Interaction.targetsFor(g, selected),
        shootable = Interaction.shootableFor(g),
        draggable = Interaction.draggableFor(g),
        lastMove = lastMove,
        canUndo = true,
        clockMs1 = clock,
        clockMs2 = clock - 41_000,
    )

    @Test fun home() {
        paparazzi.snapshot {
            MmeleTheme {
                HomeScreen(
                    config = GameConfig(mode = GameMode.SOLO, difficulty = Difficulty.HARD, scene = Scene.OKAVANGO),
                    onConfigChange = {}, soundOn = true, hapticsOn = true, onSoundChange = {}, onHapticsChange = {}, onPlay = {},
                    animate = false,
                )
            }
        }
    }

    @Test fun classicMidGamePassAndPlay() {
        val p1 = listOf("a7", "d7", "b6", "f4", "e3", "c4", "d2", "g1", "b2").map(::c)
        val p2 = listOf("g7", "f6", "e5", "d6", "e4", "c3", "a1", "g4").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2).copy(shotBy1 = 4, shotBy2 = 3, ply = 41)
        val config = GameConfig(Variant.CLASSIC, GameMode.PASS_AND_PLAY, clockMinutes = 5, scene = Scene.CHOBE)
        paparazzi.snapshot {
            MmeleTheme { GameScreenContent(ui(config, g, selected = c("b6"), lastMove = LastMove(c("d5"), c("d6")), clock = 187_000), {}, {}, animate = false) }
        }
    }

    @Test fun classicMustShootSolo() {
        val p1 = listOf("a7", "d7", "g7", "b4", "f2", "e4").map(::c)
        val p2 = listOf("a1", "d1", "g1", "b6", "e5", "c3", "f4").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2, hand1 = 2, hand2 = 3, mustShoot = true)
        val config = GameConfig(Variant.CLASSIC, GameMode.SOLO, Difficulty.HARD, scene = Scene.MAKGADIKGADI)
        paparazzi.snapshot {
            MmeleTheme { GameScreenContent(ui(config, g, lastMove = LastMove(null, c("g7"))), {}, {}, animate = false) }
        }
    }

    @Test fun classicFlying() {
        val p1 = listOf("a7", "e4", "b2").map(::c)
        val p2 = listOf("g7", "f6", "d6", "e3", "c4", "d2").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2).copy(shotBy2 = 9, shotBy1 = 6)
        val config = GameConfig(Variant.CLASSIC, GameMode.SOLO, Difficulty.MEDIUM, scene = Scene.KALAHARI)
        paparazzi.snapshot {
            MmeleTheme { GameScreenContent(ui(config, g, selected = c("e4")), {}, {}, animate = false) }
        }
    }

    @Test fun triangleGame() {
        // Placement: two cows each on the board, one each still in hand.
        val g = Rules.position(RuleSet.TRIANGLE, listOf(t("CL"), t("BR")), listOf(t("C"), t("BM")), hand1 = 1, hand2 = 1)
        val config = GameConfig(Variant.TRIANGLE, GameMode.PASS_AND_PLAY, scene = Scene.TSODILO)
        paparazzi.snapshot {
            MmeleTheme { GameScreenContent(ui(config, g, lastMove = LastMove(null, t("BM"))), {}, {}, animate = false) }
        }
    }

    /** Movement phase with bird eggs: A is selected and can jump over CL into the empty BL. */
    @Test fun triangleMidGameEggs() {
        val g = Rules.position(RuleSet.TRIANGLE, listOf(t("A"), t("CL"), t("BM")), listOf(t("C"), t("CR"), t("BR"))).copy(ply = 9)
        val config = GameConfig(
            Variant.TRIANGLE, GameMode.SOLO, Difficulty.MEDIUM, scene = Scene.KALAHARI,
            styleOne = PieceStyle.Egg(EggKind.KORI_BUSTARD), styleTwo = PieceStyle.Egg(EggKind.OSTRICH),
        )
        paparazzi.snapshot {
            MmeleTheme { GameScreenContent(ui(config, g, selected = t("A"), lastMove = LastMove(t("BL"), t("BR"))), {}, {}, animate = false) }
        }
    }

    @Test fun homeKalahari() {
        paparazzi.snapshot {
            MmeleTheme {
                HomeScreen(
                    config = GameConfig(variant = Variant.TRIANGLE, scene = Scene.KALAHARI),
                    onConfigChange = {}, soundOn = true, hapticsOn = false, onSoundChange = {}, onHapticsChange = {}, onPlay = {},
                    animate = false,
                )
            }
        }
    }

    @Test fun gameOverCard() {
        val p1 = listOf("a7", "d7", "g7", "b4", "f2", "e4").map(::c)
        val p2 = listOf("a1", "d1").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2).copy(result = GameResult.Win(Player.ONE, WinReason.REDUCED_TO_TWO), ply = 87, shotBy1 = 10)
        val config = GameConfig(Variant.CLASSIC, GameMode.SOLO, Difficulty.HARD, scene = Scene.TSODILO)
        paparazzi.snapshot {
            MmeleTheme {
                Box(Modifier.fillMaxSize()) {
                    GameScreenContent(ui(config, g), {}, {}, animate = false)
                    GameOverCard(ui(config, g), {}, {}, Modifier.align(Alignment.Center).padding(24.dp))
                }
            }
        }
    }

    @Test fun scenesFull() {
        for (scene in Scene.entries) {
            paparazzi.snapshot(name = "scene_${scene.name.lowercase()}") {
                SceneBackdrop(scene, SceneMode.FULL, Modifier.fillMaxSize(), animate = false)
            }
        }
    }

    // ------------------------------------------------------------------ piece sets

    /** The same mid-game position drawn with each piece set, so the sets can be compared. */
    private fun pieceSetBoard(name: String, one: PieceStyle, two: PieceStyle, scene: Scene) {
        val p1 = listOf("a7", "d7", "b6", "f4", "e3", "c4", "d2", "g1", "b2").map(::c)
        val p2 = listOf("g7", "f6", "e5", "d6", "e4", "c3", "a1", "g4").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2).copy(shotBy1 = 4, shotBy2 = 3, ply = 41)
        val config = GameConfig(Variant.CLASSIC, GameMode.PASS_AND_PLAY, scene = scene, styleOne = one, styleTwo = two)
        paparazzi.snapshot(name = name) {
            MmeleTheme { GameScreenContent(ui(config, g, selected = c("b6"), lastMove = LastMove(c("d5"), c("d6"))), {}, {}, animate = false) }
        }
    }

    @Test fun piecesPebbles() = pieceSetBoard("pebbles", PieceStyle.Obsidian, PieceStyle.Quartz, Scene.CHOBE)
    @Test fun piecesDiamonds() = pieceSetBoard("diamonds", PieceStyle.SmokyDiamond, PieceStyle.ClearDiamond, Scene.MAKGADIKGADI)
    @Test fun piecesEggs() = pieceSetBoard("eggs", PieceStyle.Egg(EggKind.KORI_BUSTARD), PieceStyle.Egg(EggKind.OSTRICH), Scene.OKAVANGO)
    @Test fun piecesEggsLapwingGuineafowl() = pieceSetBoard("eggs_lapwing_guineafowl", PieceStyle.Egg(EggKind.CROWNED_LAPWING), PieceStyle.Egg(EggKind.GUINEAFOWL), Scene.KALAHARI)

    /** Every individual piece, large, with per-id variation visible. */
    @Test fun pieceGallery() {
        paparazzi.snapshot {
            MmeleTheme {
                Column(Modifier.fillMaxSize().background(MmeleColors.Slate).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (style in PieceStyle.all) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (id in 1..5) PieceIcon(style, Modifier.size(52.dp), pieceId = id * 7 + 3)
                            Text("  " + style.fullName, color = MmeleColors.Bone)
                        }
                    }
                }
            }
        }
    }

    @Test fun homeEggPicker() {
        paparazzi.snapshot {
            MmeleTheme {
                HomeScreen(
                    config = GameConfig(scene = Scene.OKAVANGO, styleOne = PieceStyle.Egg(EggKind.KORI_BUSTARD), styleTwo = PieceStyle.Egg(EggKind.OSTRICH)),
                    onConfigChange = {}, soundOn = true, hapticsOn = true, onSoundChange = {}, onHapticsChange = {}, onPlay = {},
                    animate = false,
                )
            }
        }
    }

    // ------------------------------------------------------------------ online

    @Test fun onlineNotConfigured() {
        paparazzi.snapshot {
            MmeleTheme { OnlineLobbyScreen(GameConfig(scene = Scene.CHOBE), repo = null, onSession = {}, onBack = {}, animate = false) }
        }
    }

    @Test fun onlineLobby() {
        paparazzi.snapshot {
            MmeleTheme {
                OnlineLobbyScreen(
                    GameConfig(scene = Scene.TSODILO, styleOne = PieceStyle.SmokyDiamond, styleTwo = PieceStyle.ClearDiamond),
                    repo = InMemoryOnlineGameRepository(), onSession = {}, onBack = {}, animate = false, initialCode = "k7m3pq",
                )
            }
        }
    }

    @Test fun onlineWaitingForOpponent() {
        val g = Rules.newGame(RuleSet.CLASSIC)
        val config = GameConfig(Variant.CLASSIC, GameMode.ONLINE, human = Player.ONE, scene = Scene.MAKGADIKGADI, styleOne = PieceStyle.SmokyDiamond, styleTwo = PieceStyle.ClearDiamond)
        val state = ui(config, g).copy(canUndo = false, online = OnlineUi(code = "K7M3PQ", waitingForOpponent = true, opponentConnected = false))
        paparazzi.snapshot { MmeleTheme { GameScreenContent(state, {}, {}, animate = false) } }
    }

    @Test fun onlineInGameOpponentAway() {
        val p1 = listOf("a7", "d7", "b4", "f2").map(::c)
        val p2 = listOf("a1", "g1", "e5", "c3").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2, hand1 = 8, hand2 = 8, toMove = Player.TWO)
        val config = GameConfig(Variant.CLASSIC, GameMode.ONLINE, human = Player.TWO, scene = Scene.OKAVANGO, styleOne = PieceStyle.Egg(EggKind.KORI_BUSTARD), styleTwo = PieceStyle.Egg(EggKind.OSTRICH))
        val state = ui(config, g, lastMove = LastMove(null, c("f2"))).copy(
            canUndo = false,
            online = OnlineUi(code = "K7M3PQ", waitingForOpponent = false, opponentConnected = false, abandonSecondsLeft = 64, opponentOffersDraw = true),
        )
        paparazzi.snapshot { MmeleTheme { GameScreenContent(state, {}, {}, animate = false) } }
    }
}
