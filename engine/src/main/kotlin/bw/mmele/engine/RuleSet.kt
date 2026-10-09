package bw.mmele.engine

enum class Variant { CLASSIC, TRIANGLE }

/** What completing a line of three does. */
enum class MillEffect {
    /** Classic Morabaraba: the player must shoot (remove) one enemy cow. */
    SHOOT,

    /** Khutlotharo: completing a line wins immediately (sudden death). */
    WIN,
}

/**
 * All tunable rules for a variant. See docs/RULES.md for the reasoning behind each default.
 *
 * @property flyingAt a player whose cows are all placed and who has at most this many cows on
 *   the board may fly to any empty node. 0 disables flying.
 * @property loseAtOrBelow a player whose total cows (board + hand) is at or below this loses.
 *   Negative disables the rule.
 * @property noImmediateRemill MSSA "Generally Accepted Rules": a cow that breaks a mill to form a
 *   new mill may not move straight back to its original node on that player's next move if doing
 *   so forms a mill (unless it is that player's only legal move, see RULES.md).
 * @property flyingDrawPlies when at least one player is flying (down to 3 cows) and this many
 *   plies pass with no shot, the game is drawn (MSSA: ten moves each = 20 plies). 0 disables.
 * @property idleDrawPlies plies in a row without any placement or shot after which the game is
 *   drawn. Guarantees termination.
 * @property jumpOverAdjacent in the movement phase a cow may also jump over an adjacent cow (either
 *   colour) to the empty node directly beyond it on the same line, without capturing (Tsoro
 *   Yematatu rule, see docs/RULES.md).
 */
data class RuleSet(
    val variant: Variant,
    val board: BoardGraph,
    val cowsPerPlayer: Int,
    val millEffect: MillEffect,
    val flyingAt: Int,
    val loseAtOrBelow: Int,
    val noImmediateRemill: Boolean,
    val flyingDrawPlies: Int,
    val idleDrawPlies: Int,
    val jumpOverAdjacent: Boolean = false,
) {
    init {
        require(cowsPerPlayer * 2 <= board.size) { "too many cows for this board" }
        require(idleDrawPlies > 0) { "idleDrawPlies must be positive so every game terminates" }
    }

    companion object {
        val CLASSIC = RuleSet(
            variant = Variant.CLASSIC,
            board = Boards.CLASSIC,
            cowsPerPlayer = 12,
            millEffect = MillEffect.SHOOT,
            flyingAt = 3,
            loseAtOrBelow = 2,
            noImmediateRemill = true,
            flyingDrawPlies = 20,
            idleDrawPlies = 100,
        )

        val TRIANGLE = RuleSet(
            variant = Variant.TRIANGLE,
            board = Boards.TRIANGLE,
            // 3 is forced by the board: 2 cows can never make a line, 4 each would not fit on 7 points.
            cowsPerPlayer = 3,
            millEffect = MillEffect.WIN,
            flyingAt = 0,
            loseAtOrBelow = -1,
            noImmediateRemill = false,
            flyingDrawPlies = 0,
            // 15 moves each without a placement. Longer than any forced win (solver: 11 plies), so
            // the draw rule never cuts off a won game; it only ends shuffling.
            idleDrawPlies = 30,
            jumpOverAdjacent = true,
        )

        fun of(variant: Variant): RuleSet = when (variant) {
            Variant.CLASSIC -> CLASSIC
            Variant.TRIANGLE -> TRIANGLE
        }
    }
}
