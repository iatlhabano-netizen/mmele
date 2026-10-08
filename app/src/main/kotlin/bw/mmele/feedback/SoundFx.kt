package bw.mmele.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import bw.mmele.R

/** Low-latency SoundPool playback of the procedurally generated effects in res/raw. */
class SoundFx(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = HashSet<Int>()
    private val ids: Map<FeedbackEvent, Int>

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) synchronized(loaded) { loaded += sampleId } }
        fun load(res: Int) = pool.load(context, res, 1)
        ids = mapOf(
            FeedbackEvent.SELECT to load(R.raw.select),
            FeedbackEvent.PLACE to load(R.raw.place),
            FeedbackEvent.SLIDE to load(R.raw.slide),
            FeedbackEvent.FLY to load(R.raw.fly),
            FeedbackEvent.MILL to load(R.raw.mill),
            FeedbackEvent.SHOOT to load(R.raw.shoot),
            FeedbackEvent.INVALID to load(R.raw.invalid),
            FeedbackEvent.WIN to load(R.raw.win),
            FeedbackEvent.END to load(R.raw.end),
        )
    }

    fun play(event: FeedbackEvent) {
        val id = ids[event] ?: return
        if (synchronized(loaded) { id !in loaded }) return
        val vol = when (event) {
            FeedbackEvent.SELECT, FeedbackEvent.INVALID -> 0.5f
            else -> 0.9f
        }
        pool.play(id, vol, vol, 1, 0, 1f)
    }

    fun release() = pool.release()
}
