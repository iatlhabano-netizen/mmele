package bw.mmele.academy

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bw.mmele.engine.Action
import bw.mmele.engine.GameResult
import bw.mmele.engine.GameState
import bw.mmele.engine.NodeState
import bw.mmele.engine.Phase
import bw.mmele.engine.Player
import bw.mmele.engine.Rules
import bw.mmele.engine.WinReason
import bw.mmele.feedback.FeedbackEvent
import bw.mmele.ui.game.Interaction
import bw.mmele.ui.game.LastMove
import bw.mmele.ui.game.MillFlash
import bw.mmele.ui.game.ShotFx
import bw.mmele.ui.game.TokenUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Coach line under the board. */
enum class Coach { INTRO, YOUR_TURN, SHOOT_NOW, GOOD, WRONG, WRONG_PUNISHED, SOLVED }

/** A lesson is its taught position, then a practice position; only finishing both counts. */
enum class Stage { LESSON, PRACTICE }

@Immutable
data class LessonUiState(
    val game: GameState,
    val tokens: List<TokenUi>,
    val selected: Int? = null,
    val targets: Int = 0,
    val shootable: Int = 0,
    val draggable: Int = 0,
    val lastMove: LastMove? = null,
    val millFlash: MillFlash? = null,
    val shotFx: ShotFx? = null,
    val coach: Coach = Coach.INTRO,
    /** Number of hints revealed so far. */
    val hintsShown: Int = 0,
    /** Opponent animation / reset in progress: input is ignored. */
    val busy: Boolean = false,
    val solved: Boolean = false,
    val stage: Stage = Stage.LESSON,
) {
    /** Lesson position solved, practice still to do. */
    val readyForPractice: Boolean get() = solved && stage == Stage.LESSON
    val lessonComplete: Boolean get() = solved && stage == Stage.PRACTICE
    val interactive: Boolean get() = !busy && !solved && coach != Coach.INTRO
}

