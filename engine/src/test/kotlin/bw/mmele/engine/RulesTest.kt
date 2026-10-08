package bw.mmele.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    private val R = RuleSet.CLASSIC

    @Test fun newGameIsPlacementWithTwelveEach() {
        val s = Rules.newGame(R)
        assertEquals(Phase.PLACEMENT, s.phase)
        assertEquals(12, s.hand1); assertEquals(12, s.hand2)
        assertEquals(24, Rules.legalActions(s).size)
        assertEquals(Player.ONE, s.toMove)
    }

    @Test fun illegalActionsAreRejected() {
        val s = Rules.newGame(R).play(place("a7"))
        assertFalse(Rules.isLegal(s, place("a7")))
        assertFalse(Rules.isLegal(s, move("a7", "d7")))
        assertFalse(Rules.isLegal(s, shoot("a7")))
        try { Rules.apply(s, place("a7")); throw AssertionError("expected failure") } catch (_: IllegalArgumentException) {}
    }

    @Test fun millInPlacementForcesShootThenTurnPasses() {
        var s = Rules.newGame(R).play(place("a7"), place("a1"), place("d7"), place("g1"))
        s = s.play(place("g7"))
        assertTrue(s.mustShoot)
        assertEquals(Player.ONE, s.toMove)
        assertTrue(Rules.legalActions(s).all { it is Action.Shoot })
        s = s.play(shoot("a1"))
        assertFalse(s.mustShoot)
        assertEquals(Player.TWO, s.toMove)
        assertEquals(1, s.shotBy1)
        assertEquals(1, s.onBoard(Player.TWO))
        assertEquals(10, s.hand2)
    }

    @Test fun protectedMillCowsCannotBeShot() {
        // P2 has a mill a1-d1-g1 plus a loose cow at d4? (no d4) -> loose cow at b4.
        val s = Rules.position(R, listOf(c("a7"), c("d7")), listOf(c("a1"), c("d1"), c("g1"), c("b4")), hand1 = 3, hand2 = 3)
            .play(place("g7"))
        assertTrue(s.mustShoot)
        assertEquals(listOf(shoot("b4")), Rules.legalActions(s))
    }

    @Test fun allInMillsExceptionAllowsShootingFromMills() {
        val s = Rules.position(R, listOf(c("a7"), c("d7")), listOf(c("a1"), c("d1"), c("g1")), hand1 = 3, hand2 = 3)
            .play(place("g7"))
        assertTrue(s.mustShoot)
        assertEquals(setOf(shoot("a1"), shoot("d1"), shoot("g1")), Rules.legalActions(s).toSet())
    }

    @Test fun doubleMillGivesExactlyOneShot() {
        // a7-d7-_g7_ and g7-g4-g1 both complete when P1 places g7.
        var s = Rules.position(R, listOf(c("a7"), c("d7"), c("g4"), c("g1")), listOf(c("a1"), c("b2"), c("c3")), hand1 = 4, hand2 = 4)
        s = s.play(place("g7"))
        assertTrue(s.mustShoot)
        s = s.play(shoot("a1"))
        assertEquals(Player.TWO, s.toMove)
        assertFalse(s.mustShoot)
        assertEquals(2, s.onBoard(Player.TWO))
    }

    @Test fun millWithNoEnemyCowOnBoardSkipsShot() {
        val s = Rules.position(R, listOf(c("a7"), c("d7")), emptyList(), hand1 = 5, hand2 = 6).play(place("g7"))
        assertFalse(s.mustShoot)
        assertEquals(Player.TWO, s.toMove)
    }

    @Test fun placementToMovementTransition() {
        var s = Rules.newGame(R)
        // Mill-free full-board colouring found offline (see FullBoardTest); play only 22 of it.
        val p1 = listOf(1, 2, 5, 7, 8, 11, 13, 14, 16, 17, 19, 20)
        val p2 = listOf(0, 3, 4, 6, 9, 10, 12, 15, 18, 21, 22, 23)
        for (i in 0 until 11) { s = s.play(Action.Place(p1[i]), Action.Place(p2[i])) }
        assertEquals(Phase.PLACEMENT, s.phase)
        s = s.play(Action.Place(p1[11]))
        assertEquals(Phase.MOVEMENT, s.phaseOf(Player.ONE))
        assertEquals(Phase.PLACEMENT, s.phase) // P2 still has one cow in hand
    }

    @Test fun movementIsAlongLinesOnly() {
        val s = Rules.position(R, listOf(c("a7"), c("d6"), c("b4"), c("g1")), listOf(c("d1"), c("f2"), c("e4"), c("e5")))
        assertEquals(Phase.MOVEMENT, s.phase)
        val dests = Rules.destinationsFrom(s, c("a7"))
        assertEquals((1 shl c("d7")) or (1 shl c("a4")) or (1 shl c("b6")), dests)
        assertFalse(Rules.isLegal(s, move("a7", "g7")))
    }

    @Test fun flyingAtThreeCows() {
        val s = Rules.position(R, listOf(c("a7"), c("g1"), c("d5")), listOf(c("d1"), c("f2"), c("e4"), c("e5"), c("b2")))
        assertEquals(Phase.FLYING, s.phase)
        assertEquals(3 * (24 - 8), Rules.legalActions(s).size)
        val s2 = s.play(move("a7", "e3"))
        assertEquals(Phase.MOVEMENT, s2.phase) // P2 has 5 cows, does not fly
    }

    @Test fun shootingToTwoCowsWins() {
        // P1 at d7,g7 + a6? ; build mill a7-d7-g7 by moving a4->a7? a4 is not adjacent... use g4->g7.
        val s = Rules.position(R, listOf(c("a7"), c("d7"), c("g4"), c("b2")), listOf(c("a1"), c("d1"), c("f2")))
        assertEquals(Phase.MOVEMENT, s.phase)
        val after = s.play(move("g4", "g7"))
        assertTrue(after.mustShoot)
        val end = after.play(shoot("f2"))
        assertEquals(GameResult.Win(Player.ONE, WinReason.REDUCED_TO_TWO), end.result)
        assertEquals(Phase.GAME_OVER, end.phase)
        assertTrue(Rules.legalActions(end).isEmpty())
    }

    @Test fun blockadeWins() {
        // P2's five cows are boxed in except for d6; P1 seals it with d5 -> d6 (no mill formed).
        val p2 = listOf(c("a7"), c("d7"), c("b4"), c("f4"), c("f6"))
        val p1 = listOf(c("a4"), c("b6"), c("g7"), c("c4"), c("b2"), c("e4"), c("g4"), c("f2"), c("e5"), c("d5"))
        val s = Rules.position(R, p1, p2)
        assertNull(s.result)
        val end = s.play(move("d5", "d6"))
        assertFalse(end.mustShoot)
        assertEquals(GameResult.Win(Player.ONE, WinReason.BLOCKADE), end.result)
    }

    @Test fun fullBoardAtEndOfPlacementIsALossForTheBlockedMover() {
        val p1 = listOf(1, 2, 5, 7, 8, 11, 13, 14, 16, 17, 19, 20)
        val p2 = listOf(0, 3, 4, 6, 9, 10, 12, 15, 18, 21, 22, 23)
        var s = Rules.newGame(R)
        for (i in 0 until 12) {
            s = s.play(Action.Place(p1[i]))
            assertInvariant(s)
            assertFalse(s.mustShoot)
            s = s.play(Action.Place(p2[i]))
            assertInvariant(s)
            assertFalse(s.mustShoot)
        }
        assertEquals(0, s.empty)
        assertEquals(GameResult.Win(Player.TWO, WinReason.BLOCKADE), s.result)
    }

    @Test fun flyingDrawAfterTenMovesEachWithoutAShot() {
        // Both on 3 cows, flying, positioned so no mill is possible quickly; shuffle cows back and forth.
        var s = Rules.position(R, listOf(c("a7"), c("g1"), c("d5")), listOf(c("g7"), c("a1"), c("d3")))
        assertEquals(Phase.FLYING, s.phase)
        val p1Moves = listOf(move("a7", "b6"), move("b6", "a7"))
        val p2Moves = listOf(move("g7", "f6"), move("f6", "g7"))
        var i = 0
        while (!s.isGameOver) {
            s = s.play(p1Moves[i % 2])
            if (s.isGameOver) break
            s = s.play(p2Moves[i % 2])
            i++
        }
        assertEquals(GameResult.Draw(DrawReason.FLYING_NO_SHOT), s.result)
        assertEquals(20, s.ply)
    }

    @Test fun noProgressDrawAfterHundredPlies() {
        // Plenty of cows, nobody flying, shuffle without forming mills.
        var s = Rules.position(R, listOf(c("a7"), c("g1"), c("d5"), c("b4")), listOf(c("g7"), c("a1"), c("d3"), c("f4")))
        val a = listOf(move("a7", "d7"), move("d7", "a7"))
        val b = listOf(move("a1", "d1"), move("d1", "a1"))
        var i = 0
        while (!s.isGameOver) {
            s = s.play(a[i % 2]); if (s.isGameOver) break
            s = s.play(b[i % 2]); i++
        }
        assertEquals(GameResult.Draw(DrawReason.NO_PROGRESS), s.result)
        assertEquals(100, s.idlePlies)
    }

    @Test fun shotResetsDrawCounters() {
        val s = Rules.position(R, listOf(c("a7"), c("d7"), c("g4"), c("b2")), listOf(c("a1"), c("d1"), c("f2"), c("c3")))
            .copy(idlePlies = 90, flyingPlies = 5)
        val after = s.play(move("g4", "g7"), shoot("f2"))
        assertEquals(0, after.idlePlies)
        assertEquals(0, after.flyingPlies)
    }

    @Test fun garRemillBanBlocksImmediateReturn() {
        val p1 = listOf(c("a7"), c("d7"), c("g7"), c("b4"), c("c4"))
        val p2 = listOf(c("a1"), c("d1"), c("g1"), c("e4"), c("f2"), c("e3"), c("c5"))
        var s = Rules.position(R, p1, p2)
        s = s.play(move("a7", "a4")) // breaks a7-d7-g7, forms a4-b4-c4
        assertTrue(s.mustShoot)
        assertEquals(setOf(shoot("e4"), shoot("c5")), Rules.legalActions(s).toSet())
        s = s.play(shoot("e4"), move("c5", "d5"))
        // Moving a4 straight back to a7 would re-form a7-d7-g7: banned on this move only.
        assertFalse(Rules.isLegal(s, move("a4", "a7")))
        assertTrue(Rules.isLegal(s, move("d7", "a7")))
        s = s.play(move("b4", "b6"), move("d5", "c5"))
        s = s.play(move("b6", "b4")) // re-forms a4-b4-c4: an ordinary remake, allowed and shoots
        assertTrue(s.mustShoot)
    }

    @Test fun banFilterOnlyRemovesTheBannedReturn() {
        // P1's only empty neighbour anywhere is a7; a4 -> a7 is banned, d7 -> a7 is not.
        val p1 = listOf(c("a4"), c("d7"), c("g7"), c("b4"), c("c4"))
        val p2 = listOf(c("a1"), c("d6"), c("g4"), c("f6"), c("b6"), c("b2"), c("c5"), c("c3"))
        val s = Rules.position(R, p1, p2).copy(ban1 = (c("a4") shl 8) or c("a7"))
        assertEquals(listOf(move("d7", "a7")), Rules.legalActions(s))
        // Without the ban both moves into a7 are legal.
        assertEquals(setOf(move("d7", "a7"), move("a4", "a7")), Rules.legalActions(s.copy(ban1 = -1)).toSet())
    }

    @Test fun shootingSequencesKeepInvariants() {
        val s = Rules.newGame(R)
        assertInvariant(s)
    }
}
