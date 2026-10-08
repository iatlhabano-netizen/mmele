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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import bw.mmele.engine.Player
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Difficulty
import bw.mmele.feedback.FeedbackController
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.game.GameRoute
import bw.mmele.ui.game.GameViewModel
import bw.mmele.ui.home.HomeScreen
import bw.mmele.ui.scenes.Scene
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.online.OnlineGameRepository
import bw.mmele.online.OnlineSession
import bw.mmele.ui.online.OnlineLobbyScreen
import androidx.compose.runtime.remember
import bw.mmele.ui.theme.MmeleTheme

private enum class Screen { HOME, GAME, LOBBY }

/**
 * Top-level app: a tiny navigator (Home, Game, Online lobby). [onlineRepo] makes a fresh
 * transport per online visit, or returns null when online play isn't configured in this build.
 */
@Composable
fun MmeleApp(
    feedback: FeedbackController?,
    onlineRepo: () -> OnlineGameRepository? = { null },
    onShareCode: (String) -> Unit = {},
) {
    MmeleTheme {
        var variant by rememberSaveable { mutableStateOf(Variant.CLASSIC) }
        var mode by rememberSaveable { mutableStateOf(GameMode.PASS_AND_PLAY) }
        var difficulty by rememberSaveable { mutableStateOf(Difficulty.MEDIUM) }
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
        if (screen == Screen.GAME && config.mode == GameMode.ONLINE && onlineSession == null) screen = Screen.HOME

        AnimatedContent(targetState = screen, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "nav") { target ->
            when (target) {
            Screen.GAME -> {
                val s = onlineSession
                val gameConfig = if (s != null) s.configFor(scene) else config
                val vm: GameViewModel = viewModel(key = "game-$session") { GameViewModel(gameConfig, online = s) }
                GameRoute(vm, feedback, onHome = { screen = Screen.HOME; onlineSession = null }, onShareCode = onShareCode)
            }
            Screen.LOBBY -> OnlineLobbyScreen(
                config = config,
                repo = lobbyRepo,
                onSession = { onlineSession = it; session++; screen = Screen.GAME },
                onBack = { screen = Screen.HOME },
            )
            Screen.HOME -> {
                HomeScreen(
                    config = config,
                    onConfigChange = {
                        variant = it.variant; mode = it.mode; difficulty = it.difficulty; human = it.human; clock = it.clockMinutes; scene = it.scene
                        styleOneId = it.styleOne.id; styleTwoId = it.styleTwo.id
                    },
                    soundOn = sound,
                    hapticsOn = haptics,
                    onSoundChange = { sound = it },
                    onHapticsChange = { haptics = it },
                    onPlay = { onlineSession = null; session++; screen = Screen.GAME },
                    onPlayOnline = { lobbyRepo = onlineRepo(); screen = Screen.LOBBY },
                )
            }
            }
        }
    }
}
