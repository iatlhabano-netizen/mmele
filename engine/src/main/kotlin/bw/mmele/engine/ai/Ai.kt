package bw.mmele.engine.ai

import bw.mmele.engine.Action
import bw.mmele.engine.GameState
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD, EXPERT }

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
    /**
     * Classic: Easy is heuristic-random; Medium a 3-turn search; Hard and Expert deepen with a
     * transposition table under a node budget (same strength on every phone) and a time safety cap.
     * Khutlotharo is solved, so Hard/Expert play perfectly and Easy/Medium are deliberately fallible.
     */
    fun create(
        difficulty: Difficulty,
        seed: Long = System.nanoTime(),
        timeLimitMs: Long = defaultTimeMs(difficulty),
        variant: Variant = Variant.CLASSIC,
    ): AiPlayer {
        val random = Random(seed)
        if (variant == Variant.TRIANGLE) return when (difficulty) {
            Difficulty.EASY -> FallibleAi(EasyAi(random), mistakeRate = 0.35f, random)
            Difficulty.MEDIUM -> FallibleAi(
                SearchAi(SearchConfig(maxDepth = 2, timeLimitMs = timeLimitMs, useTranspositionTable = false, evaluator = Evaluator.MEDIUM), random),
                mistakeRate = 0.2f, random,
            )
            Difficulty.HARD, Difficulty.EXPERT -> PerfectTriangleAi(random)
        }
        return when (difficulty) {
            Difficulty.EASY -> EasyAi(random)
            Difficulty.MEDIUM -> SearchAi(
                SearchConfig(maxDepth = 3, timeLimitMs = timeLimitMs, useTranspositionTable = false, evaluator = Evaluator.MEDIUM, nodeBudget = 200_000),
                random,
            )
            Difficulty.HARD -> SearchAi(
                SearchConfig(maxDepth = 32, timeLimitMs = timeLimitMs, useTranspositionTable = true, evaluator = Evaluator.HARD, nodeBudget = HARD_NODES),
                random,
            )
            Difficulty.EXPERT -> SearchAi(
                SearchConfig(maxDepth = 32, timeLimitMs = timeLimitMs, useTranspositionTable = true, evaluator = Evaluator.HARD, nodeBudget = EXPERT_NODES),
                random,
            )
        }
    }

    /** Node budgets: roughly 0.3 s / 2 s of search on a desktop JVM, 1-2 s / 6-10 s on a slow phone. */
    const val HARD_NODES = 300_000L
    const val EXPERT_NODES = 2_000_000L

    /** Time safety caps; the node budget normally ends the search first. */
    fun defaultTimeMs(d: Difficulty): Long = when (d) {
        Difficulty.EASY -> 50
        Difficulty.MEDIUM -> 900
        Difficulty.HARD -> 2_500
        Difficulty.EXPERT -> 8_000
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
