package bw.mmele.engine

/**
 * The rules engine: pure functions over immutable [GameState].
 *
 * State machine (per player to move):
 * ```
 *   PLACEMENT --(all cows placed)--> MOVEMENT --(down to 3 cows)--> FLYING
 *        \             |                  |                          |
 *         +-- mill --> [must shoot] <-----+--------------------------+
 *   any --(opponent <= 2 cows | opponent has no legal move | draw rule)--> GAME_OVER
 * ```
 * Invariant (fuzz-tested): a state that is not game-over always has at least one legal action.
 */
object Rules {

    fun newGame(rules: RuleSet, firstPlayer: Player = Player.ONE): GameState =
        GameState(rules, 0, 0, rules.cowsPerPlayer, rules.cowsPerPlayer, firstPlayer)

    /**
     * Build an arbitrary position (for tests, puzzles and the future Academy). The returned state
     * is "settled": if the player to move has already lost or the game is otherwise over, the
     * result is filled in.
     */
    fun position(
        rules: RuleSet,
        p1Nodes: Collection<Int>,
        p2Nodes: Collection<Int>,
        hand1: Int = 0,
        hand2: Int = 0,
        toMove: Player = Player.ONE,
        mustShoot: Boolean = false,
    ): GameState {
        val m1 = p1Nodes.fold(0) { a, n -> a or (1 shl n) }
        val m2 = p2Nodes.fold(0) { a, n -> a or (1 shl n) }
        require(m1 and m2 == 0) { "overlapping cows" }
        require(p1Nodes.all { it in 0 until rules.board.size } && p2Nodes.all { it in 0 until rules.board.size })
        val s = GameState(rules, m1, m2, hand1, hand2, toMove, mustShoot = mustShoot)
        return if (mustShoot) s else settle(s)
    }

    // ---------------------------------------------------------------- queries

    /** Indices of mills owned completely by [mask] that pass through [node]. */
    fun millsThrough(board: BoardGraph, mask: Int, node: Int): List<Int> =
        board.millsAt[node].filter { (board.millMasks[it] and mask) == board.millMasks[it] }

    fun formsMill(board: BoardGraph, mask: Int, node: Int): Boolean {
        for (m in board.millsAt[node]) {
            val mm = board.millMasks[m]
            if (mm and mask == mm) return true
        }
        return false
    }

    /** Bitmask of all of [mask]'s cows that currently sit in a complete mill. */
    fun cowsInMills(board: BoardGraph, mask: Int): Int {
        var r = 0
        for (mm in board.millMasks) if (mm and mask == mm) r = r or mm
        return r
    }

    /** All mills (as node triples) completely owned by [p]. */
    fun millsOf(state: GameState, p: Player): List<IntArray> {
        val mask = state.maskOf(p)
        return state.board.mills.filterIndexed { i, _ -> state.board.millMasks[i] and mask == state.board.millMasks[i] }
    }

    /**
     * Cows the mover may shoot: enemy cows not in a mill, or - if every enemy cow is in a mill -
     * any enemy cow.
     */
    fun shootableMask(state: GameState): Int {
        val enemy = state.maskOf(state.toMove.opponent)
        val free = enemy and cowsInMills(state.board, enemy).inv()
        return if (free != 0) free else enemy
    }

    /** Bitmask of nodes the cow at [from] may legally move to (respecting the remill ban). */
    fun destinationsFrom(state: GameState, from: Int): Int {
        if (state.result != null || state.mustShoot) return 0
        val p = state.toMove
        if (state.handOf(p) > 0 || state.maskOf(p) and (1 shl from) == 0) return 0
        var mask = 0
        for (a in legalActions(state)) if (a is Action.Move && a.from == from) mask = mask or (1 shl a.to)
        return mask
    }

    fun legalActions(state: GameState): List<Action> {
        if (state.result != null) return emptyList()
        val out = ArrayList<Action>(32)
        if (state.mustShoot) {
            forEachBit(shootableMask(state)) { out += Action.Shoot(it) }
            return out
        }
        val p = state.toMove
        val empty = state.empty
        if (state.handOf(p) > 0) {
            forEachBit(empty) { out += Action.Place(it) }
            return out
        }
        return rawMoves(state, p, empty, applyBan = true)
    }

    fun isLegal(state: GameState, action: Action): Boolean = action in legalActions(state)

    private fun rawMoves(state: GameState, p: Player, empty: Int, applyBan: Boolean): List<Action> {
        val out = ArrayList<Action>(32)
        val own = state.maskOf(p)
        val flying = state.isFlying(p)
        forEachBit(own) { from ->
            val dests = if (flying) empty else (state.board.neighborMask[from] and empty) or jumpDests(state, from, empty)
            forEachBit(dests) { to -> out += Action.Move(from, to) }
        }
        val ban = state.banOf(p)
        if (!applyBan || ban < 0 || out.isEmpty()) return out
        val cow = ban shr 8
        val dest = ban and 0xFF
        val filtered = out.filterNot {
            it is Action.Move && it.from == cow && it.to == dest &&
                formsMill(state.board, (own and (1 shl cow).inv()) or (1 shl dest), dest)
        }
        // Never let the ban create a blockade: if it removes the only move(s), the move stays legal.
        return filtered.ifEmpty { out }
    }

    private fun hasAnyLegalAction(state: GameState): Boolean {
        if (state.result != null) return false
        if (state.mustShoot) return state.maskOf(state.toMove.opponent) != 0
        val p = state.toMove
        val empty = state.empty
        if (empty == 0) return false
        if (state.handOf(p) > 0) return true
        val own = state.maskOf(p)
        if (own == 0) return false
        if (state.isFlying(p)) return true
        var reach = 0
        forEachBit(own) { reach = reach or state.board.neighborMask[it] or jumpDests(state, it, empty) }
        return reach and empty != 0
    }

