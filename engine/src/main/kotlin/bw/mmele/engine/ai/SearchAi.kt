package bw.mmele.engine.ai

import bw.mmele.engine.Action
import bw.mmele.engine.GameResult
import bw.mmele.engine.GameState
import bw.mmele.engine.Rules
import kotlin.random.Random

data class SearchConfig(
    val maxDepth: Int,
    val timeLimitMs: Long,
    val useTranspositionTable: Boolean,
    val evaluator: Evaluator,
)

/**
 * Negamax with alpha-beta pruning and iterative deepening. Depth counts *turns*: a "must shoot"
 * follow-up belongs to the same turn and does not consume depth. With [SearchConfig.useTranspositionTable]
 * a Zobrist-keyed table stores bounds and best moves (Hard). A hard time cap guarantees the UI
 * never waits long; the best move of the last completed iteration is returned.
 */
class SearchAi(private val config: SearchConfig, private val random: Random) : AiPlayer {

    private class Timeout : RuntimeException(null, null, false, false)

    private val ttSize = 1 shl 18
    private val ttKeys = if (config.useTranspositionTable) LongArray(ttSize) else LongArray(0)
    private val ttData = if (config.useTranspositionTable) LongArray(ttSize) else LongArray(0)

    private var deadline = 0L
    private var nodes = 0L
    private var stop: () -> Boolean = { false }
    private var abortable = false

    // Move-ordering memory (reset per search): two "killer" quiet moves per ply that caused a
    // beta cut-off in a sibling, and a history score per action code.
    private val killers = IntArray(MAX_PLY * 2) { -1 }
    private val historyScore = IntArray(4096)

    /** Statistics of the last search, for tests / debugging. */
    var lastDepthCompleted = 0
        private set
    var lastNodes = 0L
        private set

    /**
     * True when the last search was not cut short by the time cap: it finished [SearchConfig.maxDepth],
     * proved a forced win/loss, or had only one legal move.
     */
    var lastSearchComplete = true
        private set

    /** Synchronized: the search keeps per-instance scratch state (TT, counters). */
    @Synchronized
    override fun chooseAction(state: GameState, shouldStop: () -> Boolean): Action {
        val rootActions = Rules.legalActions(state)
        require(rootActions.isNotEmpty()) { "no legal actions" }
        lastDepthCompleted = 0
        lastNodes = 0
        lastSearchComplete = true
        if (rootActions.size == 1) return rootActions[0]
        stop = shouldStop
        killers.fill(-1)
        historyScore.fill(0)
        deadline = System.nanoTime() + config.timeLimitMs * 1_000_000
        nodes = 0
        // Shuffle first so equal-valued moves vary from game to game.
        var ordered = order(state, rootActions.shuffled(random), -1, 0)
        var best = ordered[0]
        var complete = false
        for (depth in 1..config.maxDepth) {
            abortable = depth > 1
            try {
                var alpha = -INF
                var bestHere: Action? = null
                val scores = HashMap<Action, Int>()
                for (a in ordered) {
                    val child = Rules.applyTrusted(state, a)
                    val score = if (child.toMove == state.toMove && child.result == null) {
                        search(child, depth, alpha, INF, 1)
                    } else {
                        -search(child, depth - 1, -INF, -alpha, 1)
                    }
                    scores[a] = score
                    if (score > alpha || bestHere == null) { alpha = maxOf(alpha, score); bestHere = a }
                }
                best = bestHere!!
                lastDepthCompleted = depth
                ordered = ordered.sortedByDescending { scores[it] ?: -INF }
                if (depth == config.maxDepth || kotlin.math.abs(alpha) >= WIN - 1000) { complete = true; break } // done, or forced result
            } catch (_: Timeout) {
                break
            }
            if (System.nanoTime() > deadline) break
        }
        lastNodes = nodes
        lastSearchComplete = complete
        return best
    }

