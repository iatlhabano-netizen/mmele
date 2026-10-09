package bw.mmele.engine.ai

import bw.mmele.engine.GameState
import bw.mmele.engine.MillEffect
import bw.mmele.engine.Player
import bw.mmele.engine.Rules

/** Static evaluation from the point of view of player [p]. Positive = good for [p]. */
class Evaluator(
    private val cow: Int,
    private val mill: Int,
    private val openTwo: Int,
    private val mobility: Int,
    private val positional: Int,
    private val runningMill: Int,
    private val doubleThreat: Int,
) {
    fun evaluate(s: GameState, p: Player): Int = side(s, p) - side(s, p.opponent)

    private fun side(s: GameState, p: Player): Int {
        val b = s.board
        val own = s.maskOf(p)
        val enemy = s.maskOf(p.opponent)
        val empty = s.empty
        var score = 0
        if (s.rules.millEffect == MillEffect.SHOOT) score += s.totalCows(p) * cow

        var mills = 0
        var twos = 0
        for (mm in b.millMasks) {
            val o = Integer.bitCount(mm and own)
            if (o == 3) mills++
            else if (o == 2 && mm and enemy == 0) twos++
        }
        score += mills * mill + twos * openTwo
        if (doubleThreat != 0 && twos >= 2) score += doubleThreat

        if (mobility != 0 && s.handOf(p) == 0 && !s.isFlying(p)) {
            var mob = 0
            Rules.forEachBit(own) { mob += Integer.bitCount(Rules.stepDests(s, it, empty)) }
            score += mob * mobility
        }
        if (positional != 0) {
            // Intersection control: nodes with more lines through them are worth more.
            Rules.forEachBit(own) { score += (b.neighbors[it].size + b.millsAt[it].size) * positional }
        }
        if (runningMill != 0 && s.handOf(p) == 0) {
            // "Running" / double mill: a cow that sits in a mill and can step into another mill,
            // shooting every move; or an open mill that can be closed next move.
            Rules.forEachBit(own) { from ->
                val inMill = Rules.formsMill(b, own, from)
                val dests = if (s.isFlying(p)) empty else Rules.stepDests(s, from, empty)
                Rules.forEachBit(dests) { to ->
                    val moved = (own and (1 shl from).inv()) or (1 shl to)
                    if (Rules.formsMill(b, moved, to)) score += if (inMill) runningMill else runningMill / 3
                }
            }
        }
        return score
    }

    companion object {
        val MEDIUM = Evaluator(cow = 100, mill = 18, openTwo = 10, mobility = 3, positional = 0, runningMill = 0, doubleThreat = 0)
        val HARD = Evaluator(cow = 100, mill = 14, openTwo = 9, mobility = 4, positional = 1, runningMill = 30, doubleThreat = 25)
    }
}
