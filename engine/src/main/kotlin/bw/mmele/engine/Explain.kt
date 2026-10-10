package bw.mmele.engine

/** Why an action is not allowed, so the UI can say something more useful than a buzz. */
enum class IllegalReason {
    GAME_OVER,
    /** A mill was just formed: shoot before doing anything else. */
    MUST_SHOOT_FIRST,
    /** Shooting: that point is empty or holds your own cow. */
    SHOOT_ENEMY_ONLY,
    /** Shooting: the cow sits in a mill and the opponent still has cows outside mills. */
    PROTECTED_IN_MILL,
    /** Placement phase: cows in hand must be placed before any cow moves. */
    PLACE_FIRST,
    /** Placement: the point is taken. */
    OCCUPIED,
    /** Moving: the chosen cow is the opponent's (or there is no cow there). */
    NOT_YOUR_COW,
    /** Moving: the destination is taken. */
    DESTINATION_OCCUPIED,
    /** Moving (classic, not flying): only along a line to the next point. */
    SLIDE_ONLY,
    /** Moving (Khutlotharo): a step to the next point, or a jump over the middle cow into the empty end. */
    STEP_OR_JUMP,
    /** That cow has nowhere to go. */
    COW_IS_STUCK,
    /** MSSA rule: a cow that broke a mill to make a new one may not go straight back to remake it. */
    NO_IMMEDIATE_REMILL,
}

object Explain {
    /** Null when [action] is legal in [state]; otherwise the most helpful reason it is not. */
    fun whyIllegal(state: GameState, action: Action): IllegalReason? {
        if (state.result != null) return IllegalReason.GAME_OVER
        if (Rules.isLegal(state, action)) return null
        val p = state.toMove
        val own = state.maskOf(p)
        val enemy = state.maskOf(p.opponent)
        fun bit(n: Int) = 1 shl n
        if (state.mustShoot) {
            if (action !is Action.Shoot) return IllegalReason.MUST_SHOOT_FIRST
            return if (enemy and bit(action.target) == 0) IllegalReason.SHOOT_ENEMY_ONLY else IllegalReason.PROTECTED_IN_MILL
        }
        return when (action) {
            is Action.Shoot -> IllegalReason.SHOOT_ENEMY_ONLY
            is Action.Place -> if (state.handOf(p) == 0) IllegalReason.COW_IS_STUCK else IllegalReason.OCCUPIED
            is Action.Move -> when {
                state.handOf(p) > 0 -> IllegalReason.PLACE_FIRST
                own and bit(action.from) == 0 -> IllegalReason.NOT_YOUR_COW
                state.occupied and bit(action.to) != 0 -> IllegalReason.DESTINATION_OCCUPIED
                Rules.destinationsFrom(state, action.from) == 0 -> IllegalReason.COW_IS_STUCK
                isBanned(state, action) -> IllegalReason.NO_IMMEDIATE_REMILL
                state.rules.jumpOverAdjacent -> IllegalReason.STEP_OR_JUMP
                else -> IllegalReason.SLIDE_ONLY
            }
        }
    }

    private fun isBanned(state: GameState, m: Action.Move): Boolean {
        val ban = state.banOf(state.toMove)
        return ban >= 0 && ban shr 8 == m.from && ban and 0xFF == m.to
    }
}
