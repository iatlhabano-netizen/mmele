package bw.mmele

import bw.mmele.engine.Action
import bw.mmele.engine.Boards
import bw.mmele.engine.IllegalReason
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Ai
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameIntent
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.game.GameViewModel
import bw.mmele.ui.game.ReplayModel
import bw.mmele.ui.game.hintMask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HintReplayIllegalTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()
    private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)

    private fun vm(mode: GameMode = GameMode.PASS_AND_PLAY) = GameViewModel(
        GameConfig(Variant.CLASSIC, mode), aiDispatcher = dispatcher,
        aiFactory = { Ai.create(Difficulty.EASY, seed = 1) },
        hintFactory = { Ai.create(Difficulty.MEDIUM, seed = 2, timeLimitMs = 200) },
    )

    @Test fun illegalTapsExplainWhy() = runTest(dispatcher) {
        val vm = vm()
        vm.onIntent(GameIntent.NodeTapped(c("a7")))
        vm.onIntent(GameIntent.NodeTapped(c("a7"))) // Light taps Dark's point
        assertEquals(IllegalReason.OCCUPIED, vm.state.value.illegal?.reason)
        val id = vm.state.value.illegal!!.id
        vm.onIntent(GameIntent.NodeTapped(c("a7")))
        assertTrue("same reason again gets a new id", vm.state.value.illegal!!.id != id)
        vm.onIntent(GameIntent.NodeTapped(c("g1"))) // a legal move clears it
        assertNull(vm.state.value.illegal)
        // Mill -> must shoot: tapping an empty point explains.
        for (l in listOf("d7", "a1", "g7")) vm.onIntent(GameIntent.NodeTapped(c(l)))
        assertTrue(vm.state.value.game.mustShoot)
        vm.onIntent(GameIntent.NodeTapped(c("d1")))
        assertEquals(IllegalReason.SHOOT_ENEMY_ONLY, vm.state.value.illegal?.reason)
        vm.onIntent(GameIntent.DismissIllegal)
        assertNull(vm.state.value.illegal)
    }

    @Test fun hintSuggestsALegalMoveAndClearsWhenPlayed() = runTest(dispatcher) {
        val vm = vm()
        for (l in listOf("a7", "a1", "d7")) vm.onIntent(GameIntent.NodeTapped(c(l))) // Dark threatens g7
        vm.onIntent(GameIntent.Hint)
        advanceUntilIdle()
        val hint = vm.state.value.hint
        assertNotNull(hint)
        assertEquals("Light should block the mill", Action.Place(c("g7")), hint)
        assertEquals(1 shl c("g7"), hintMask(hint))
        vm.onIntent(GameIntent.NodeTapped(c("g7")))
        assertNull(vm.state.value.hint)
    }

    @Test fun noHintOnlineOrOnTheComputersTurn() = runTest(dispatcher) {
        val solo = vm(GameMode.SOLO)
        assertTrue(solo.state.value.canHint)
        solo.onIntent(GameIntent.NodeTapped(c("a7")))
        assertTrue(!solo.state.value.canHint || solo.state.value.game.toMove == solo.state.value.config.human)
    }

    @Test fun replayRebuildsEveryPosition() = runTest(dispatcher) {
        val vm = vm()
        val moves = listOf("a7", "a1", "d7", "g1", "g7").map { c(it) }
        for (m in moves) vm.onIntent(GameIntent.NodeTapped(m))
        vm.onIntent(GameIntent.NodeTapped(c("a1"))) // shoot
        val played = vm.movesSoFar()
        assertEquals(6, played.size)
        val positions = ReplayModel.positions(vm.state.value.config, played)
        assertEquals(7, positions.size)
        assertEquals(vm.state.value.game, positions.last())
        assertEquals(0, ReplayModel.tokens(positions.first()).size)
        assertEquals(4, ReplayModel.tokens(positions.last()).size)
    }
}
