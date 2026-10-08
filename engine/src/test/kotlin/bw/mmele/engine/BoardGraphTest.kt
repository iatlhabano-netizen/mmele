package bw.mmele.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BoardGraphTest {
    private val b = Boards.CLASSIC

    @Test fun classicHas24Nodes40Edges20Mills() {
        assertEquals(24, b.size)
        assertEquals(40, b.edges.size)
        assertEquals(20, b.mills.size)
        assertEquals(24, b.labels.toSet().size)
    }

    @Test fun adjacencyIsSymmetricAndIrreflexive() {
        for (a in 0 until b.size) {
            assertTrue(!b.isAdjacent(a, a))
            for (x in b.neighbors[a]) assertTrue("$a-$x not symmetric", b.isAdjacent(x, a))
        }
        val t = Boards.TRIANGLE
        for (a in 0 until t.size) for (x in t.neighbors[a]) assertTrue(t.isAdjacent(x, a))
    }

    @Test fun degreesMatchTheDrawnBoard() {
        // Outer and inner rings: 3 links per node; middle ring: 4 (two ring links + in + out).
        for (n in 0 until 24) {
            val expected = if (n / 8 == 1) 4 else 3
            assertEquals("degree of ${b.label(n)}", expected, b.neighbors[n].size)
        }
    }

    @Test fun specificAdjacencies() {
        assertTrue(b.isAdjacent(c("a7"), c("d7")))
        assertTrue(b.isAdjacent(c("a7"), c("b6")))   // corner diagonal
        assertTrue(b.isAdjacent(c("b6"), c("c5")))   // corner diagonal
        assertTrue(b.isAdjacent(c("d7"), c("d6")))   // midline
        assertTrue(b.isAdjacent(c("d6"), c("d5")))
        assertTrue(!b.isAdjacent(c("a7"), c("g7")))
        assertTrue(!b.isAdjacent(c("d5"), c("d3")))  // no centre point
        assertTrue(!b.isAdjacent(c("a7"), c("c5")))
    }

    @Test fun everyMillIsAStraightConnectedLine() {
        for (m in b.mills) {
            val (x, y, z) = m.map { b.positions[it] }
            val cross = (y.x - x.x) * (z.y - x.y) - (y.y - x.y) * (z.x - x.x)
            assertTrue("mill ${m.map(b::label)} not collinear", abs(cross) < 1e-6)
            assertTrue(b.isAdjacent(m[0], m[1]) && b.isAdjacent(m[1], m[2]))
        }
    }

    /** Enumerate every straight path of three linked nodes; it must equal the mill list (all 20, nothing else). */
    @Test fun allTwentyMillsAndOnlyThose() {
        val found = mutableSetOf<Set<Int>>()
        for (mid in 0 until b.size) for (a in b.neighbors[mid]) for (z in b.neighbors[mid]) {
            if (a >= z) continue
            val (p, q, r) = listOf(a, mid, z).map { b.positions[it] }
            val cross = (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x)
            if (abs(cross) < 1e-6) found += setOf(a, mid, z)
        }
        assertEquals(20, found.size)
        assertEquals(found, b.mills.map { it.toSet() }.toSet())
        val diagonals = b.mills.count { m -> m.all { it % 8 % 2 == 0 } && m.map { it / 8 }.toSet().size == 3 }
        assertEquals(4, diagonals)
    }

    @Test fun eachMillIsDetectedWhenCompleted() {
        for ((i, m) in b.mills.withIndex()) {
            val s = Rules.position(RuleSet.CLASSIC, listOf(m[0], m[1]), emptyList(), hand1 = 5, hand2 = 5)
            val pickOpp = (0 until 24).first { it !in m }
            val s2 = s.copy(p2 = 1 shl pickOpp)
            val after = Rules.apply(s2, Action.Place(m[2]))
            assertTrue("mill $i ${m.map(b::label)} should trigger shoot", after.mustShoot)
            assertEquals(Player.ONE, after.toMove)
        }
    }

    @Test fun triangleShape() {
        val t = Boards.TRIANGLE
        assertEquals(10, t.size)
        assertEquals(15, t.edges.size)
        assertEquals(9, t.mills.size)
        assertEquals(3, t.neighbors[0].size)
        for (m in t.mills) {
            val (x, y, z) = m.map { t.positions[it] }
            val cross = (y.x - x.x) * (z.y - x.y) - (y.y - x.y) * (z.x - x.x)
            assertTrue(abs(cross) < 1e-6)
        }
    }
}
