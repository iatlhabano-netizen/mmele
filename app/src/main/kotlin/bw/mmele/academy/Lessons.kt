package bw.mmele.academy

import androidx.annotation.StringRes
import bw.mmele.engine.Action
import bw.mmele.engine.Boards
import bw.mmele.engine.RuleSet
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import bw.mmele.engine.WinReason
import com.tshipiworks.mmele.R

/** An Academy lesson: text (string resources) around one [Puzzle]. Lessons unlock in list order. */
data class Lesson(
    val id: String,
    @StringRes val title: Int,
    @StringRes val intro: Int,
    @StringRes val goal: Int,
    val hints: List<Int>,
    @StringRes val success: Int,
    val puzzle: Puzzle,
) {
    val variant: Variant get() = puzzle.start.rules.variant
}

object Lessons {
    private fun c(label: String) = Boards.CLASSIC.labels.indexOf(label).also { require(it >= 0) { label } }
    private fun t(label: String) = Boards.TRIANGLE.labels.indexOf(label).also { require(it >= 0) { label } }
    private fun cs(vararg l: String) = l.map(::c)
    private fun ts(vararg l: String) = l.map(::t)

    /** 1. Placement: two in a row, complete the mill and shoot. */
    val MAKING_A_MILL = Lesson(
        id = "mill",
        title = R.string.lesson_mill_title,
        intro = R.string.lesson_mill_intro,
        goal = R.string.lesson_mill_goal,
        hints = listOf(R.string.lesson_mill_hint1, R.string.lesson_mill_hint2),
        success = R.string.lesson_mill_success,
        puzzle = Puzzle(
            start = Rules.position(RuleSet.CLASSIC, cs("a7", "d7"), cs("b6", "f4"), hand1 = 10, hand2 = 10),
            goal = Goal.MillAndShoot,
            solution = listOf(Action.Place(c("g7")), Action.Shoot(c("b6"))),
        ),
    )

    /** 2. Movement: no mill in sight, but one step leaves Light with no move at all (Go tswala). */
    val TRAPPING = Lesson(
        id = "trap",
        title = R.string.lesson_trap_title,
        intro = R.string.lesson_trap_intro,
        goal = R.string.lesson_trap_goal,
        hints = listOf(R.string.lesson_trap_hint1, R.string.lesson_trap_hint2),
        success = R.string.lesson_trap_success,
        puzzle = Puzzle(
            start = Rules.position(RuleSet.CLASSIC, cs("g7", "d6", "a1", "b4", "c4"), cs("a7", "d7", "a4", "b6")),
            goal = Goal.Win(WinReason.BLOCKADE),
            solution = listOf(Action.Move(c("c4"), c("c5"))),
        ),
    )

    /** 3. Nearly locked in: only a mill on the d-file keeps Dark alive for the next two turns. */
    val ESCAPING = Lesson(
        id = "escape",
        title = R.string.lesson_escape_title,
        intro = R.string.lesson_escape_intro,
        goal = R.string.lesson_escape_goal,
        hints = listOf(R.string.lesson_escape_hint1, R.string.lesson_escape_hint2),
        success = R.string.lesson_escape_success,
        puzzle = Puzzle(
            start = Rules.position(
                RuleSet.CLASSIC,
                cs("d7", "g7", "f6", "f4", "d5", "e5", "d3"),
                cs("a7", "g4", "d1", "a4", "b6", "f2", "c5", "e4", "e3"),
            ),
            goal = Goal.Survive,
            turns = 2,
            // Light's reply in between (deterministic): d1-a1 makes a mill and shoots f4.
            solution = listOf(Action.Move(c("f6"), c("d6")), Action.Shoot(c("g4")), Action.Move(c("g7"), c("g4"))),
        ),
    )

    /** 4. Down to three cows you fly: strike first, before Light completes its own line. */
    val FLYING = Lesson(
        id = "fly",
        title = R.string.lesson_fly_title,
        intro = R.string.lesson_fly_intro,
        goal = R.string.lesson_fly_goal,
        hints = listOf(R.string.lesson_fly_hint1, R.string.lesson_fly_hint2),
        success = R.string.lesson_fly_success,
        puzzle = Puzzle(
            start = Rules.position(RuleSet.CLASSIC, cs("a1", "d1", "c4"), cs("f6", "f4", "d5")),
            goal = Goal.Win(WinReason.REDUCED_TO_TWO),
            solution = listOf(Action.Move(c("c4"), c("g1")), Action.Shoot(c("f6"))),
        ),
    )

    /** 5. Khutlotharo: a jump over a cow completes the line and wins on the spot. */
    val KHUTLOTHARO = Lesson(
        id = "triangle",
        title = R.string.lesson_tri_title,
        intro = R.string.lesson_tri_intro,
        goal = R.string.lesson_tri_goal,
        hints = listOf(R.string.lesson_tri_hint1, R.string.lesson_tri_hint2),
        success = R.string.lesson_tri_success,
        puzzle = Puzzle(
            start = Rules.position(RuleSet.TRIANGLE, ts("C", "BM", "BL"), ts("CL", "CR", "BR")),
            goal = Goal.Win(WinReason.LINE_OF_THREE),
            solution = listOf(Action.Move(t("BL"), t("A"))),
        ),
    )

    val ALL: List<Lesson> = listOf(MAKING_A_MILL, TRAPPING, ESCAPING, FLYING, KHUTLOTHARO)
}
