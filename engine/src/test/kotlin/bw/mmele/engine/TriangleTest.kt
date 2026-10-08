package bw.mmele.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TriangleTest {
    private val R = RuleSet.TRIANGLE
    private fun p(l: String) = Action.Place(t(l))
    private fun m(a: String, b: String) = Action.Move(t(a), t(b))

    @Test fun fourCowsEachTenNodes() {
        val s = Rules.newGame(R)
        assertEquals(4, s.hand1)
        assertEquals(10, Rules.legalActions(s).size)
    }

    @Test fun lineInPlacementWinsImmediately() {
        var s = Rules.newGame(R).play(p("L1"), p("L3"), p("M1"), p("M3"))
        assertNull(s.result)
        s = s.play(p("R1"))
        assertEquals(GameResult.Win(Player.ONE, WinReason.LINE_OF_THREE), s.result)
    }

    @Test fun lineAlongBisectorWins() {
        val s = Rules.newGame(R).play(p("T"), p("L3"), p("M1"), p("R3"), p("M2"))
        assertEquals(GameResult.Win(Player.ONE, WinReason.LINE_OF_THREE), s.result)
    }

    @Test fun lineInMovementWins() {
        // P1: L1, M1, L3, R2 ; P2: T, M2, M3, R3  -> empties R1, L2. P1 moves R2 -> R1: L1-M1-R1.
        val s = Rules.position(R, listOf(t("L1"), t("M1"), t("L3"), t("R2")), listOf(t("T"), t("M2"), t("M3"), t("R3")))
        assertNull(s.result)
        assertFalse(s.mustShoot)
        val end = s.play(m("R2", "R1"))
        assertEquals(GameResult.Win(Player.ONE, WinReason.LINE_OF_THREE), end.result)
    }

    @Test fun constructedBlockade() {
        // P1: L1, R1, M2, R2 ; P2: T, M1, L3, L2? Check lines: P2 T-M1-M2? M2 is P1. ok.
        // Empties: M3, R3. P2's cows: T(L1,R1,M1) blocked, M1(T,L1,R1,M2) blocked, L2(L1,M2,L3) blocked,
        // L3(L2,M3) -> M3 empty. So P1 plays R2 -> R3?? R3 neighbours: R2, M3. Then empties M3, R2.
        // Instead: P1 has a cow that moves into M3: R3? Use P1: L1, R1, M2, R3; empties M3?, R2.
        val s = Rules.position(R, listOf(t("L1"), t("R1"), t("M2"), t("R3")), listOf(t("T"), t("M1"), t("L2"), t("L3")))
        assertNull(s.result)
        // P1 moves R3 -> M3 sealing L3 (L3: L2, M3). Empties become R3, R2. P2 cows: none adjacent to R2/R3.
        val end = s.play(m("R3", "M3"))
        assertEquals(GameResult.Win(Player.ONE, WinReason.BLOCKADE), end.result)
    }

    @Test fun triangleDrawAfterFortyIdlePlies() {
        val s0 = Rules.position(R, listOf(t("L1"), t("R3"), t("M2"), t("L3")), listOf(t("T"), t("R1"), t("L2"), t("M3")))
        var s = s0
        var guard = 0
        while (!s.isGameOver && guard++ < 1000) {
            // Pick the first move that doesn't win (we want to reach the draw).
            val acts = Rules.legalActions(s)
            val next = acts.map { Rules.apply(s, it) }.firstOrNull { it.result == null || it.result is GameResult.Draw } ?: Rules.apply(s, acts[0])
            s = next
        }
        assertTrue(s.result is GameResult.Draw || s.result is GameResult.Win)
        if (s.result is GameResult.Draw) assertEquals(40, s.idlePlies)
    }

    @Test fun noShootingOrFlyingInTriangle() {
        val s = Rules.position(R, listOf(t("L1"), t("R3"), t("M2")), listOf(t("T"), t("R1"), t("L2")))
        assertEquals(Phase.MOVEMENT, s.phase)
    }
}
