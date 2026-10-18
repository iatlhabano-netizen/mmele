package bw.mmele.online

import bw.mmele.online.ranked.Bracket
import bw.mmele.online.ranked.BracketMatch
import bw.mmele.online.ranked.PlayerStanding
import bw.mmele.online.ranked.RankedRepository
import bw.mmele.online.ranked.TournamentInfo
import bw.mmele.online.ranked.TournamentStatus
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

/** Reads ratings/ and tournaments/; writes only what the rules allow (create, join, leave). */
class FirebaseRankedRepository(private val db: FirebaseDatabase, private val auth: FirebaseAuth) : RankedRepository {
    override suspend fun myUid(): String = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: error("sign-in failed")

    private fun standing(s: DataSnapshot) = PlayerStanding(
        uid = s.key.orEmpty(),
        rating = s.child("rating").getValue(Int::class.java) ?: 1200,
        points = s.child("points").getValue(Int::class.java) ?: 1000,
        games = s.child("games").getValue(Int::class.java) ?: 0,
        wins = s.child("wins").getValue(Int::class.java) ?: 0,
        name = s.child("name").getValue(String::class.java).orEmpty(),
    )

    override suspend fun myStanding(): PlayerStanding? {
        val s = db.getReference("ratings").child(myUid()).get().await()
        return if (s.exists()) standing(s) else null
    }

    override suspend fun leaderboard(limit: Int): List<PlayerStanding> {
        myUid()
        return db.getReference("ratings").orderByChild("rating").limitToLast(limit).get().await()
            .children.map(::standing).filter { it.games > 0 }.sortedByDescending { it.rating }
    }

    override fun tournaments(): Flow<List<TournamentInfo>> = callbackFlow {
        myUid()
        val q = db.getReference("tournaments").orderByKey().limitToLast(30)
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { trySend(s.children.mapNotNull(::parse).reversed()) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        q.addValueEventListener(l)
        awaitClose { q.removeEventListener(l) }
    }

    private fun parse(t: DataSnapshot): TournamentInfo? {
        val size = t.child("size").getValue(Int::class.java) ?: return null
        val status = when (t.child("status").getValue(String::class.java)) { "running" -> TournamentStatus.RUNNING; "finished" -> TournamentStatus.FINISHED; else -> TournamentStatus.OPEN }
        val ms = t.child("matches").children.mapNotNull { m ->
            val id = m.key ?: return@mapNotNull null
            BracketMatch(m.child("round").getValue(Int::class.java) ?: 1, m.child("index").getValue(Int::class.java) ?: 0,
                m.child("one").getValue(String::class.java), m.child("two").getValue(String::class.java), m.child("winner").getValue(String::class.java)).takeIf { it.id == id }
        }.sortedWith(compareBy({ it.round }, { it.index }))
        return TournamentInfo(
            id = t.key ?: return null,
            name = t.child("name").getValue(String::class.java).orEmpty(),
            size = size,
            entry = t.child("entry").getValue(Int::class.java) ?: Bracket.ENTRY,
            createdBy = t.child("createdBy").getValue(String::class.java).orEmpty(),
            status = status,
            entrants = t.child("entrants").children.mapNotNull { it.key },
            bracket = if (ms.size == size - 1) Bracket(size, ms) else null,
            rooms = t.child("matches").children.mapNotNull { m -> m.child("room").getValue(String::class.java)?.let { m.key!! to it } }.toMap(),
            champion = t.child("champion").getValue(String::class.java),
            rejected = t.child("rejected").children.associate { it.key!! to (it.getValue(String::class.java) ?: "") },
        )
    }

    override suspend fun createTournament(name: String, size: Int): Result<String> = runCatching {
        val me = myUid()
        val ref = db.getReference("tournaments").push()
        ref.setValue(mapOf("name" to name.trim().take(40).ifEmpty { "Mmele Cup" }, "size" to size, "entry" to Bracket.ENTRY, "createdBy" to me, "createdAt" to ServerValue.TIMESTAMP, "status" to "open")).await()
        ref.key!!
    }

    override suspend fun join(id: String): Result<Unit> = runCatching {
        db.getReference("tournaments/$id/entrants/${myUid()}").setValue(ServerValue.TIMESTAMP).await()
    }

    override suspend fun leave(id: String): Result<Unit> = runCatching {
        db.getReference("tournaments/$id/entrants/${myUid()}").removeValue().await()
    }
}
