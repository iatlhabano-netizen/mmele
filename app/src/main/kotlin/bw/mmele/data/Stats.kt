package bw.mmele.data

import bw.mmele.engine.GameResult
import bw.mmele.engine.Player
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameMode

enum class Outcome { WIN, LOSS, DRAW }

/** One row of the Stats screen. [difficulty] is set only for games against the computer. */
data class StatKey(val mode: GameMode, val difficulty: Difficulty?, val variant: Variant) {
    fun encode(): String = "${mode.name}.${difficulty?.name ?: "-"}.${variant.name}"

    companion object {
        fun of(config: GameConfig) = StatKey(config.mode, config.difficulty.takeIf { config.mode == GameMode.SOLO }, config.variant)

        fun decode(s: String): StatKey? = runCatching {
            val (m, d, v) = s.split('.')
            StatKey(GameMode.valueOf(m), if (d == "-") null else Difficulty.valueOf(d), Variant.valueOf(v))
        }.getOrNull()

        /** Every row the Stats screen shows, in display order. */
        val ALL: List<StatKey> = Variant.entries.flatMap { v ->
            Difficulty.entries.map { StatKey(GameMode.SOLO, it, v) } +
                StatKey(GameMode.PASS_AND_PLAY, null, v) + StatKey(GameMode.ONLINE, null, v)
        }
    }
}

/** Counts for one row, plus the current and best run of consecutive wins (a loss or draw ends a run). */
data class Tally(val wins: Int = 0, val losses: Int = 0, val draws: Int = 0, val streak: Int = 0, val bestStreak: Int = 0) {
    val games get() = wins + losses + draws
    fun plus(o: Outcome) = when (o) {
        Outcome.WIN -> copy(wins = wins + 1, streak = streak + 1, bestStreak = maxOf(bestStreak, streak + 1))
        Outcome.LOSS -> copy(losses = losses + 1, streak = 0)
        Outcome.DRAW -> copy(draws = draws + 1, streak = 0)
    }
}

object StatsRules {
    /**
     * Outcome from the player's side: vs Computer and online, the person holding the phone;
     * in Pass & Play there is no single "you", so WIN means Dark won and LOSS means Light won.
     */
    fun outcomeOf(config: GameConfig, result: GameResult): Outcome = when (result) {
        is GameResult.Draw -> Outcome.DRAW
        is GameResult.Win -> {
            val me = if (config.mode == GameMode.PASS_AND_PLAY) Player.ONE else config.human
            if (result.winner == me) Outcome.WIN else Outcome.LOSS
        }
    }
}
