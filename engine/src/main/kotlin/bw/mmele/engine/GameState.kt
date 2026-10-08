package bw.mmele.engine

/** Phase of the player to move (derived, never stored). */
enum class Phase(val setswana: String, val english: String) {
    PLACEMENT("Go baya", "Placing"),
    MOVEMENT("Go sutisa", "Moving"),
    FLYING("Go fofa", "Flying"),
    GAME_OVER("Go fedile", "Game over"),
}

enum class WinReason {
    /** Opponent reduced to two cows (board + hand). */
    REDUCED_TO_TWO,

    /** Opponent has no legal move on their turn ("Go tswala" - locked in). */
    BLOCKADE,

    /** Triangle variant: three in a line. */
    LINE_OF_THREE,

    /** Opponent's clock ran out (app-level turn timer). */
    TIMEOUT,

    RESIGNATION,

    /** Online: opponent left and did not come back before the abandon timeout. */
    ABANDONED,
}

enum class DrawReason {
    /** MSSA rule: a player is down to three cows and no cow is shot within ten moves each. */
    FLYING_NO_SHOT,

    /** Safety net: [RuleSet.idleDrawPlies] plies without any placement or shot. */
    NO_PROGRESS,

    /** Online: both players agreed to a draw. */
    AGREEMENT,
}

sealed interface GameResult {
    data class Win(val winner: Player, val reason: WinReason) : GameResult
    data class Draw(val reason: DrawReason) : GameResult
}

/**
 * Immutable snapshot of a game. Occupancy is stored as bitmasks over the board's nodes.
 * Create with [Rules.newGame] or [Rules.position]; advance with [Rules.apply].
 */
data class GameState(
    val rules: RuleSet,
    val p1: Int,
    val p2: Int,
    val hand1: Int,
    val hand2: Int,
    val toMove: Player,
    /** True when [toMove] has just formed a mill and must now shoot an enemy cow. */
    val mustShoot: Boolean = false,
    val result: GameResult? = null,
    val ply: Int = 0,
    /** Plies since the last placement or shot. */
    val idlePlies: Int = 0,
    /** Plies without a shot while at least one player is flying. */
    val flyingPlies: Int = 0,
    /** Packed remill ban (cow shl 8 or forbiddenDestination) per player, -1 when none. */
    val ban1: Int = -1,
    val ban2: Int = -1,
    /** Cows shot by each player. */
    val shotBy1: Int = 0,
    val shotBy2: Int = 0,
) {
    val board: BoardGraph get() = rules.board

    fun maskOf(p: Player): Int = if (p == Player.ONE) p1 else p2
    fun handOf(p: Player): Int = if (p == Player.ONE) hand1 else hand2
    fun onBoard(p: Player): Int = Integer.bitCount(maskOf(p))
    fun totalCows(p: Player): Int = onBoard(p) + handOf(p)
    fun shotBy(p: Player): Int = if (p == Player.ONE) shotBy1 else shotBy2
    fun banOf(p: Player): Int = if (p == Player.ONE) ban1 else ban2

    val occupied: Int get() = p1 or p2
    val empty: Int get() = board.fullMask and occupied.inv()

    fun at(node: Int): NodeState = when {
        p1 and (1 shl node) != 0 -> NodeState.PLAYER_ONE
        p2 and (1 shl node) != 0 -> NodeState.PLAYER_TWO
        else -> NodeState.EMPTY
    }

    fun ownerAt(node: Int): Player? = when (at(node)) {
        NodeState.PLAYER_ONE -> Player.ONE
        NodeState.PLAYER_TWO -> Player.TWO
        NodeState.EMPTY -> null
    }

    val isGameOver: Boolean get() = result != null

    /** Phase for a given player (placement / movement / flying), ignoring game over. */
    fun phaseOf(p: Player): Phase = when {
        handOf(p) > 0 -> Phase.PLACEMENT
        rules.flyingAt > 0 && onBoard(p) <= rules.flyingAt -> Phase.FLYING
        else -> Phase.MOVEMENT
    }

    /** Phase of the state machine: the mover's phase, or [Phase.GAME_OVER]. */
    val phase: Phase get() = if (result != null) Phase.GAME_OVER else phaseOf(toMove)

    fun isFlying(p: Player): Boolean = phaseOf(p) == Phase.FLYING
}
