package bw.mmele.online

import bw.mmele.online.chat.CHAT_EMOJI
import bw.mmele.online.chat.ChatBlocks
import bw.mmele.online.chat.ChatKind
import bw.mmele.online.chat.ChatMessage
import bw.mmele.online.chat.ChatPolicy
import bw.mmele.online.chat.ProfanityFilter
import bw.mmele.online.chat.QuickPhrase
import bw.mmele.online.ranked.Bracket
import bw.mmele.online.ranked.Elo
import bw.mmele.online.ranked.FeatureFlags
import bw.mmele.online.ranked.PlayerStanding
import bw.mmele.online.ranked.Ranked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChatTest {
    private val filter = ProfanityFilter.bundled()

    @Test fun bundledListLoadsEnglishAndSetswana() {
        assertFalse(filter.isClean("you are a fucking idiot"))
        assertFalse(filter.isClean("MASEPA"))
        assertTrue(filter.isClean("Good game, ke a leboga! Class, assess, Scunthorpe"))
    }

    @Test fun cleanMasksWholeWordsIncludingLeetAndStretching() {
        assertEquals("what the ******", filter.clean("what the fuuuck"))
        assertEquals("**** off", filter.clean("\$h1t off"))
        assertEquals("passes the class", filter.clean("passes the class"))
    }

    @Test fun policyLimitsLengthRateAndAllowedContent() {
        val ok = ChatPolicy.prepare(ChatKind.TEXT, "  hello   there ", filter, null, 0)
        assertEquals(ChatPolicy.Check.Ok(ChatKind.TEXT, "hello there"), ok)
        assertEquals(ChatPolicy.Check.Rejected(ChatPolicy.Reason.TOO_LONG), ChatPolicy.prepare(ChatKind.TEXT, "x".repeat(121), filter, null, 0))
        assertEquals(ChatPolicy.Check.Rejected(ChatPolicy.Reason.EMPTY), ChatPolicy.prepare(ChatKind.TEXT, "   ", filter, null, 0))
        assertEquals(ChatPolicy.Check.Rejected(ChatPolicy.Reason.TOO_FAST), ChatPolicy.prepare(ChatKind.EMOJI, "👍", filter, 1_000, 2_000))
        assertTrue(ChatPolicy.prepare(ChatKind.EMOJI, "👍", filter, 1_000, 2_600) is ChatPolicy.Check.Ok)
        assertEquals(ChatPolicy.Check.Rejected(ChatPolicy.Reason.NOT_ALLOWED), ChatPolicy.prepare(ChatKind.PHRASE, "buy_coins", filter, null, 0))
        assertEquals(ChatPolicy.Check.Rejected(ChatPolicy.Reason.DISABLED), ChatPolicy.prepare(ChatKind.TEXT, "hi", filter, null, 0, freeTextEnabled = false))
    }

    @Test fun phrasesHaveBothLanguagesAndMatchTheRules() {
        val rules = File("../firebase/database.rules.json").readText()
        for (p in QuickPhrase.entries) {
            assertTrue(p.en.isNotBlank() && p.tn.isNotBlank())
            assertTrue("${p.id} missing from rules", rules.contains(p.id))
        }
        CHAT_EMOJI.forEach { assertTrue(it.length <= 4) }
    }

    @Test fun mutedAndBlockedMessagesAreHidden() {
        val mine = ChatMessage("1", Seat.ONE, "me", ChatKind.TEXT, "hi", 0)
        val theirs = mine.copy(id = "2", seat = Seat.TWO, uid = "them")
        assertTrue(ChatBlocks().visible(theirs, "me"))
        assertFalse(ChatBlocks(muted = true).visible(theirs, "me"))
        assertTrue(ChatBlocks(muted = true).visible(mine, "me"))
        assertFalse(ChatBlocks(blockedUids = setOf("them")).visible(theirs, "me"))
    }
}

class RankedTest {
    @Test fun eloVectorsMatchTheCloudFunction() {
        // Same vectors as firebase/chat_ranked.test.mjs (functions/ranked.js).
        val s = Ranked.settle(PlayerStanding("a", games = 0), PlayerStanding("b", points = 5, games = 40), "one")
        assertEquals(1220, s.one.rating); assertEquals(1188, s.two.rating)
        assertEquals(1005, s.one.points); assertEquals(0, s.two.points)
        assertEquals(1482 to 1318, Elo.update(1500, 1300, 0.0))
        val d = Ranked.settle(PlayerStanding("a", rating = 1400, points = 10, games = 50), PlayerStanding("b", points = 10, games = 50), "draw")
        assertEquals(10, d.one.points); assertNull(d.winner)
        assertTrue(d.one.rating < 1400)
    }

    @Test fun realMoneyIsOff() = assertFalse(FeatureFlags.REAL_MONEY_ENABLED)

    @Test fun bracketRunsToAChampionForEverySize() {
        for (n in listOf(4, 8, 16)) {
            var b = Bracket.create((1..n).map { "p$it" })
            assertEquals(n - 1, b.matches.size)
            while (!b.isFinished) {
                val m = b.playable().first()
                b = b.report(m.id, m.one!!)
            }
            assertEquals("p1", b.champion)
            val st = b.standings()
            assertEquals(1 to "p1", st.first())
            assertEquals(2, st[1].first) // runner-up
            assertEquals(n, st.size)
        }
        assertEquals(224 to 96, Bracket.prizes(16))
    }

    @Test(expected = IllegalArgumentException::class) fun bracketRejectsOutsiders() {
        Bracket.create(listOf("a", "b", "c", "d")).report("r1m0", "c")
    }

    @Test(expected = IllegalArgumentException::class) fun bracketRejectsOddSizes() { Bracket.create(listOf("a", "b", "c")) }
}
