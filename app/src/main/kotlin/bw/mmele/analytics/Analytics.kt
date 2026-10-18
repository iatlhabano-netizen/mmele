package bw.mmele.analytics

import android.content.Context
import android.os.Bundle

/**
 * Product analytics + crash reports (Firebase Analytics / Crashlytics). Only active when the
 * build has google-services.json AND the player hasn't opted out (Settings → "Share usage &
 * crash data"). Events carry no personal data: game names, modes, results, step numbers.
 * `first_open` is collected automatically by Firebase (it is a reserved name).
 */
interface Analytics {
    fun log(event: String, params: Map<String, Any> = emptyMap())
    fun setEnabled(enabled: Boolean)

    object None : Analytics {
        override fun log(event: String, params: Map<String, Any>) {}
        override fun setEnabled(enabled: Boolean) {}
    }
}

/** Event names and parameter shapes in one place (also listed in docs/ANALYTICS.md). */
object Events {
    const val MATCH_STARTED = "match_started"      // game, mode
    const val MATCH_COMPLETED = "match_completed"  // game, mode, result
    const val TUTORIAL_STEP = "tutorial_step"      // lesson, step
    const val ROOM_CREATED = "online_room_created" // ranked
    const val ROOM_JOINED = "online_room_joined"
    const val CHAT_SENT = "chat_sent"              // kind (p/e/t); never the text
    const val RANKED_MATCH = "ranked_match"        // result
    const val TOURNAMENT_JOINED = "tournament_joined" // size
    const val FACT_SHOWN = "fact_shown"            // category
    val ALL = listOf(MATCH_STARTED, MATCH_COMPLETED, TUTORIAL_STEP, ROOM_CREATED, ROOM_JOINED, CHAT_SENT, RANKED_MATCH, TOURNAMENT_JOINED, FACT_SHOWN)
}

/** Holder so screens can log without threading a parameter everywhere. */
object Tracker {
    @Volatile var impl: Analytics = Analytics.None
    fun log(event: String, vararg params: Pair<String, Any>) { runCatching { impl.log(event, params.toMap()) } }
}

class FirebaseAnalyticsTracker private constructor(private val fa: com.google.firebase.analytics.FirebaseAnalytics) : Analytics {
    override fun log(event: String, params: Map<String, Any>) {
        val b = Bundle()
        for ((k, v) in params) when (v) {
            is Int -> b.putLong(k, v.toLong()); is Long -> b.putLong(k, v); is Boolean -> b.putString(k, v.toString())
            else -> b.putString(k, v.toString().take(100))
        }
        fa.logEvent(event, b)
    }

    override fun setEnabled(enabled: Boolean) {
        fa.setAnalyticsCollectionEnabled(enabled)
        runCatching { com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled }
    }

    companion object {
        /** Null when Firebase isn't configured in this build. */
        fun createOrNull(context: Context, configured: Boolean): Analytics? {
            if (!configured) return null
            return runCatching {
                com.google.firebase.FirebaseApp.getApps(context).firstOrNull() ?: com.google.firebase.FirebaseApp.initializeApp(context) ?: return null
                FirebaseAnalyticsTracker(com.google.firebase.analytics.FirebaseAnalytics.getInstance(context))
            }.getOrNull()
        }
    }
}
