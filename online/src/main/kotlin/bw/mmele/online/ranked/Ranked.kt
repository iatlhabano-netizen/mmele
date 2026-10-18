package bw.mmele.online.ranked

/**
 * Virtual points only. Points have no cash value, cannot be bought, sold, transferred for money
 * or cashed out. Anything involving real money is behind [FeatureFlags.REAL_MONEY_ENABLED]
 * (false, and there is no payment code at all). See docs/REAL_MONEY.md.
 *
 * Settlement is done by the Cloud Function in firebase/functions (same formulas, mirrored in
 * firebase/functions/ranked.js and checked against this file by RankedTest + ranked.test.mjs).
 * Clients can never write ratings, points, ledgers or tournament results (security rules).
 */
object FeatureFlags {
    const val REAL_MONEY_ENABLED = false
}

object Elo {
    const val START_RATING = 1200
    const val START_POINTS = 1000
    /** Fixed virtual stake per ranked match, moved from loser to winner (capped at the loser's balance). */
    const val STAKE = 10
    const val K_NEW = 40   // first 30 ranked games
    const val K = 24

    fun expected(a: Int, b: Int): Double = 1.0 / (1.0 + Math.pow(10.0, (b - a) / 400.0))

    /** New ratings for A and B; [scoreA] is 1 (A won), 0.5 (draw) or 0 (B won). */
    fun update(a: Int, b: Int, scoreA: Double, gamesA: Int = 30, gamesB: Int = 30): Pair<Int, Int> {
        val ka = if (gamesA < 30) K_NEW else K
        val kb = if (gamesB < 30) K_NEW else K
        val ea = expected(a, b)
        val na = Math.round(a + ka * (scoreA - ea)).toInt()
        val nb = Math.round(b + kb * ((1 - scoreA) - (1 - ea))).toInt()
        return na to nb
    }
}

data class PlayerStanding(val uid: String, val rating: Int = Elo.START_RATING, val points: Int = Elo.START_POINTS, val games: Int = 0, val wins: Int = 0, val name: String = "")

/** Result of one ranked match, as the Cloud Function settles it. */
data class Settlement(val winner: PlayerStanding?, val one: PlayerStanding, val two: PlayerStanding)

object Ranked {
    /** [winner] is "one", "two" or "draw". Mirrors settleMatch() in firebase/functions/ranked.js. */
    fun settle(one: PlayerStanding, two: PlayerStanding, winner: String, stake: Int = Elo.STAKE): Settlement {
        val score = when (winner) { "one" -> 1.0; "two" -> 0.0; "draw" -> 0.5; else -> error("bad winner $winner") }
        val (r1, r2) = Elo.update(one.rating, two.rating, score, one.games, two.games)
        val moved = when (winner) { "one" -> minOf(stake, two.points); "two" -> minOf(stake, one.points); else -> 0 }
        val p1 = one.points + if (winner == "one") moved else if (winner == "two") -moved else 0
        val p2 = two.points + if (winner == "two") moved else if (winner == "one") -moved else 0
        val n1 = one.copy(rating = r1, points = p1, games = one.games + 1, wins = one.wins + if (winner == "one") 1 else 0)
        val n2 = two.copy(rating = r2, points = p2, games = two.games + 1, wins = two.wins + if (winner == "two") 1 else 0)
        return Settlement(when (winner) { "one" -> n1; "two" -> n2; else -> null }, n1, n2)
    }
}
