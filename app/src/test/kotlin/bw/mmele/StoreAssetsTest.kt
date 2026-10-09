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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import bw.mmele.ui.about.AboutScreen
import bw.mmele.ui.about.LicenceEntry
import bw.mmele.ui.board.BoardCanvas
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.tshipiworks.mmele.R

/*
 * Google Play store assets, rendered from the real composables and drawables (no mock-ups).
 * `./gradlew :app:recordPaparazziDebug --tests '*Store*'` then tools/export_store_assets.sh
 * copies them to /workspace/mmele-store with Play's file names and sizes.
 */

private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)
private fun t(l: String) = Boards.TRIANGLE.labels.indexOf(l)

private fun tokensOf(g: GameState): List<TokenUi> =
    (0 until g.board.size).mapNotNull { n -> g.ownerAt(n)?.let { TokenUi(n + 1, it, n) } }

private fun ui(config: GameConfig, g: GameState, selected: Int? = null, lastMove: LastMove? = null, clock: Long = 0) = GameUiState(
    config = config, game = g, tokens = tokensOf(g), selected = selected,
    targets = Interaction.targetsFor(g, selected), shootable = Interaction.shootableFor(g), draggable = Interaction.draggableFor(g),
    lastMove = lastMove, canUndo = true, clockMs1 = clock, clockMs2 = clock - 41_000,
)

private fun device(w: Int, h: Int, dpi: Density) = DeviceConfig.PIXEL_6.copy(
    screenWidth = w, screenHeight = h, xdpi = dpi.dpiValue, ydpi = dpi.dpiValue, density = dpi,
    orientation = if (w > h) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
)

/** 512 x 512 high-res icon: the adaptive launcher icon's two layers, full bleed (Play applies the mask). */
class StoreIconTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = device(512, 512, Density.MEDIUM), theme = "android:Theme.Material.NoActionBar", useDeviceResolution = true)

    @Test fun icon() {
        paparazzi.snapshot(name = "icon_512") {
            Box(Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            }
        }
    }
}

