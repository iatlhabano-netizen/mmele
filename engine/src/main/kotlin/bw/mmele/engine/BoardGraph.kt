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

    /**
     * Straight-line jumps (Tsoro Yematatu style): for each node, packed `over * 32 + to` entries
     * meaning "from here over the adjacent node `over` to `to` beyond it on the same line".
     * Derived from the mills, whose nodes are listed in line order (middle node in the middle).
     */
    val jumps: Array<IntArray> = Array(size) { n ->
        this.mills.flatMap { m ->
            when (n) {
                m[0] -> listOf(m[1] * 32 + m[2])
                m[2] -> listOf(m[1] * 32 + m[0])
                else -> emptyList()
            }
        }.distinct().toIntArray()
    }

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
     * Khutlotharo: the 7-point triangle as drawn by a Motswana player (same layout as Zimbabwe's
     * Tsoro Yematatu). Apex at the top, a crossbar halfway down, a base of three, and a centre
     * line from the apex to the base midpoint. Indices / labels:
     * ```
     *            0 A
     *     1 CL   2 C   3 CR
     *  4 BL      5 BM      6 BR
     * ```
     * Lines of three (each listed in line order): A-CL-BL, A-CR-BR, A-C-BM, CL-C-CR, BL-BM-BR.
     * Positions are normalised to a tall isosceles triangle (height : base about 1.19 : 1, as in
     * the hand drawing); the canvas fits them uniformly.
     */
    val TRIANGLE: BoardGraph by lazy {
        val left = 0.08f
        val right = 0.92f
        val pos = listOf(
            Pos(0.5f, 0f),                                   // A
            Pos((0.5f + left) / 2, 0.5f),                    // CL - midpoint of the left side
            Pos(0.5f, 0.5f),                                 // C
            Pos((0.5f + right) / 2, 0.5f),                   // CR - midpoint of the right side
            Pos(left, 1f),                                   // BL
            Pos(0.5f, 1f),                                   // BM
            Pos(right, 1f),                                  // BR
        )
        val labels = listOf("A", "CL", "C", "CR", "BL", "BM", "BR")
        val lines = listOf(listOf(0, 1, 4), listOf(0, 3, 6), listOf(0, 2, 5), listOf(1, 2, 3), listOf(4, 5, 6))
        val edges = lines.flatMap { listOf(it[0] to it[1], it[1] to it[2]) }
        BoardGraph("Khutlotharo (triangle)", pos, edges, lines, labels)
    }
}
