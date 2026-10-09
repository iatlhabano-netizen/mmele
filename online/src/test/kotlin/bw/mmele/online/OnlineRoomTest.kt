package bw.mmele.online

import bw.mmele.engine.Action
import bw.mmele.engine.Boards
import bw.mmele.engine.DrawReason
import bw.mmele.engine.GameResult
import bw.mmele.engine.Player
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import bw.mmele.engine.WinReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Two clients sharing one in-memory store — the same contract Firebase must satisfy. */
class OnlineRoomTest {
    private var now = 1_000L
    private val store = RoomStore()
    private val host = InMemoryOnlineGameRepository(store, { now }, Random(1))
    private val guest = InMemoryOnlineGameRepository(store, { now }, Random(2))
    private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)

    @Test fun roomCodesAreSixUnambiguousChars() {
        val rng = Random(42)
        repeat(500) {
            val code = randomRoomCode(rng)
            assertEquals(6, code.length)
            assertTrue(code.none { it in "01OIL" })
            assertTrue(code.all { it in ROOM_CODE_ALPHABET })
        }
    }

    @Test fun createJoinAndBothSeeTheSameGame() = runTest {
        val room = host.createRoom(RoomConfig(Variant.CLASSIC))
        assertEquals(RoomPhase.LOBBY, room.phase)
        val joined = guest.joinRoom(room.code.lowercase()) // case-insensitive
        assertEquals(RoomPhase.PLAYING, joined.phase)
        assertEquals(setOf(Seat.ONE, Seat.TWO), joined.connected)

        assertTrue(host.submitMove(room.code, Seat.ONE, Action.Place(c("a7"))) is SubmitResult.Accepted)
        assertTrue(guest.submitMove(room.code, Seat.TWO, Action.Place(c("d7"))) is SubmitResult.Accepted)
        val seenByHost = host.observe(room.code).first()
        val seenByGuest = guest.observe(room.code).first()
        assertEquals(seenByHost, seenByGuest)
        assertEquals(2, seenByGuest.moves.size)
        val g = seenByGuest.replay()
        assertEquals(Player.ONE, g.ownerAt(c("a7")))
        assertEquals(Player.TWO, g.ownerAt(c("d7")))
    }

    @Test fun outOfTurnAndIllegalMovesAreRejected() = runTest {
        val code = host.createRoom(RoomConfig()).code
        guest.joinRoom(code)
        assertTrue(guest.submitMove(code, Seat.TWO, Action.Place(c("a7"))) is SubmitResult.Rejected) // not your turn
        host.submitMove(code, Seat.ONE, Action.Place(c("a7")))
        assertTrue(guest.submitMove(code, Seat.TWO, Action.Place(c("a7"))) is SubmitResult.Rejected) // occupied
        assertTrue(guest.submitMove(code, Seat.TWO, Action.Move(c("a7"), c("d7"))) is SubmitResult.Rejected) // still placing
        assertEquals(1, store.peek(code)!!.moves.size)
    }

    @Test fun moveBeforeOpponentJoinsIsRejected() = runTest {
        val code = host.createRoom(RoomConfig()).code
        assertTrue(host.submitMove(code, Seat.ONE, Action.Place(c("a7"))) is SubmitResult.Rejected)
    }

    @Test fun thirdPlayerCannotJoinAndUnknownCodeFails() = runTest {
        val code = host.createRoom(RoomConfig()).code
        guest.joinRoom(code)
        val third = InMemoryOnlineGameRepository(store, { now })
        assertTrue(runCatching { third.joinRoom(code) }.exceptionOrNull() is RoomFullException)
        assertTrue(runCatching { third.joinRoom("ZZZZZZ") }.exceptionOrNull() is RoomNotFoundException)
    }

    @Test fun randomGamesStayInSyncToTheEnd() = runTest {
        val rng = Random(7)
        repeat(5) {
            val code = host.createRoom(RoomConfig(if (it % 2 == 0) Variant.CLASSIC else Variant.TRIANGLE)).code
            guest.joinRoom(code)
            var local = Rules.newGame(store.peek(code)!!.config.rules)
            while (!local.isGameOver) {
                val action = Rules.legalActions(local).let { a -> a[rng.nextInt(a.size)] }
                val repo = if (local.toMove == Player.ONE) host else guest
                val r = repo.submitMove(code, local.toMove.toSeat(), action)
                assertTrue("rejected: $r", r is SubmitResult.Accepted)
                local = Rules.apply(local, action)
            }
            val room = store.peek(code)!!
            assertEquals(RoomPhase.FINISHED, room.phase)
            assertEquals(local, room.replay())
            assertEquals(local.result, RoomRules.resultOf(room))
        }
    }

    @Test fun resignGivesOpponentTheWin() = runTest {
        val code = host.createRoom(RoomConfig()).code
        guest.joinRoom(code)
        guest.resign(code, Seat.TWO)
        val room = store.peek(code)!!
        assertEquals(RoomPhase.FINISHED, room.phase)
        assertEquals(GameResult.Win(Player.ONE, WinReason.RESIGNATION), RoomRules.resultOf(room))
        assertTrue(host.submitMove(code, Seat.ONE, Action.Place(c("a7"))) is SubmitResult.Rejected)
    }

    @Test fun drawNeedsBothSidesAndAMoveWithdrawsTheOffer() = runTest {
        val code = host.createRoom(RoomConfig()).code
        guest.joinRoom(code)
        host.offerDraw(code, Seat.ONE)
        assertEquals(1, store.peek(code)!!.offers.size)
        host.submitMove(code, Seat.ONE, Action.Place(c("a7")))
        assertTrue(store.peek(code)!!.offers.isEmpty())
        host.offerDraw(code, Seat.ONE)
        guest.offerDraw(code, Seat.TWO)
        val room = store.peek(code)!!
        assertEquals(RoomPhase.FINISHED, room.phase)
        assertEquals(GameResult.Draw(DrawReason.AGREEMENT), RoomRules.resultOf(room))
    }

    @Test fun rematchResetsMovesOnceBothAgree() = runTest {
        val code = host.createRoom(RoomConfig()).code
        guest.joinRoom(code)
        host.submitMove(code, Seat.ONE, Action.Place(c("a7")))
        assertTrue(host.offerRematch(code, Seat.ONE) is SubmitResult.Rejected) // still playing
        host.resign(code, Seat.ONE)
        host.offerRematch(code, Seat.ONE)
        assertEquals(RoomPhase.FINISHED, store.peek(code)!!.phase)
        guest.offerRematch(code, Seat.TWO)
        val room = store.peek(code)!!
        assertEquals(RoomPhase.PLAYING, room.phase)
        assertTrue(room.moves.isEmpty())
        assertNull(room.resigned)
    }

    @Test fun disconnectReconnectAndAbandonTimeout() = runTest {
        val code = host.createRoom(RoomConfig()).code
        guest.joinRoom(code)
        host.submitMove(code, Seat.ONE, Action.Place(c("a7")))
        guest.disconnect(code, Seat.TWO)
        assertFalse(Seat.TWO in store.peek(code)!!.connected)
        // Too early to claim.
        now += 30_000
        assertTrue(host.claimAbandoned(code, Seat.ONE) is SubmitResult.Rejected)
        // Guest comes back: the countdown is cancelled and the game continues where it was.
        val back = guest.reconnect(code, Seat.TWO)
        assertTrue(Seat.TWO in back.connected)
        assertNull(RoomRules.abandonCountdown(back, Seat.ONE, now))
        assertTrue(guest.submitMove(code, Seat.TWO, Action.Place(c("d7"))) is SubmitResult.Accepted)
        // Leaves again, and this time stays away.
        guest.disconnect(code, Seat.TWO)
        now += RoomRules.ABANDON_AFTER_MS - 1
        assertNotNull(RoomRules.abandonCountdown(store.peek(code)!!, Seat.ONE, now))
        assertTrue(host.claimAbandoned(code, Seat.ONE) is SubmitResult.Rejected)
        now += 1
        assertTrue(host.claimAbandoned(code, Seat.ONE) is SubmitResult.Accepted)
        val room = store.peek(code)!!
        assertEquals(RoomPhase.ABANDONED, room.phase)
        assertEquals(GameResult.Win(Player.ONE, WinReason.ABANDONED), RoomRules.resultOf(room))
    }

    @Test fun corruptMoveListIsDetected() {
        val bad = RoomSnapshot(
            code = "ABCDEF", config = RoomConfig(), phase = RoomPhase.PLAYING,
            moves = listOf(MoveRecord(0, Seat.TWO, Action.Place(0))), connected = Seat.entries.toSet(),
        )
        assertTrue(runCatching { bad.replay() }.isFailure)
        assertTrue(RoomRules.submit(bad, Seat.ONE, Action.Place(1), 0) is SubmitResult.Rejected)
    }

    @Test fun hostLeavingAFinishedRoomDeletesIt() = runTest {
        val code = host.createRoom(RoomConfig(Variant.CLASSIC)).code
        guest.joinRoom(code)
        // Unfinished: leaving only marks the seat away, so the player can come back.
        host.leaveRoom(code, Seat.ONE, RoomPhase.PLAYING)
        assertNotNull(store.peek(code))
        assertFalse(Seat.ONE in store.peek(code)!!.connected)
        guest.resign(code, Seat.TWO)
        // The guest leaving a finished room doesn't delete it (the host may still be looking).
        guest.leaveRoom(code, Seat.TWO, RoomPhase.FINISHED)
        assertNotNull(store.peek(code))
        host.leaveRoom(code, Seat.ONE, RoomPhase.FINISHED)
        assertNull(store.peek(code))
    }

    @Test fun deleteMyDataRemovesOnlyMyRooms() = runTest {
        val a = host.createRoom(RoomConfig(Variant.CLASSIC)).code
        val b = guest.createRoom(RoomConfig(Variant.TRIANGLE)).code
        host.joinRoom(b)
        val other = InMemoryOnlineGameRepository(store, { now }, Random(3))
        val c = other.createRoom(RoomConfig(Variant.CLASSIC)).code
        val r = host.deleteMyData()
        assertEquals(2, r.roomsDeleted)
        assertTrue(r.identityDeleted)
        assertNull(store.peek(a))
        assertNull(store.peek(b))
        assertNotNull(store.peek(c))
        assertEquals(0, host.deleteMyData().roomsDeleted) // idempotent
    }

    @Test fun securityRulesUseTheSameRetention() {
        val rules = java.io.File("../firebase/database.rules.json").readText()
        assertTrue(rules.contains("now - ${RoomRules.RETENTION_DAYS * 86_400_000L}"))
        assertTrue(java.io.File("../firebase/purge.mjs").readText().contains("--days N (default ${RoomRules.RETENTION_DAYS})"))
    }
}
