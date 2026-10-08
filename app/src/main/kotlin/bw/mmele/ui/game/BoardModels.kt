package bw.mmele.ui.game

import androidx.compose.runtime.Immutable
import bw.mmele.engine.Player

/** A cow on the board with a stable identity, so the UI can animate it between nodes. */
@Immutable
data class TokenUi(val id: Int, val player: Player, val node: Int)

/** A freshly formed mill (or winning line) to trace in gold. */
@Immutable
data class MillFlash(val id: Int, val lines: List<List<Int>>, val player: Player)

/** A cow that was just shot (drawn fading out). */
@Immutable
data class ShotFx(val id: Int, val node: Int, val player: Player)

@Immutable
data class LastMove(val from: Int?, val to: Int)
