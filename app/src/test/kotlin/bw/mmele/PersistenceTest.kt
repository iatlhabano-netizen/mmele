package bw.mmele

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import bw.mmele.data.Outcome
import bw.mmele.data.StatKey
import bw.mmele.data.StatsRules
import bw.mmele.engine.Action
import bw.mmele.engine.Boards
import bw.mmele.engine.GameResult
import bw.mmele.engine.Player
import bw.mmele.engine.Variant
import bw.mmele.engine.WinReason
import bw.mmele.engine.ai.Ai
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameIntent
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.game.GameRecord
import bw.mmele.ui.game.GameViewModel
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.scenes.Scene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class PersistenceTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)

    private val pnp = GameConfig(
        Variant.CLASSIC, GameMode.PASS_AND_PLAY, Difficulty.HARD, Player.ONE, clockMinutes = 1,
        scene = Scene.TSODILO, styleOne = PieceStyle.all.last(), styleTwo = PieceStyle.all.first(),
    )

    private fun TestScope.vm(config: GameConfig = pnp, saved: SavedStateHandle? = null, resume: GameRecord? = null,
                             onSave: (GameRecord?) -> Unit = {}, onResult: (GameConfig, GameResult) -> Unit = { _, _ -> }) =
        GameViewModel(
            config, aiDispatcher = dispatcher, minAiDelayMs = 0,
            aiFactory = { Ai.create(Difficulty.EASY, seed = 3) },
            clockNow = { testScheduler.currentTime },
            resume = resume, savedState = saved, onSaveGame = onSave, onResult = onResult,
        )

    @Test fun recordRoundTripsThroughText() {
        val rec = GameRecord(
            config = pnp,
            actions = listOf(Action.Place(c("a7")), Action.Place(c("a1")), Action.Place(c("d7")), Action.Place(c("g1")),
                Action.Place(c("g7")), Action.Shoot(c("a1"))),
            clocksBefore = listOf(60_000L to 60_000L, 58_000L to 60_000L, 58_000L to 57_500L, 51_000L to 57_500L,
                51_000L to 50_000L, 49_000L to 50_000L),
            clockMs1 = 48_000, clockMs2 = 50_000,
        )
        val text = rec.encode()
        assertEquals(rec, GameRecord.decode(text))
        assertEquals(c("d7"), (GameRecord.decode(text)!!.actions[2] as Action.Place).to)
        // Timeouts survive too.
        val timed = rec.copy(timedOut = Player.TWO)
        assertEquals(timed, GameRecord.decode(timed.encode()))
        assertEquals(WinReason.TIMEOUT, (timed.replay()!!.result as GameResult.Win).reason)
    }

    @Test fun corruptOrIllegalRecordsAreIgnored() {
        assertNull(GameRecord.decode(null))
        assertNull(GameRecord.decode("garbage"))
        assertNull(GameRecord.decode("mmele-game/1|v=CLASSIC"))
        val ok = GameRecord(pnp, listOf(Action.Place(c("a7"))), listOf(1L to 1L), 1, 1).encode()
        // Same node twice is illegal: the replay check rejects the record.
        val bad = ok.replace("a=0:1:1", "a=0:1:1,0:1:1")
        assertNotNull(GameRecord.decode(ok))
        assertNull(GameRecord.decode(bad))
    }

    @Test fun resumeRestoresBoardClocksAndUndo() = runTest(dispatcher) {
        var saved: GameRecord? = null
        val a = vm(onSave = { saved = it })
        for (l in listOf("a7", "a1", "d7", "g1")) { advanceTimeBy(2_000); a.onIntent(GameIntent.NodeTapped(c(l))) }
        runCurrent()
        val before = a.state.value
        a.onHidden() // app to background: clocks stop and the game is saved
        val rec = saved!!
        assertEquals(4, rec.actions.size)
        assertEquals(before.clockMs1, rec.clockMs1)
        assertTrue(rec.clockMs1 < 60_000)
        a.viewModelScope_cancel()

        val b = vm(resume = GameRecord.decode(rec.encode()))
        val after = b.state.value
        assertEquals(before.game, after.game)
        assertEquals(before.tokens.map { it.node to it.player }.toSet(), after.tokens.map { it.node to it.player }.toSet())
        assertEquals(before.clockMs1, after.clockMs1)
        assertEquals(before.clockMs2, after.clockMs2)
        assertTrue(after.canUndo)
        b.onIntent(GameIntent.Undo)
        assertEquals(3, b.record()!!.actions.size)
        b.viewModelScope_cancel()
    }

    /** Review fix: the clock is monotonic and stops while the app is in the background. */
    @Test fun clockStopsWhileBackgrounded() = runTest(dispatcher) {
        val a = vm()
        a.onIntent(GameIntent.NodeTapped(c("a7"))) // Light's clock starts
        advanceTimeBy(3_000); runCurrent()
        a.onHidden()
        advanceTimeBy(600_000); runCurrent() // ten minutes in the background
        assertEquals(60_000L, a.state.value.clockMs1)
        assertTrue("light still has time", a.state.value.clockMs2 in 56_000L..57_100L)
        assertTrue(!a.state.value.game.isGameOver)
        a.onShown()
        advanceTimeBy(2_000); runCurrent()
        a.onIntent(GameIntent.NodeTapped(c("g1")))
        assertTrue("only foreground time is charged: ${a.state.value.clockMs2}", a.state.value.clockMs2 in 54_900L..55_100L)
        a.viewModelScope_cancel()
    }

    @Test fun savedStateHandleSurvivesProcessDeath() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val a = GameViewModel.create(pnp, handle, resume = null, onSaveGame = {}, onResult = { _, _ -> })
        a.onIntent(GameIntent.NodeTapped(c("d6")))
        a.onIntent(GameIntent.NodeTapped(c("d2")))
        val game = a.state.value.game
        a.viewModelScope_cancel()
        // "Process death": a new ViewModel from only the saved handle, even with different home settings.
        val restored = SavedStateHandle(mapOf(GameViewModel.SAVED_KEY to handle.get<String>(GameViewModel.SAVED_KEY)))
        val b = GameViewModel.create(GameConfig(Variant.TRIANGLE), restored, resume = null, onSaveGame = {}, onResult = { _, _ -> })
        assertEquals(game, b.state.value.game)
        assertEquals(pnp, b.state.value.config)
        b.viewModelScope_cancel()
    }

    @Test fun undoAfterTimeoutRestoresTheClockAndPlayGoesOn() = runTest(dispatcher) {
        val results = ArrayList<GameResult>()
        val v = vm(onResult = { _, r -> results += r })
        advanceTimeBy(5_000)
        v.onIntent(GameIntent.NodeTapped(c("a7"))) // Dark used ~5 s
        runCurrent()
        val lightBefore = v.state.value.clockMs2
        assertEquals(60_000, lightBefore)
        advanceTimeBy(61_000) // Light never moves: flag falls
        runCurrent()
        val over = v.state.value.game.result as GameResult.Win
        assertEquals(WinReason.TIMEOUT, over.reason)
        assertEquals(0L, v.state.value.clockMs2)

        v.onIntent(GameIntent.Undo) // back to before Dark's move, with the clocks of that moment
        runCurrent()
        val s = v.state.value
        assertNull("undo must not re-end the game", s.game.result)
        assertEquals(60_000L, s.clockMs2)
        assertEquals("Dark keeps the time it had when it moved", 55_000L, s.clockMs1)
        advanceTimeBy(1_000)
        runCurrent()
        assertNull(v.state.value.game.result) // the clock is running again, not instantly expired
        v.onIntent(GameIntent.NodeTapped(c("d7")))
        assertEquals(Player.TWO, v.state.value.game.toMove)
        assertEquals("the timeout counted once in stats", 1, results.size)
        v.viewModelScope_cancel()
    }

    @Test fun statsOutcomes() {
        val solo = GameConfig(Variant.CLASSIC, GameMode.SOLO, Difficulty.HARD, human = Player.TWO)
        assertEquals(Outcome.WIN, StatsRules.outcomeOf(solo, GameResult.Win(Player.TWO, WinReason.BLOCKADE)))
        assertEquals(Outcome.LOSS, StatsRules.outcomeOf(solo, GameResult.Win(Player.ONE, WinReason.BLOCKADE)))
        assertEquals(Outcome.DRAW, StatsRules.outcomeOf(solo, GameResult.Draw(bw.mmele.engine.DrawReason.NO_PROGRESS)))
        assertEquals(StatKey(GameMode.SOLO, Difficulty.HARD, Variant.CLASSIC), StatKey.of(solo))
        assertEquals(StatKey(GameMode.PASS_AND_PLAY, null, Variant.CLASSIC), StatKey.of(pnp))
        for (k in StatKey.ALL) assertEquals(k, StatKey.decode(k.encode()))
        assertEquals(12, StatKey.ALL.size)
    }
}

/** Stop a test ViewModel's coroutines (the clock loop) without Android's ViewModelStore. */
private fun GameViewModel.viewModelScope_cancel() = viewModelScope.cancel()
