package bw.mmele.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Randomised self-play: every game must terminate and never get stuck. */
class FuzzTest {

    private fun playRandomGame(rules: RuleSet, rnd: Random, greedy: Boolean): GameState {
        var s = Rules.newGame(rules)
        var steps = 0
        while (!s.isGameOver) {
            val acts = Rules.legalActions(s)
            assertTrue("stuck at ply ${s.ply}: $s", acts.isNotEmpty())
            // Greedy mode prefers mill-forming moves so games reach flying / endgames more often.
            val a = if (greedy && rnd.nextFloat() < 0.7f) {
                acts.firstOrNull { formsMill(s, it) } ?: acts.random(rnd)
            } else acts.random(rnd)
            val next = Rules.apply(s, a)
            checkConsistency(s, next)
            s = next
            steps++
            assertTrue("game did not terminate", steps < 5000)
        }
        assertNotNull(s.result)
        assertTrue(Rules.legalActions(s).isEmpty())
        return s
    }

    private fun formsMill(s: GameState, a: Action): Boolean {
        val own = s.maskOf(s.toMove)
        val (newOwn, to) = when (a) {
            is Action.Place -> (own or (1 shl a.to)) to a.to
            is Action.Move -> ((own and (1 shl a.from).inv()) or (1 shl a.to)) to a.to
            is Action.Shoot -> return false
        }
        return Rules.formsMill(s.board, newOwn, to)
    }

    private fun checkConsistency(prev: GameState, s: GameState) {
        assertEquals(0, s.p1 and s.p2)
        val r = s.rules
        for (p in Player.entries) {
            assertTrue(s.handOf(p) >= 0)
            assertEquals(r.cowsPerPlayer, s.totalCows(p) + s.shotBy(p.opponent))
        }
        if (s.mustShoot) assertEquals(prev.toMove, s.toMove)
        assertTrue(s.ply == prev.ply + 1)
    }

    @Test fun thousandsOfRandomClassicGamesTerminate() {
        val rnd = Random(20261008)
        val reasons = HashMap<Any, Int>()
        repeat(3000) { i ->
            val end = playRandomGame(RuleSet.CLASSIC, rnd, greedy = i % 2 == 0)
            val key: Any = when (val r = end.result!!) { is GameResult.Win -> r.reason; is GameResult.Draw -> r.reason }
            reasons[key] = (reasons[key] ?: 0) + 1
        }
        println("classic fuzz outcomes: $reasons")
        // The fuzzer should exercise the main ways a game can end.
        assertTrue(reasons.containsKey(WinReason.REDUCED_TO_TWO))
        assertTrue(reasons.containsKey(DrawReason.FLYING_NO_SHOT) || reasons.containsKey(DrawReason.NO_PROGRESS))
    }

    @Test fun thousandsOfRandomTriangleGamesTerminate() {
        val rnd = Random(7)
        val reasons = HashMap<Any, Int>()
        repeat(5000) { i ->
            val end = playRandomGame(RuleSet.TRIANGLE, rnd, greedy = i % 3 == 0)
            val key: Any = when (val r = end.result!!) { is GameResult.Win -> r.reason; is GameResult.Draw -> r.reason }
            reasons[key] = (reasons[key] ?: 0) + 1
        }
        println("triangle fuzz outcomes: $reasons")
        assertTrue(reasons.containsKey(WinReason.LINE_OF_THREE))
        assertTrue(reasons.containsKey(DrawReason.NO_PROGRESS))
        // With jumps a full 3-v-3 board always leaves the mover a move (every empty point is
        // reachable from all but two points), so blockades cannot happen.
        assertTrue(!reasons.containsKey(WinReason.BLOCKADE))
    }

    @Test fun slideOnlyTriangleGamesTerminateAndCanBlockade() {
        val rules = RuleSet.TRIANGLE.copy(jumpOverAdjacent = false)
        val rnd = Random(11)
        val reasons = HashMap<Any, Int>()
        repeat(3000) { i ->
            val end = playRandomGame(rules, rnd, greedy = i % 3 == 0)
            val key: Any = when (val r = end.result!!) { is GameResult.Win -> r.reason; is GameResult.Draw -> r.reason }
            reasons[key] = (reasons[key] ?: 0) + 1
        }
        println("slide-only triangle fuzz outcomes: $reasons")
        assertTrue(reasons.containsKey(WinReason.LINE_OF_THREE))
        assertTrue(reasons.containsKey(WinReason.BLOCKADE))
    }
}
