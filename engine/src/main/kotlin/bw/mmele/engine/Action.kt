package bw.mmele.engine

/** Everything a player can do on their turn. */
sealed interface Action {
    /** Placement phase: drop a cow from hand on an empty node. */
    data class Place(val to: Int) : Action

    /** Movement / flying phase: move one cow (to an adjacent node, or anywhere when flying). */
    data class Move(val from: Int, val to: Int) : Action

    /** "Must shoot" sub-state after forming a mill: remove one enemy cow. */
    data class Shoot(val target: Int) : Action

    /** Compact integer encoding (used by the AI's transposition table). */
    fun encode(): Int = when (this) {
        is Place -> to
        is Move -> 32 + from * 32 + to
        is Shoot -> 2048 + target
    }

    companion object {
        fun decode(code: Int): Action = when {
            code < 32 -> Place(code)
            code < 2048 -> (code - 32).let { Move(it / 32, it % 32) }
            else -> Shoot(code - 2048)
        }
    }
}
