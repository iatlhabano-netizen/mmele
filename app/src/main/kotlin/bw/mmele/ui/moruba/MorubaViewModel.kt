package bw.mmele.ui.moruba

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bw.mmele.engine.Player
import bw.mmele.engine.ai.Difficulty
import bw.mmele.engine.moruba.MorubaAi
import bw.mmele.engine.moruba.MorubaConfig
import bw.mmele.engine.moruba.MorubaEngine
import bw.mmele.engine.moruba.MorubaState
import bw.mmele.engine.moruba.SowStep
import bw.mmele.feedback.FeedbackEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Moruba setup: board size, stones, vs Computer (with level) or Pass & Play. */
data class MorubaSetup(
    val holes: Int = 8,
    val stones: Int = 2,
    val vsComputer: Boolean = true,
    val difficulty: Difficulty = Difficulty.MEDIUM,
) {
    val config get() = MorubaConfig(holes, stones)
}

data class MorubaUiState(
    val setup: MorubaSetup,
    /** The rules state (the position after the last completed turn). */
    val game: MorubaState,
    /** What the board shows; differs from [game] while a turn is being animated. */
    val pits: List<Int>,
    val inHand: Int = 0,
    /** Pit the sowing hand is over, while animating. */
    val active: Int? = null,
    val lastCapture: List<Int> = emptyList(),
    val animating: Boolean = false,
    val aiThinking: Boolean = false,
    val showGameOver: Boolean = false,
    val turns: Int = 0,
) {
    val human get() = Player.ONE
    val isHumanTurn get() = !game.isGameOver && !animating && (!setup.vsComputer || game.toMove == human)
    fun canTap(pit: Int) = isHumanTurn && MorubaEngine.isLegal(game, pit)
}

sealed interface MorubaIntent {
    data class Tap(val pit: Int) : MorubaIntent
    data object Rematch : MorubaIntent
    data object DismissGameOver : MorubaIntent
}

/** One-shot effect; [rate] is the sound pitch for stone drops (0.95-1.05). */
data class MorubaEffect(val event: FeedbackEvent, val rate: Float = 1f)

/**
 * MVVM + UDF like [bw.mmele.ui.game.GameViewModel]: intents in, [state] out, sounds/haptics as
 * one-shot [effects]. A turn is computed in one go by [MorubaEngine.play] and then replayed step by
 * step in a coroutine so every stone visibly drops (with a tick and a drop sound).
 */
class MorubaViewModel(
    private val setup: MorubaSetup,
    private val aiDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val stepDelayMs: Long = 140,
    private val aiFactory: (Difficulty) -> MorubaAi = { MorubaAi.create(it) },
    private val random: Random = Random.Default,
    /** Called once when a game ends (the app draws the next Botswana fact here). */
    private val onGameOver: (winner: Player) -> Unit = {},
) : ViewModel() {
    private val _state = MutableStateFlow(fresh())
    val state: StateFlow<MorubaUiState> = _state.asStateFlow()
    private val _effects = MutableSharedFlow<MorubaEffect>(extraBufferCapacity = 64)
    val effects: SharedFlow<MorubaEffect> = _effects.asSharedFlow()
    private val ai by lazy { aiFactory(setup.difficulty) }
    private var job: Job? = null

    private fun fresh(): MorubaUiState {
        val g = MorubaState.initial(setup.config)
        return MorubaUiState(setup, g, g.pits)
    }

    fun onIntent(i: MorubaIntent) {
        when (i) {
            is MorubaIntent.Tap -> {
                val s = _state.value
                if (!s.canTap(i.pit)) { if (s.isHumanTurn) _effects.tryEmit(MorubaEffect(FeedbackEvent.INVALID)); return }
                play(i.pit)
            }
            MorubaIntent.Rematch -> { job?.cancel(); _state.value = fresh() }
            MorubaIntent.DismissGameOver -> _state.update { it.copy(showGameOver = false) }
        }
    }

    private fun play(pit: Int) {
        job = viewModelScope.launch {
            val before = _state.value.game
            val turn = MorubaEngine.play(before, pit)
            _state.update { it.copy(animating = true, lastCapture = emptyList()) }
            val pits = before.pits.toMutableList()
            var hand = 0
            for (step in turn.steps) {
                when (step) {
                    is SowStep.PickUp -> { hand += step.count; pits[step.pit] = 0; _effects.tryEmit(MorubaEffect(FeedbackEvent.SELECT)) }
                    is SowStep.Drop -> {
                        hand--; pits[step.pit]++
                        _effects.tryEmit(MorubaEffect(FeedbackEvent.DROP, 0.95f + random.nextFloat() * 0.1f))
                    }
                    is SowStep.Capture -> { step.pits.forEach { pits[it] = 0 }; _effects.tryEmit(MorubaEffect(FeedbackEvent.SHOOT)) }
                    SowStep.StepCapReached -> Unit
                }
                val active = when (step) { is SowStep.PickUp -> step.pit; is SowStep.Drop -> step.pit; else -> _state.value.active }
                val cap = (step as? SowStep.Capture)?.pits ?: _state.value.lastCapture
                _state.update { it.copy(pits = pits.toList(), inHand = hand, active = active, lastCapture = cap) }
                delay(if (step is SowStep.Capture) stepDelayMs * 3 else stepDelayMs)
            }
            val next = turn.result
            _state.update { it.copy(game = next, pits = next.pits, inHand = 0, active = null, animating = false, turns = it.turns + 1) }
            val w = next.winner
            if (w != null) {
                val humanWon = !setup.vsComputer || w == Player.ONE
                _effects.tryEmit(MorubaEffect(if (humanWon) FeedbackEvent.WIN else FeedbackEvent.END))
                onGameOver(w)
                delay(900)
                _state.update { it.copy(showGameOver = true) }
            } else if (setup.vsComputer && next.toMove != Player.ONE) {
                _state.update { it.copy(aiThinking = true) }
                val start = System.nanoTime()
                val m = withContext(aiDispatcher) { ai.choose(next) }
                val waited = (System.nanoTime() - start) / 1_000_000
                if (waited < 400) delay(400 - waited)
                _state.update { it.copy(aiThinking = false) }
                if (m != null) play(m)
            }
        }
    }
}
