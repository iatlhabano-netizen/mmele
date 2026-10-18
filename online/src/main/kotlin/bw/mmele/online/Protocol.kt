package bw.mmele.online

import bw.mmele.engine.Action
import bw.mmele.engine.GameResult
import bw.mmele.engine.Player
import bw.mmele.engine.RuleSet
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import bw.mmele.engine.WinReason

/**
 * Wire format for an online room. The room stores an **ordered list of moves** and every
 * client rebuilds the position by replaying them through the shared `:engine` rules, so the
 * server never has to understand mills — it only has to accept the next legal action from the
 * seat whose turn it is. See docs/ONLINE_SETUP.md.
 */

const val ROOM_CODE_LENGTH = 6
/** No 0/O, 1/I/L — codes are read aloud over the phone. */
const val ROOM_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

enum class Seat { ONE, TWO }

fun Seat.toPlayer() = if (this == Seat.ONE) Player.ONE else Player.TWO
fun Player.toSeat() = if (this == Player.ONE) Seat.ONE else Seat.TWO
val Seat.other: Seat get() = if (this == Seat.ONE) Seat.TWO else Seat.ONE

/** What a room was created with. Immutable for the life of the room. */
data class RoomConfig(
    val variant: Variant = Variant.CLASSIC,
    val clockMinutes: Int = 0,
    val styleOne: String = "obsidian",
    val styleTwo: String = "quartz",
    /** Ranked match: settled by the Cloud Function (Elo + fixed virtual stake). */
    val ranked: Boolean = false,
) {
    val rules: RuleSet get() = RuleSet.of(variant)
}

/** A single committed action, in play order. [ply] is its index (0-based). */
data class MoveRecord(val ply: Int, val seat: Seat, val action: Action)

enum class RoomPhase { LOBBY, PLAYING, FINISHED, ABANDONED }

data class Offer(val seat: Seat, val kind: OfferKind)
enum class OfferKind { DRAW, REMATCH }

data class RoomSnapshot(
    val code: String,
    val config: RoomConfig,
    val host: Seat = Seat.ONE,
    val phase: RoomPhase = RoomPhase.LOBBY,
    val moves: List<MoveRecord> = emptyList(),
    val connected: Set<Seat> = emptySet(),
    val resigned: Seat? = null,
    val offers: List<Offer> = emptyList(),
    val updatedAtMs: Long = 0,
    /** When each absent seat dropped off (server time). Cleared on reconnect. */
    val leftAtMs: Map<Seat, Long> = emptyMap(),
    /** Server time of the last move (or of the game's start): the idle-claim clock. */
    val lastMoveAtMs: Long = 0,
) {
    /** Replay the move list. Throws if the list is internally inconsistent (corrupt room). */
    fun replay(): bw.mmele.engine.GameState {
        var g = Rules.newGame(config.rules)
        for (m in moves) {
            require(m.seat.toPlayer() == g.toMove) { "move ${m.ply} played out of turn" }
            require(Rules.isLegal(g, m.action)) { "move ${m.ply} is illegal" }
            g = Rules.apply(g, m.action)
        }
        return g
    }
}

/** Outcome of trying to append a move. */
sealed interface SubmitResult {
    data class Accepted(val room: RoomSnapshot) : SubmitResult
    data class Rejected(val reason: String) : SubmitResult
}

/** Pure room transitions — the same function the clients and the (optional) server use. */
object RoomRules {
    /** A room with nobody connected for this long is abandoned. */
    const val ABANDON_AFTER_MS = 90_000L

    /**
     * A player who doesn't move for this long (connected or not) lets the opponent claim the game.
     * Enforced by the security rules too (lastMoveAt + 300000 <= now, toMove = the idle seat).
     */
    const val IDLE_CLAIM_AFTER_MS = 300_000L

    /** The idle player sees a countdown for this last stretch. */
    const val IDLE_WARNING_MS = 120_000L

    /** Rooms idle this long may be deleted by anyone (security rules) and by firebase/purge.mjs. */
    const val RETENTION_DAYS = 30

