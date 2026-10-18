package bw.mmele.feedback

/** Discrete game events that produce sound and/or haptic feedback. */
/** [DROP] is one Moruba stone landing in a pit (played with a slight random pitch). */
enum class FeedbackEvent { SELECT, PLACE, SLIDE, FLY, MILL, SHOOT, INVALID, WIN, END, DROP }

/** Plays the matching sound + haptic for an event, honouring the user's toggles. */
class FeedbackController(
    private val sounds: SoundFx?,
    private val haptics: Haptics?,
) {
    var soundEnabled = true
    var hapticsEnabled = true

    fun play(event: FeedbackEvent, rate: Float = 1f) {
        if (soundEnabled) sounds?.play(event, rate)
        if (hapticsEnabled) haptics?.perform(event)
    }

    fun release() {
        sounds?.release()
    }
}
