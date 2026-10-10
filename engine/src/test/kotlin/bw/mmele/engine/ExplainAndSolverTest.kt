package bw.mmele.engine

import bw.mmele.engine.ai.Ai
import bw.mmele.engine.ai.Difficulty
import bw.mmele.engine.ai.Evaluator
import bw.mmele.engine.ai.PerfectTriangleAi
import bw.mmele.engine.ai.SearchAi
import bw.mmele.engine.ai.SearchConfig
import bw.mmele.engine.ai.TriangleSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExplainAndSolverTest {
    private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)
    private fun t(l: String) = Boards.TRIANGLE.labels.indexOf(l)

    @Test fun explainsEveryKindOfIllegalAction() {
        val placing = Rules.position(RuleSet.CLASSIC, listOf(c("a7")), listOf(c("g7")), hand1 = 10, hand2 = 10)
        assertNull(Explain.whyIllegal(placing, Action.Place(c("d7"))))
        assertEquals(IllegalReason.OCCUPIED, Explain.whyIllegal(placing, Action.Place(c("g7"))))
        assertEquals(IllegalReason.PLACE_FIRST, Explain.whyIllegal(placing, Action.Move(c("a7"), c("d7"))))

        val moving = Rules.position(RuleSet.CLASSIC, listOf(c("a7"), c("d7"), c("a1"), c("e4")), listOf(c("g7"), c("d1"), c("g1"), c("b4")))
        assertEquals(IllegalReason.NOT_YOUR_COW, Explain.whyIllegal(moving, Action.Move(c("g7"), c("g4"))))
        assertEquals(IllegalReason.DESTINATION_OCCUPIED, Explain.whyIllegal(moving, Action.Move(c("a7"), c("d7"))))
        assertEquals(IllegalReason.SLIDE_ONLY, Explain.whyIllegal(moving, Action.Move(c("e4"), c("a4"))))

        // Dark completes d7-d6-d5? Build a must-shoot state instead.
        val mill = Rules.apply(Rules.position(RuleSet.CLASSIC, listOf(c("a7"), c("d7")), listOf(c("g1"), c("d1"), c("a1")), hand1 = 9, hand2 = 9), Action.Place(c("g7")))
        assertTrue(mill.mustShoot)
        assertEquals(IllegalReason.MUST_SHOOT_FIRST, Explain.whyIllegal(mill, Action.Place(c("d6"))))
        assertEquals(IllegalReason.SHOOT_ENEMY_ONLY, Explain.whyIllegal(mill, Action.Shoot(c("a7"))))
        assertEquals(IllegalReason.SHOOT_ENEMY_ONLY, Explain.whyIllegal(mill, Action.Shoot(c("d6"))))
        // Light's bottom row a1-d1-g1 is a mill and Light has no other cows: then any cow may be shot.
        assertNull(Explain.whyIllegal(mill, Action.Shoot(c("d1"))))
        val protectedState = Rules.apply(
            Rules.position(RuleSet.CLASSIC, listOf(c("a7"), c("d7")), listOf(c("g1"), c("d1"), c("a1"), c("e4")), hand1 = 9, hand2 = 8),
            Action.Place(c("g7")),
        )
        assertEquals(IllegalReason.PROTECTED_IN_MILL, Explain.whyIllegal(protectedState, Action.Shoot(c("d1"))))

        val tri = Rules.position(RuleSet.TRIANGLE, listOf(t("A"), t("BL")), listOf(t("BM"), t("CR")))
        assertNull(Explain.whyIllegal(tri, Action.Move(t("A"), t("BR")))) // jump over CR
        assertEquals(IllegalReason.STEP_OR_JUMP, Explain.whyIllegal(tri, Action.Move(t("BL"), t("C"))))
        assertEquals(IllegalReason.GAME_OVER, Explain.whyIllegal(Rules.forfeit(tri, Player.ONE, WinReason.RESIGNATION), Action.Move(t("A"), t("BR"))))
    }

    @Test fun triangleSolverIsSelfConsistentAndPerfectNeverLoses() {
        assertTrue("table size ${TriangleSolver.size}", TriangleSolver.size > 200)
        val start = Rules.newGame(RuleSet.TRIANGLE)
        val v0 = TriangleSolver.valueOf(start)!!
        // Consistency on random positions: a win has a child that loses, a loss has only winning children.
        val rnd = Random(5)
        repeat(300) {
            var s = start
            repeat(rnd.nextInt(12)) { if (!s.isGameOver) s = Rules.apply(s, Rules.legalActions(s).random(rnd)) }
            if (s.isGameOver) return@repeat
            val v = TriangleSolver.valueOf(s)!!
            val kids = Rules.legalActions(s).map { TriangleSolver.valueOf(Rules.apply(s, it))!! }
            val msg = "state ${s.p1}/${s.p2} h${s.hand1}/${s.hand2} ${s.toMove} v=$v kids=$kids"
            when {
                v == TriangleSolver.DRAW -> assertTrue(msg, kids.none { it < 0 && it != TriangleSolver.DRAW } && kids.any { it == TriangleSolver.DRAW })
                v > 0 -> assertEquals(msg, v, kids.filter { it < 0 && it != TriangleSolver.DRAW }.maxOf { it }.let { -it })
                else -> assertTrue(msg, kids.all { it > 0 })
            }
        }
        // Perfect play never loses the start position's value, against any opponent.
        repeat(30) { g ->
            val perfectSide = if (g % 2 == 0) Player.ONE else Player.TWO
            val perfect = PerfectTriangleAi(Random(g.toLong()))
            val other = Ai.create(Difficulty.values()[g % 3], seed = g.toLong(), timeLimitMs = 30, variant = Variant.TRIANGLE)
            var s = start
            while (!s.isGameOver) s = Rules.apply(s, (if (s.toMove == perfectSide) perfect else other).chooseAction(s))
            val r = s.result
            assertTrue("perfect side lost game $g: $r (start value $v0)", !(r is GameResult.Win && r.winner != perfectSide))
        }
    }

    /** Regression: TT entries must not leak scores across different draw-counter states. */
    @Test fun transpositionTableRespectsDrawCounters() {
        val rnd = Random(9)
        val easy = Ai.create(Difficulty.EASY, seed = 9)
        var checked = 0
        repeat(400) {
            if (checked >= 12) return@repeat
            var s = Rules.newGame(RuleSet.CLASSIC)
            var steps = 30 + rnd.nextInt(60)
            while (!s.isGameOver && steps-- > 0) s = Rules.apply(s, easy.chooseAction(s))
            if (s.isGameOver || s.mustShoot || s.handOf(s.toMove) > 0 || s.handOf(s.toMove.opponent) > 0) return@repeat
            val near = s.copy(idlePlies = s.rules.idleDrawPlies - 3) // three quiet plies from a draw
            val far = s.copy(idlePlies = 0)
            fun hard() = SearchAi(SearchConfig(maxDepth = 3, timeLimitMs = 60_000, useTranspositionTable = true, evaluator = Evaluator.HARD), Random(1))
            val fresh = hard().also { it.chooseAction(near) }.lastScore
            val shared = hard()
            shared.chooseAction(far) // fills the table with "no draw in sight" scores
            shared.chooseAction(near)
            assertEquals("draw-clock leak on ${s.p1}/${s.p2}", fresh, shared.lastScore)
            checked++
        }
        assertTrue(checked >= 5)
    }

    @Test fun nodeBudgetMakesSearchIndependentOfTime() {
        val s = Rules.apply(Rules.apply(Rules.newGame(RuleSet.CLASSIC), Action.Place(c("d6"))), Action.Place(c("d2")))
        fun run(timeMs: Long) = SearchAi(SearchConfig(32, timeMs, true, Evaluator.HARD, nodeBudget = 20_000), Random(3))
            .let { it.chooseAction(s) to it.lastDepthCompleted }
        assertEquals(run(30_000), run(60_000))
    }
}