/** Drives one Academy lesson on top of [PuzzleSession]: input, animation of replies, feedback. */
class LessonViewModel(
    val lesson: Lesson,
    private val onSolved: () -> Unit = {},
    private val stepDelayMs: Long = 700,
) : ViewModel() {

    private var session = PuzzleSession(lesson.puzzle)
    private var nextId = 1
    private var flashId = 0
    private var job: Job? = null

    private val _state = MutableStateFlow(fresh(lesson.puzzle.start, Coach.INTRO))
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<FeedbackEvent> = _events

    private fun fresh(g: GameState, coach: Coach, hints: Int = 0, stage: Stage = currentStage): LessonUiState {
        nextId = 1
        val tokens = (0 until g.board.size).mapNotNull { n -> g.ownerAt(n)?.let { TokenUi(nextId++, it, n) } }
        return decorate(LessonUiState(game = g, tokens = tokens, coach = coach, hintsShown = hints, stage = stage))
    }

    private var currentStage = Stage.LESSON

    /** After the taught position: load the practice position (no hints there). */
    fun startPractice() {
        job?.cancel()
        currentStage = Stage.PRACTICE
        session = PuzzleSession(lesson.practice)
        _state.value = fresh(lesson.practice.start, Coach.YOUR_TURN, 0, Stage.PRACTICE)
    }

    private fun decorate(s: LessonUiState): LessonUiState {
        val g = s.game
        val live = !s.busy && !s.solved && g.toMove == lesson.puzzle.human && g.result == null
        val sel = s.selected?.takeIf { live && Rules.destinationsFrom(g, it) != 0 }
        return s.copy(
            selected = sel,
            targets = if (live) Interaction.targetsFor(g, sel) else 0,
            shootable = if (live) Interaction.shootableFor(g) else 0,
            draggable = if (live) Interaction.draggableFor(g) else 0,
        )
    }

    fun begin() = _state.update { decorate(it.copy(coach = Coach.YOUR_TURN)) }

    fun showHint() = _state.update {
        if (it.stage == Stage.PRACTICE) it else it.copy(hintsShown = (it.hintsShown + 1).coerceAtMost(lesson.hints.size))
    }

    fun restart() {
        job?.cancel()
        session.restart()
        _state.value = fresh(session.puzzle.start, Coach.YOUR_TURN, _state.value.hintsShown)
    }

    fun onTap(node: Int?) {
        val ui = _state.value
        if (!ui.interactive) return
        val g = ui.game
        if (node == null) { select(null); return }
        when {
            g.mustShoot -> if (ui.shootable and (1 shl node) != 0) play(Action.Shoot(node)) else invalid()
            g.handOf(g.toMove) > 0 -> if (g.at(node) == NodeState.EMPTY) play(Action.Place(node)) else invalid()
            else -> {
                val sel = ui.selected
                when {
                    sel != null && ui.targets and (1 shl node) != 0 -> play(Action.Move(sel, node))
                    node == sel -> select(null)
                    ui.draggable and (1 shl node) != 0 -> { select(node); emit(FeedbackEvent.SELECT) }
                    else -> { select(null); invalid() }
                }
            }
        }
    }

    fun onDragStart(node: Int) {
        val ui = _state.value
        if (!ui.interactive || ui.draggable and (1 shl node) == 0) return
        if (ui.selected != node) { select(node); emit(FeedbackEvent.SELECT) }
    }

    fun onDrop(from: Int, to: Int?) {
        val ui = _state.value
        if (!ui.interactive) return
        if (to != null && Rules.isLegal(ui.game, Action.Move(from, to))) play(Action.Move(from, to)) else select(null)
    }

    private fun select(n: Int?) = _state.update { decorate(it.copy(selected = n)) }
    private fun invalid() = emit(FeedbackEvent.INVALID)
    private fun emit(e: FeedbackEvent) { _events.tryEmit(e) }

    /** Show one action on the board (tokens, mill trace, shot fade) and play its sound. */
    private fun show(a: Action) {
        val ui = _state.value
        val g = ui.game
        val mover = g.toMove
        val next = Rules.apply(g, a)
        var tokens = ui.tokens
        var last = ui.lastMove
        var shot = ui.shotFx
        var flash = ui.millFlash
        when (a) {
            is Action.Place -> { tokens = tokens + TokenUi(nextId++, mover, a.to); last = LastMove(null, a.to) }
            is Action.Move -> { tokens = tokens.map { if (it.node == a.from) it.copy(node = a.to) else it }; last = LastMove(a.from, a.to) }
            is Action.Shoot -> { tokens = tokens.filterNot { it.node == a.target }; shot = ShotFx(++flashId, a.target, mover.opponent) }
        }
        val line = a !is Action.Shoot && (next.mustShoot || (next.result as? GameResult.Win)?.reason == WinReason.LINE_OF_THREE)
        if (line) {
            val to = if (a is Action.Place) a.to else (a as Action.Move).to
            flash = MillFlash(++flashId, Rules.millsThrough(next.board, next.maskOf(mover), to).map { next.board.mills[it].toList() }, mover)
        }
        emit(
            when {
                line -> FeedbackEvent.MILL
                a is Action.Shoot -> FeedbackEvent.SHOOT
                a is Action.Place -> FeedbackEvent.PLACE
                g.phaseOf(mover) == Phase.FLYING -> FeedbackEvent.FLY
                else -> FeedbackEvent.SLIDE
            },
        )
        _state.update { decorate(it.copy(game = next, tokens = tokens, lastMove = last, shotFx = shot, millFlash = flash, selected = null)) }
    }

    private fun play(a: Action) {
        show(a)
        when (val r = session.play(a)) {
            StepResult.Illegal -> invalid()
            StepResult.Continue -> _state.update { decorate(it.copy(coach = Coach.SHOOT_NOW)) }
            is StepResult.Good -> animate(r.reply) { _state.update { decorate(it.copy(coach = Coach.YOUR_TURN, busy = false)) } }
            is StepResult.Wrong -> {
                emit(FeedbackEvent.INVALID)
                val coach = if (r.refutation.isEmpty()) Coach.WRONG else Coach.WRONG_PUNISHED
                _state.update { it.copy(coach = coach, busy = true) }
                animate(r.refutation, coach) {
                    delay(stepDelayMs * 2)
                    val hints = _state.value.hintsShown
                    _state.value = fresh(r.backTo, coach, hints)
                }
            }
            is StepResult.Solved -> animate(r.reply) {
                emit(FeedbackEvent.WIN)
                _state.update { decorate(it.copy(coach = Coach.SOLVED, solved = true, busy = false)) }
                if (currentStage == Stage.PRACTICE) onSolved()
            }
        }
    }

    private fun animate(actions: List<Action>, coach: Coach = Coach.GOOD, then: suspend () -> Unit) {
        _state.update { decorate(it.copy(busy = true, coach = coach)) }
        job = viewModelScope.launch {
            for (a in actions) {
                delay(stepDelayMs)
                show(a)
            }
            then()
        }
    }

    val human: Player get() = lesson.puzzle.human
}
