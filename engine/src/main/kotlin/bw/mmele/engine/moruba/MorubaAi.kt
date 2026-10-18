package bw.mmele.engine.moruba

import bw.mmele.engine.Player
import bw.mmele.engine.ai.Difficulty
import kotlin.random.Random

/** Computer opponents for Moruba. Every level returns a legal pit, or null when there is none. */
interface MorubaAi {
    fun choose(s: MorubaState): Int?

    companion object {
        fun create(level: Difficulty, seed: Long = System.nanoTime()): MorubaAi = when (level) {
            Difficulty.EASY -> RandomMorubaAi(Random(seed))
            Difficulty.MEDIUM -> GreedyMorubaAi(Random(seed))
            else -> MinimaxMorubaAi(depth = if (level == Difficulty.HARD) 4 else 5)
        }
    }
}

/** Easy: any legal move. */
class RandomMorubaAi(private val rnd: Random) : MorubaAi {
    override fun choose(s: MorubaState) = MorubaEngine.legalMoves(s).takeIf { it.isNotEmpty() }?.random(rnd)
}

/** Medium: biggest capture, then the longest sowing (most laps/drops); ties broken at random. */
class GreedyMorubaAi(private val rnd: Random) : MorubaAi {
    override fun choose(s: MorubaState): Int? {
        val moves = MorubaEngine.legalMoves(s).shuffled(rnd)
        return moves.maxByOrNull { m ->
            val t = MorubaEngine.play(s, m)
            if (t.result.winner == s.toMove) Int.MAX_VALUE else t.captured * 10_000 + t.drops
        }
    }
}

/**
 * Hard / Expert: negamax with alpha-beta. Evaluation (from the mover's side): board presence
 * (stones), mobility (pits with ≥ 2) and vulnerability: the biggest inner + outer double capture
 * the opponent could make next turn is subtracted.
 */
class MinimaxMorubaAi(private val depth: Int) : MorubaAi {
    override fun choose(s: MorubaState): Int? {
        val moves = MorubaEngine.legalMoves(s)
        if (moves.isEmpty()) return null
        var best = moves.first(); var alpha = -INF
        for (m in order(s, moves)) {
            val v = -search(MorubaEngine.play(s, m).result, depth - 1, -INF, -alpha)
            if (v > alpha) { alpha = v; best = m }
        }
        return best
    }

    private fun order(s: MorubaState, moves: List<Int>) = moves.sortedByDescending { MorubaEngine.play(s, it).captured }

    private fun search(s: MorubaState, d: Int, a0: Int, beta: Int): Int {
        s.winner?.let { return if (it == s.toMove) WIN + d else -WIN - d }
        if (d <= 0) return evaluate(s)
        var a = a0
        for (m in MorubaEngine.legalMoves(s)) {
            val v = -search(MorubaEngine.play(s, m).result, d - 1, -beta, -a)
            if (v > a) a = v
            if (a >= beta) break
        }
        return a
    }

    companion object {
        private const val INF = 1_000_000_000
        private const val WIN = 1_000_000

        fun evaluate(s: MorubaState): Int {
            val me = s.toMove; val op = me.opponent
            val h = s.holes
            fun mobility(p: Player) = MorubaEngine.loop(h, p).count { s.pits[it] >= 2 }
            val presence = s.stonesOf(me) - s.stonesOf(op)
            val mob = mobility(me) - mobility(op)
            // Vulnerability: our best capture now is good for us; theirs would be computed one ply on.
            val threat = MorubaEngine.legalMoves(s).maxOfOrNull { MorubaEngine.play(s, it).captured } ?: 0
            return presence * 10 + mob * 4 + threat * 6
        }
    }
}
