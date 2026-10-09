package bw.mmele.online

import bw.mmele.engine.Action
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * Process-local rooms. Two instances created with the same [store] see the same games, which is
 * how the two-client tests work and how the app can demo online play before Firebase is wired.
 */
class InMemoryOnlineGameRepository(
    private val store: RoomStore = RoomStore(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val rng: Random = Random.Default,
) : OnlineGameRepository {

    /** Rooms this client created or joined: the in-memory stand-in for `users/{uid}/rooms`. */
    private val mine = LinkedHashSet<String>()

    override suspend fun createRoom(config: RoomConfig): RoomSnapshot = store.mutate { rooms ->
        var code = randomRoomCode(rng)
        repeat(8) { if (code in rooms) code = randomRoomCode(rng) }
        val room = RoomRules.create(code, config, clock())
        rooms[code] = room
        mine += code
        room
    }

    override suspend fun joinRoom(code: String): RoomSnapshot {
        val key = code.trim().uppercase()
        return store.mutate { rooms ->
            val room = rooms[key] ?: throw RoomNotFoundException(key)
            when (val r = RoomRules.join(room, clock())) {
                is SubmitResult.Accepted -> { rooms[key] = r.room; mine += key; r.room }
                is SubmitResult.Rejected -> throw if (r.reason.contains("full")) RoomFullException(key) else IllegalStateException(r.reason)
            }
        }
    }

    override suspend fun reconnect(code: String, seat: Seat): RoomSnapshot = store.mutate { rooms ->
        val room = rooms[code] ?: throw RoomNotFoundException(code)
        val next = RoomRules.connect(room, seat, clock())
        rooms[code] = next
        next
    }

    override fun observe(code: String): Flow<RoomSnapshot> = store.watch(code)

    override suspend fun submitMove(code: String, seat: Seat, action: Action) = apply(code) { RoomRules.submit(it, seat, action, clock()) }
    override suspend fun resign(code: String, seat: Seat) = apply(code) { RoomRules.resign(it, seat, clock()) }
    override suspend fun offerDraw(code: String, seat: Seat) = apply(code) { RoomRules.offerDraw(it, seat, clock()) }
    override suspend fun offerRematch(code: String, seat: Seat) = apply(code) { RoomRules.offerRematch(it, seat, clock()) }
    override suspend fun claimAbandoned(code: String, seat: Seat) = apply(code) { RoomRules.claimAbandoned(it, seat, clock()) }

    override suspend fun disconnect(code: String, seat: Seat) {
        store.mutate<Unit> { rooms ->
            val room = rooms[code] ?: return@mutate
            rooms[code] = RoomRules.disconnect(room, seat, clock())
        }
    }

    override fun close() {}

    override suspend fun deleteRoom(code: String) {
        store.delete(code)
        mine -= code
    }

    override suspend fun deleteMyData(): DeleteResult {
        val codes = mine.toList()
        codes.forEach { store.delete(it) }
        mine.clear()
        return DeleteResult(codes.size, identityDeleted = true)
    }

    private suspend fun apply(code: String, op: (RoomSnapshot) -> SubmitResult): SubmitResult = store.mutate {
        val room = it[code] ?: return@mutate SubmitResult.Rejected("no such room")
        when (val r = op(room)) {
            is SubmitResult.Accepted -> { it[code] = r.room; r }
            is SubmitResult.Rejected -> r
        }
    }
}

/** Shared mutable map of rooms. One per process in the app; one per test. */
class RoomStore {
    private val mutex = Mutex()
    private val rooms = HashMap<String, RoomSnapshot>()
    private val flows = HashMap<String, MutableStateFlow<RoomSnapshot?>>()

    fun watch(code: String): Flow<RoomSnapshot> {
        val key = code.trim().uppercase()
        val flow = synchronized(flows) { flows.getOrPut(key) { MutableStateFlow(rooms[key]) } }
        return flow.filterNotNull()
    }

    suspend fun <T> mutate(block: (MutableMap<String, RoomSnapshot>) -> T): T = mutex.withLock {
        // Publish after the block so callers can write `rooms[code] = ...` inside.
        val result = block(rooms)
        synchronized(flows) { for ((code, room) in rooms) flows.getOrPut(code) { MutableStateFlow(null) }.value = room }
        result
    }

    fun peek(code: String): RoomSnapshot? = rooms[code]

    suspend fun delete(code: String) = mutex.withLock {
        rooms.remove(code)
        synchronized(flows) { flows[code]?.value = null }
    }
}
