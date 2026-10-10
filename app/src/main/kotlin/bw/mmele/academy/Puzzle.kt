package bw.mmele.academy

import bw.mmele.engine.Action
import bw.mmele.engine.GameResult
import bw.mmele.engine.GameState
import bw.mmele.engine.Player
import bw.mmele.engine.Rules
import bw.mmele.engine.WinReason
import bw.mmele.engine.ai.Evaluator
import bw.mmele.engine.ai.SearchAi
import bw.mmele.engine.ai.SearchConfig
import kotlin.random.Random

/** What the learner must achieve. Checked after each of their complete turns (move + any shot). */
sealed interface Goal {
    /** Complete a mill and shoot one of the opponent's cows. */
    data object MillAndShoot : Goal

    /** Win the game, for the given reason (null = any). */
    data class Win(val reason: WinReason?) : Goal

    /** Still have a legal move after the opponent's replies, for [Puzzle.turns] turns. */
    data object Survive : Goal
}

/**
 * A scripted position. The learner always plays [human]; [turns] is how many of their turns the
 * goal may take. [solution] is the intended line: the learner's actions only (shots included),
 * against [PuzzleOpponent]'s deterministic replies.
 */
data class Puzzle(
    val start: GameState,
    val goal: Goal,
    val turns: Int = 1,
    val solution: List<Action>,
    val human: Player = Player.ONE,
)

/**
 * Exact AND-OR search over the puzzle's short horizon: can the learner *force* the goal from a
 * position where it is their turn, whatever the opponent replies? Horizons are tiny (<= 2 turns),
 * so this is instant even on the 24-point board.
 */
object PuzzleSolver {

    /** Every way the side to move can finish its turn: a move, or a move plus the shot it earns. */
    fun completeTurns(s: GameState): List<Pair<List<Action>, GameState>> {
        val out = ArrayList<Pair<List<Action>, GameState>>()
        for (a in Rules.legalActions(s)) {
            val c = Rules.apply(s, a)
            if (c.result == null && c.toMove == s.toMove) {
                for (b in Rules.legalActions(c)) out += listOf(a, b) to Rules.apply(c, b)
            } else {
                out += listOf(a) to c
            }
        }
        return out
    }

    fun goalMet(p: Puzzle, s: GameState): Boolean = when (val g = p.goal) {
        Goal.MillAndShoot -> s.shotBy(p.human) > p.start.shotBy(p.human)
        is Goal.Win -> (s.result as? GameResult.Win)?.let { it.winner == p.human && (g.reason == null || it.reason == g.reason) } == true
        Goal.Survive -> false // judged after the opponent's last reply, see afterTurn
    }

    private fun lost(p: Puzzle, s: GameState) = s.result != null && (s.result as? GameResult.Win)?.winner != p.human

    /** [s]: the learner to move (possibly mid-turn, must shoot) with [turnsLeft] turns including this one. */
    fun canForce(p: Puzzle, s: GameState, turnsLeft: Int): Boolean {
        if (turnsLeft <= 0 || s.result != null) return false
        for (a in Rules.legalActions(s)) {
            val c = Rules.apply(s, a)
            val ok = if (c.result == null && c.toMove == s.toMove) canForce(p, c, turnsLeft) else afterTurn(p, c, turnsLeft)
            if (ok) return true
        }
        return false
    }

    /** [c]: just after the learner's complete turn. */
    fun afterTurn(p: Puzzle, c: GameState, turnsLeft: Int): Boolean {
        if (goalMet(p, c)) return true
        if (c.result != null) return p.goal == Goal.Survive && !lost(p, c)
        if (p.goal != Goal.Survive && turnsLeft <= 1) return false
        return refutation(p, c, turnsLeft) == null
    }

    /** An opponent reply after which the learner can no longer force the goal, or null if none exists. */
    fun refutation(p: Puzzle, c: GameState, turnsLeft: Int): List<Action>? {
        for ((reply, d) in completeTurns(c)) {
            val fine = when {
                d.result != null -> !lost(p, d)
                p.goal == Goal.Survive && turnsLeft <= 1 -> true // survived the last reply
                else -> canForce(p, d, turnsLeft - 1)
            }
            if (!fine) return reply
        }
        return null
    }
}

