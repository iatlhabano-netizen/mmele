package bw.mmele

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import bw.mmele.data.AppStore
import bw.mmele.data.Outcome
import bw.mmele.data.StatKey
import bw.mmele.feedback.FeedbackController
import bw.mmele.feedback.Haptics
import bw.mmele.feedback.SoundFx
import bw.mmele.online.FirebaseOnlineGameRepository
import bw.mmele.ui.AppDataSink
import bw.mmele.ui.MmeleApp
import com.tshipiworks.mmele.BuildConfig
import com.tshipiworks.mmele.R
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AppCompatActivity for per-app language (AppCompatDelegate) on every Android version. The
 * system splash stays up while the small preference files load off the main thread.
 */
class MainActivity : AppCompatActivity() {
    private var feedback: FeedbackController? = null
    @Volatile private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val fb = FeedbackController(
            sounds = runCatching { SoundFx(applicationContext) }.getOrNull(),
            haptics = runCatching { Haptics(applicationContext) }.getOrNull(),
        )
        feedback = fb
        val prefs = ModePrefs(applicationContext)
        val store = AppStore(applicationContext)
        // Writes outlive the screen that triggered them (e.g. a save while leaving).
        val sink = object : AppDataSink {
            fun io(block: suspend () -> Unit) { lifecycleScope.launch { withContext(NonCancellable) { runCatching { block() } } } }
            override fun saveGame(encoded: String?) = io { store.saveGame(encoded) }
            override fun addResult(key: StatKey, outcome: Outcome) = io { store.addResult(key, outcome) }
            override fun resetStats() = io { store.resetStats() }
            override fun setLessonsDone(n: Int) = io { store.setLessonsDone(n) }
            override fun setHowToPlaySeen() = io { store.setHowToPlaySeen() }
        }
        lifecycleScope.launch {
            // DataStore reads on its own IO dispatcher; the main thread only waits on the splash.
            val choice = prefs.load()
            val data = store.load()
            setContent {
                MmeleApp(
                    feedback = fb,
                    onlineRepo = { FirebaseOnlineGameRepository.createOrNull(applicationContext) },
                    onShareCode = ::shareRoomCode,
                    initialMode = choice.mode,
                    initialDifficulty = choice.difficulty,
                    onlineConfigured = BuildConfig.FIREBASE_CONFIGURED,
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    onModeChoice = { mode, difficulty -> lifecycleScope.launch { prefs.save(ModePrefs.Choice(mode, difficulty)) } },
                    data = data,
                    sink = sink,
                    language = AppLanguage.current(),
                    onLanguage = { it.apply() },
                )
            }
            ready = true
        }
    }

    /** Android share sheet with the room code (WhatsApp, SMS, …). */
    private fun shareRoomCode(code: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, code))
        }
        startActivity(Intent.createChooser(send, getString(R.string.share_title)))
    }

    override fun onDestroy() {
        feedback?.release()
        feedback = null
        super.onDestroy()
    }
}
