package bw.mmele.online.ranked

import kotlinx.coroutines.flow.Flow

enum class TournamentStatus { OPEN, RUNNING, FINISHED }

data class TournamentInfo(
    val id: String,
    val name: String,
    val size: Int,
    val entry: Int,
    val createdBy: String,
    val status: TournamentStatus,
    val entrants: List<String>,
    /** Present once running: the bracket (with each match's room code, if opened). */
    val bracket: Bracket? = null,
    val rooms: Map<String, String> = emptyMap(),
    val champion: String? = null,
    /** Why the server refused your entry: "full", "points" or "closed". */
    val rejected: Map<String, String> = emptyMap(),
)

/**
 * Ranked play and tournaments (virtual points only). Reads are direct; every balance or rating
 * change is made by the Cloud Function (firebase/functions/index.js), never by the client.
 */
interface RankedRepository {
    suspend fun myUid(): String
    /** Null until the player's first settled ranked match or tournament entry (show the start values). */
    suspend fun myStanding(): PlayerStanding?
    suspend fun leaderboard(limit: Int = 50): List<PlayerStanding>
    fun tournaments(): Flow<List<TournamentInfo>>
    suspend fun createTournament(name: String, size: Int): Result<String>
    suspend fun join(id: String): Result<Unit>
    suspend fun leave(id: String): Result<Unit>
}
