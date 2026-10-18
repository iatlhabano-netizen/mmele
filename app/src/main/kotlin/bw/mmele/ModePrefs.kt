package bw.mmele

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.game.GameMode
import kotlinx.coroutines.flow.first

private val Context.homePrefs: DataStore<Preferences> by preferencesDataStore(name = "home")

/** Remembers the home-screen mode and AI difficulty across launches. First launch: vs Computer, Medium. */
class ModePrefs(private val context: Context) {
    data class Choice(val mode: GameMode = GameMode.SOLO, val difficulty: Difficulty = Difficulty.MEDIUM)

    suspend fun load(): Choice {
        val p = runCatching { context.homePrefs.data.first() }.getOrNull() ?: return Choice()
        return Choice(
            mode = p[MODE]?.let { n -> GameMode.entries.firstOrNull { it.name == n } } ?: GameMode.SOLO,
            difficulty = p[DIFFICULTY]?.let { n -> Difficulty.entries.firstOrNull { it.name == n } } ?: Difficulty.MEDIUM,
        )
    }

    suspend fun save(choice: Choice) {
        context.homePrefs.edit {
            it[MODE] = choice.mode.name
            it[DIFFICULTY] = choice.difficulty.name
        }
    }

    private companion object {
        val MODE = stringPreferencesKey("mode")
        val DIFFICULTY = stringPreferencesKey("difficulty")
    }
}

/** Settings toggles: online chat (on by default) and usage/crash data sharing (on unless opted out). */
class SettingsPrefs(private val context: Context) {
    data class Settings(val chatEnabled: Boolean = true, val analyticsEnabled: Boolean = true)

    suspend fun load(): Settings {
        val p = runCatching { context.homePrefs.data.first() }.getOrNull() ?: return Settings()
        return Settings(p[CHAT] ?: true, p[ANALYTICS] ?: true)
    }

    suspend fun save(s: Settings) {
        context.homePrefs.edit { it[CHAT] = s.chatEnabled; it[ANALYTICS] = s.analyticsEnabled }
    }

    private companion object {
        val CHAT = booleanPreferencesKey("chat_enabled")
        val ANALYTICS = booleanPreferencesKey("analytics_enabled")
    }
}
