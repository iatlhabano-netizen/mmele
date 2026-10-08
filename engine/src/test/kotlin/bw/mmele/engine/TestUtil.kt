package bw.mmele.engine

import org.junit.Assert.assertTrue

/** Find a classic node index by its notation label, e.g. "a7". */
fun c(label: String): Int = Boards.CLASSIC.labels.indexOf(label).also { require(it >= 0) { "no node $label" } }

fun t(label: String): Int = Boards.TRIANGLE.labels.indexOf(label).also { require(it >= 0) { "no node $label" } }

fun GameState.play(vararg actions: Action): GameState = actions.fold(this) { s, a -> Rules.apply(s, a) }

fun place(label: String) = Action.Place(c(label))
fun move(from: String, to: String) = Action.Move(c(from), c(to))
fun shoot(label: String) = Action.Shoot(c(label))

fun assertInvariant(s: GameState) {
    if (!s.isGameOver) assertTrue("non-terminal state without legal actions: $s", Rules.legalActions(s).isNotEmpty())
}
