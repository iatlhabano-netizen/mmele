package bw.mmele.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Tactile feedback built on [VibrationEffect]. Uses predefined effects on API 29+ and custom
 * amplitude waveforms where the motor supports amplitude control, degrading gracefully to
 * simple timings-only patterns (API 26+) or nothing at all.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Throwable) {
        null
    }

    private val available = vibrator?.hasVibrator() == true
    private val amplitude = available && vibrator?.hasAmplitudeControl() == true

    fun perform(event: FeedbackEvent) {
        if (!available) return
        try {
            when (event) {
                FeedbackEvent.SELECT -> predefinedOr(EFFECT_TICK) { oneShot(8, 70) }
                FeedbackEvent.DROP -> predefinedOr(EFFECT_TICK) { oneShot(6, 60) }
                FeedbackEvent.PLACE -> predefinedOr(EFFECT_CLICK) { oneShot(14, 140) }
                FeedbackEvent.SLIDE -> predefinedOr(EFFECT_TICK) { oneShot(10, 110) }
                FeedbackEvent.FLY -> waveform(longArrayOf(0, 12, 40, 16), intArrayOf(0, 60, 0, 160))
                // Crisp "snap": a sharp hit, short gap, then a firmer second pulse.
                FeedbackEvent.MILL -> waveform(longArrayOf(0, 14, 34, 22, 30, 10), intArrayOf(0, 255, 0, 200, 0, 120))
                FeedbackEvent.SHOOT -> predefinedOr(EFFECT_HEAVY_CLICK) { oneShot(32, 255) }
                FeedbackEvent.INVALID -> predefinedOr(EFFECT_DOUBLE_CLICK) {
                    waveform(longArrayOf(0, 12, 60, 12), intArrayOf(0, 120, 0, 120))
                }
                FeedbackEvent.WIN -> waveform(
                    longArrayOf(0, 20, 60, 24, 60, 30, 60, 60),
                    intArrayOf(0, 90, 0, 140, 0, 200, 0, 255),
                )
                FeedbackEvent.END -> waveform(longArrayOf(0, 40, 80, 40), intArrayOf(0, 150, 0, 80))
            }
        } catch (_: Throwable) {
            // Never let haptics crash the game (OEM quirks, missing permission, etc.).
        }
    }

    private inline fun predefinedOr(effectId: Int, fallback: () -> Unit) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            fallback()
        }
    }

    private fun oneShot(ms: Long, amp: Int) {
        val v = vibrator ?: return
        v.vibrate(VibrationEffect.createOneShot(ms, if (amplitude) amp else VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun waveform(timings: LongArray, amps: IntArray) {
        val v = vibrator ?: return
        if (amplitude) {
            v.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        } else {
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }

    private companion object {
        // VibrationEffect.EFFECT_* constants (API 29) inlined so the class loads on API 26-28.
        const val EFFECT_CLICK = 0
        const val EFFECT_DOUBLE_CLICK = 1
        const val EFFECT_TICK = 2
        const val EFFECT_HEAVY_CLICK = 5
    }
}