    fun create(code: String, config: RoomConfig, now: Long) = RoomSnapshot(
        code = code, config = config, phase = RoomPhase.LOBBY, connected = setOf(Seat.ONE), updatedAtMs = now,
    )

    fun join(room: RoomSnapshot, now: Long): SubmitResult {
        if (room.phase == RoomPhase.ABANDONED) return SubmitResult.Rejected("room abandoned")
        if (Seat.TWO in room.connected && room.phase != RoomPhase.LOBBY) {
            return SubmitResult.Rejected("room is full")
        }
        val next = room.copy(
            connected = room.connected + Seat.TWO,
            phase = if (room.phase == RoomPhase.LOBBY) RoomPhase.PLAYING else room.phase,
            updatedAtMs = now,
            lastMoveAtMs = if (room.phase == RoomPhase.LOBBY) now else room.lastMoveAtMs,
        )
        return SubmitResult.Accepted(next)
    }

    /** Host (or a returning guest) marks itself present. */
    fun connect(room: RoomSnapshot, seat: Seat, now: Long): RoomSnapshot =
        room.copy(connected = room.connected + seat, leftAtMs = room.leftAtMs - seat, updatedAtMs = now)

    fun disconnect(room: RoomSnapshot, seat: Seat, now: Long): RoomSnapshot =
        room.copy(connected = room.connected - seat, leftAtMs = room.leftAtMs + (seat to now), updatedAtMs = now)

    /** Milliseconds until [claimant] may claim the game because the other seat left, or null if it can't. */
    fun abandonCountdown(room: RoomSnapshot, claimant: Seat, now: Long): Long? {
        if (room.phase != RoomPhase.PLAYING || claimant !in room.connected) return null
        val gone = claimant.other
        if (gone in room.connected) return null
        val left = room.leftAtMs[gone] ?: return null
        return (left + ABANDON_AFTER_MS - now).coerceAtLeast(0)
    }

    /** The present player claims the win once the other has been gone for [ABANDON_AFTER_MS]. */
    fun claimAbandoned(room: RoomSnapshot, claimant: Seat, now: Long): SubmitResult {
        val left = abandonCountdown(room, claimant, now) ?: return SubmitResult.Rejected("opponent is here")
        if (left > 0) return SubmitResult.Rejected("wait ${left / 1000}s")
        return SubmitResult.Accepted(room.copy(phase = RoomPhase.ABANDONED, resigned = claimant.other, updatedAtMs = now))
    }

    /** Seat whose turn it is in a playing room (null if the room isn't playing or the game is over). */
    fun toMove(room: RoomSnapshot): Seat? {
        if (room.phase != RoomPhase.PLAYING) return null
        val g = runCatching { room.replay() }.getOrNull() ?: return null
        if (g.isGameOver) return null
        return if (g.toMove == bw.mmele.engine.Player.ONE) Seat.ONE else Seat.TWO
    }

    /**
     * Milliseconds until [claimant] may claim the game because the other seat, whose turn it is,
     * hasn't moved; null when it's [claimant]'s own turn or the room isn't playing.
     */
    fun idleCountdown(room: RoomSnapshot, claimant: Seat, now: Long): Long? {
        if (room.lastMoveAtMs <= 0 || toMove(room) != claimant.other) return null
        return (room.lastMoveAtMs + IDLE_CLAIM_AFTER_MS - now).coerceAtLeast(0)
    }

    fun claimIdle(room: RoomSnapshot, claimant: Seat, now: Long): SubmitResult {
        val left = idleCountdown(room, claimant, now) ?: return SubmitResult.Rejected("it's your move")
        if (left > 0) return SubmitResult.Rejected("wait ${left / 1000}s")
        return SubmitResult.Accepted(room.copy(phase = RoomPhase.ABANDONED, resigned = claimant.other, updatedAtMs = now))
    }

