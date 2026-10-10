package bw.mmele.ui.game

import androidx.lifecycle.SavedStateHandle
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
import bw.mmele.engine.DrawReason
import bw.mmele.online.MoveRecord
import bw.mmele.online.OfferKind
import bw.mmele.online.OnlineSession
import bw.mmele.online.RoomPhase
import bw.mmele.online.RoomRules
import bw.mmele.online.RoomSnapshot
import bw.mmele.online.SubmitResult
import bw.mmele.online.other
import bw.mmele.online.toPlayer
import bw.mmele.online.toSeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    /** Set for online games: the room you're seated in. */
    private val online: OnlineSession? = null,
    private val now: () -> Long = System::currentTimeMillis,
    /** Offline game to continue (from [GameRecord.decode]); its config must equal [config]. */
    private val resume: GameRecord? = null,
    /** Survives process death; the current game is mirrored here after every change. */
    private val savedState: SavedStateHandle? = null,
    /** Persists the in-progress offline game across app restarts; null = nothing to continue. */
    private val onSaveGame: (GameRecord?) -> Unit = {},
    /** Called once when a game ends (for stats). */
    private val onResult: (GameConfig, GameResult) -> Unit = { _, _ -> },
) : ViewModel() {

    /** State before an action, with both clocks at that moment: undo restores all of it. */
    private data class Snapshot(
        val game: GameState,
        val tokens: List<TokenUi>,
        val lastMove: LastMove?,
        val nextId: Int,
        val clockMs1: Long,
        val clockMs2: Long,
        val action: Action,
    )

    private val rules = RuleSet.of(config.variant)
    private val history = ArrayList<Snapshot>()
    private var nextTokenId = 1
    private var flashId = 0
    private var aiJob: Job? = null
    private var clockJob: Job? = null
    private var gameOverJob: Job? = null
    private var ai: AiPlayer? = null

    // Online bookkeeping: moves we have applied locally, in order, and the last room we saw.
    private val localMoves = ArrayList<MoveRecord>()
    private var room: RoomSnapshot? = online?.room
    private var pendingSubmits = 0
    private val submitLock = Mutex()
    private var message: String? = null
    private var resultReported = false
    private var clockPaused = false
    /** Whose clock ran out, when the game ended on time (not part of the move list). */
    private var timedOut: Player? = null

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<FeedbackEvent> = _events

    init {
        if (online == null && resume != null) restore(resume)
        startClock()
        maybeRunAi()
        if (online != null) startOnline(online)
    }

    private fun initialState(): GameUiState {
        val game = Rules.newGame(rules)
        val clock = config.clockMinutes * 60_000L
        val ui = GameUiState(config = config, game = game, tokens = emptyList(), clockMs1 = clock, clockMs2 = clock)
        return decorate(if (online != null) ui.copy(online = onlineUi(online.room)) else ui)
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.NodeTapped -> onTap(intent.node)
            is GameIntent.DragStarted -> onDragStart(intent.node)
            is GameIntent.Dropped -> onDrop(intent.from, intent.to)
            GameIntent.Undo -> undo()
            GameIntent.Rematch -> rematch()
            GameIntent.DismissGameOver -> _state.update { it.copy(showGameOver = false) }
            GameIntent.Resign -> onlineOp { repo, code, seat -> repo.resign(code, seat) }
            GameIntent.OfferDraw -> onlineOp { repo, code, seat -> repo.offerDraw(code, seat) }
            GameIntent.ClaimAbandoned -> onlineOp { repo, code, seat -> repo.claimAbandoned(code, seat) }
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

    private fun commit(action: Action, fromRoom: MoveRecord? = null) {
        val ui = _state.value
        val g = ui.game
        if (g.isGameOver || !Rules.isLegal(g, action)) { invalid(); return }
        if (online == null) history += Snapshot(g, ui.tokens, ui.lastMove, nextTokenId, ui.clockMs1, ui.clockMs2, action)
        val mover = g.toMove
        val next = Rules.apply(g, action)
        if (online != null) {
            localMoves += fromRoom ?: MoveRecord(localMoves.size, mover.toSeat(), action)
            if (fromRoom == null) submit(action)
        }

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
        if (next.isGameOver) onGameOver(next) else { persist(); maybeRunAi() }
    }

    private fun onGameOver(g: GameState) {
        clockJob?.cancel()
        val r = g.result
        if (r != null && !resultReported) { resultReported = true; onResult(config, r) }
        persist()
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
        if (config.mode == GameMode.ONLINE) return null
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
        timedOut = null
        while (history.size > idx) history.removeAt(history.lastIndex)
        nextTokenId = snap.nextId
        _state.update {
            decorate(
                it.copy(
                    game = snap.game, tokens = snap.tokens, lastMove = snap.lastMove, selected = null,
                    millFlash = null, shotFx = null, aiThinking = false, showGameOver = false,
                    // Clock-aware: time goes back to what it was, so undoing a timeout really resumes play.
                    clockMs1 = snap.clockMs1, clockMs2 = snap.clockMs2,
                ),
            )
        }
        emit(FeedbackEvent.SELECT)
        persist()
        if (clockJob?.isActive != true) startClock()
        maybeRunAi()
    }

    private fun rematch() {
        if (online != null) { onlineOp { repo, code, seat -> repo.offerRematch(code, seat) }; return }
        aiJob?.cancel()
        gameOverJob?.cancel()
        history.clear()
        nextTokenId = 1
        resultReported = false
        timedOut = null
        _state.value = initialState()
        persist()
        startClock()
        maybeRunAi()
    }

    // ------------------------------------------------------------------ clock

    private fun startClock() {
        clockJob?.cancel()
        if (config.clockMinutes <= 0 || clockPaused || _state.value.game.isGameOver) return
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
                    timedOut = p
                    val over = Rules.forfeit(ui.game, p, WinReason.TIMEOUT)
                    _state.update { decorate(it.copy(game = over, selected = null, aiThinking = false)) }
                    onGameOver(over)
                    break
                }
            }
        }
    }

    /** The app went to the background: stop the clocks (no one is looking) and save. */
    fun onHidden() {
        if (online != null) return
        clockPaused = true
        clockJob?.cancel()
        persist()
    }

    fun onShown() {
        if (online != null || !clockPaused) return
        clockPaused = false
        startClock()
    }

    // ------------------------------------------------------------------ persistence

    /** The current offline game as a [GameRecord] (null online). */
    fun record(): GameRecord? {
        if (online != null) return null
        val ui = _state.value
        return GameRecord(
            config = config,
            actions = history.map { it.action },
            clocksBefore = history.map { it.clockMs1 to it.clockMs2 },
            clockMs1 = ui.clockMs1,
            clockMs2 = ui.clockMs2,
            timedOut = timedOut?.takeIf { (ui.game.result as? GameResult.Win)?.reason == WinReason.TIMEOUT },
        )
    }

    /** Mirror to SavedStateHandle (process death) and hand to [onSaveGame] (app restarts). */
    private fun persist() {
        val rec = record() ?: return
        savedState?.set(SAVED_KEY, rec.encode())
        // Only unfinished games are offered as "Continue game"; a fresh board isn't worth saving.
        onSaveGame(rec.takeIf { !_state.value.game.isGameOver && rec.actions.isNotEmpty() })
    }

    /** Rebuild history, tokens and clocks by replaying a record without any feedback. */
    private fun restore(rec: GameRecord) {
        var g = Rules.newGame(rules)
        var tokens = emptyList<TokenUi>()
        var last: LastMove? = null
        nextTokenId = 1
        history.clear()
        for ((i, a) in rec.actions.withIndex()) {
            if (g.isGameOver || !Rules.isLegal(g, a)) break
            val (c1, c2) = rec.clocksBefore[i]
            history += Snapshot(g, tokens, last, nextTokenId, c1, c2, a)
            val mover = g.toMove
            when (a) {
                is Action.Place -> { tokens = tokens + TokenUi(nextTokenId++, mover, a.to); last = LastMove(null, a.to) }
                is Action.Move -> { tokens = tokens.map { if (it.node == a.from) it.copy(node = a.to) else it }; last = LastMove(a.from, a.to) }
                is Action.Shoot -> tokens = tokens.filterNot { it.node == a.target }
            }
            g = Rules.apply(g, a)
        }
        if (rec.timedOut != null && !g.isGameOver) { g = Rules.forfeit(g, rec.timedOut, WinReason.TIMEOUT); timedOut = rec.timedOut }
        // A finished game was already counted in the stats before it was saved.
        resultReported = g.isGameOver
        _state.value = decorate(
            _state.value.copy(game = g, tokens = tokens, lastMove = last, clockMs1 = rec.clockMs1, clockMs2 = rec.clockMs2,
                showGameOver = g.isGameOver),
        )
        savedState?.set(SAVED_KEY, rec.encode())
    }

    override fun onCleared() {
        aiJob?.cancel()
        clockJob?.cancel()
        online?.let { s ->
            // Leaving the screen marks you away; the opponent sees it and the abandon timer starts.
            CoroutineScope(Dispatchers.IO).launch(NonCancellable) {
                // The host removes a finished room; otherwise just mark this seat away.
                runCatching { s.repo.leaveRoom(s.room.code, s.seat, room?.phase ?: s.room.phase) }
                s.repo.close()
            }
        }
    }

    // ------------------------------------------------------------------ online

    private fun startOnline(s: OnlineSession) {
        if (s.room.moves.isNotEmpty()) rebuild(s.room) // reconnecting into a game in progress
        viewModelScope.launch {
            s.repo.observe(s.room.code)
                .catch { e -> message = "Connection problem: ${e.message ?: "unknown"}"; refreshOnline() }
                .collect { onRoom(it) }
        }
    }

    private var countdownJob: Job? = null

    private fun onlineOp(op: suspend (bw.mmele.online.OnlineGameRepository, String, bw.mmele.online.Seat) -> SubmitResult) {
        val s = online ?: return
        viewModelScope.launch {
            val r = runCatching { submitLock.withLock { op(s.repo, s.room.code, s.seat) } }
                .getOrElse { SubmitResult.Rejected(it.message ?: "network error") }
            if (r is SubmitResult.Rejected) { message = "Not sent: ${r.reason}"; refreshOnline() }
            else { message = null; refreshOnline() }
        }
    }

    private fun submit(action: Action) {
        val s = online ?: return
        pendingSubmits++
        viewModelScope.launch {
            val r = runCatching { submitLock.withLock { s.repo.submitMove(s.room.code, s.seat, action) } }
                .getOrElse { SubmitResult.Rejected(it.message ?: "network error") }
            pendingSubmits--
            if (r is SubmitResult.Rejected) {
                message = "Move not accepted (${r.reason}) — board resynced"
                room?.let { rebuild(it) }
            }
            refreshOnline()
        }
    }

    /** Apply a room snapshot from the server. */
    private fun onRoom(r: RoomSnapshot) {
        val s = online ?: return
        room = r
        if (r.phase == RoomPhase.LOBBY) { refreshOnline(); return }
        val common = minOf(r.moves.size, localMoves.size)
        val diverged = (0 until common).any { r.moves[it] != localMoves[it] } ||
            (r.moves.size < localMoves.size && pendingSubmits == 0)
        if (diverged) {
            rebuild(r)
        } else {
            for (i in localMoves.size until r.moves.size) {
                val m = r.moves[i]
                val g = _state.value.game
                if (g.isGameOver || g.toMove != m.seat.toPlayer() || !Rules.isLegal(g, m.action)) { corrupt(); return }
                commit(m.action, fromRoom = m)
            }
        }
        applyRoomResult(r, s)
        refreshOnline()
    }

    /** Resignation, abandonment and agreed draws end the game outside the move list. */
    private fun applyRoomResult(r: RoomSnapshot, s: OnlineSession) {
        val g = _state.value.game
        if (g.result != null) return
        val res = RoomRules.resultOf(r) ?: return
        val outOfBand = r.resigned != null || (res as? GameResult.Draw)?.reason == DrawReason.AGREEMENT
        if (!outOfBand) return
        val over = g.copy(result = res)
        _state.update { decorate(it.copy(game = over, selected = null)) }
        onGameOver(over)
    }

    /** Rebuild the whole board from the room (rematch, rejection, or reconnect). */
    private fun rebuild(r: RoomSnapshot) {
        gameOverJob?.cancel()
        var g = Rules.newGame(rules)
        var tokens = emptyList<TokenUi>()
        var last: LastMove? = null
        nextTokenId = 1
        for (m in r.moves) {
            if (g.toMove != m.seat.toPlayer() || !Rules.isLegal(g, m.action)) { corrupt(); return }
            val mover = g.toMove
            when (val a = m.action) {
                is Action.Place -> { tokens = tokens + TokenUi(nextTokenId++, mover, a.to); last = LastMove(null, a.to) }
                is Action.Move -> { tokens = tokens.map { if (it.node == a.from) it.copy(node = a.to) else it }; last = LastMove(a.from, a.to) }
                is Action.Shoot -> tokens = tokens.filterNot { it.node == a.target }
            }
            g = Rules.apply(g, m.action)
        }
        localMoves.clear(); localMoves.addAll(r.moves)
        history.clear()
        _state.update {
            decorate(it.copy(game = g, tokens = tokens, lastMove = last, selected = null, millFlash = null, shotFx = null, showGameOver = false))
        }
        if (g.isGameOver) onGameOver(g)
    }

    private fun corrupt() {
        val s = online ?: return
        message = "The other phone sent a move the rules don't allow. Game stopped."
        val g = _state.value.game
        if (g.result == null) {
            val over = Rules.forfeit(g, s.seat.other.toPlayer(), WinReason.RESIGNATION)
            _state.update { decorate(it.copy(game = over, selected = null)) }
            onGameOver(over)
        }
        refreshOnline()
    }

    private fun refreshOnline() {
        val r = room ?: return
        val ui = onlineUi(r)
        _state.update { decorate(it.copy(online = ui)) }
        // Tick the "opponent disconnected" countdown only while it matters.
        if (ui.abandonSecondsLeft != null && ui.abandonSecondsLeft > 0 && countdownJob?.isActive != true) {
            countdownJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000)
                    val left = room?.let { RoomRules.abandonCountdown(it, online!!.seat, now()) }
                    refreshOnlineState()
                    if (left == null || left <= 0) break
                }
            }
        }
    }

    private fun refreshOnlineState() {
        val r = room ?: return
        _state.update { decorate(it.copy(online = onlineUi(r))) }
    }

    private fun onlineUi(r: RoomSnapshot): bw.mmele.ui.game.OnlineUi {
        val seat = online!!.seat
        val opp = seat.other
        val left = RoomRules.abandonCountdown(r, seat, now())
        return OnlineUi(
            code = r.code,
            waitingForOpponent = r.phase == RoomPhase.LOBBY,
            opponentConnected = opp in r.connected,
            iOfferedDraw = r.phase == RoomPhase.PLAYING && r.offers.any { it.seat == seat && it.kind == OfferKind.DRAW },
            opponentOffersDraw = r.phase == RoomPhase.PLAYING && r.offers.any { it.seat == opp && it.kind == OfferKind.DRAW },
            iOfferedRematch = r.offers.any { it.seat == seat && it.kind == OfferKind.REMATCH },
            opponentOffersRematch = r.offers.any { it.seat == opp && it.kind == OfferKind.REMATCH },
            abandonSecondsLeft = left?.let { ((it + 999) / 1000).toInt() },
            syncing = pendingSubmits > 0,
            message = message,
        )
    }

    companion object {
        const val SAVED_KEY = "mmele.game"

        /**
         * Factory used by the app: a record in [savedState] (process death) wins over [resume]
         * (Continue game); the game's config comes from whichever record is used.
         */
        fun create(
            config: GameConfig,
            savedState: SavedStateHandle?,
            resume: GameRecord?,
            onSaveGame: (GameRecord?) -> Unit,
            onResult: (GameConfig, GameResult) -> Unit,
        ): GameViewModel {
            val rec = GameRecord.decode(savedState?.get<String>(SAVED_KEY)) ?: resume
            return GameViewModel(rec?.config ?: config, resume = rec, savedState = savedState, onSaveGame = onSaveGame, onResult = onResult)
        }

        /** Whether a rule set shoots (classic) or wins (triangle) on a line. Exposed for the UI copy. */
        fun shootsOnMill(config: GameConfig) = RuleSet.of(config.variant).millEffect == MillEffect.SHOOT
    }
}