    private fun search(s: GameState, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        nodes++
        if (abortable && (nodes and 1023L) == 0L && (System.nanoTime() > deadline || stop())) throw Timeout()
        val result = s.result
        if (result != null) {
            return when (result) {
                is GameResult.Draw -> 0
                is GameResult.Win -> if (result.winner == s.toMove) WIN - ply else -WIN + ply
            }
        }
        if (depth <= 0 && !s.mustShoot) return config.evaluator.evaluate(s, s.toMove)

        var alpha = alphaIn
        var ttMove = -1
        var key = 0L
        var slot = 0
        if (config.useTranspositionTable) {
            key = Zobrist.hash(s)
            slot = (key ushr 46).toInt() and (ttSize - 1)
            if (ttKeys[slot] == key) {
                val d = ttData[slot]
                val eDepth = (d and 0xFF).toInt()
                val flag = ((d shr 8) and 0x3).toInt()
                val eScore = (d shr 32).toInt()
                ttMove = ((d shr 10) and 0xFFF).toInt() - 1
                if (eDepth >= depth && kotlin.math.abs(eScore) < WIN - 1000) {
                    when (flag) {
                        EXACT -> return eScore
                        LOWER -> if (eScore >= beta) return eScore
                        UPPER -> if (eScore <= alpha) return eScore
                    }
                }
            }
        }

        val actions = order(s, Rules.legalActions(s), ttMove, ply)
        var bestScore = -INF
        var bestMove = -1
        for (a in actions) {
            val child = Rules.applyTrusted(s, a)
            val score = if (child.toMove == s.toMove && child.result == null) {
                search(child, depth, alpha, beta, ply + 1) // shoot follow-up: same side, same depth
            } else {
                -search(child, depth - 1, -beta, -alpha, ply + 1)
            }
            if (score > bestScore) { bestScore = score; bestMove = a.encode() }
            if (score > alpha) alpha = score
            if (alpha >= beta) {
                if (a !is Action.Shoot && !Heuristics.formsMill(s, a)) rememberCutoff(a.encode(), ply, depth)
                break
            }
        }

        if (config.useTranspositionTable) {
            val flag = when {
                bestScore <= alphaIn -> UPPER
                bestScore >= beta -> LOWER
                else -> EXACT
            }
            ttKeys[slot] = key
            ttData[slot] = (bestScore.toLong() shl 32) or (((bestMove + 1).toLong() and 0xFFF) shl 10) or
                (flag.toLong() shl 8) or (depth.toLong() and 0xFF)
        }
        return bestScore
    }

    private fun rememberCutoff(code: Int, ply: Int, depth: Int) {
        if (ply < MAX_PLY) {
            val k = ply * 2
            if (killers[k] != code) { killers[k + 1] = killers[k]; killers[k] = code }
        }
        val h = historyScore[code] + depth * depth
        historyScore[code] = if (h > HISTORY_CAP) HISTORY_CAP else h
    }

    /**
     * Move ordering, best first: TT move, moves that complete a mill, shots of cows in an enemy
     * open two, moves that block an enemy mill, killer moves of this ply, then quiet moves by
     * history score and intersection value. Keys are computed once per move (not per comparison).
     */
    private fun order(s: GameState, actions: List<Action>, ttMove: Int, ply: Int): List<Action> {
        val n = actions.size
        if (n <= 1) return actions
        val k1 = if (ply < MAX_PLY) killers[ply * 2] else -1
        val k2 = if (ply < MAX_PLY) killers[ply * 2 + 1] else -1
        val keys = IntArray(n)
        for (i in 0 until n) {
            val a = actions[i]
            val code = a.encode()
            var k = 0
            if (code == ttMove) k += 8_000_000
            when (a) {
                is Action.Shoot -> {
                    if (Heuristics.isThreatCow(s, a.target)) k += 1_000_000
                    k += s.board.millsAt[a.target].size * 10
                }
                else -> {
                    when {
                        Heuristics.formsMill(s, a) -> k += 2_000_000
                        Heuristics.blocksMill(s, a) -> k += 1_000_000
                        code == k1 -> k += 600_000
                        code == k2 -> k += 500_000
                    }
                    val to = Heuristics.targetOf(a)
                    k += historyScore[code] + s.board.millsAt[to].size * 10 + s.board.neighbors[to].size
                }
            }
            keys[i] = k
        }
        // Insertion sort by key, descending: lists are short (<= ~70) and stable for equal keys,
        // which keeps the root shuffle meaningful.
        val idx = IntArray(n) { it }
        for (i in 1 until n) {
            val cur = idx[i]
            var j = i - 1
            while (j >= 0 && keys[idx[j]] < keys[cur]) { idx[j + 1] = idx[j]; j-- }
            idx[j + 1] = cur
        }
        return List(n) { actions[idx[it]] }
    }

    companion object {
        const val INF = 1_000_000_000
        const val WIN = 10_000_000
        private const val EXACT = 0
        private const val LOWER = 1
        private const val UPPER = 2
        private const val MAX_PLY = 64
        private const val HISTORY_CAP = 400_000
    }
}