/**
 * The scripted opponent: it plays a refutation when the learner went wrong (so mistakes are
 * shown, not forgiven), otherwise a sensible engine move. Fully deterministic, so the intended
 * solution in each [Puzzle] is reproducible in tests.
 */
object PuzzleOpponent {
    fun reply(p: Puzzle, afterLearner: GameState, turnsLeft: Int): List<Action> {
        PuzzleSolver.refutation(p, afterLearner, turnsLeft)?.let { return it }
        val ai = SearchAi(SearchConfig(maxDepth = 2, timeLimitMs = 60_000, useTranspositionTable = false, evaluator = Evaluator.MEDIUM), Random(7))
        var s = afterLearner
        val side = s.toMove
        val first = ai.chooseAction(s)
        s = Rules.apply(s, first)
        // A mill earns a shot in the same turn.
        return if (s.result == null && s.toMove == side) listOf(first, ai.chooseAction(s)) else listOf(first)
    }
}

/** What happened after the learner's action. */
sealed interface StepResult {
    /** Mid-turn (a mill was formed, now shoot). */
    data object Continue : StepResult

    /** The action isn't legal here. */
    data object Illegal : StepResult

    /**
     * A complete turn that can't reach the goal. [refutation] is the opponent's punishing reply
     * (empty when the goal simply wasn't reached); the board then goes back to [backTo].
     */
    data class Wrong(val backTo: GameState, val refutation: List<Action>) : StepResult

    /** Right so far; the opponent answers with [reply] (apply them for the animation). */
    data class Good(val reply: List<Action>) : StepResult

    /** Goal reached. [reply] is the opponent's final (failed) answer in survival puzzles. */
    data class Solved(val reply: List<Action> = emptyList()) : StepResult
}

/** One attempt at a puzzle: pure state machine driven by the Academy screen and by the tests. */
class PuzzleSession(val puzzle: Puzzle) {
    var state: GameState = puzzle.start
        private set
    private var turnStart: GameState = puzzle.start
    private var turnsUsed = 0
    var solved = false
        private set
    var mistakes = 0
        private set

    val turnsLeft: Int get() = puzzle.turns - turnsUsed

    fun play(a: Action): StepResult {
        if (solved || state.result != null || state.toMove != puzzle.human || !Rules.isLegal(state, a)) return StepResult.Illegal
        state = Rules.apply(state, a)
        if (state.result == null && state.toMove == puzzle.human) return StepResult.Continue // must shoot
        val left = turnsLeft
        if (!PuzzleSolver.afterTurn(puzzle, state, left)) {
            mistakes++
            val canPunish = state.result == null && (puzzle.goal == Goal.Survive || left > 1)
            val refutation = if (canPunish) PuzzleSolver.refutation(puzzle, state, left).orEmpty() else emptyList()
            state = turnStart
            return StepResult.Wrong(turnStart, refutation)
        }
        if (PuzzleSolver.goalMet(puzzle, state) || state.result != null || (puzzle.goal == Goal.Survive && left <= 1)) {
            var reply = emptyList<Action>()
            if (puzzle.goal == Goal.Survive && state.result == null) {
                // Show the opponent's last attempt; the learner survives it by construction.
                reply = PuzzleOpponent.reply(puzzle, state, left)
                reply.forEach { state = Rules.apply(state, it) }
            }
            solved = true
            return StepResult.Solved(reply)
        }
        val reply = PuzzleOpponent.reply(puzzle, state, left)
        reply.forEach { state = Rules.apply(state, it) }
        turnsUsed++
        turnStart = state
        return StepResult.Good(reply)
    }

    fun restart() {
        state = puzzle.start
        turnStart = puzzle.start
        turnsUsed = 0
        solved = false
    }
}
