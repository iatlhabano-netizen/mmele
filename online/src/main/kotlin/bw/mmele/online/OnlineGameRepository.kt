package bw.mmele.online

import bw.mmele.engine.Action
import kotlinx.coroutines.flow.Flow

/**
 * Transport for one online game. Implementations:
 *  - [InMemoryOnlineGameRepository] — two clients in one process (tests, and the app until
 *    Firebase is configured).
 *  - `FirebaseOnlineGameRepository` (app module) — Realtime Database, only compiled in when
 *    `google-services.json` is present. See docs/ONLINE_SETUP.md.
 *
 * The repository is a pipe. Legality lives in [RoomRules], which every implementation must apply
 * before a move becomes visible, so a modified client cannot push an illegal action.
 */
interface OnlineGameRepository {
    /** Host a new room. The caller sits in [Seat.ONE]. */
    suspend fun createRoom(config: RoomConfig): RoomSnapshot

    /** Join an existing room by code. The caller sits in [Seat.TWO]. */
    suspend fun joinRoom(code: String): RoomSnapshot

    /** Re-attach to a room you already belong to (process death, network drop). */
    suspend fun reconnect(code: String, seat: Seat): RoomSnapshot

    /** Live snapshots, including your own moves echoed back. */
    fun observe(code: String): Flow<RoomSnapshot>

    suspend fun submitMove(code: String, seat: Seat, action: Action): SubmitResult
    suspend fun resign(code: String, seat: Seat): SubmitResult
    suspend fun offerDraw(code: String, seat: Seat): SubmitResult
    suspend fun offerRematch(code: String, seat: Seat): SubmitResult

    /** Claim the win after the opponent has been away for [RoomRules.ABANDON_AFTER_MS]. */
    suspend fun claimAbandoned(code: String, seat: Seat): SubmitResult

    /** Tell the room this seat's app is backgrounded or gone. */
    suspend fun disconnect(code: String, seat: Seat)

    /** Drop the local subscription. Does not by itself resign. */
    fun close()
}

class RoomNotFoundException(code: String) : Exception("No room with code $code")
class RoomFullException(code: String) : Exception("Room $code is full")
