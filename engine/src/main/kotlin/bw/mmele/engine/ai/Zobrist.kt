package bw.mmele.engine.ai

import bw.mmele.engine.GameState
import bw.mmele.engine.Player
import kotlin.random.Random

/** Zobrist hashing of the parts of a [GameState] that matter for search. */
object Zobrist {
    private val rnd = Random(0x4D4D454C45L)
    private val cells = Array(2) { LongArray(32) { rnd.nextLong() } }
    private val hand = Array(2) { LongArray(32) { rnd.nextLong() } }
    private val side = rnd.nextLong()
    private val shoot = rnd.nextLong()
    private val bans = Array(2) { LongArray(1024) { rnd.nextLong() } }

    fun hash(s: GameState): Long {
        var h = 0L
        var m = s.p1
        while (m != 0) { val b = Integer.numberOfTrailingZeros(m); h = h xor cells[0][b]; m = m and (m - 1) }
        m = s.p2
        while (m != 0) { val b = Integer.numberOfTrailingZeros(m); h = h xor cells[1][b]; m = m and (m - 1) }
        h = h xor hand[0][s.hand1 and 31] xor hand[1][s.hand2 and 31]
        if (s.toMove == Player.TWO) h = h xor side
        if (s.mustShoot) h = h xor shoot
        if (s.ban1 >= 0) h = h xor bans[0][((s.ban1 shr 8) * 32 + (s.ban1 and 0xFF)) and 1023]
        if (s.ban2 >= 0) h = h xor bans[1][((s.ban2 shr 8) * 32 + (s.ban2 and 0xFF)) and 1023]
        return h
    }
}
