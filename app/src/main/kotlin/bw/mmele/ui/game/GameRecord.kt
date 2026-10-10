package bw.mmele.ui.game

import bw.mmele.engine.Action
import bw.mmele.engine.GameState
import bw.mmele.engine.Player
import bw.mmele.engine.RuleSet
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import bw.mmele.engine.WinReason
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.scenes.Scene

/**
 * A saved offline game: its settings, every action from the start with both clocks as they were
 * just before it (so undo can restore time), the clocks now, and a forfeit that isn't in the move
 * list (a timeout). Replaying [actions] from a new game rebuilds the exact position, which is far
 * more robust than storing engine internals.
 *
 * Text format (one line, versioned): `mmele-game/1|key=value|...`, see [encode].
 */
data class GameRecord(
    val config: GameConfig,
    val actions: List<Action>,
    /** clocksBefore[i] = (dark ms, light ms) at the moment actions[i] was played. */
    val clocksBefore: List<Pair<Long, Long>>,
    val clockMs1: Long,
    val clockMs2: Long,
    /** Player whose clock ran out, if the game ended on time. */
    val timedOut: Player? = null,
) {
    init {
        require(actions.size == clocksBefore.size) { "one clock snapshot per action" }
    }

    /** Replays the moves; null if they aren't legal (corrupt or from an incompatible version). */
    fun replay(): GameState? {
        var g = Rules.newGame(RuleSet.of(config.variant))
        for (a in actions) {
            if (g.isGameOver || !Rules.isLegal(g, a)) return null
            g = Rules.apply(g, a)
        }
        return if (timedOut != null) Rules.forfeit(g, timedOut, WinReason.TIMEOUT) else g
    }

    fun encode(): String = buildString {
        append(MAGIC)
        fun field(k: String, v: Any) { append('|').append(k).append('=').append(v) }
        field("v", config.variant.name)
        field("m", config.mode.name)
        field("d", config.difficulty.name)
        field("h", config.human.name)
        field("c", config.clockMinutes)
        field("s", config.scene.name)
        field("p1", config.styleOne.id)
        field("p2", config.styleTwo.id)
        field("t", "$clockMs1,$clockMs2")
        if (timedOut != null) field("x", timedOut.name)
        field("a", actions.indices.joinToString(",") { "${actions[it].encode()}:${clocksBefore[it].first}:${clocksBefore[it].second}" })
    }

    companion object {
        private const val MAGIC = "mmele-game/1"

        fun decode(text: String?): GameRecord? = runCatching {
            if (text == null || !text.startsWith(MAGIC)) return null
            val f = text.removePrefix(MAGIC).split('|').filter { it.isNotEmpty() }
                .associate { it.substringBefore('=') to it.substringAfter('=') }
            val config = GameConfig(
                variant = Variant.valueOf(f.getValue("v")),
                mode = GameMode.valueOf(f.getValue("m")),
                difficulty = Difficulty.valueOf(f.getValue("d")),
                human = Player.valueOf(f.getValue("h")),
                clockMinutes = f.getValue("c").toInt(),
                scene = Scene.valueOf(f.getValue("s")),
                styleOne = PieceStyle.fromId(f["p1"]) ?: PieceStyle.Obsidian,
                styleTwo = PieceStyle.fromId(f["p2"]) ?: PieceStyle.Quartz,
            )
            if (config.mode == GameMode.ONLINE) return null
            val (t1, t2) = f.getValue("t").split(',').map { it.toLong() }
            val entries = f.getValue("a").split(',').filter { it.isNotEmpty() }.map { e ->
                val (code, c1, c2) = e.split(':')
                Action.decode(code.toInt()) to (c1.toLong() to c2.toLong())
            }
            GameRecord(
                config = config,
                actions = entries.map { it.first },
                clocksBefore = entries.map { it.second },
                clockMs1 = t1,
                clockMs2 = t2,
                timedOut = f["x"]?.let { Player.valueOf(it) },
            ).takeIf { it.replay() != null }
        }.getOrNull()
    }
}
