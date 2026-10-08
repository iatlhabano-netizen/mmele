package bw.mmele.engine

/** The two sides. [ONE] plays the dark (obsidian) cows and moves first, as in the MSSA rules. */
enum class Player {
    ONE, TWO;

    val opponent: Player get() = if (this == ONE) TWO else ONE
}

/** State of a single board node (junction). */
enum class NodeState { EMPTY, PLAYER_ONE, PLAYER_TWO }

fun Player.toNodeState(): NodeState = if (this == Player.ONE) NodeState.PLAYER_ONE else NodeState.PLAYER_TWO
