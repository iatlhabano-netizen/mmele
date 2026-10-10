package bw.mmele.academy

import bw.mmele.engine.Action
import bw.mmele.engine.Rules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademyTest {

    @Test fun fiveLessonsWithUniqueIds() {
        assertEquals(5, Lessons.ALL.size)
        assertEquals(Lessons.ALL.size, Lessons.ALL.map { it.id }.toSet().size)
        for (l in Lessons.ALL) {
            assertNull("${l.id}: start must not be over", l.puzzle.start.result)
            assertEquals("${l.id}: learner moves first", l.puzzle.human, l.puzzle.start.toMove)
            assertTrue("${l.id}: at least one hint", l.hints.isNotEmpty())
        }
    }

    @Test fun goalIsForcedFromTheStart() {
        for (l in Lessons.ALL) {
            val p = l.puzzle
            assertTrue("${l.id}: goal not achievable", PuzzleSolver.canForce(p, p.start, p.turns))
        }
    }

    @Test fun intendedSolutionSolvesAgainstTheScriptedOpponent() {
        for (l in Lessons.ALL) {
            val session = PuzzleSession(l.puzzle)
            var last: StepResult? = null
            for (a in l.puzzle.solution) {
                last = session.play(a)
                assertFalse("${l.id}: $a was judged wrong", last is StepResult.Wrong || last is StepResult.Illegal)
            }
            assertTrue("${l.id}: solution ended with $last", last is StepResult.Solved && session.solved)
            assertEquals(0, session.mistakes)
        }
    }

    /** A real puzzle: the first move is not a coin flip. Some complete first turns must fail. */
    @Test fun wrongFirstMovesExistAndAreRejected() {
        for (l in Lessons.ALL) {
            val p = l.puzzle
            val turns = PuzzleSolver.completeTurns(p.start)
            val good = turns.filter { (_, s) -> PuzzleSolver.afterTurn(p, s, p.turns) }
            val bad = turns - good.toSet()
            assertTrue("${l.id}: no wrong moves", bad.size >= 2)
            // Every move that fails is caught by the session and the board is reset.
            val (actions, _) = bad.first()
            val session = PuzzleSession(p)
            var r: StepResult? = null
            for (a in actions) r = session.play(a)
            assertTrue("${l.id}: $actions not rejected ($r)", r is StepResult.Wrong)
            assertEquals(p.start, session.state)
            assertEquals(1, session.mistakes)
        }
    }

    @Test fun oneTurnLessonsHaveASingleCorrectMove() {
        for (l in Lessons.ALL.filter { it.puzzle.turns == 1 }) {
            val p = l.puzzle
            val firstMoves = Rules.legalActions(p.start).filter { a ->
                val c = Rules.apply(p.start, a)
                if (c.result == null && c.toMove == p.human) Rules.legalActions(c).any { PuzzleSolver.afterTurn(p, Rules.apply(c, it), 1) }
                else PuzzleSolver.afterTurn(p, c, 1)
            }
            assertEquals("${l.id}: $firstMoves", 1, firstMoves.size)
        }
    }

    @Test fun escapeRequiresTheMillAndPunishesOtherMoves() {
        val p = Lessons.ESCAPING.puzzle
        val first = p.solution.first() as Action.Move
        assertTrue("solution must form a mill", Rules.apply(p.start, first).mustShoot)
        // A different first move is refuted: Light locks Dark in, and the session shows it.
        val wrong = Rules.legalActions(p.start).first { it != first }
        val session = PuzzleSession(p)
        val r = session.play(wrong)
        assertTrue(r is StepResult.Wrong && r.refutation.isNotEmpty())
    }
}
