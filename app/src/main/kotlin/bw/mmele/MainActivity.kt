package bw.mmele

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import bw.mmele.feedback.FeedbackController
import bw.mmele.feedback.Haptics
import bw.mmele.feedback.SoundFx
import bw.mmele.ui.MmeleApp
import android.content.Intent
import bw.mmele.online.FirebaseOnlineGameRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private var feedback: FeedbackController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val fb = FeedbackController(
            sounds = runCatching { SoundFx(applicationContext) }.getOrNull(),
            haptics = runCatching { Haptics(applicationContext) }.getOrNull(),
        )
        feedback = fb
        // One tiny preferences file; reading it before the first frame avoids a flash of the wrong mode.
        val prefs = ModePrefs(applicationContext)
        val saved = runBlocking { prefs.load() }
        setContent {
            MmeleApp(
                feedback = fb,
                onlineRepo = { FirebaseOnlineGameRepository.createOrNull(applicationContext) },
                onShareCode = ::shareRoomCode,
                initialMode = saved.mode,
                initialDifficulty = saved.difficulty,
                onModeChoice = { mode, difficulty -> lifecycleScope.launch { prefs.save(ModePrefs.Choice(mode, difficulty)) } },
            )
        }
    }

    /** Android share sheet with the room code (WhatsApp, SMS, …). */
    private fun shareRoomCode(code: String) {
        val text = "Play Mmele (Morabaraba) with me! Open the app, tap \"Play online\" and enter room code $code"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "Share room code"))
    }

    override fun onDestroy() {
        feedback?.release()
        feedback = null
        super.onDestroy()
    }
}
