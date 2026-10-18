package bw.mmele

import bw.mmele.online.Seat
import bw.mmele.online.chat.ChatKind
import bw.mmele.online.chat.ChatMessage
import bw.mmele.online.chat.ChatReport
import bw.mmele.online.chat.ChatRepository
import bw.mmele.online.ranked.FeatureFlags
import bw.mmele.ui.chat.ChatIntent
import bw.mmele.ui.chat.ChatNotice
import bw.mmele.ui.chat.ChatViewModel
import com.tshipiworks.mmele.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeChat : ChatRepository {
    val flow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val sent = mutableListOf<Pair<ChatKind, String>>()
    val reports = mutableListOf<ChatReport>()
    val blockedSet = mutableSetOf<String>()
    override fun observe(code: String) = flow
    override suspend fun send(code: String, seat: Seat, kind: ChatKind, body: String): Result<Unit> { sent += kind to body; return Result.success(Unit) }
    override suspend fun report(report: ChatReport): Result<Unit> { reports += report; return Result.success(Unit) }
    override suspend fun block(uid: String): Result<Unit> { blockedSet += uid; return Result.success(Unit) }
    override suspend fun unblock(uid: String): Result<Unit> { blockedSet -= uid; return Result.success(Unit) }
    override suspend fun blocked(): Set<String> = blockedSet
    override suspend fun deleteChat(code: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun msg(id: String, seat: Seat, uid: String, body: String = "gg") = ChatMessage(id, seat, uid, ChatKind.TEXT, body, 0)

    @Test fun textIsFilteredRateLimitedAndCleared() = runTest(dispatcher) {
        val repo = FakeChat(); var t = 10_000L
        val vm = ChatViewModel(repo, "ABC234", Seat.ONE, now = { t })
        val notices = mutableListOf<ChatNotice>()
        val job = launch { vm.notices.collect { notices += it } }
        advanceUntilIdle()
        vm.onIntent(ChatIntent.Draft("hello"))
        vm.onIntent(ChatIntent.Send(ChatKind.TEXT, "well played shit"))
        advanceUntilIdle()
        assertEquals("", vm.state.value.draft)
        assertTrue(repo.sent.single().second.contains("****"))
        vm.onIntent(ChatIntent.Send(ChatKind.EMOJI, "👍"))
        advanceUntilIdle()
        assertEquals(listOf(ChatNotice.TOO_FAST), notices)
        t += 2_000
        vm.onIntent(ChatIntent.Send(ChatKind.TEXT, "x".repeat(121)))
        advanceUntilIdle()
        assertEquals(ChatNotice.TOO_LONG, notices.last())
        assertEquals(1, repo.sent.size)
        job.cancel()
    }

    @Test fun unreadMuteBlockAndReport() = runTest(dispatcher) {
        val repo = FakeChat()
        val vm = ChatViewModel(repo, "ABC234", Seat.ONE)
        advanceUntilIdle()
        repo.flow.value = listOf(msg("1", Seat.TWO, "them"), msg("2", Seat.ONE, "me"))
        advanceUntilIdle()
        assertEquals(1, vm.state.value.unread)
        vm.onIntent(ChatIntent.Open(true))
        assertEquals(0, vm.state.value.unread)
        vm.onIntent(ChatIntent.Mute(true))
        assertEquals(listOf("2"), vm.state.value.messages.map { it.id })
        vm.onIntent(ChatIntent.Mute(false))
        assertEquals(2, vm.state.value.messages.size)
        vm.onIntent(ChatIntent.Report(vm.state.value.messages.first()))
        vm.onIntent(ChatIntent.Block(vm.state.value.messages.first()))
        advanceUntilIdle()
        assertEquals("them", repo.reports.single().reportedUid)
        assertTrue("them" in repo.blockedSet)
        assertEquals(listOf("2"), vm.state.value.messages.map { it.id })
    }

    @Test fun realMoneyIsOff() {
        assertFalse(FeatureFlags.REAL_MONEY_ENABLED)
        assertFalse(BuildConfig.REAL_MONEY_ENABLED)
    }
}
