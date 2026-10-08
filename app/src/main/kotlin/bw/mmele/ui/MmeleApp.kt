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
import bw.mmele.ui.theme.MmeleTheme

/** Top-level app: a tiny two-destination navigator (Home <-> Game). */
@Composable
fun MmeleApp(feedback: FeedbackController?) {
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
        var inGame by rememberSaveable { mutableStateOf(false) }
        feedback?.soundEnabled = sound
        feedback?.hapticsEnabled = haptics
        val config = GameConfig(variant, mode, difficulty, human, clock, scene)

        AnimatedContent(targetState = inGame, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "nav") { game ->
            if (game) {
                val vm: GameViewModel = viewModel(key = "game-$session") { GameViewModel(config) }
                GameRoute(vm, feedback, onHome = { inGame = false })
            } else {
                HomeScreen(
                    config = config,
                    onConfigChange = {
                        variant = it.variant; mode = it.mode; difficulty = it.difficulty; human = it.human; clock = it.clockMinutes; scene = it.scene
                    },
                    soundOn = sound,
                    hapticsOn = haptics,
                    onSoundChange = { sound = it },
                    onHapticsChange = { haptics = it },
                    onPlay = { session++; inGame = true },
                )
            }
        }
    }
}
