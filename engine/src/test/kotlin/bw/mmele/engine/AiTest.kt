package bw.mmele.engine

import bw.mmele.engine.ai.Ai
import bw.mmele.engine.ai.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AiTest {

    /** Random positions sampled from random play, including must-shoot and flying states. */
    private fun samplePositions(rules: RuleSet, count: Int, seed: Int): List<GameState> {
        val rnd = Random(seed)
        val out = ArrayList<GameState>()
        while (out.size < count) {
            var s = Rules.newGame(rules)
            while (!s.isGameOver) {
                if (rnd.nextFloat() < 0.08f) out += s
                s = Rules.apply(s, Rules.legalActions(s).random(rnd))
            }
        }
        return out.take(count)
    }

    @Test fun everyDifficultyAlwaysReturnsALegalAction() {
        for (rules in listOf(RuleSet.CLASSIC, RuleSet.TRIANGLE)) {
            val positions = samplePositions(rules, 60, 99)
            assertTrue(positions.any { it.mustShoot } || rules.millEffect == MillEffect.WIN)
            for (d in Difficulty.entries) {
                val ai = Ai.create(d, seed = 1, timeLimitMs = 60)
                for (s in positions) {
                    val a = ai.chooseAction(s)
                    assertTrue("$d returned illegal $a", Rules.isLegal(s, a))
                }
            }
        }
    }

    @Test fun aiVsAiGamesComplete() {
        for (rules in listOf(RuleSet.CLASSIC, RuleSet.TRIANGLE)) {
            val a = Ai.create(Difficulty.HARD, seed = 3, timeLimitMs = 40)
            val b = Ai.create(Difficulty.MEDIUM, seed = 4, timeLimitMs = 40)
            var s = Rules.newGame(rules)
            while (!s.isGameOver) {
                val act = (if (s.toMove == Player.ONE) a else b).chooseAction(s)
                s = Rules.apply(s, act)
            }
            assertTrue(s.result != null)
        }
    }

    @Test fun searchAisCompleteAMillAndShootTheThreat() {
        // P1 can close a7-d7-g7 by placing g7.
        val s = Rules.position(RuleSet.CLASSIC, listOf(c("a7"), c("d7")), listOf(c("a1"), c("d1"), c("b2")), hand1 = 8, hand2 = 8)
        for (d in listOf(Difficulty.MEDIUM, Difficulty.HARD)) {
            val ai = Ai.create(d, seed = 5, timeLimitMs = 400)
            val a = ai.chooseAction(s)
            // Either close our mill or block theirs (a1-d1-g1 threatens): both are reasonable,
            // but a mill + shooting a1/d1 handles both, so the search must find g7 or g1.
            assertTrue("$d chose $a", a == place("g7") || a == place("g1"))
        }
    }

    @Test fun hardTakesImmediateTriangleWin() {
        // P1 A, CL, BM; P2 C, CR, BR; empty BL. BM -> BL completes A-CL-BL.
        val s = Rules.position(RuleSet.TRIANGLE, listOf(t("A"), t("CL"), t("BM")), listOf(t("C"), t("CR"), t("BR")))
        for (d in listOf(Difficulty.MEDIUM, Difficulty.HARD)) {
            val ai = Ai.create(d, seed = 1, timeLimitMs = 300)
            assertEquals("$d", Action.Move(t("BM"), t("BL")), ai.chooseAction(s))
        }
    }

    @Test fun aiRespectsTimeCap() {
        val s = Rules.newGame(RuleSet.CLASSIC).play(place("a7"), place("d4".let { "c4" }))
        val ai = Ai.create(Difficulty.HARD, seed = 1, timeLimitMs = 300)
        val t0 = System.nanoTime()
        ai.chooseAction(s)
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("took $ms ms", ms < 1500)
    }

    @Test fun aiStopsWhenCancelled() {
        val s = Rules.newGame(RuleSet.CLASSIC)
        val ai = Ai.create(Difficulty.HARD, seed = 1, timeLimitMs = 60_000)
        val t0 = System.nanoTime()
        val deadline = t0 + 200_000_000
        val a = ai.chooseAction(s) { System.nanoTime() > deadline }
        assertTrue(Rules.isLegal(s, a))
        assertTrue((System.nanoTime() - t0) / 1_000_000 < 2000)
    }

    /** Sanity check on strength ordering (time-capped, so kept loose). */
    @Test fun hardBeatsEasyAndMediumBeatsEasy() {
        fun match(strong: Difficulty, games: Int): Int {
            var wins = 0
            repeat(games) { g ->
                val strongSide = if (g % 2 == 0) Player.ONE else Player.TWO
                val a = Ai.create(strong, seed = g.toLong(), timeLimitMs = 30)
                val b = Ai.create(Difficulty.EASY, seed = 100L + g)
                var s = Rules.newGame(RuleSet.CLASSIC)
                while (!s.isGameOver) s = Rules.apply(s, (if (s.toMove == strongSide) a else b).chooseAction(s))
                val r = s.result
                if (r is GameResult.Win && r.winner == strongSide) wins++
            }
            println("$strong vs EASY: $wins/$games")
            return wins
        }
        assertTrue(match(Difficulty.HARD, 6) >= 5)
        assertTrue(match(Difficulty.MEDIUM, 6) >= 4)
    }
}
