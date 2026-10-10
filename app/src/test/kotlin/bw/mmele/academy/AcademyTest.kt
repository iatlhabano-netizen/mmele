package bw.mmele.academy

import bw.mmele.engine.Action
import bw.mmele.engine.Rules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademyTest {
    /** Every puzzle in the Academy: each lesson's main position and its practice position. */
    private val all: List<Pair<String, Puzzle>> = Lessons.ALL.flatMap { listOf(it.id to it.puzzle, "${it.id}-practice" to it.practice) }

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
        for ((id, p) in all) {
            assertNull("$id: start must not be over", p.start.result)
            assertTrue("$id: goal not achievable", PuzzleSolver.canForce(p, p.start, p.turns))
        }
    }

    @Test fun practiceIsADifferentPositionForTheSameSkill() {
        for (l in Lessons.ALL) {
            assertTrue(l.id, l.practice.start != l.puzzle.start)
            assertEquals(l.id, l.puzzle.goal, l.practice.goal)
            assertEquals(l.id, l.puzzle.start.rules, l.practice.start.rules)
        }
    }

    @Test fun intendedSolutionSolvesAgainstTheScriptedOpponent() {
        for ((id, p) in all) {
            val session = PuzzleSession(p)
            var last: StepResult? = null
            for (a in p.solution) {
                last = session.play(a)
                assertFalse("$id: $a was judged wrong", last is StepResult.Wrong || last is StepResult.Illegal)
            }
            assertTrue("$id: solution ended with $last", last is StepResult.Solved && session.solved)
            assertEquals(0, session.mistakes)
        }
    }

    /** A real puzzle: the first move is not a coin flip. Some complete first turns must fail. */
    @Test fun wrongFirstMovesExistAndAreRejected() {
        for ((id, p) in all) {
            val turns = PuzzleSolver.completeTurns(p.start)
            val good = turns.filter { (_, s) -> PuzzleSolver.afterTurn(p, s, p.turns) }
            val bad = turns - good.toSet()
            assertTrue("$id: no wrong moves", bad.size >= 2)
            // Every move that fails is caught by the session and the board is reset.
            val (actions, _) = bad.first()
            val session = PuzzleSession(p)
            var r: StepResult? = null
            for (a in actions) r = session.play(a)
            assertTrue("$id: $actions not rejected ($r)", r is StepResult.Wrong)
            assertEquals(p.start, session.state)
            assertEquals(1, session.mistakes)
        }
    }

    @Test fun oneTurnLessonsHaveASingleCorrectMove() {
        for ((id, p) in all.filter { it.second.turns == 1 }) {
            val firstMoves = Rules.legalActions(p.start).filter { a ->
                val c = Rules.apply(p.start, a)
                if (c.result == null && c.toMove == p.human) Rules.legalActions(c).any { PuzzleSolver.afterTurn(p, Rules.apply(c, it), 1) }
                else PuzzleSolver.afterTurn(p, c, 1)
            }
            assertEquals("$id: $firstMoves", 1, firstMoves.size)
        }
    }

    /** Prints the scripted line of a multi-turn puzzle (used to fill in its solution). */
    @Test fun printEscapePracticeLine() {
        val p = Lessons.ESCAPING.practice
        val sb = StringBuilder()
        var s = p.start
        var left = p.turns
        while (left > 0) {
            val (acts, after) = PuzzleSolver.completeTurns(s).first { (_, c) -> PuzzleSolver.afterTurn(p, c, left) }
            sb.append(acts.joinToString { it.describe() }).append(" | ")
            if (left == 1) break
            val reply = PuzzleOpponent.reply(p, after, left)
            sb.append("reply ").append(reply.joinToString { it.describe() }).append(" | ")
            s = reply.fold(after) { st, a -> Rules.apply(st, a) }
            left--
        }
        java.io.File("build/escape-practice-line.txt").writeText(sb.toString())
    }

    private fun Action.describe() = when (this) {
        is Action.Place -> "Place(${bw.mmele.engine.Boards.CLASSIC.label(to)})"
        is Action.Move -> "Move(${bw.mmele.engine.Boards.CLASSIC.label(from)},${bw.mmele.engine.Boards.CLASSIC.label(to)})"
        is Action.Shoot -> "Shoot(${bw.mmele.engine.Boards.CLASSIC.label(target)})"
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
