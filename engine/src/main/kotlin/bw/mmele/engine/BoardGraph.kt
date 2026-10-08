package bw.mmele.engine

/** Normalised layout position of a node, both coordinates in 0..1 (y grows downwards). */
data class Pos(val x: Float, val y: Float)

/**
 * An immutable board graph: nodes, undirected adjacency (the drawn lines), and the set of
 * "mills" (straight lines of three adjacent nodes). Occupancy is represented elsewhere as
 * Int bitmasks, so a board may have at most 31 nodes.
 */
class BoardGraph(
    val name: String,
    val positions: List<Pos>,
    edges: List<Pair<Int, Int>>,
    mills: List<List<Int>>,
    val labels: List<String>,
) {
    val size: Int = positions.size

    /** Normalised, de-duplicated edges with first < second. */
    val edges: List<Pair<Int, Int>> = edges.map { (a, b) -> if (a < b) a to b else b to a }.distinct()

    val neighbors: Array<IntArray>
    val neighborMask: IntArray
    val mills: List<IntArray> = mills.map { it.toIntArray() }
    val millMasks: IntArray = this.mills.map { m -> m.fold(0) { acc, n -> acc or (1 shl n) } }.toIntArray()

    /** For each node, the indices (into [mills]) of every mill passing through it. */
    val millsAt: Array<IntArray>
    val fullMask: Int = (1 shl size) - 1

    init {
        require(size in 1..31) { "board size must be 1..31" }
        require(labels.size == size) { "one label per node" }
        val adj = Array(size) { mutableSetOf<Int>() }
        for ((a, b) in this.edges) {
            require(a != b && a in 0 until size && b in 0 until size) { "bad edge $a-$b" }
            adj[a] += b
            adj[b] += a
        }
        neighbors = Array(size) { adj[it].sorted().toIntArray() }
        neighborMask = IntArray(size) { n -> neighbors[n].fold(0) { acc, x -> acc or (1 shl x) } }
        for (m in this.mills) require(m.size == 3 && m.all { it in 0 until size }) { "bad mill" }
        millsAt = Array(size) { n -> this.mills.indices.filter { n in this.mills[it] }.toIntArray() }
    }

    fun isAdjacent(a: Int, b: Int): Boolean = neighborMask[a] and (1 shl b) != 0

    fun label(node: Int): String = labels[node]
}

/** Built-in boards. */
object Boards {

    /**
     * The standard Morabaraba / Mmele board: three concentric squares joined by the four
     * midlines and the four corner diagonals. 24 nodes, 40 edges, 20 mills.
     *
     * Index = ring * 8 + k, ring 0 = outer, 1 = middle, 2 = inner; k runs clockwise from the
     * top-left corner: 0 TL, 1 top-mid, 2 TR, 3 right-mid, 4 BR, 5 bottom-mid, 6 BL, 7 left-mid.
     * Labels use the usual morris notation (files a-g left to right, ranks 1-7 bottom to top).
     */
    val CLASSIC: BoardGraph by lazy {
        val pos = ArrayList<Pos>()
        val labels = ArrayList<String>()
        val edges = ArrayList<Pair<Int, Int>>()
        val mills = ArrayList<List<Int>>()
        for (ring in 0..2) {
            val lo = ring
            val hi = 6 - ring
            val mid = 3
            val xy = listOf(lo to lo, mid to lo, hi to lo, hi to mid, hi to hi, mid to hi, lo to hi, lo to mid)
            for ((x, y) in xy) {
                pos += Pos(x / 6f, y / 6f)
                labels += "${'a' + x}${7 - y}"
            }
        }
        fun n(ring: Int, k: Int) = ring * 8 + ((k % 8) + 8) % 8
        for (ring in 0..2) {
            for (k in 0..7) edges += n(ring, k) to n(ring, k + 1)
            for (k in intArrayOf(0, 2, 4, 6)) mills += listOf(n(ring, k), n(ring, k + 1), n(ring, k + 2))
        }
        for (k in 0..7) {
            // odd k = midline spokes, even k = corner diagonals
            edges += n(0, k) to n(1, k)
            edges += n(1, k) to n(2, k)
            mills += listOf(n(0, k), n(1, k), n(2, k))
        }
        BoardGraph("Mmele (Morabaraba)", pos, edges, mills, labels)
    }

    /**
     * Khutlotharo - a reconstructed small triangle board (see docs/RULES.md).
     * An apex plus three crossbars of three nodes each, joined by the two sides and a central
     * bisector: 10 nodes. Index 0 = apex, then row r (1..3) left/centre/right = 1 + 3(r-1) + c.
     * Lines of three are any three consecutive nodes on a side, the bisector or a crossbar (9 lines).
     */
    val TRIANGLE: BoardGraph by lazy {
        fun n(r: Int, c: Int) = if (r == 0) 0 else 1 + 3 * (r - 1) + c
        val pos = ArrayList<Pos>()
        val labels = ArrayList<String>()
        pos += Pos(0.5f, 0f); labels += "T"
        for (r in 1..3) for (c in 0..2) {
            val half = r / 6f
            pos += Pos(0.5f + (c - 1) * half, r / 3f)
            labels += "${"LMR"[c]}$r"
        }
        val lines = listOf(
            listOf(0, n(1, 0), n(2, 0), n(3, 0)),
            listOf(0, n(1, 2), n(2, 2), n(3, 2)),
            listOf(0, n(1, 1), n(2, 1), n(3, 1)),
            listOf(n(1, 0), n(1, 1), n(1, 2)),
            listOf(n(2, 0), n(2, 1), n(2, 2)),
            listOf(n(3, 0), n(3, 1), n(3, 2)),
        )
        val edges = ArrayList<Pair<Int, Int>>()
        val mills = ArrayList<List<Int>>()
        for (line in lines) {
            for (i in 0 until line.size - 1) edges += line[i] to line[i + 1]
            for (i in 0 until line.size - 2) mills += line.subList(i, i + 3)
        }
        BoardGraph("Khutlotharo (triangle)", pos, edges, mills, labels)
    }
}
