package bw.mmele.online.ranked

/**
 * Single-elimination bracket for 4, 8 or 16 players (virtual entry, virtual prizes).
 * Match ids are "r{round}m{index}" (round 1 = first round). Mirrors firebase/functions/bracket.js.
 */
data class BracketMatch(val round: Int, val index: Int, val one: String?, val two: String?, val winner: String? = null) {
    val id get() = "r${round}m$index"
}

data class Bracket(val size: Int, val matches: List<BracketMatch>) {
    init { require(size in SIZES) }
    val rounds get() = Integer.numberOfTrailingZeros(size)
    fun match(id: String) = matches.first { it.id == id }
    val champion: String? get() = matches.firstOrNull { it.round == rounds }?.winner
    val isFinished get() = champion != null

    /** Records [winner] for [matchId] and seeds them into the next round. */
    fun report(matchId: String, winner: String): Bracket {
        val m = match(matchId)
        require(m.winner == null) { "$matchId already decided" }
        require(winner == m.one || winner == m.two) { "$winner is not in $matchId" }
        var ms = matches.map { if (it.id == matchId) it.copy(winner = winner) else it }
        if (m.round < rounds) {
            val nextId = "r${m.round + 1}m${m.index / 2}"
            ms = ms.map { if (it.id == nextId) (if (m.index % 2 == 0) it.copy(one = winner) else it.copy(two = winner)) else it }
        }
        return copy(matches = ms)
    }

    /** Matches ready to play (both players known, no winner yet). */
    fun playable() = matches.filter { it.one != null && it.two != null && it.winner == null }

    /** Standings: champion first, then by round reached (deepest first); ties share a place. */
    fun standings(): List<Pair<Int, String>> {
        val reached = HashMap<String, Int>()
        for (m in matches) for (p in listOfNotNull(m.one, m.two)) reached[p] = maxOf(reached[p] ?: 0, m.round)
        val champ = champion
        val sorted = reached.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { if (it.key == champ) Int.MAX_VALUE else it.value }.thenBy { it.key })
        val out = ArrayList<Pair<Int, String>>()
        var place = 0; var prevKey: Int? = null
        sorted.forEachIndexed { i, e ->
            val key = if (e.key == champ) Int.MAX_VALUE else e.value
            if (key != prevKey) place = i + 1
            prevKey = key
            out += place to e.key
        }
        return out
    }

    companion object {
        val SIZES = setOf(4, 8, 16)
        const val ENTRY = 20

        /** Prizes from the virtual pot: winner 70%, runner-up 30%. */
        fun prizes(size: Int, entry: Int = ENTRY): Pair<Int, Int> { val pot = size * entry; val first = pot * 7 / 10; return first to pot - first }

        fun create(players: List<String>): Bracket {
            val n = players.size
            require(n in SIZES && players.toSet().size == n)
            val rounds = Integer.numberOfTrailingZeros(n)
            val ms = ArrayList<BracketMatch>()
            for (i in 0 until n / 2) ms += BracketMatch(1, i, players[2 * i], players[2 * i + 1])
            for (r in 2..rounds) for (i in 0 until (n shr r)) ms += BracketMatch(r, i, null, null)
            return Bracket(n, ms)
        }
    }
}