    fun submit(room: RoomSnapshot, seat: Seat, action: Action, now: Long): SubmitResult {
        if (room.phase != RoomPhase.PLAYING) return SubmitResult.Rejected("not playing")
        if (seat !in room.connected) return SubmitResult.Rejected("seat disconnected")
        val g = runCatching { room.replay() }.getOrElse { return SubmitResult.Rejected("corrupt room") }
        if (g.isGameOver) return SubmitResult.Rejected("game over")
        if (g.toMove != seat.toPlayer()) return SubmitResult.Rejected("not your turn")
        if (!Rules.isLegal(g, action)) return SubmitResult.Rejected("illegal move")
        val record = MoveRecord(room.moves.size, seat, action)
        val after = Rules.apply(g, action)
        return SubmitResult.Accepted(
            room.copy(
                moves = room.moves + record,
                phase = if (after.isGameOver) RoomPhase.FINISHED else RoomPhase.PLAYING,
                offers = emptyList(), // a move withdraws pending draw offers
                updatedAtMs = now,
                lastMoveAtMs = now,
            ),
        )
    }

    fun resign(room: RoomSnapshot, seat: Seat, now: Long): SubmitResult {
        if (room.phase != RoomPhase.PLAYING) return SubmitResult.Rejected("not playing")
        return SubmitResult.Accepted(room.copy(phase = RoomPhase.FINISHED, resigned = seat, updatedAtMs = now))
    }

    fun offerDraw(room: RoomSnapshot, seat: Seat, now: Long): SubmitResult {
        if (room.phase != RoomPhase.PLAYING) return SubmitResult.Rejected("not playing")
        if (room.offers.any { it.kind == OfferKind.DRAW && it.seat == seat.other }) {
            // The other side already offered: accepting ends the game as a draw.
            return SubmitResult.Accepted(
                room.copy(phase = RoomPhase.FINISHED, offers = listOf(Offer(seat, OfferKind.DRAW), Offer(seat.other, OfferKind.DRAW)), updatedAtMs = now),
            )
        }
        val offers = room.offers.filterNot { it.kind == OfferKind.DRAW } + Offer(seat, OfferKind.DRAW)
        return SubmitResult.Accepted(room.copy(offers = offers, updatedAtMs = now))
    }

    fun offerRematch(room: RoomSnapshot, seat: Seat, now: Long): SubmitResult {
        if (room.phase != RoomPhase.FINISHED && room.phase != RoomPhase.ABANDONED) {
            return SubmitResult.Rejected("game still going")
        }
        if (room.offers.any { it.kind == OfferKind.REMATCH && it.seat == seat.other }) {
            // Both agreed: a fresh game in the same room, same seats.
            return SubmitResult.Accepted(
                room.copy(phase = RoomPhase.PLAYING, moves = emptyList(), resigned = null, offers = emptyList(), updatedAtMs = now, lastMoveAtMs = now),
            )
        }
        val offers = room.offers.filterNot { it.kind == OfferKind.REMATCH } + Offer(seat, OfferKind.REMATCH)
        return SubmitResult.Accepted(room.copy(offers = offers, updatedAtMs = now))
    }

    /** Result implied by a finished/abandoned snapshot, or null while playing. */
    fun resultOf(room: RoomSnapshot): GameResult? {
        room.resigned?.let {
            val reason = if (room.phase == RoomPhase.ABANDONED) WinReason.ABANDONED else WinReason.RESIGNATION
            return GameResult.Win(it.other.toPlayer(), reason)
        }
        if (room.offers.count { it.kind == OfferKind.DRAW } == 2) return GameResult.Draw(bw.mmele.engine.DrawReason.AGREEMENT)
        if (room.phase == RoomPhase.FINISHED || room.phase == RoomPhase.PLAYING) {
            return runCatching { room.replay().result }.getOrNull()
        }
        return null
    }

}

fun randomRoomCode(rng: kotlin.random.Random = kotlin.random.Random.Default): String =
    buildString(ROOM_CODE_LENGTH) { repeat(ROOM_CODE_LENGTH) { append(ROOM_CODE_ALPHABET[rng.nextInt(ROOM_CODE_ALPHABET.length)]) } }
