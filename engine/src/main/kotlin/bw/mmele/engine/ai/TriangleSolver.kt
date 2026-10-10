package bw.mmele.engine.ai

import bw.mmele.engine.Action
import bw.mmele.engine.GameResult
import bw.mmele.engine.GameState
import bw.mmele.engine.Player
import bw.mmele.engine.RuleSet
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import kotlin.random.Random

/**
 * Khutlotharo solved exactly by retrograde analysis over every reachable position (a few
 * thousand: 7 points, 3 cows each), using the engine's own move generator so the rules can never
 * disagree with tools/solve_triangle.py. Built lazily once (~20 ms on a desktop JVM).
 *
 * Values ignore the 15-moves-each no-progress draw; [PerfectTriangleAi] accounts for it by only
 * playing for a win that lands before the draw rule would.
 */
object TriangleSolver {
    /**
     * Value encoding, always for the side to move: `+d` = wins with its d-th ply from now (d >= 1),
     * `-(d + 1)` = loses after d more plies (so -1 = has already lost), [DRAW] = neither side can force.
     */
    const val DRAW = Int.MIN_VALUE

    private fun key(s: GameState): Int =
        s.p1 or (s.p2 shl 7) or (s.hand1 shl 14) or (s.hand2 shl 16) or ((if (s.toMove == Player.TWO) 1 else 0) shl 18)

    /** Strip the draw counters: the solver's positions are counter-free. */
    private fun norm(s: GameState) = s.copy(idlePlies = 0, ply = 0)

    private val table: Map<Int, Int> by lazy { solve() }

    private fun solve(): Map<Int, Int> {
        val start = Rules.newGame(RuleSet.TRIANGLE)
        val states = HashMap<Int, GameState>()
        val kids = HashMap<Int, IntArray>()
        val queue = ArrayDeque<GameState>()
        states[key(start)] = start
        queue += start
        while (queue.isNotEmpty()) {
            val s = queue.removeFirst()
            if (s.result != null) continue
            val ks = Rules.legalActions(s).map { a ->
                val n = norm(Rules.applyTrusted(s, a))
                val k = key(n)
                if (states.putIfAbsent(k, n) == null) queue += n
                k
            }
            kids[key(s)] = ks.toIntArray()
        }
        val value = HashMap<Int, Int>()
        for ((k, s) in states) if (s.result is GameResult.Win) value[k] = -1 // mover already lost
        var changed = true
        while (changed) {
            changed = false
            for ((k, ks) in kids) {
                var bestWin = Int.MAX_VALUE
                var allLose = true
                var longest = 0
                for (ck in ks) {
                    val v = value[ck]
                    if (v == null) { allLose = false; continue }
                    if (v < 0) { bestWin = minOf(bestWin, -v); allLose = false } // opponent loses after -v-1 more plies
                    else longest = maxOf(longest, v)
                }
                val nv = when {
                    bestWin != Int.MAX_VALUE -> bestWin
                    allLose -> -(longest + 2)
                    else -> null
                } ?: continue
                if (value[k] != nv) { value[k] = nv; changed = true }
            }
        }
        for (k in states.keys) if (k !in value) value[k] = DRAW
        return value
    }

    /** Exact value of [s] for the side to move; null if [s] isn't a triangle position. */
    fun valueOf(s: GameState): Int? {
        if (s.rules.variant != Variant.TRIANGLE) return null
        if (s.result is GameResult.Win) return -1
        if (s.result != null) return DRAW
        return table[key(norm(s))]
    }

    /** Number of positions in the table (for tests and docs). */
    val size: Int get() = table.size
}

/**
 * Perfect Khutlotharo: wins as fast as possible (when the win lands before the no-progress draw),
 * otherwise holds the draw, otherwise loses as slowly as possible. Ties are broken at random so
 * games differ.
 */
class PerfectTriangleAi(private val random: Random) : AiPlayer {
    override fun chooseAction(state: GameState, shouldStop: () -> Boolean): Action {
        val actions = Rules.legalActions(state)
        require(actions.isNotEmpty()) { "no legal actions" }
        if (state.rules.variant != Variant.TRIANGLE) return actions.random(random)
        fun rank(a: Action): Long {
            val c = Rules.applyTrusted(state, a)
            val r = c.result
            if (r is GameResult.Win) return if (r.winner == state.toMove) 4_000_000 else -4_000_000
            if (r is GameResult.Draw) return 0
            // Plies left before the no-progress draw; a line made on the very last ply still wins.
            val left = state.rules.idleDrawPlies - c.idlePlies
            val v = TriangleSolver.valueOf(c) ?: return 0
            return when {
                v == TriangleSolver.DRAW -> 0
                v < 0 -> if (-v - 1 <= left) 3_000_000L + v else 0 // opponent loses: faster is better
                else -> if (v <= left) -3_000_000L + v else 0 // opponent wins: slower is better
            }
        }
        val ranked = actions.map { it to rank(it) }
        val best = ranked.maxOf { it.second }
        return ranked.filter { it.second == best }.map { it.first }.random(random)
    }
}

/** Plays like [inner] but picks a random legal move with probability [mistakeRate]. */
class FallibleAi(private val inner: AiPlayer, private val mistakeRate: Float, private val random: Random) : AiPlayer {
    override fun chooseAction(state: GameState, shouldStop: () -> Boolean): Action =
        if (random.nextFloat() < mistakeRate) Rules.legalActions(state).random(random) else inner.chooseAction(state, shouldStop)
}
