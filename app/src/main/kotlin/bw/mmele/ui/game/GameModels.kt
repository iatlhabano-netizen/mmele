package bw.mmele.ui.game

import androidx.compose.runtime.Immutable
import bw.mmele.engine.Action
import bw.mmele.engine.GameState
import bw.mmele.engine.Player
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.scenes.Scene

enum class GameMode { PASS_AND_PLAY, SOLO }

@Immutable
data class GameConfig(
    val variant: Variant = Variant.CLASSIC,
    val mode: GameMode = GameMode.PASS_AND_PLAY,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    /** In solo mode, which side the human plays (ONE = dark, moves first). */
    val human: Player = Player.ONE,
    /** Chess-clock minutes per player; 0 = no clock. */
    val clockMinutes: Int = 0,
    /** Botswana destination illustrated behind the board. */
    val scene: Scene = Scene.OKAVANGO,
)

/** Everything the game screen renders. Produced only by [GameViewModel]. */
@Immutable
data class GameUiState(
    val config: GameConfig,
    val game: GameState,
    val tokens: List<TokenUi>,
    val selected: Int? = null,
    val targets: Int = 0,
    val shootable: Int = 0,
    val draggable: Int = 0,
    val lastMove: LastMove? = null,
    val millFlash: MillFlash? = null,
    val shotFx: ShotFx? = null,
    val aiThinking: Boolean = false,
    val clockMs1: Long = 0,
    val clockMs2: Long = 0,
    val canUndo: Boolean = false,
    val showGameOver: Boolean = false,
) {
    val isHumanTurn: Boolean
        get() = !game.isGameOver && (config.mode == GameMode.PASS_AND_PLAY || game.toMove == config.human)

    fun clockOf(p: Player) = if (p == Player.ONE) clockMs1 else clockMs2
}

/** User intents (UDF: the screen only ever sends these). */
sealed interface GameIntent {
    data class NodeTapped(val node: Int?) : GameIntent
    data class DragStarted(val node: Int) : GameIntent
    data class Dropped(val from: Int, val to: Int?) : GameIntent
    data object Undo : GameIntent
    data object Rematch : GameIntent
    data object DismissGameOver : GameIntent
}

/** Derive the interaction masks for a state (pure, shared by ViewModel and previews/screenshots). */
object Interaction {
    fun targetsFor(game: GameState, selected: Int?): Int = when {
        game.isGameOver || game.mustShoot -> 0
        game.handOf(game.toMove) > 0 -> game.empty
        selected != null -> Rules.destinationsFrom(game, selected)
        else -> 0
    }

    fun shootableFor(game: GameState): Int = if (game.mustShoot && !game.isGameOver) Rules.shootableMask(game) else 0

    fun draggableFor(game: GameState): Int {
        if (game.isGameOver || game.mustShoot || game.handOf(game.toMove) > 0) return 0
        var m = 0
        for (a in Rules.legalActions(game)) if (a is Action.Move) m = m or (1 shl a.from)
        return m
    }
}
