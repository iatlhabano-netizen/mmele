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
        val g = Rules.position(RuleSet.TRIANGLE, listOf(t("L1"), t("M2"), t("R3")), listOf(t("T"), t("L2"), t("M3")), hand1 = 1, hand2 = 1)
        val config = GameConfig(Variant.TRIANGLE, GameMode.PASS_AND_PLAY, scene = Scene.TSODILO)
        paparazzi.snapshot {
            MmeleTheme { GameScreenContent(ui(config, g, lastMove = LastMove(null, t("M3"))), {}, {}, animate = false) }
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
}
