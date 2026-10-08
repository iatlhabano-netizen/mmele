package bw.mmele

import bw.mmele.engine.Boards
import bw.mmele.engine.DrawReason
import bw.mmele.engine.GameResult
import bw.mmele.engine.Player
import bw.mmele.engine.Variant
import bw.mmele.engine.WinReason
import bw.mmele.online.InMemoryOnlineGameRepository
import bw.mmele.online.OnlineSession
import bw.mmele.online.RoomConfig
import bw.mmele.online.RoomRules
import bw.mmele.online.RoomStore
import bw.mmele.online.Seat
import bw.mmele.ui.game.GameIntent
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.game.GameViewModel
import bw.mmele.ui.online.sanitizeRoomCode
import bw.mmele.ui.pieces.EggKind
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.scenes.Scene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Two phones in one JVM: host and guest ViewModels over a shared in-memory room store. */
@OptIn(ExperimentalCoroutinesApi::class)
class OnlineViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private var clock = 10_000L
    private val store = RoomStore()
    private val hostRepo = InMemoryOnlineGameRepository(store, { clock })
    private val guestRepo = InMemoryOnlineGameRepository(store, { clock })

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)

    private suspend fun TestScope.pair(config: RoomConfig = RoomConfig(Variant.CLASSIC, 0, "egg-kori_bustard", "egg-ostrich")): Pair<GameViewModel, GameViewModel> {
        val room = hostRepo.createRoom(config)
        val hostSession = OnlineSession(hostRepo, room, Seat.ONE)
        val host = GameViewModel(hostSession.configFor(Scene.CHOBE), online = hostSession, now = { clock })
        runCurrent()
        assertTrue(host.state.value.online!!.waitingForOpponent)
        assertFalse(host.state.value.isHumanTurn) // can't move alone in the lobby
        val joined = guestRepo.joinRoom(room.code)
        val guestSession = OnlineSession(guestRepo, joined, Seat.TWO)
        val guest = GameViewModel(guestSession.configFor(Scene.OKAVANGO), online = guestSession, now = { clock })
        runCurrent()
        return host to guest
    }

    @Test fun movesSyncBothWaysAndTurnsAreEnforced() = runTest(dispatcher) {
        val (host, guest) = pair()
        assertFalse(host.state.value.online!!.waitingForOpponent)
        assertTrue(host.state.value.online!!.opponentConnected)
        assertTrue(host.state.value.isHumanTurn)
        assertFalse(guest.state.value.isHumanTurn)
        // Pieces come from the room for both phones; scenes stay personal.
        assertEquals(PieceStyle.Egg(EggKind.KORI_BUSTARD), guest.state.value.config.styleOne)
        assertEquals(Scene.OKAVANGO, guest.state.value.config.scene)
        assertEquals(GameMode.ONLINE, guest.state.value.config.mode)

        guest.onIntent(GameIntent.NodeTapped(c("d7"))) // not guest's turn: ignored
        runCurrent()
        assertTrue(guest.state.value.tokens.isEmpty())

        host.onIntent(GameIntent.NodeTapped(c("a7")))
        runCurrent()
        assertEquals(Player.ONE, guest.state.value.game.ownerAt(c("a7")))
        assertTrue(guest.state.value.isHumanTurn)
        guest.onIntent(GameIntent.NodeTapped(c("d7")))
        runCurrent()
        assertEquals(Player.TWO, host.state.value.game.ownerAt(c("d7")))
        assertEquals(host.state.value.game, guest.state.value.game)
        assertFalse(host.state.value.canUndo) // no undo online
    }

    @Test fun millAndShotPropagate() = runTest(dispatcher) {
        val (host, guest) = pair()
        val seq = listOf("a7" to host, "a1" to guest, "d7" to host, "g1" to guest, "g7" to host)
        for ((l, vm) in seq) { vm.onIntent(GameIntent.NodeTapped(c(l))); runCurrent() }
        assertTrue(host.state.value.game.mustShoot)
        assertTrue(host.state.value.isHumanTurn) // shooter keeps the turn
        host.onIntent(GameIntent.NodeTapped(c("a1")))
        runCurrent()
        assertNull(guest.state.value.game.ownerAt(c("a1")))
        assertEquals(host.state.value.game, guest.state.value.game)
        assertEquals(host.state.value.tokens.map { it.node }.sorted(), guest.state.value.tokens.map { it.node }.sorted())
    }

    @Test fun resignShowsResultOnBothPhonesThenRematch() = runTest(dispatcher) {
        val (host, guest) = pair()
        host.onIntent(GameIntent.NodeTapped(c("a7"))); runCurrent()
        guest.onIntent(GameIntent.Resign); runCurrent()
        assertEquals(GameResult.Win(Player.ONE, WinReason.RESIGNATION), host.state.value.game.result)
        assertEquals(GameResult.Win(Player.ONE, WinReason.RESIGNATION), guest.state.value.game.result)

        host.onIntent(GameIntent.Rematch); runCurrent()
        assertTrue(host.state.value.online!!.iOfferedRematch)
        assertTrue(guest.state.value.online!!.opponentOffersRematch)
        assertNotNull(host.state.value.game.result) // still over until both agree
        guest.onIntent(GameIntent.Rematch); runCurrent()
        for (vm in listOf(host, guest)) {
            assertNull(vm.state.value.game.result)
            assertTrue(vm.state.value.tokens.isEmpty())
            assertFalse(vm.state.value.showGameOver)
        }
        host.onIntent(GameIntent.NodeTapped(c("d6"))); runCurrent()
        assertEquals(Player.ONE, guest.state.value.game.ownerAt(c("d6")))
    }

    @Test fun drawOfferAndAccept() = runTest(dispatcher) {
        val (host, guest) = pair()
        host.onIntent(GameIntent.OfferDraw); runCurrent()
        assertTrue(guest.state.value.online!!.opponentOffersDraw)
        assertTrue(host.state.value.online!!.iOfferedDraw)
        guest.onIntent(GameIntent.OfferDraw); runCurrent() // accept
        assertEquals(GameResult.Draw(DrawReason.AGREEMENT), host.state.value.game.result)
        assertEquals(GameResult.Draw(DrawReason.AGREEMENT), guest.state.value.game.result)
    }

    @Test fun disconnectIndicatorReconnectAndAbandonClaim() = runTest(dispatcher) {
        val (host, guest) = pair()
        host.onIntent(GameIntent.NodeTapped(c("a7"))); runCurrent()
        guestRepo.disconnect(host.state.value.online!!.code, Seat.TWO); runCurrent()
        assertFalse(host.state.value.online!!.opponentConnected)
        assertEquals(90, host.state.value.online!!.abandonSecondsLeft)

        // Back within the window: indicator clears, game carries on.
        val code = host.state.value.online!!.code
        guestRepo.reconnect(code, Seat.TWO); runCurrent()
        assertTrue(host.state.value.online!!.opponentConnected)
        assertNull(host.state.value.online!!.abandonSecondsLeft)

        guestRepo.disconnect(code, Seat.TWO); runCurrent()
        host.onIntent(GameIntent.ClaimAbandoned); runCurrent()
        assertNull(host.state.value.game.result) // too early
        assertNotNull(host.state.value.online!!.message)
        clock += RoomRules.ABANDON_AFTER_MS
        dispatcher.scheduler.advanceTimeBy(1_100); runCurrent()
        assertEquals(0, host.state.value.online!!.abandonSecondsLeft)
        host.onIntent(GameIntent.ClaimAbandoned); runCurrent()
        assertEquals(GameResult.Win(Player.ONE, WinReason.ABANDONED), host.state.value.game.result)
    }

    @Test fun lateJoinerRebuildsGameInProgress() = runTest(dispatcher) {
        val (host, guest) = pair()
        host.onIntent(GameIntent.NodeTapped(c("a7"))); runCurrent()
        guest.onIntent(GameIntent.NodeTapped(c("d7"))); runCurrent()
        // Guest's phone restarts: a fresh ViewModel re-attaches from the stored move list.
        val code = host.state.value.online!!.code
        val back = guestRepo.reconnect(code, Seat.TWO)
        val s = OnlineSession(guestRepo, back, Seat.TWO)
        val again = GameViewModel(s.configFor(Scene.TSODILO), online = s, now = { clock })
        runCurrent()
        assertEquals(host.state.value.game, again.state.value.game)
        assertEquals(2, again.state.value.tokens.size)
    }

    @Test fun roomCodeInputIsSanitized() {
        assertEquals("ABC234", sanitizeRoomCode(" abc-234 "))
        assertEquals("ABCDEF", sanitizeRoomCode("abcdefgh"))
        assertEquals("", sanitizeRoomCode("01OIL"))
    }

    @Test fun pieceStylesHaveStableIdsAndContrastingDefaults() {
        val ids = PieceStyle.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        for (st in PieceStyle.all) assertEquals(st, PieceStyle.fromId(st.id))
        assertTrue(PieceStyle.contrastOk(PieceStyle.Obsidian, PieceStyle.Quartz))
        assertTrue(PieceStyle.contrastOk(PieceStyle.SmokyDiamond, PieceStyle.ClearDiamond))
        val dark = EggKind.entries.filter { PieceStyle.Egg(it).isDark }
        val light = EggKind.entries.filterNot { PieceStyle.Egg(it).isDark }
        assertTrue(dark.isNotEmpty() && light.isNotEmpty())
        for (d in dark) for (l in light) assertTrue("$d vs $l", PieceStyle.contrastOk(PieceStyle.Egg(d), PieceStyle.Egg(l)))
    }
}
