package bw.mmele.online

import bw.mmele.engine.Action

/**
 * Exact multi-path writes the Firebase transport sends, kept platform-free so they can be
 * exported as a fixture and replayed against the Realtime Database emulator
 * (`firebase/payloads.test.mjs`). [SERVER_TIME] is swapped for `ServerValue.TIMESTAMP` on
 * Android and `serverTimestamp()` in the emulator test.
 */
object RtdbPayloads {
    const val SERVER_TIME = "__SERVER_TIME__"

    fun create(room: RoomSnapshot, hostUid: String): Map<String, Any?> =
        Wire.toMap(room, hostUid, null).toMutableMap().apply {
            this["createdAt"] = SERVER_TIME
            this["updatedAt"] = SERVER_TIME
        }

    fun join(guestUid: String): Map<String, Any?> = mapOf(
        "guestUid" to guestUid,
        "connected/two" to true,
        "phase" to Wire.PHASE_PLAYING,
        "updatedAt" to SERVER_TIME,
        "lastMoveAt" to SERVER_TIME,
        "toMove" to "one",
    )

    fun reconnect(seat: Seat): Map<String, Any?> {
        val s = Wire.seatName(seat)
        return mapOf("connected/$s" to true, "leftAt/$s" to null, "updatedAt" to SERVER_TIME)
    }

    fun disconnect(seat: Seat): Map<String, Any?> {
        val s = Wire.seatName(seat)
        return mapOf("connected/$s" to false, "leftAt/$s" to SERVER_TIME)
    }

    /** [before] is the room as read just before the move; [after] is RoomRules' accepted result. */
    fun move(before: RoomSnapshot, after: RoomSnapshot, seat: Seat, action: Action): Map<String, Any?> {
        val n = before.moves.size
        val m = mutableMapOf<String, Any?>(
            "moves/$n" to mapOf("by" to Wire.seatName(seat), "m" to Wire.encodeAction(seat, action).substringAfter('|')),
            "moveCount" to n + 1,
            "updatedAt" to SERVER_TIME,
            "lastMoveAt" to SERVER_TIME,
        )
        // Whose turn it is next (a mill keeps the turn for the shot); the idle-claim rule reads it.
        RoomRules.toMove(after)?.let { m["toMove"] = Wire.seatName(it) }
        if (after.phase == RoomPhase.FINISHED) m["phase"] = Wire.PHASE_FINISHED
        else if (before.offers.any { it.kind == OfferKind.DRAW }) m["offers/draw"] = null
        return m
    }

    fun resign(seat: Seat): Map<String, Any?> =
        mapOf("resigned" to Wire.seatName(seat), "phase" to Wire.PHASE_FINISHED, "updatedAt" to SERVER_TIME)

    fun offerDraw(after: RoomSnapshot, seat: Seat): Map<String, Any?> {
        val m = mutableMapOf<String, Any?>("offers/draw/${Wire.seatName(seat)}" to true, "updatedAt" to SERVER_TIME)
        if (after.phase == RoomPhase.FINISHED) m["phase"] = Wire.PHASE_FINISHED
        return m
    }

    fun offerRematch(after: RoomSnapshot, seat: Seat): Map<String, Any?> =
        if (after.phase == RoomPhase.PLAYING) {
            mapOf(
                "moves" to null, "moveCount" to 0, "resigned" to null, "offers" to null, "phase" to Wire.PHASE_PLAYING,
                "updatedAt" to SERVER_TIME, "lastMoveAt" to SERVER_TIME, "toMove" to "one",
            )
        } else {
            mapOf("offers/rematch/${Wire.seatName(seat)}" to true, "updatedAt" to SERVER_TIME)
        }

    /** Same shape as an abandon claim; the rules accept it on the idle clock instead. */
    fun claimIdle(claimant: Seat): Map<String, Any?> =
        mapOf("resigned" to Wire.seatName(claimant.other), "phase" to Wire.PHASE_ABANDONED, "updatedAt" to SERVER_TIME)

    fun claimAbandoned(claimant: Seat): Map<String, Any?> =
        mapOf("resigned" to Wire.seatName(claimant.other), "phase" to Wire.PHASE_ABANDONED, "updatedAt" to SERVER_TIME)
}
