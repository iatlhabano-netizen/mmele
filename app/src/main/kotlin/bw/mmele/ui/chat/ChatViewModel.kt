package bw.mmele.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bw.mmele.analytics.Events
import bw.mmele.analytics.Tracker
import bw.mmele.online.Seat
import bw.mmele.online.chat.ChatBlocks
import bw.mmele.online.chat.ChatKind
import bw.mmele.online.chat.ChatMessage
import bw.mmele.online.chat.ChatPolicy
import bw.mmele.online.chat.ChatReport
import bw.mmele.online.chat.ChatRepository
import bw.mmele.online.chat.ProfanityFilter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val blocks: ChatBlocks = ChatBlocks(),
    val unread: Int = 0,
    val open: Boolean = false,
    val draft: String = "",
) {
    val remaining get() = ChatPolicy.MAX_TEXT - draft.length
}

sealed interface ChatIntent {
    data class Send(val kind: ChatKind, val body: String) : ChatIntent
    data class Draft(val text: String) : ChatIntent
    data class Open(val open: Boolean) : ChatIntent
    data class Mute(val muted: Boolean) : ChatIntent
    data class Block(val message: ChatMessage) : ChatIntent
    data class Report(val message: ChatMessage) : ChatIntent
}

enum class ChatNotice { SENT_REPORT, BLOCKED, TOO_FAST, TOO_LONG, FAILED }

/** Chat for one online room; same UDF shape as the game: intents in, state out, one-shot notices. */
class ChatViewModel(
    private val repo: ChatRepository,
    private val code: String,
    private val seat: Seat,
    private val filter: ProfanityFilter = ProfanityFilter.bundled(),
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()
    private val _notices = MutableSharedFlow<ChatNotice>(extraBufferCapacity = 8)
    val notices: SharedFlow<ChatNotice> = _notices.asSharedFlow()
    private var lastSentAt: Long? = null
    private var all: List<ChatMessage> = emptyList()

    init {
        viewModelScope.launch { _state.update { it.copy(blocks = it.blocks.copy(blockedUids = repo.blocked())) }; refresh() }
        viewModelScope.launch {
            repo.observe(code).catch { }.collect { list ->
                val newFromThem = list.count { m -> m.seat != seat && all.none { it.id == m.id } }
                all = list
                _state.update { it.copy(unread = if (it.open) 0 else it.unread + newFromThem) }
                refresh()
            }
        }
    }

    private fun refresh() = _state.update { s -> s.copy(messages = all.filter { it.seat == seat || s.blocks.visible(it, myUid = "") }) }

    fun onIntent(i: ChatIntent) {
        when (i) {
            is ChatIntent.Draft -> _state.update { it.copy(draft = i.text.take(ChatPolicy.MAX_TEXT + 20)) }
            is ChatIntent.Open -> _state.update { it.copy(open = i.open, unread = 0) }
            is ChatIntent.Mute -> { _state.update { it.copy(blocks = it.blocks.copy(muted = i.muted)) }; refresh() }
            is ChatIntent.Send -> send(i.kind, i.body)
            is ChatIntent.Block -> viewModelScope.launch {
                repo.block(i.message.uid)
                _state.update { it.copy(blocks = it.blocks.copy(blockedUids = it.blocks.blockedUids + i.message.uid)) }
                refresh(); _notices.tryEmit(ChatNotice.BLOCKED)
            }
            is ChatIntent.Report -> viewModelScope.launch {
                val r = repo.report(ChatReport(code, "", i.message.uid, i.message.id, i.message.body, now()))
                _notices.tryEmit(if (r.isSuccess) ChatNotice.SENT_REPORT else ChatNotice.FAILED)
            }
        }
    }

    private fun send(kind: ChatKind, body: String) {
        when (val c = ChatPolicy.prepare(kind, body, filter, lastSentAt, now())) {
            is ChatPolicy.Check.Rejected -> _notices.tryEmit(
                when (c.reason) { ChatPolicy.Reason.TOO_FAST -> ChatNotice.TOO_FAST; ChatPolicy.Reason.TOO_LONG -> ChatNotice.TOO_LONG; else -> ChatNotice.FAILED },
            )
            is ChatPolicy.Check.Ok -> {
                lastSentAt = now()
                if (kind == ChatKind.TEXT) _state.update { it.copy(draft = "") }
                viewModelScope.launch {
                    val r = repo.send(code, seat, c.kind, c.body)
                    if (r.isSuccess) Tracker.log(Events.CHAT_SENT, "kind" to c.kind.wire) else _notices.tryEmit(ChatNotice.FAILED)
                }
            }
        }
    }
}
