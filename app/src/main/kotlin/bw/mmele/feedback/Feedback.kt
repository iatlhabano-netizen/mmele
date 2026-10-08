package bw.mmele.feedback

/** Discrete game events that produce sound and/or haptic feedback. */
enum class FeedbackEvent { SELECT, PLACE, SLIDE, FLY, MILL, SHOOT, INVALID, WIN, END }

/** Plays the matching sound + haptic for an event, honouring the user's toggles. */
class FeedbackController(
    private val sounds: SoundFx?,
    private val haptics: Haptics?,
) {
    var soundEnabled = true
    var hapticsEnabled = true

    fun play(event: FeedbackEvent) {
        if (soundEnabled) sounds?.play(event)
        if (hapticsEnabled) haptics?.perform(event)
    }

    fun release() {
        sounds?.release()
    }
}
