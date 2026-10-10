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
    /** A fresh position testing the same skill, played after [puzzle] before the next lesson unlocks. */
    val practice: Puzzle,
) {
    val variant: Variant get() = puzzle.start.rules.variant
}

object Lessons {
    private fun c(label: String) = Boards.CLASSIC.labels.indexOf(label).also { require(it >= 0) { label } }
    private fun t(label: String) = Boards.TRIANGLE.labels.indexOf(label).also { require(it >= 0) { label } }
    private fun cs(vararg l: String) = l.map(::c)
    private fun ts(vararg l: String) = l.map(::t)

    /** The classic board turned half a turn (a7 <-> g1): same geometry, a new-looking position. */
    private fun rot(n: Int) = (n / 8) * 8 + (n % 8 + 4) % 8
    private fun rot(l: List<Int>) = l.map(::rot)
    private fun rot(a: Action): Action = when (a) {
        is Action.Place -> Action.Place(rot(a.to))
        is Action.Move -> Action.Move(rot(a.from), rot(a.to))
        is Action.Shoot -> Action.Shoot(rot(a.target))
    }

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
        practice = Puzzle(
            start = Rules.position(RuleSet.CLASSIC, cs("d1", "d2"), cs("f4", "b6", "e5"), hand1 = 10, hand2 = 9),
            goal = Goal.MillAndShoot,
            solution = listOf(Action.Place(c("d3")), Action.Shoot(c("f4"))),
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
        practice = Puzzle(
            start = Rules.position(RuleSet.CLASSIC, rot(cs("g7", "d6", "a1", "b4", "c4")), rot(cs("a7", "d7", "a4", "b6"))),
            goal = Goal.Win(WinReason.BLOCKADE),
            solution = listOf(rot(Action.Move(c("c4"), c("c5")))),
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
        practice = Puzzle(
            start = Rules.position(
                RuleSet.CLASSIC,
                rot(cs("d7", "g7", "f6", "f4", "d5", "e5", "d3")),
                rot(cs("a7", "g4", "d1", "a4", "b6", "f2", "c5", "e4", "e3")),
            ),
            goal = Goal.Survive,
            turns = 2,
            solution = PRACTICE_ESCAPE,
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
        practice = Puzzle(
            start = Rules.position(RuleSet.CLASSIC, cs("a7", "g7", "c3"), cs("b2", "d2", "e5")),
            goal = Goal.Win(WinReason.REDUCED_TO_TWO),
            solution = listOf(Action.Move(c("c3"), c("d7")), Action.Shoot(c("e5"))),
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
        practice = Puzzle(
            start = Rules.position(RuleSet.TRIANGLE, ts("C", "BM", "BR"), ts("CR", "CL", "BL")),
            goal = Goal.Win(WinReason.LINE_OF_THREE),
            solution = listOf(Action.Move(t("BR"), t("A"))),
        ),
    )

    val ALL: List<Lesson> = listOf(MAKING_A_MILL, TRAPPING, ESCAPING, FLYING, KHUTLOTHARO)
}

/**
 * The deterministic scripted line of the escape practice (AcademyTest.printEscapePracticeLine writes
 * it out): b2-d2 mills and shoots d7; Light answers a4-a7 and shoots b4; then a1-a4.
 */
private val PRACTICE_ESCAPE: List<Action> by lazy {
    val c = { l: String -> Boards.CLASSIC.labels.indexOf(l) }
    listOf(Action.Move(c("b2"), c("d2")), Action.Shoot(c("d7")), Action.Move(c("a1"), c("a4")))
}
