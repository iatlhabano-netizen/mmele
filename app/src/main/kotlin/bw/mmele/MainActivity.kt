package bw.mmele

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import bw.mmele.feedback.FeedbackController
import bw.mmele.feedback.Haptics
import bw.mmele.feedback.SoundFx
import bw.mmele.ui.MmeleApp

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
        setContent { MmeleApp(fb) }
    }

    override fun onDestroy() {
        feedback?.release()
        feedback = null
        super.onDestroy()
    }
}
