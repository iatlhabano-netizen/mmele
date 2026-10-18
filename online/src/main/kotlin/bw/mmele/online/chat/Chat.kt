package bw.mmele.online.chat

import bw.mmele.online.Seat
import kotlinx.coroutines.flow.Flow

/** Phase A: fixed phrases, shown in the reader's language (only the [id] goes over the wire). */
enum class QuickPhrase(val id: String, val en: String, val tn: String) {
    HELLO("hello", "Hello!", "Dumela!"),
    GOOD_LUCK("good_luck", "Good luck!", "Masego!"),
    YOUR_TURN("your_turn", "Your turn", "Ke nako ya gago"),
    NICE_MILL("nice_mill", "Nice mill!", "Mola o montle!"),
    NICE_MOVE("nice_move", "Nice move!", "Motsamao o montle!"),
    THINKING("thinking", "Let me think…", "A ke akanye…"),
    OOPS("oops", "Oops!", "Ijoo!"),
    WELL_PLAYED("well_played", "Well played", "O tshamekile sentle"),
    GOOD_GAME("good_game", "Good game", "Motshameko o monate"),
    REMATCH("rematch", "Rematch?", "Re tshameke gape?"),
    THANKS("thanks", "Thanks!", "Ke a leboga!"),
    BYE("bye", "Bye, go well", "Sala sentle");

    companion object { fun byId(id: String) = entries.firstOrNull { it.id == id } }
}

/** Small fixed emoji set (also validated by the security rules). */
val CHAT_EMOJI = listOf("👍", "👏", "😄", "😮", "😅", "🔥", "🐄", "🎉", "🤝", "🙏")

enum class ChatKind(val wire: String) {
    PHRASE("p"), EMOJI("e"), TEXT("t");
    companion object { fun of(w: String?) = entries.firstOrNull { it.wire == w } }
}

data class ChatMessage(
    val id: String,
    val seat: Seat,
    val uid: String,
    val kind: ChatKind,
    /** Phrase id, emoji, or filtered free text. */
    val body: String,
    val at: Long,
)

/** Client-side checks; the same limits are enforced by firebase/database.rules.json. */
object ChatPolicy {
    const val MAX_TEXT = 120
    /** Minimum gap between two messages from one player (rules: users/$uid/lastChatAt). */
    const val MIN_GAP_MS = 1_500L
    /** Messages kept per room; older ones are removed by the sender's client and by purge.mjs. */
    const val KEEP_LAST = 100

    sealed interface Check {
        data class Ok(val kind: ChatKind, val body: String) : Check
        data class Rejected(val reason: Reason) : Check
    }
    enum class Reason { EMPTY, TOO_LONG, TOO_FAST, DISABLED, NOT_ALLOWED }

    fun prepare(kind: ChatKind, raw: String, filter: ProfanityFilter, lastSentAt: Long?, now: Long, freeTextEnabled: Boolean = true): Check {
        if (lastSentAt != null && now - lastSentAt < MIN_GAP_MS) return Check.Rejected(Reason.TOO_FAST)
        return when (kind) {
            ChatKind.PHRASE -> if (QuickPhrase.byId(raw) != null) Check.Ok(kind, raw) else Check.Rejected(Reason.NOT_ALLOWED)
            ChatKind.EMOJI -> if (raw in CHAT_EMOJI) Check.Ok(kind, raw) else Check.Rejected(Reason.NOT_ALLOWED)
            ChatKind.TEXT -> {
                if (!freeTextEnabled) return Check.Rejected(Reason.DISABLED)
                val t = raw.trim().replace(Regex("\\s+"), " ")
                when {
                    t.isEmpty() -> Check.Rejected(Reason.EMPTY)
                    t.length > MAX_TEXT -> Check.Rejected(Reason.TOO_LONG)
                    else -> Check.Ok(kind, filter.clean(t))
                }
            }
        }
    }
}

/**
 * Word-list filter: whole words (case-insensitive, ignoring simple l33t swaps and repeated
 * letters) are replaced by asterisks. The list is a plain text file, one word per line, `#` for
 * comments: online/src/main/resources/bw/mmele/online/chat/profanity.txt (English + Setswana).
 */
class ProfanityFilter(words: Collection<String>) {
    private val bad = words.map { norm(it) }.filter { it.isNotEmpty() }.toSet()

    fun isClean(text: String) = tokens(text).none { norm(it.value) in bad }

    fun clean(text: String): String {
        val sb = StringBuilder(text)
        for (m in tokens(text)) if (norm(m.value) in bad) for (i in m.range) sb.setCharAt(i, '*')
        return sb.toString()
    }

    private fun tokens(t: String) = Regex("[\\p{L}\\p{N}@$*!]+").findAll(t)

    companion object {
        private val leet = mapOf('0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '@' to 'a', '$' to 's', '!' to 'i')
        /** Lower-case, undo l33t swaps, letters only, and collapse repeated letters ("fuuuck" == "fuck"). */
        fun norm(w: String): String {
            val s = w.lowercase().map { leet[it] ?: it }.filter { it.isLetter() }
            val out = StringBuilder()
            for (c in s) if (out.isEmpty() || out[out.length - 1] != c) out.append(c)
            return out.toString()
        }

        fun parseList(text: String) = text.lineSequence().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.toList()

        /** The bundled list (English + Setswana). */
        fun bundled(): ProfanityFilter {
            val txt = ProfanityFilter::class.java.getResourceAsStream("/bw/mmele/online/chat/profanity.txt")?.bufferedReader()?.readText().orEmpty()
            return ProfanityFilter(parseList(txt))
        }
    }
}

/** Players you muted (hidden for this session) or blocked (hidden always, rooms refused). */
data class ChatBlocks(val muted: Boolean = false, val blockedUids: Set<String> = emptySet()) {
    fun visible(m: ChatMessage, myUid: String) = m.uid == myUid || (!muted && m.uid !in blockedUids)
}

data class ChatReport(val roomCode: String, val reporterUid: String, val reportedUid: String, val messageId: String, val body: String, val at: Long)

/** Chat transport for one room. Firebase: chats/{code} (players only) + reports/ (write-only). */
interface ChatRepository {
    fun observe(code: String): Flow<List<ChatMessage>>
    suspend fun send(code: String, seat: Seat, kind: ChatKind, body: String): Result<Unit>
    suspend fun report(report: ChatReport): Result<Unit>
    suspend fun block(uid: String): Result<Unit>
    suspend fun unblock(uid: String): Result<Unit>
    suspend fun blocked(): Set<String>
    /** Called with the room: chat is deleted together with the room. */
    suspend fun deleteChat(code: String)
}
