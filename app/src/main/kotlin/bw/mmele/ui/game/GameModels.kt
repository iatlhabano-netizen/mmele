package bw.mmele.ui.game

import bw.mmele.engine.IllegalReason

import androidx.compose.runtime.Immutable
import bw.mmele.engine.Action
import bw.mmele.engine.GameState
import bw.mmele.engine.Player
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.scenes.Scene
import bw.mmele.ui.pieces.PieceStyle

enum class GameMode { PASS_AND_PLAY, SOLO, ONLINE }

@Immutable
data class GameConfig(
    val variant: Variant = Variant.CLASSIC,
    val mode: GameMode = GameMode.SOLO,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    /** In solo mode, which side the human plays (ONE = dark, moves first). */
    val human: Player = Player.ONE,
    /** Chess-clock minutes per player; 0 = no clock. */
    val clockMinutes: Int = 0,
    /** Botswana destination illustrated behind the board. */
    val scene: Scene = Scene.OKAVANGO,
    val styleOne: PieceStyle = PieceStyle.Obsidian,
    val styleTwo: PieceStyle = PieceStyle.Quartz,
) {
    fun styleOf(player: bw.mmele.engine.Player) = if (player == bw.mmele.engine.Player.ONE) styleOne else styleTwo
}


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
    /** Non-null only in online games. */
    val online: OnlineUi? = null,
    /** Why the last tap/drop was refused; [IllegalNotice.id] changes so the same reason shows again. */
    val illegal: IllegalNotice? = null,
    /** The move the hint suggests for the side to move, until something is played. */
    val hint: Action? = null,
    val hinting: Boolean = false,
) {
    /** Hints are for offline games (vs Computer, Pass & Play) on a human turn. */
    val canHint: Boolean get() = online == null && isHumanTurn && !aiThinking
    val isHumanTurn: Boolean
        get() = !game.isGameOver && (config.mode == GameMode.PASS_AND_PLAY || game.toMove == config.human) &&
            (online == null || !online.waitingForOpponent)

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
    data object Resign : GameIntent
    data object OfferDraw : GameIntent
    data object ClaimAbandoned : GameIntent
    data object ClaimIdle : GameIntent
    data object Hint : GameIntent
    data object DismissIllegal : GameIntent
}

@Immutable
data class IllegalNotice(val id: Int, val reason: IllegalReason)

/** Online-only status shown around the board. */
@Immutable
data class OnlineUi(
    val code: String,
    val waitingForOpponent: Boolean,
    val opponentConnected: Boolean,
    val iOfferedDraw: Boolean = false,
    val opponentOffersDraw: Boolean = false,
    val iOfferedRematch: Boolean = false,
    val opponentOffersRematch: Boolean = false,
    /** Seconds until you may claim the win because the opponent left; null while they're here. */
    val abandonSecondsLeft: Int? = null,
    /** Opponent's turn: seconds until you may claim because they haven't moved (0 = claim now). */
    val idleClaimSecondsLeft: Int? = null,
    /** Your turn: seconds before the opponent may claim the game; set only in the last two minutes. */
    val myIdleSecondsLeft: Int? = null,
    /** A move is on its way to the server. */
    val syncing: Boolean = false,
    val message: OnlineMessage? = null,
)

/** Transient online notice; [detail] is a technical reason from the network/server (not translated). */
@Immutable
data class OnlineMessage(val kind: Kind, val detail: String? = null) {
    enum class Kind { CONNECTION, NOT_SENT, REJECTED, CORRUPT }

    @androidx.compose.runtime.Composable
    fun text(): String {
        val d = detail ?: androidx.compose.ui.res.stringResource(com.tshipiworks.mmele.R.string.online_unknown_error)
        return when (kind) {
            Kind.CONNECTION -> androidx.compose.ui.res.stringResource(com.tshipiworks.mmele.R.string.online_msg_connection, d)
            Kind.NOT_SENT -> androidx.compose.ui.res.stringResource(com.tshipiworks.mmele.R.string.online_msg_not_sent, d)
            Kind.REJECTED -> androidx.compose.ui.res.stringResource(com.tshipiworks.mmele.R.string.online_msg_rejected, d)
            Kind.CORRUPT -> androidx.compose.ui.res.stringResource(com.tshipiworks.mmele.R.string.online_msg_corrupt)
        }
    }
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
