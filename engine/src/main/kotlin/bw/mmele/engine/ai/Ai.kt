package bw.mmele.engine.ai

import bw.mmele.engine.Action
import bw.mmele.engine.GameState
import bw.mmele.engine.Rules
import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD }

/** A computer opponent. Implementations are pure JVM and safe to call from a background thread. */
interface AiPlayer {
    /**
     * Choose a legal action for `state.toMove`. Must only be called when the game is not over.
     * [shouldStop] is polled during search so callers (e.g. a cancelled coroutine) can abort early;
     * the best action found so far is then returned.
     */
    fun chooseAction(state: GameState, shouldStop: () -> Boolean = { false }): Action
}

object Ai {
    fun create(difficulty: Difficulty, seed: Long = System.nanoTime(), timeLimitMs: Long = defaultTimeMs(difficulty)): AiPlayer =
        when (difficulty) {
            Difficulty.EASY -> EasyAi(Random(seed))
            Difficulty.MEDIUM -> SearchAi(
                SearchConfig(maxDepth = 3, timeLimitMs = timeLimitMs, useTranspositionTable = false, evaluator = Evaluator.MEDIUM),
                Random(seed),
            )
            Difficulty.HARD -> SearchAi(
                SearchConfig(maxDepth = 32, timeLimitMs = timeLimitMs, useTranspositionTable = true, evaluator = Evaluator.HARD),
                Random(seed),
            )
        }

    fun defaultTimeMs(d: Difficulty): Long = when (d) {
        Difficulty.EASY -> 50
        Difficulty.MEDIUM -> 900
        Difficulty.HARD -> 1600
    }
}

/**
 * Easy: heuristic-guided random. Usually (not always) takes a mill when one is available,
 * often blocks an opponent's open two, otherwise plays randomly.
 */
class EasyAi(private val random: Random) : AiPlayer {
    override fun chooseAction(state: GameState, shouldStop: () -> Boolean): Action {
        val actions = Rules.legalActions(state)
        require(actions.isNotEmpty()) { "no legal actions" }
        if (state.mustShoot) {
            // Prefer shooting cows that are part of an enemy "two" (threat), else random.
            val threats = actions.filter { Heuristics.isThreatCow(state, (it as Action.Shoot).target) }
            return if (threats.isNotEmpty() && random.nextFloat() < 0.6f) threats.random(random) else actions.random(random)
        }
        val winning = actions.filter { Heuristics.formsMill(state, it) }
        if (winning.isNotEmpty() && random.nextFloat() < 0.75f) return winning.random(random)
        val blocking = actions.filter { Heuristics.blocksMill(state, it) }
        if (blocking.isNotEmpty() && random.nextFloat() < 0.5f) return blocking.random(random)
        return actions.random(random)
    }
}

/** Small helpers shared by the AIs. */
internal object Heuristics {
    fun targetOf(a: Action): Int = when (a) {
        is Action.Place -> a.to
        is Action.Move -> a.to
        is Action.Shoot -> a.target
    }

    fun formsMill(state: GameState, a: Action): Boolean {
        val own = state.maskOf(state.toMove)
        val newOwn = when (a) {
            is Action.Place -> own or (1 shl a.to)
            is Action.Move -> (own and (1 shl a.from).inv()) or (1 shl a.to)
            is Action.Shoot -> return false
        }
        return Rules.formsMill(state.board, newOwn, targetOf(a))
    }

    /** Does landing on the action's target occupy the empty third node of an enemy two-in-a-line? */
    fun blocksMill(state: GameState, a: Action): Boolean {
        if (a is Action.Shoot) return false
        val to = targetOf(a)
        val enemy = state.maskOf(state.toMove.opponent)
        val b = state.board
        for (m in b.millsAt[to]) {
            val others = b.millMasks[m] and (1 shl to).inv()
            if (others and enemy == others) return true
        }
        return false
    }

    /** Is the enemy cow at [node] part of a two-in-a-line with an empty third? */
    fun isThreatCow(state: GameState, node: Int): Boolean {
        val b = state.board
        val enemy = state.maskOf(state.toMove.opponent)
        val empty = state.empty
        for (m in b.millsAt[node]) {
            val mm = b.millMasks[m]
            if (Integer.bitCount(mm and enemy) == 2 && Integer.bitCount(mm and empty) == 1) return true
        }
        return false
    }
}
