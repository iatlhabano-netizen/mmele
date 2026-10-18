package bw.mmele.engine.moruba

import bw.mmele.engine.Player

/**
 * Moruba (Nsolo), the four-row Southern African mancala. Pure Kotlin, immutable.
 *
 * Layout: [holes] columns by 4 rows, index `row * holes + col`. Rows 0 (outer) and 1 (inner) belong
 * to [Player.TWO] (top), rows 2 (inner) and 3 (outer) to [Player.ONE] (bottom). Each player sows
 * counter-clockwise (as seen by a viewer above the board) around their own 2 × holes loop only.
 * See docs/RULES.md ("Moruba").
 */
data class MorubaConfig(val holes: Int = 8, val stonesPerPit: Int = 2, val stepCap: Int = DEFAULT_STEP_CAP) {
    init {
        require(holes in setOf(6, 8, 10)) { "holes must be 6, 8 or 10" }
        require(stonesPerPit in 2..4) { "stones per pit must be 2..4" }
        require(stepCap > 0)
    }

    companion object {
        /** Maximum stones dropped in one turn before the relay is forcibly stopped (see RULES.md). */
        const val DEFAULT_STEP_CAP = 2_000
    }
}

data class MorubaState(
    val config: MorubaConfig,
    /** Stones per pit, index row * holes + col. */
    val pits: List<Int>,
    val toMove: Player = Player.ONE,
    val captured1: Int = 0,
    val captured2: Int = 0,
    val winner: Player? = null,
    val ply: Int = 0,
) {
    val holes get() = config.holes
    val isGameOver get() = winner != null
    fun stonesOf(p: Player) = MorubaEngine.loop(holes, p).sumOf { pits[it] }

    companion object {
        fun initial(config: MorubaConfig = MorubaConfig()) =
            MorubaState(config, List(4 * config.holes) { config.stonesPerPit })
    }
}

/** One animation-sized step of a turn. */
sealed interface SowStep {
    /** Stones lifted from [pit] (the starting pit, or a relay pick-up). */
    data class PickUp(val pit: Int, val count: Int) : SowStep
    /** One stone dropped into [pit]. */
    data class Drop(val pit: Int) : SowStep
    /** Stones taken from the opponent's [pits] (inner then outer). */
    data class Capture(val pits: List<Int>, val count: Int) : SowStep
    /** The relay hit [MorubaConfig.stepCap] and the turn was stopped. */
    data object StepCapReached : SowStep
}

data class MorubaTurn(val start: Int, val steps: List<SowStep>, val result: MorubaState) {
    val captured get() = steps.filterIsInstance<SowStep.Capture>().sumOf { it.count }
    val drops get() = steps.count { it is SowStep.Drop }
}

object MorubaEngine {
    fun rowOf(holes: Int, pit: Int) = pit / holes
    fun colOf(holes: Int, pit: Int) = pit % holes
    fun owner(holes: Int, pit: Int): Player = if (rowOf(holes, pit) <= 1) Player.TWO else Player.ONE
    fun isInner(holes: Int, pit: Int) = rowOf(holes, pit) == 1 || rowOf(holes, pit) == 2

    /** The player's 2 × holes pits in counter-clockwise sowing order. */
    fun loop(holes: Int, p: Player): List<Int> = List(2 * holes) { i ->
        if (p == Player.ONE) {
            if (i < holes) 3 * holes + i else 2 * holes + (2 * holes - 1 - i)
        } else {
            if (i < holes) (holes - 1 - i) else holes + (i - holes)
        }
    }

    private val loopCache = HashMap<Pair<Int, Player>, Pair<List<Int>, IntArray>>()
    private fun loopWithIndex(holes: Int, p: Player) = synchronized(loopCache) {
        loopCache.getOrPut(holes to p) {
            val l = loop(holes, p)
            val idx = IntArray(4 * holes) { -1 }
            l.forEachIndexed { i, pit -> idx[pit] = i }
            l to idx
        }
    }

    /** The opponent's inner pit facing an inner pit, and the outer pit behind it. */
    fun opposite(holes: Int, innerPit: Int): Pair<Int, Int> {
        val c = colOf(holes, innerPit)
        return if (rowOf(holes, innerPit) == 2) (holes + c) to c else (2 * holes + c) to (3 * holes + c)
    }

    fun legalMoves(s: MorubaState): List<Int> =
        if (s.isGameOver) emptyList() else loop(s.holes, s.toMove).filter { s.pits[it] >= 2 }

    fun isLegal(s: MorubaState, pit: Int) = !s.isGameOver && pit in s.pits.indices &&
        owner(s.holes, pit) == s.toMove && s.pits[pit] >= 2

    /**
     * Plays a whole turn from [pit] and returns every step for the animation plus the new state.
     * Relay: a last stone in an occupied pit lifts that pit and sowing continues. A last stone in an
     * empty inner pit captures the facing inner + outer enemy pits if the facing inner pit has
     * stones; any other empty landing ends the turn. After [MorubaConfig.stepCap] drops the turn
     * ends where it is (it can never loop forever).
     */
    fun play(s: MorubaState, pit: Int): MorubaTurn {
        require(isLegal(s, pit)) { "illegal Moruba move $pit" }
        val h = s.holes
        val me = s.toMove
        val (loop, idx) = loopWithIndex(h, me)
        val b = s.pits.toIntArray()
        val steps = ArrayList<SowStep>()
        var pos = idx[pit]
        var hand = b[pit]; b[pit] = 0
        steps += SowStep.PickUp(pit, hand)
        var drops = 0
        var cap1 = s.captured1; var cap2 = s.captured2
        var capped = false
        while (true) {
            while (hand > 0 && !capped) {
                pos = (pos + 1) % loop.size
                b[loop[pos]]++; hand--; drops++
                steps += SowStep.Drop(loop[pos])
                if (drops >= s.config.stepCap) capped = true
            }
            val last = loop[pos]
            if (capped) {
                // Stones still in hand go back into the pit the relay stopped on (nothing is lost).
                b[last] += hand; hand = 0
                steps += SowStep.StepCapReached; break
            }
            if (b[last] > 1) {
                hand = b[last]; b[last] = 0
                steps += SowStep.PickUp(last, hand)
                continue
            }
            if (isInner(h, last)) {
                val (oi, oo) = opposite(h, last)
                if (b[oi] > 0) {
                    val n = b[oi] + b[oo]
                    steps += SowStep.Capture(listOf(oi, oo), n)
                    b[oi] = 0; b[oo] = 0
                    if (me == Player.ONE) cap1 += n else cap2 += n
                }
            }
            break
        }
        val next = MorubaState(s.config, b.toList(), me.opponent, cap1, cap2, null, s.ply + 1)
        return MorubaTurn(pit, steps, next.copy(winner = winnerOf(next)))
    }

    /** The side to move loses with no stones or no pit holding two or more. */
    fun winnerOf(s: MorubaState): Player? {
        val mover = s.toMove
        val any = loop(s.holes, mover).any { s.pits[it] >= 2 }
        return if (any) null else mover.opponent
    }
}