    /** Non-flying destinations from [from]: adjacent empty nodes plus jump landings (if allowed). */
    fun stepDests(state: GameState, from: Int, empty: Int = state.empty): Int =
        (state.board.neighborMask[from] and empty) or jumpDests(state, from, empty)

    /** Empty nodes reachable from [from] by jumping over one adjacent occupied node (if the rules allow). */
    private fun jumpDests(state: GameState, from: Int, empty: Int): Int {
        if (!state.rules.jumpOverAdjacent) return 0
        var m = 0
        for (j in state.board.jumps[from]) {
            val over = j / 32
            val to = j % 32
            if (empty and (1 shl over) == 0 && empty and (1 shl to) != 0) m = m or (1 shl to)
        }
        return m
    }

    // ---------------------------------------------------------------- transitions

    /** Apply a legal action. Throws [IllegalArgumentException] for illegal actions. */
    fun apply(state: GameState, action: Action): GameState {
        require(state.result == null) { "game is over" }
        require(isLegal(state, action)) { "illegal action $action in phase ${state.phase} (mustShoot=${state.mustShoot})" }
        return applyTrusted(state, action)
    }

    /** Apply an action already known to be legal (used by the AI for speed). */
    fun applyTrusted(state: GameState, action: Action): GameState {
        val p = state.toMove
        val board = state.board
        val rules = state.rules
        when (action) {
            is Action.Shoot -> {
                val bit = 1 shl action.target
                val s = if (p == Player.ONE) {
                    state.copy(p2 = state.p2 and bit.inv(), shotBy1 = state.shotBy1 + 1)
                } else {
                    state.copy(p1 = state.p1 and bit.inv(), shotBy2 = state.shotBy2 + 1)
                }
                return settle(
                    s.copy(mustShoot = false, toMove = p.opponent, ply = state.ply + 1, idlePlies = 0, flyingPlies = 0),
                )
            }
            is Action.Place, is Action.Move -> {
                val own = state.maskOf(p)
                val to: Int
                val newOwn: Int
                var hand = state.handOf(p)
                var ban = state.banOf(p)
                var idle = state.idlePlies + 1
                var flyingPlies = state.flyingPlies
                if (action is Action.Place) {
                    to = action.to
                    newOwn = own or (1 shl to)
                    hand -= 1
                    idle = 0
                } else {
                    action as Action.Move
                    to = action.to
                    newOwn = (own and (1 shl action.from).inv()) or (1 shl to)
                    val brokeMill = formsMill(board, own, action.from)
                    val formed = formsMill(board, newOwn, to)
                    ban = if (rules.noImmediateRemill && brokeMill && formed) (to shl 8) or action.from else -1
                    val someoneFlying = rules.flyingAt > 0 &&
                        (state.isFlying(Player.ONE) || state.isFlying(Player.TWO))
                    flyingPlies = if (someoneFlying) flyingPlies + 1 else 0
                }
                val formed = formsMill(board, newOwn, to)
                var s = if (p == Player.ONE) {
                    state.copy(p1 = newOwn, hand1 = hand, ban1 = ban)
                } else {
                    state.copy(p2 = newOwn, hand2 = hand, ban2 = ban)
                }
                s = s.copy(ply = state.ply + 1, idlePlies = idle, flyingPlies = flyingPlies)
                if (formed) {
                    when (rules.millEffect) {
                        MillEffect.WIN -> return s.copy(toMove = p.opponent, result = GameResult.Win(p, WinReason.LINE_OF_THREE))
                        MillEffect.SHOOT -> if (s.maskOf(p.opponent) != 0) return s.copy(mustShoot = true)
                        // Opponent has no cow on the board to shoot (possible only early in placement): no shot.
                    }
                }
                return settle(s.copy(toMove = p.opponent))
            }
        }
    }

    /** Record a forfeit (timeout / resignation) by [loser]. */
    fun forfeit(state: GameState, loser: Player, reason: WinReason): GameState =
        if (state.result != null) state else state.copy(result = GameResult.Win(loser.opponent, reason))

    /** Check end-of-game conditions for the player now to move. */
    private fun settle(s: GameState): GameState {
        if (s.result != null || s.mustShoot) return s
        val x = s.toMove
        val r = s.rules
        val result: GameResult? = when {
            r.loseAtOrBelow >= 0 && s.totalCows(x) <= r.loseAtOrBelow -> GameResult.Win(x.opponent, WinReason.REDUCED_TO_TWO)
            r.loseAtOrBelow >= 0 && s.totalCows(x.opponent) <= r.loseAtOrBelow -> GameResult.Win(x, WinReason.REDUCED_TO_TWO)
            !hasAnyLegalAction(s) -> GameResult.Win(x.opponent, WinReason.BLOCKADE)
            r.flyingDrawPlies > 0 && s.flyingPlies >= r.flyingDrawPlies -> GameResult.Draw(DrawReason.FLYING_NO_SHOT)
            s.idlePlies >= r.idleDrawPlies -> GameResult.Draw(DrawReason.NO_PROGRESS)
            else -> null
        }
        return if (result != null) s.copy(result = result) else s
    }

    inline fun forEachBit(mask: Int, block: (Int) -> Unit) {
        var m = mask
        while (m != 0) {
            val bit = Integer.numberOfTrailingZeros(m)
            block(bit)
            m = m and (m - 1)
        }
    }
}
