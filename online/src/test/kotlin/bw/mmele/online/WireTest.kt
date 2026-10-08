package bw.mmele.online

import bw.mmele.engine.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WireTest {
    @Test fun roundTrip() {
        val room = RoomSnapshot(
            code = "ABC234",
            config = RoomConfig(styleOne = "kori", styleTwo = "ostrich"),
            phase = RoomPhase.PLAYING,
            moves = listOf(
                MoveRecord(0, Seat.ONE, Action.Place(3)),
                MoveRecord(1, Seat.TWO, Action.Place(9)),
                MoveRecord(2, Seat.ONE, Action.Move(3, 5)),
                MoveRecord(3, Seat.TWO, Action.Shoot(9)),
            ),
            connected = setOf(Seat.ONE),
            leftAtMs = mapOf(Seat.TWO to 50L),
            offers = listOf(Offer(Seat.ONE, OfferKind.DRAW)),
            updatedAtMs = 99,
        )
        val back = Wire.fromMap("ABC234", Wire.toMap(room, "host", "guest"))
        assertEquals(room, back)
    }

    @Test fun gapInMoveListIsRejected() {
        val map = Wire.toMap(RoomSnapshot(code = "ABC234", config = RoomConfig(), phase = RoomPhase.PLAYING), "h", null).toMutableMap()
        map["moves"] = mapOf("0" to "one|P|1", "2" to "two|P|2")
        assertNull(Wire.fromMap("ABC234", map))
    }

    @Test fun garbageMoveIsDroppedAndThenRejectedAsAGap() {
        val map = Wire.toMap(RoomSnapshot(code = "ABC234", config = RoomConfig()), "h", null).toMutableMap()
        map["moves"] = mapOf("0" to "one|P|1", "1" to "nonsense")
        assertNull(Wire.fromMap("ABC234", map))
    }
}
