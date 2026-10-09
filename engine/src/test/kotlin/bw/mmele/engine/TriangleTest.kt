package bw.mmele.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Khutlotharo: 7 points, 3 cows each, line of three wins, slides plus Tsoro-Yematatu jumps. */
class TriangleTest {
    private val R = RuleSet.TRIANGLE
    private val SLIDE_ONLY = R.copy(jumpOverAdjacent = false)
    private fun p(l: String) = Action.Place(t(l))
    private fun m(a: String, b: String) = Action.Move(t(a), t(b))
    private fun moves(s: GameState) = Rules.legalActions(s).map { (it as Action.Move).let { mv -> Boards.TRIANGLE.label(mv.from) + ">" + Boards.TRIANGLE.label(mv.to) } }.toSet()

    // P1: A, CL, BM   P2: C, CR, BR   empty: BL   (no line for either side)
    private fun oneEmptyAtBL(rules: RuleSet, toMove: Player = Player.ONE) =
        Rules.position(rules, listOf(t("A"), t("CL"), t("BM")), listOf(t("C"), t("CR"), t("BR")), toMove = toMove)

    @Test fun threeCowsEachSevenPoints() {
        val s = Rules.newGame(R)
        assertEquals(3, s.hand1)
        assertEquals(3, s.hand2)
        assertEquals(7, Rules.legalActions(s).size)
    }

    @Test fun everyLineWinsInPlacement() {
        for (line in Boards.TRIANGLE.mills) {
            val others = (0 until 7).filter { it !in line.toList() }
            val s = Rules.newGame(R).play(
                Action.Place(line[0]), Action.Place(others[0]),
                Action.Place(line[1]), Action.Place(others[1]),
                Action.Place(line[2]),
            )
            assertEquals("line ${line.map(Boards.TRIANGLE::label)}", GameResult.Win(Player.ONE, WinReason.LINE_OF_THREE), s.result)
        }
    }

    @Test fun lineOfThreeOnlyOnDrawnLines() {
        // CL, C, BM are three P1 cows but not one drawn line: no win.
        val s = Rules.newGame(R).play(p("CL"), p("A"), p("C"), p("CR"), p("BM"))
        assertNull(s.result)
    }

    @Test fun movementAfterAllSixPlaced() {
        val s = Rules.newGame(R).play(p("A"), p("C"), p("CL"), p("CR"), p("BM"), p("BR"))
        assertNull(s.result)
        assertEquals(Phase.MOVEMENT, s.phase)
        assertEquals(Integer.bitCount(s.empty), 1)
    }

    @Test fun slidesAndJumpsIntoTheEmptyPoint() {
        val s = oneEmptyAtBL(R)
        // Slides: CL>BL, BM>BL. Jump: A over CL to BL (A-CL-BL line). BR could jump over BM too, but it is P2's.
        assertEquals(setOf("CL>BL", "BM>BL", "A>BL"), moves(s))
        assertEquals(setOf("CL>BL", "BM>BL"), moves(oneEmptyAtBL(SLIDE_ONLY)))
    }

    @Test fun jumpOverOpponentWithoutCapture() {
        val s = oneEmptyAtBL(R, toMove = Player.TWO)
        assertTrue("BR>BL" in moves(s)) // BR jumps over P1's BM
        val after = s.play(m("BR", "BL"))
        assertEquals(Player.ONE, after.ownerAt(t("BM"))) // nothing captured
        assertNull(after.ownerAt(t("BR")))
    }

    @Test fun noJumpOverAnEmptyPointOrAroundACorner() {
        // P1: A, BL, C   P2: CL, BM, BR? P2 lines none. Empties: CR. A->CR slide ok; BL cannot reach CR.
        val s = Rules.position(R, listOf(t("A"), t("BL"), t("C")), listOf(t("CL"), t("BM"), t("BR")))
        val ms = moves(s)
        assertTrue("A>CR" in ms)
        assertTrue("CL>CR" !in ms) // not P1's
        assertTrue(ms.none { it.startsWith("BL>") }) // BL-BM-BR: BR occupied, BL-CL-A: A occupied
        // C jumps? C is a middle point: it can only slide (to CR).
        assertTrue("C>CR" in ms)
    }

    @Test fun lineInMovementWins() {
        val end = oneEmptyAtBL(R).play(m("BM", "BL")) // A-CL-BL
        assertEquals(GameResult.Win(Player.ONE, WinReason.LINE_OF_THREE), end.result)
    }

    @Test fun noLegalMoveLosesWhenSlidingOnly() {
        // P2 to move with sliding only: C, CR, BR all cut off from the empty BL -> P1 wins.
        val s = oneEmptyAtBL(SLIDE_ONLY, toMove = Player.TWO)
        assertEquals(GameResult.Win(Player.ONE, WinReason.BLOCKADE), s.result)
        // With jumps BR escapes over BM, so the game continues.
        assertNull(oneEmptyAtBL(R, toMove = Player.TWO).result)
    }

    @Test fun drawAfterThirtyIdlePlies() {
        var s = Rules.newGame(R).play(p("A"), p("C"), p("CL"), p("CR"), p("BM"), p("BR"))
        var guard = 0
        while (!s.isGameOver && guard++ < 1000) {
            val acts = Rules.legalActions(s)
            s = acts.map { Rules.apply(s, it) }.firstOrNull { it.result == null || it.result is GameResult.Draw } ?: Rules.apply(s, acts[0])
        }
        assertTrue(s.isGameOver)
        if (s.result is GameResult.Draw) assertEquals(30, s.idlePlies)
    }

    @Test fun noShootingOrFlying() {
        val s = Rules.position(R, listOf(t("A"), t("BL"), t("C")), listOf(t("CL"), t("BM"), t("BR")))
        assertEquals(Phase.MOVEMENT, s.phase)
        assertFalse(s.mustShoot)
    }
}
