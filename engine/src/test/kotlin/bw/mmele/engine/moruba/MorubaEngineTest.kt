package bw.mmele.engine.moruba

import bw.mmele.engine.Player
import bw.mmele.engine.ai.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MorubaEngineTest {
    private val cfg = MorubaConfig(holes = 8, stonesPerPit = 2)
    private fun empty(c: MorubaConfig = cfg) = MorubaState(c, List(4 * c.holes) { 0 })
    private fun MorubaState.with(vararg p: Pair<Int, Int>) = copy(pits = pits.toMutableList().also { l -> p.forEach { (i, n) -> l[i] = n } })
    private fun pit(row: Int, col: Int, h: Int = 8) = row * h + col

    @Test fun loopsAreDisjointAndCoverOwnRows() {
        for (h in listOf(6, 8, 10)) {
            val a = MorubaEngine.loop(h, Player.ONE); val b = MorubaEngine.loop(h, Player.TWO)
            assertEquals((2 * h until 4 * h).toSet(), a.toSet())
            assertEquals((0 until 2 * h).toSet(), b.toSet())
        }
    }

    @Test fun stonesNeverLeaveOwnSideAndTotalIsConserved() {
        var s = MorubaState.initial(cfg); val rnd = Random(7)
        repeat(200) {
            if (s.isGameOver) return
            val before = s
            val m = MorubaEngine.legalMoves(s).random(rnd)
            val t = MorubaEngine.play(s, m)
            t.steps.filterIsInstance<SowStep.Drop>().forEach { assertEquals(before.toMove, MorubaEngine.owner(8, it.pit)) }
            assertEquals(before.pits.sum() + before.captured1 + before.captured2, t.result.pits.sum() + t.result.captured1 + t.result.captured2)
            s = t.result
        }
    }

    @Test fun relayContinuesFromOccupiedPit() {
        // P1 outer row col0 has 2: drops into col1 (empty) and col2 (has 1) -> relay picks 2 from col2.
        val s = empty().with(pit(3, 0) to 2, pit(3, 2) to 1)
        val t = MorubaEngine.play(s, pit(3, 0))
        val pick = t.steps.filterIsInstance<SowStep.PickUp>()
        assertEquals(listOf(pit(3, 0), pit(3, 2)), pick.map { it.pit })
        assertEquals(2, pick[1].count)
        // relay sows col3, col4 (both empty outer) and ends.
        assertEquals(1, t.result.pits[pit(3, 4)])
        assertEquals(Player.TWO, t.result.toMove)
    }

    @Test fun frontAndBackCapture() {
        // P1 outer col7 has 2: drops col... loop: outer 0..7 then inner 7..0. From outer col6 with 2 -> outer col7, inner col7 (empty).
        val s = empty().with(pit(3, 6) to 2, pit(1, 7) to 3, pit(0, 7) to 4, pit(1, 0) to 2)
        val t = MorubaEngine.play(s, pit(3, 6))
        assertEquals(7, t.captured)
        assertEquals(0, t.result.pits[pit(1, 7)]); assertEquals(0, t.result.pits[pit(0, 7)])
        assertEquals(7, t.result.captured1)
        assertTrue(t.steps.last() is SowStep.Capture)
    }

    @Test fun emptyOuterLandingJustEnds() {
        val s = empty().with(pit(3, 0) to 2, pit(1, 2) to 5, pit(1, 0) to 2)
        val t = MorubaEngine.play(s, pit(3, 0))
        assertEquals(0, t.captured)
        assertEquals(1, t.steps.count { it is SowStep.PickUp })
    }

    @Test fun multiLapRelayTerminates() {
        val rnd = Random(42)
        repeat(300) {
            val c = MorubaConfig(holes = listOf(6, 8, 10).random(rnd), stonesPerPit = rnd.nextInt(2, 5))
            var s = MorubaState.initial(c)
            repeat(60) {
                if (s.isGameOver) return@repeat
                val t = MorubaEngine.play(s, MorubaEngine.legalMoves(s).random(rnd))
                assertTrue(t.drops <= c.stepCap)
                s = t.result
            }
        }
        // A tiny cap forces the stop.
        val c = MorubaConfig(stepCap = 3)
        val t = MorubaEngine.play(MorubaState.initial(c), MorubaEngine.legalMoves(MorubaState.initial(c)).first())
        assertTrue(t.steps.last() is SowStep.StepCapReached)
        assertEquals(3, t.drops)
    }

    @Test fun stalemateLosesForSideWithoutMove() {
        // After P1 moves, P2 has only singletons -> P1 wins.
        val s = empty().with(pit(3, 0) to 2, pit(1, 3) to 1, pit(0, 5) to 1)
        val t = MorubaEngine.play(s, pit(3, 0))
        assertEquals(Player.ONE, t.result.winner)
        assertTrue(MorubaEngine.legalMoves(t.result).isEmpty())
    }

    @Test fun victoryWhenOpponentHasNoStones() {
        val s = empty().with(pit(3, 6) to 2, pit(1, 7) to 2, pit(0, 7) to 1)
        val t = MorubaEngine.play(s, pit(3, 6))
        assertEquals(0, t.result.stonesOf(Player.TWO))
        assertEquals(Player.ONE, t.result.winner)
    }

    @Test fun initialStateHasNoWinner() = assertNull(MorubaEngine.winnerOf(MorubaState.initial()))

    @Test fun everyAiReturnsLegalMoves() {
        for (d in Difficulty.entries) {
            val ai = MorubaAi.create(d, seed = 3)
            var s = MorubaState.initial(MorubaConfig(holes = 6))
            repeat(30) {
                if (s.isGameOver) return@repeat
                val m = ai.choose(s)!!
                assertTrue("$d chose $m", MorubaEngine.isLegal(s, m))
                s = MorubaEngine.play(s, m).result
            }
        }
        assertNull(MorubaAi.create(Difficulty.HARD).choose(empty().with(pit(3, 0) to 1)))
    }
}
