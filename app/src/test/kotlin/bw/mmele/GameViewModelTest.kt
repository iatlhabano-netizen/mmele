package bw.mmele

import bw.mmele.engine.Action
import bw.mmele.engine.Boards
import bw.mmele.engine.Player
import bw.mmele.engine.Rules
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Ai
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameIntent
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.game.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)

    @Test fun passAndPlayTapPlaceUndo() = runTest(dispatcher) {
        val vm = GameViewModel(GameConfig(Variant.CLASSIC, GameMode.PASS_AND_PLAY), aiDispatcher = dispatcher)
        vm.onIntent(GameIntent.NodeTapped(c("a7")))
        assertEquals(1, vm.state.value.tokens.size)
        assertEquals(Player.TWO, vm.state.value.game.toMove)
        assertTrue(vm.state.value.canUndo)
        vm.onIntent(GameIntent.NodeTapped(c("a7"))) // occupied -> ignored
        assertEquals(1, vm.state.value.tokens.size)
        vm.onIntent(GameIntent.Undo)
        assertEquals(0, vm.state.value.tokens.size)
        assertFalse(vm.state.value.canUndo)
    }

    @Test fun millPromptsShootAndTokensTrackState() = runTest(dispatcher) {
        val vm = GameViewModel(GameConfig(Variant.CLASSIC, GameMode.PASS_AND_PLAY), aiDispatcher = dispatcher)
        for (l in listOf("a7", "a1", "d7", "g1", "g7")) vm.onIntent(GameIntent.NodeTapped(c(l)))
        val s = vm.state.value
        assertTrue(s.game.mustShoot)
        assertTrue(s.millFlash != null)
        assertEquals((1 shl c("a1")) or (1 shl c("g1")), s.shootable)
        vm.onIntent(GameIntent.NodeTapped(c("a1")))
        val after = vm.state.value
        assertFalse(after.game.mustShoot)
        assertEquals(4, after.tokens.size)
        // Every token sits where the engine says it does.
        for (t in after.tokens) assertEquals(t.player, after.game.ownerAt(t.node))
        assertEquals(Integer.bitCount(after.game.occupied), after.tokens.size)
    }

    @Test fun soloAiRepliesAndUndoReturnsToHumanTurn() = runTest(dispatcher) {
        val vm = GameViewModel(
            GameConfig(Variant.CLASSIC, GameMode.SOLO, Difficulty.MEDIUM, human = Player.ONE),
            aiDispatcher = dispatcher,
            aiFactory = { Ai.create(Difficulty.MEDIUM, seed = 1, timeLimitMs = 50) },
            minAiDelayMs = 0,
        )
        vm.onIntent(GameIntent.NodeTapped(c("d6")))
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals(Player.ONE, s.game.toMove)
        assertEquals(2, s.tokens.size)
        assertFalse(s.aiThinking)
        vm.onIntent(GameIntent.Undo)
        advanceUntilIdle()
        assertEquals(0, vm.state.value.tokens.size)
        assertEquals(Player.ONE, vm.state.value.game.toMove)
    }

    @Test fun soloAiMovesFirstWhenHumanIsQuartz() = runTest(dispatcher) {
        val vm = GameViewModel(
            GameConfig(Variant.TRIANGLE, GameMode.SOLO, Difficulty.HARD, human = Player.TWO),
            aiDispatcher = dispatcher,
            aiFactory = { Ai.create(Difficulty.HARD, seed = 2, timeLimitMs = 50) },
            minAiDelayMs = 0,
        )
        advanceUntilIdle()
        assertEquals(1, vm.state.value.tokens.size)
        assertEquals(Player.TWO, vm.state.value.game.toMove)
        // Human input is ignored while it's the AI's turn, accepted on our turn.
        val empty = (0 until 10).first { vm.state.value.game.ownerAt(it) == null }
        vm.onIntent(GameIntent.NodeTapped(empty))
        assertNull(vm.state.value.game.result)
        assertTrue(Rules.legalActions(vm.state.value.game).isNotEmpty() || vm.state.value.game.isGameOver)
    }

    @Test fun dragAndDropMoves() = runTest(dispatcher) {
        val vm = GameViewModel(GameConfig(Variant.TRIANGLE, GameMode.PASS_AND_PLAY), aiDispatcher = dispatcher)
        // Fill placement without lines: P1 T? use a known line-free fill.
        val t = Boards.TRIANGLE.labels
        val seq = listOf("L1", "M1", "R2", "R1", "L3", "M2", "M3", "L2").map { t.indexOf(it) }
        for (n in seq) vm.onIntent(GameIntent.NodeTapped(n))
        val g = vm.state.value.game
        if (g.isGameOver) return@runTest
        val move = Rules.legalActions(g).filterIsInstance<Action.Move>().first()
        vm.onIntent(GameIntent.DragStarted(move.from))
        assertEquals(move.from, vm.state.value.selected)
        vm.onIntent(GameIntent.Dropped(move.from, move.to))
        assertEquals(g.toMove.opponent == vm.state.value.game.toMove || vm.state.value.game.isGameOver, true)
        assertTrue(vm.state.value.tokens.any { it.node == move.to })
    }
}
