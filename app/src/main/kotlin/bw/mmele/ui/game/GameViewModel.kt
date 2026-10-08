package bw.mmele.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bw.mmele.engine.Action
import bw.mmele.engine.GameResult
import bw.mmele.engine.GameState
import bw.mmele.engine.MillEffect
import bw.mmele.engine.Phase
import bw.mmele.engine.Player
import bw.mmele.engine.RuleSet
import bw.mmele.engine.Rules
import bw.mmele.engine.WinReason
import bw.mmele.engine.ai.Ai
import bw.mmele.engine.ai.AiPlayer
import bw.mmele.feedback.FeedbackEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MVVM + unidirectional data flow: intents in via [onIntent], a single immutable [GameUiState]
 * out via [state], and one-shot feedback (sound / haptics) via [events].
 */
class GameViewModel(
    private val config: GameConfig,
    private val aiDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val aiFactory: (GameConfig) -> AiPlayer = { Ai.create(it.difficulty) },
    private val minAiDelayMs: Long = 450,
) : ViewModel() {

    private data class Snapshot(val game: GameState, val tokens: List<TokenUi>, val lastMove: LastMove?, val nextId: Int)

    private val rules = RuleSet.of(config.variant)
    private val history = ArrayList<Snapshot>()
    private var nextTokenId = 1
    private var flashId = 0
    private var aiJob: Job? = null
    private var clockJob: Job? = null
    private var gameOverJob: Job? = null
    private var ai: AiPlayer? = null

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<FeedbackEvent> = _events

    init {
        startClock()
        maybeRunAi()
    }

    private fun initialState(): GameUiState {
        val game = Rules.newGame(rules)
        val clock = config.clockMinutes * 60_000L
        return decorate(GameUiState(config = config, game = game, tokens = emptyList(), clockMs1 = clock, clockMs2 = clock))
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.NodeTapped -> onTap(intent.node)
            is GameIntent.DragStarted -> onDragStart(intent.node)
            is GameIntent.Dropped -> onDrop(intent.from, intent.to)
            GameIntent.Undo -> undo()
            GameIntent.Rematch -> rematch()
            GameIntent.DismissGameOver -> _state.update { it.copy(showGameOver = false) }
        }
    }

    // ------------------------------------------------------------------ input handling

    private fun onTap(node: Int?) {
        val ui = _state.value
        if (!ui.isHumanTurn) return
        val g = ui.game
        if (node == null) { select(null); return }
        when {
            g.mustShoot -> {
                if (ui.shootable and (1 shl node) != 0) commit(Action.Shoot(node)) else invalid()
            }
            g.handOf(g.toMove) > 0 -> {
                if (g.at(node) == bw.mmele.engine.NodeState.EMPTY) commit(Action.Place(node)) else invalid()
            }
            else -> {
                val sel = ui.selected
                when {
                    sel != null && ui.targets and (1 shl node) != 0 -> commit(Action.Move(sel, node))
                    node == sel -> select(null)
                    ui.draggable and (1 shl node) != 0 -> { select(node); emit(FeedbackEvent.SELECT) }
                    g.ownerAt(node) == g.toMove -> { select(null); invalid() } // own cow with no moves
                    else -> { if (sel != null) select(null) else invalid() }
                }
            }
        }
    }

    private fun onDragStart(node: Int) {
        val ui = _state.value
        if (!ui.isHumanTurn || ui.draggable and (1 shl node) == 0) return
        if (ui.selected != node) { select(node); emit(FeedbackEvent.SELECT) }
    }

    private fun onDrop(from: Int, to: Int?) {
        val ui = _state.value
        if (!ui.isHumanTurn) return
        if (to != null && Rules.isLegal(ui.game, Action.Move(from, to))) commit(Action.Move(from, to))
        else { select(null); if (to == null) Unit else invalid() }
    }

    private fun select(node: Int?) = _state.update { decorate(it.copy(selected = node)) }

    private fun invalid() = emit(FeedbackEvent.INVALID)

    private fun emit(e: FeedbackEvent) { _events.tryEmit(e) }

    // ------------------------------------------------------------------ core transition

    private fun commit(action: Action) {
        val ui = _state.value
        val g = ui.game
        if (g.isGameOver || !Rules.isLegal(g, action)) { invalid(); return }
        history += Snapshot(g, ui.tokens, ui.lastMove, nextTokenId)
        val mover = g.toMove
        val next = Rules.apply(g, action)

        var tokens = ui.tokens
        var lastMove = ui.lastMove
        var shot: ShotFx? = ui.shotFx
        var flash: MillFlash? = ui.millFlash
        when (action) {
            is Action.Place -> {
                tokens = tokens + TokenUi(nextTokenId++, mover, action.to)
                lastMove = LastMove(null, action.to)
            }
            is Action.Move -> {
                tokens = tokens.map { if (it.node == action.from) it.copy(node = action.to) else it }
                lastMove = LastMove(action.from, action.to)
            }
            is Action.Shoot -> {
                tokens = tokens.filterNot { it.node == action.target }
                shot = ShotFx(++flashId, action.target, mover.opponent)
            }
        }
        val formedLine = action !is Action.Shoot && (next.mustShoot ||
            (next.result as? GameResult.Win)?.reason == WinReason.LINE_OF_THREE)
        if (formedLine) {
            val to = if (action is Action.Place) action.to else (action as Action.Move).to
            val lines = Rules.millsThrough(next.board, next.maskOf(mover), to).map { next.board.mills[it].toList() }
            flash = MillFlash(++flashId, lines, mover)
        }

        // feedback
        when {
            formedLine -> emit(FeedbackEvent.MILL)
            action is Action.Shoot -> emit(FeedbackEvent.SHOOT)
            action is Action.Place -> emit(FeedbackEvent.PLACE)
            action is Action.Move -> emit(if (g.phaseOf(mover) == Phase.FLYING) FeedbackEvent.FLY else FeedbackEvent.SLIDE)
        }

        _state.update {
            decorate(
                it.copy(
                    game = next, tokens = tokens, selected = null, lastMove = lastMove,
                    millFlash = flash, shotFx = shot, canUndo = true,
                ),
            )
        }
        if (next.isGameOver) onGameOver(next) else maybeRunAi()
    }

    private fun onGameOver(g: GameState) {
        clockJob?.cancel()
        val r = g.result
        val humanWon = r is GameResult.Win && (config.mode == GameMode.PASS_AND_PLAY || r.winner == config.human)
        emit(if (humanWon) FeedbackEvent.WIN else FeedbackEvent.END)
        gameOverJob?.cancel()
        gameOverJob = viewModelScope.launch {
            delay(1100) // let the final animation land
            _state.update { it.copy(showGameOver = true) }
        }
    }

    /** Recompute derived masks for the current game/selection. */
    private fun decorate(ui: GameUiState): GameUiState {
        val g = ui.game
        val human = ui.isHumanTurn
        val sel = ui.selected?.takeIf { human && Rules.destinationsFrom(g, it) != 0 }
        return ui.copy(
            selected = sel,
            targets = if (human) Interaction.targetsFor(g, sel) else 0,
            shootable = if (human) Interaction.shootableFor(g) else 0,
            draggable = if (human) Interaction.draggableFor(g) else 0,
            canUndo = history.isNotEmpty() && undoTarget() != null,
        )
    }

    // ------------------------------------------------------------------ AI

    private fun maybeRunAi() {
        val ui = _state.value
        if (config.mode != GameMode.SOLO || ui.game.isGameOver || ui.game.toMove == config.human) return
        val snapshot = ui.game
        val player = ai ?: aiFactory(config).also { ai = it }
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(aiThinking = true) }
            val started = System.currentTimeMillis()
            val job = coroutineContext.job
            val action = withContext(aiDispatcher) { player.chooseAction(snapshot) { !job.isActive } }
            val elapsed = System.currentTimeMillis() - started
            val pause = if (snapshot.mustShoot) minAiDelayMs / 2 else minAiDelayMs
            if (elapsed < pause) delay(pause - elapsed)
            if (!isActive || _state.value.game !== snapshot) return@launch
            _state.update { it.copy(aiThinking = false) }
            commit(action)
        }
    }

    // ------------------------------------------------------------------ undo / rematch

    /** Index in [history] that undo would restore, or null. */
    private fun undoTarget(): Int? {
        if (history.isEmpty()) return null
        if (config.mode == GameMode.PASS_AND_PLAY) return history.lastIndex
        // Solo: rewind to the start of the human's previous turn (skipping AI moves and shot sub-steps).
        for (i in history.indices.reversed()) {
            val g = history[i].game
            if (g.toMove == config.human && !g.mustShoot) return i
        }
        return null
    }

    private fun undo() {
        val idx = undoTarget() ?: return
        aiJob?.cancel()
        gameOverJob?.cancel()
        val snap = history[idx]
        while (history.size > idx) history.removeAt(history.lastIndex)
        nextTokenId = snap.nextId
        _state.update {
            decorate(
                it.copy(
                    game = snap.game, tokens = snap.tokens, lastMove = snap.lastMove, selected = null,
                    millFlash = null, shotFx = null, aiThinking = false, showGameOver = false,
                ),
            )
        }
        emit(FeedbackEvent.SELECT)
        if (clockJob?.isActive != true) startClock()
        maybeRunAi()
    }

    private fun rematch() {
        aiJob?.cancel()
        gameOverJob?.cancel()
        history.clear()
        nextTokenId = 1
        _state.value = initialState()
        startClock()
        maybeRunAi()
    }

    // ------------------------------------------------------------------ clock

    private fun startClock() {
        clockJob?.cancel()
        if (config.clockMinutes <= 0) return
        clockJob = viewModelScope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(100)
                val now = System.currentTimeMillis()
                val dt = now - last
                last = now
                val ui = _state.value
                if (ui.game.isGameOver) break
                val p = ui.game.toMove
                val remaining = (ui.clockOf(p) - dt).coerceAtLeast(0)
                _state.update { if (p == Player.ONE) it.copy(clockMs1 = remaining) else it.copy(clockMs2 = remaining) }
                if (remaining == 0L) {
                    aiJob?.cancel()
                    val over = Rules.forfeit(ui.game, p, WinReason.TIMEOUT)
                    _state.update { decorate(it.copy(game = over, selected = null, aiThinking = false)) }
                    onGameOver(over)
                    break
                }
            }
        }
    }

    override fun onCleared() {
        aiJob?.cancel()
        clockJob?.cancel()
    }

    companion object {
        /** Whether a rule set shoots (classic) or wins (triangle) on a line. Exposed for the UI copy. */
        fun shootsOnMill(config: GameConfig) = RuleSet.of(config.variant).millEffect == MillEffect.SHOOT
    }
}