/** 1024 x 500 feature graphic: Okavango sunset, the board with a mill, and the name. */
class StoreFeatureGraphicTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = device(1024, 500, Density.XHIGH), theme = "android:Theme.Material.NoActionBar", useDeviceResolution = true)

    @Test fun featureGraphic() {
        val p1 = listOf("a7", "d7", "g7", "b4", "f2").map(::c)
        val p2 = listOf("a1", "d6", "e4", "g4").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2, hand1 = 3, hand2 = 4)
        paparazzi.snapshot(name = "feature_graphic") {
            MmeleTheme {
                Box(Modifier.fillMaxSize()) {
                    SceneBackdrop(Scene.OKAVANGO, SceneMode.FULL, Modifier.fillMaxSize(), animate = false)
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to MmeleColors.Slate.copy(alpha = 0.88f), 0.55f to MmeleColors.Slate.copy(alpha = 0.35f), 1f to Color.Transparent)))
                    Row(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Mmele", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontSize = 52.sp), color = MmeleColors.GoldBright)
                            Text("Morabaraba of Botswana", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = MmeleColors.Bone, maxLines = 1, softWrap = false)
                            Spacer(Modifier.height(10.dp))
                            Text("Place your cows · form a mill · shoot", style = MaterialTheme.typography.titleSmall, color = MmeleColors.Bone)
                        }
                        Box(
                            Modifier.fillMaxHeight(0.88f).aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(MmeleColors.Slate.copy(alpha = 0.9f)),
                        ) {
                            BoardCanvas(
                                board = g.board, tokens = tokensOf(g), modifier = Modifier.fillMaxSize().padding(6.dp),
                                lastMove = LastMove(null, c("g7")), interactive = false, animate = false, drawBackground = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Six 1080 x 1920 (9:16) phone screenshots of real screens. */
class StoreScreenshotsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = device(1080, 1920, Density.create(420)), theme = "android:Theme.Material.NoActionBar", useDeviceResolution = true)

    @Test fun s1Home() = paparazzi.snapshot(name = "phone_1_home") {
        MmeleTheme {
            HomeScreen(
                config = GameConfig(mode = GameMode.SOLO, difficulty = Difficulty.MEDIUM, scene = Scene.OKAVANGO),
                onConfigChange = {}, soundOn = true, hapticsOn = true, onSoundChange = {}, onHapticsChange = {}, onPlay = {}, animate = false,
            )
        }
    }

    @Test fun s2Classic() {
        val p1 = listOf("a7", "d7", "b6", "f4", "e3", "c4", "d2", "g1", "b2").map(::c)
        val p2 = listOf("g7", "f6", "e5", "d6", "e4", "c3", "a1", "g4").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2).copy(shotBy1 = 4, shotBy2 = 3, ply = 41)
        val config = GameConfig(Variant.CLASSIC, GameMode.SOLO, Difficulty.HARD, scene = Scene.CHOBE)
        paparazzi.snapshot(name = "phone_2_classic") {
            MmeleTheme { GameScreenContent(ui(config, g, selected = c("b6"), lastMove = LastMove(c("d5"), c("d6"))), {}, {}, animate = false) }
        }
    }

    @Test fun s3Khutlotharo() {
        val g = Rules.position(RuleSet.TRIANGLE, listOf(t("A"), t("CL"), t("BM")), listOf(t("C"), t("CR"), t("BR"))).copy(ply = 9)
        val config = GameConfig(Variant.TRIANGLE, GameMode.SOLO, Difficulty.MEDIUM, scene = Scene.KALAHARI)
        paparazzi.snapshot(name = "phone_3_khutlotharo") {
            MmeleTheme { GameScreenContent(ui(config, g, selected = t("A"), lastMove = LastMove(t("BL"), t("BR"))), {}, {}, animate = false) }
        }
    }

    @Test fun s4PieceSets() {
        val p1 = listOf("a7", "d7", "g7", "b4", "f2", "e4").map(::c)
        val p2 = listOf("a1", "d1", "g1", "b6", "e5", "c3", "f4").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2, hand1 = 2, hand2 = 3, mustShoot = true)
        val config = GameConfig(
            Variant.CLASSIC, GameMode.PASS_AND_PLAY, scene = Scene.OKAVANGO,
            styleOne = PieceStyle.Egg(EggKind.KORI_BUSTARD), styleTwo = PieceStyle.Egg(EggKind.OSTRICH),
        )
        paparazzi.snapshot(name = "phone_4_bird_eggs") {
            MmeleTheme { GameScreenContent(ui(config, g, lastMove = LastMove(null, c("g7"))), {}, {}, animate = false) }
        }
    }

    @Test fun s5Scene() {
        val p1 = listOf("a7", "d7", "g7", "b4", "f2", "e4").map(::c)
        val p2 = listOf("a1", "d1").map(::c)
        val g = Rules.position(RuleSet.CLASSIC, p1, p2).copy(result = GameResult.Win(Player.ONE, WinReason.REDUCED_TO_TWO), ply = 87, shotBy1 = 10)
        val config = GameConfig(Variant.CLASSIC, GameMode.SOLO, Difficulty.HARD, scene = Scene.TSODILO, styleOne = PieceStyle.SmokyDiamond, styleTwo = PieceStyle.ClearDiamond)
        paparazzi.snapshot(name = "phone_5_scene_tsodilo") {
            MmeleTheme {
                Box(Modifier.fillMaxSize()) {
                    GameScreenContent(ui(config, g), {}, {}, animate = false)
                    GameOverCard(ui(config, g), {}, {}, Modifier.align(Alignment.Center).padding(24.dp))
                }
            }
        }
    }

    @Test fun s6OnlineRoomCode() {
        val g = Rules.newGame(RuleSet.CLASSIC)
        val config = GameConfig(Variant.CLASSIC, GameMode.ONLINE, human = Player.ONE, scene = Scene.MAKGADIKGADI, styleOne = PieceStyle.SmokyDiamond, styleTwo = PieceStyle.ClearDiamond)
        val state = ui(config, g).copy(canUndo = false, online = OnlineUi(code = "K7M3PQ", waitingForOpponent = true, opponentConnected = false))
        paparazzi.snapshot(name = "phone_6_online_room_code") { MmeleTheme { GameScreenContent(state, {}, {}, animate = false) } }
    }

    /** Not a store asset: the About screen, for review. */
    @Test fun about() = paparazzi.snapshot(name = "about") {
        MmeleTheme {
            AboutScreen(
                versionName = "1.0.0", versionCode = 1, onBack = {}, onDeleteOnlineData = { bw.mmele.online.DeleteResult(1, true) },
                licences = listOf(
                    LicenceEntry("Jetpack Compose UI", "1.7.6", "Apache License 2.0", null, "https://developer.android.com/jetpack/compose"),
                    LicenceEntry("Firebase Realtime Database", "21.0.0", "Apache License 2.0", null, "https://firebase.google.com"),
                ),
            )
        }
    }
}
