package bw.mmele.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.appData: DataStore<Preferences> by preferencesDataStore(name = "mmele")

/**
 * Everything the app remembers on the phone (never uploaded, excluded from backup):
 * the in-progress offline game, stats, Academy progress and whether "How to play" was offered.
 */
class AppStore(private val context: Context) {

    data class Snapshot(
        val savedGame: String?,
        val stats: Map<StatKey, Tally>,
        /** Number of Academy lessons completed in order (lesson i is unlocked when i <= this). */
        val lessonsDone: Int,
        val howToPlaySeen: Boolean,
    )

    suspend fun load(): Snapshot {
        val p = runCatching { context.appData.data.first() }.getOrNull()
            ?: return Snapshot(null, emptyMap(), 0, false)
        val stats = HashMap<StatKey, Tally>()
        for ((k, v) in p.asMap()) {
            val name = k.name
            if (!name.startsWith(STAT_PREFIX) || v !is Int) continue
            val key = StatKey.decode(name.removePrefix(STAT_PREFIX).substringBeforeLast('.')) ?: continue
            val t = stats[key] ?: Tally()
            stats[key] = when (name.substringAfterLast('.')) {
                "w" -> t.copy(wins = v)
                "l" -> t.copy(losses = v)
                "d" -> t.copy(draws = v)
                "s" -> t.copy(streak = v)
                "b" -> t.copy(bestStreak = v)
                else -> t
            }
        }
        return Snapshot(p[SAVED_GAME], stats, p[LESSONS_DONE] ?: 0, p[HOW_TO_PLAY_SEEN] ?: false)
    }

    suspend fun saveGame(encoded: String?) {
        context.appData.edit { if (encoded == null) it.remove(SAVED_GAME) else it[SAVED_GAME] = encoded }
    }

    /** Adds one result; returns the updated tally (wins/losses/draws and the win streak). */
    suspend fun addResult(key: StatKey, outcome: Outcome): Tally {
        var out = Tally()
        context.appData.edit { p ->
            fun k(x: String) = intPreferencesKey("$STAT_PREFIX${key.encode()}.$x")
            val before = Tally(p[k("w")] ?: 0, p[k("l")] ?: 0, p[k("d")] ?: 0, p[k("s")] ?: 0, p[k("b")] ?: 0)
            out = before.plus(outcome)
            p[k("w")] = out.wins; p[k("l")] = out.losses; p[k("d")] = out.draws
            p[k("s")] = out.streak; p[k("b")] = out.bestStreak
        }
        return out
    }

    suspend fun resetStats() {
        context.appData.edit { prefs ->
            prefs.asMap().keys.filter { it.name.startsWith(STAT_PREFIX) }.forEach { prefs.remove(it) }
        }
    }

    suspend fun setLessonsDone(n: Int) {
        context.appData.edit { it[LESSONS_DONE] = maxOf(n, it[LESSONS_DONE] ?: 0) }
    }

    suspend fun setHowToPlaySeen() {
        context.appData.edit { it[HOW_TO_PLAY_SEEN] = true }
    }

    /** Persisted no-repeat cycles for the game-over facts and photos (see [FactCards]). */
    suspend fun loadDecks(): Pair<DeckState?, DeckState?> {
        val p = runCatching { context.appData.data.first() }.getOrNull() ?: return null to null
        return DeckState.decode(p[FACT_DECK]) to DeckState.decode(p[PHOTO_DECK])
    }

    suspend fun saveDecks(facts: DeckState, photos: DeckState?) {
        context.appData.edit {
            it[FACT_DECK] = facts.encode()
            if (photos != null) it[PHOTO_DECK] = photos.encode()
        }
    }

    private companion object {
        val FACT_DECK = stringPreferencesKey("fact_deck")
        val PHOTO_DECK = stringPreferencesKey("photo_deck")
        const val STAT_PREFIX = "stat."
        val SAVED_GAME = stringPreferencesKey("saved_game")
        val LESSONS_DONE = intPreferencesKey("lessons_done")
        val HOW_TO_PLAY_SEEN = booleanPreferencesKey("how_to_play_seen")
    }
}
