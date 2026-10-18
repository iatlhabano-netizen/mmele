package bw.mmele.online

import bw.mmele.online.chat.ChatKind
import bw.mmele.online.chat.ChatMessage
import bw.mmele.online.chat.ChatPolicy
import bw.mmele.online.chat.ChatReport
import bw.mmele.online.chat.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * chats/{code}: readable and writable only by the room's two players (database.rules.json), one
 * message per 1.5 s per player (users/{uid}/lastChatAt stamped in the same write), ≤ 120 chars.
 * Deleted with the room (and by purge.mjs for orphans). Reports go to reports/ (admin-only read).
 */
class FirebaseChatRepository(private val db: FirebaseDatabase, private val auth: FirebaseAuth) : ChatRepository {
    private fun uid() = auth.currentUser?.uid ?: error("not signed in")

    override fun observe(code: String): Flow<List<ChatMessage>> = callbackFlow {
        val q = db.getReference("chats").child(code).orderByKey().limitToLast(ChatPolicy.KEEP_LAST)
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                trySend(s.children.mapNotNull { c ->
                    val kind = ChatKind.of(c.child("k").getValue(String::class.java)) ?: return@mapNotNull null
                    ChatMessage(
                        id = c.key ?: return@mapNotNull null,
                        seat = if (c.child("seat").getValue(String::class.java) == "two") Seat.TWO else Seat.ONE,
                        uid = c.child("uid").getValue(String::class.java).orEmpty(),
                        kind = kind,
                        body = c.child("b").getValue(String::class.java).orEmpty(),
                        at = c.child("at").getValue(Long::class.java) ?: 0L,
                    )
                })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        q.addValueEventListener(l)
        awaitClose { q.removeEventListener(l) }
    }

    override suspend fun send(code: String, seat: Seat, kind: ChatKind, body: String): Result<Unit> = runCatching {
        val me = uid()
        val key = db.getReference("chats").child(code).push().key ?: error("no key")
        db.reference.updateChildren(
            mapOf(
                "chats/$code/$key" to mapOf("uid" to me, "seat" to Wire.seatName(seat), "k" to kind.wire, "b" to body, "at" to ServerValue.TIMESTAMP),
                "users/$me/lastChatAt" to ServerValue.TIMESTAMP,
            ),
        ).await()
    }

    override suspend fun report(report: ChatReport): Result<Unit> = runCatching {
        db.getReference("reports").push().setValue(
            mapOf(
                "reporterUid" to uid(), "reportedUid" to report.reportedUid, "room" to report.roomCode,
                "messageId" to report.messageId, "body" to report.body.take(ChatPolicy.MAX_TEXT), "at" to ServerValue.TIMESTAMP,
            ),
        ).await()
    }

    override suspend fun block(uid: String): Result<Unit> = runCatching { db.getReference("users/${uid()}/blocked/$uid").setValue(true).await() }
    override suspend fun unblock(uid: String): Result<Unit> = runCatching { db.getReference("users/${uid()}/blocked/$uid").removeValue().await() }
    override suspend fun blocked(): Set<String> = runCatching {
        db.getReference("users/${uid()}/blocked").get().await().children.mapNotNull { it.key }.toSet()
    }.getOrDefault(emptySet())

    override suspend fun deleteChat(code: String) { runCatching { db.getReference("chats").child(code).removeValue().await() } }
}
