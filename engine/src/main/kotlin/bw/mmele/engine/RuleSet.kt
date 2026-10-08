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
            cowsPerPlayer = 4,
            millEffect = MillEffect.WIN,
            flyingAt = 0,
            loseAtOrBelow = -1,
            noImmediateRemill = false,
            flyingDrawPlies = 0,
            idleDrawPlies = 40,
        )

        fun of(variant: Variant): RuleSet = when (variant) {
            Variant.CLASSIC -> CLASSIC
            Variant.TRIANGLE -> TRIANGLE
        }
    }
}
