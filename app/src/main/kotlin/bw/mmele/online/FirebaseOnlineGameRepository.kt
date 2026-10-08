package bw.mmele.online

import android.content.Context
import bw.mmele.BuildConfig
import bw.mmele.engine.Action
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Realtime Database transport. Why RTDB rather than Firestore: native presence via
 * `onDisconnect()` (Firestore has none without Cloud Functions), lower latency for tiny
 * frequent writes, and the free Spark plan covers it. Legality is checked twice: locally with
 * [RoomRules] before writing, and structurally by `firebase/database.rules.json` (append-only,
 * your own seat, exact index). Every client replays the move list through `:engine`, so an
 * illegal move from a modified client is detected and surfaced rather than silently applied.
 */
class FirebaseOnlineGameRepository(
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth,
) : OnlineGameRepository {

    private var presence: Pair<DatabaseReference, ValueEventListener>? = null
    private val rooms get() = db.getReference("rooms")

    private suspend fun uid(): String =
        auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: error("Anonymous sign-in failed")

    override suspend fun createRoom(config: RoomConfig): RoomSnapshot {
        val me = uid()
        repeat(6) {
            val code = randomRoomCode()
            val room = RoomRules.create(code, config, System.currentTimeMillis())
            val ok = runCatching { rooms.child(code).setValue(server(RtdbPayloads.create(room, me))).await() }.isSuccess // denied if code taken
            if (ok) {
                watchPresence(code, Seat.ONE)
                return room
            }
        }
        error("Could not reserve a room code — check your connection")
    }

    override suspend fun joinRoom(code: String): RoomSnapshot {
        val key = code.trim().uppercase()
        val me = uid()
        val snap = rooms.child(key).get().await()
        if (!snap.exists()) throw RoomNotFoundException(key)
        val guestUid = snap.child("guestUid").getValue(String::class.java)
        val hostUid = snap.child("hostUid").getValue(String::class.java)
        if (guestUid == me) return reconnect(key, Seat.TWO)
        if (hostUid == me) return reconnect(key, Seat.ONE)
        if (guestUid != null) throw RoomFullException(key)
        runCatching { rooms.child(key).updateChildren(server(RtdbPayloads.join(me))).await() }
            .onFailure { throw RoomFullException(key) }
        watchPresence(key, Seat.TWO)
        return read(key)
    }

    override suspend fun reconnect(code: String, seat: Seat): RoomSnapshot {
        uid()
        rooms.child(code).updateChildren(server(RtdbPayloads.reconnect(seat))).await()
        watchPresence(code, seat)
        return read(code)
    }

    /** Re-asserts presence whenever the socket reconnects, and arms onDisconnect. */
    private fun watchPresence(code: String, seat: Seat) {
        presence?.let { (ref, l) -> ref.removeEventListener(l) }
        val s = Wire.seatName(seat)
        val room = rooms.child(code)
        val info = db.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) != true) return
                room.child("connected/$s").onDisconnect().setValue(false)
                room.child("leftAt/$s").onDisconnect().setValue(ServerValue.TIMESTAMP)
                room.updateChildren(mapOf("connected/$s" to true, "leftAt/$s" to null))
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        info.addValueEventListener(listener)
        presence = info to listener
    }

    private suspend fun read(code: String): RoomSnapshot {
        val snap = rooms.child(code).get().await()
        @Suppress("UNCHECKED_CAST")
        val value = snap.value as? Map<*, *> ?: throw RoomNotFoundException(code)
        return Wire.fromMap(code, value) ?: error("Room $code is corrupt")
    }

    override fun observe(code: String): Flow<RoomSnapshot> = callbackFlow {
        val ref = rooms.child(code)
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.value as? Map<*, *> ?: return
                Wire.fromMap(code, value)?.let { trySend(it) }
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(l)
        awaitClose { ref.removeEventListener(l) }
    }

    override suspend fun submitMove(code: String, seat: Seat, action: Action): SubmitResult {
        val room = read(code)
        val r = RoomRules.submit(room, seat, action, System.currentTimeMillis())
        if (r !is SubmitResult.Accepted) return r
        return write(code, RtdbPayloads.move(room, r.room, seat, action), r)
    }

    override suspend fun resign(code: String, seat: Seat): SubmitResult {
        val r = RoomRules.resign(read(code), seat, System.currentTimeMillis())
        if (r !is SubmitResult.Accepted) return r
        return write(code, RtdbPayloads.resign(seat), r)
    }

    override suspend fun offerDraw(code: String, seat: Seat): SubmitResult {
        val r = RoomRules.offerDraw(read(code), seat, System.currentTimeMillis())
        if (r !is SubmitResult.Accepted) return r
        return write(code, RtdbPayloads.offerDraw(r.room, seat), r)
    }

    override suspend fun offerRematch(code: String, seat: Seat): SubmitResult {
        val r = RoomRules.offerRematch(read(code), seat, System.currentTimeMillis())
        if (r !is SubmitResult.Accepted) return r
        return write(code, RtdbPayloads.offerRematch(r.room, seat), r)
    }

    override suspend fun claimAbandoned(code: String, seat: Seat): SubmitResult {
        val r = RoomRules.claimAbandoned(read(code), seat, System.currentTimeMillis())
        if (r !is SubmitResult.Accepted) return r
        return write(code, RtdbPayloads.claimAbandoned(seat), r)
    }

    override suspend fun disconnect(code: String, seat: Seat) {
        runCatching { rooms.child(code).updateChildren(server(RtdbPayloads.disconnect(seat))).await() }
    }

    override fun close() {
        presence?.let { (ref, l) -> ref.removeEventListener(l) }
        presence = null
    }

    private suspend fun write(code: String, update: Map<String, Any?>, ok: SubmitResult): SubmitResult =
        runCatching { rooms.child(code).updateChildren(server(update)).await() }
            .fold({ ok }, { SubmitResult.Rejected(it.message ?: "write rejected") })

    /** Swap the platform-free timestamp sentinel for Firebase's server timestamp. */
    private fun server(map: Map<String, Any?>): Map<String, Any?> = map.mapValues { (_, v) -> serverValue(v) }

    private fun serverValue(v: Any?): Any? = when (v) {
        RtdbPayloads.SERVER_TIME -> ServerValue.TIMESTAMP
        is Map<*, *> -> v.entries.associate { (k, x) -> k.toString() to serverValue(x) }
        else -> v
    }

    companion object {
        /** Null when the developer has not added google-services.json — the UI then says so. */
        fun createOrNull(context: Context): FirebaseOnlineGameRepository? {
            if (!BuildConfig.FIREBASE_CONFIGURED) return null
            return runCatching {
                val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context) ?: return null
                FirebaseOnlineGameRepository(FirebaseDatabase.getInstance(app), FirebaseAuth.getInstance(app))
            }.getOrNull()
        }
    }
}
