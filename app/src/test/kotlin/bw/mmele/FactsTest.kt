package bw.mmele

import bw.mmele.data.BotswanaContent
import bw.mmele.data.DeckState
import bw.mmele.data.FactCards
import bw.mmele.data.NoRepeatDeck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class FactsTest {
    private val facts = BotswanaContent.parseFacts(File("src/main/res/raw/botswana_facts.json").readText())
    private val photos = BotswanaContent.parsePhotos(File("src/main/assets/photos/photos.json").readText())

    private fun norm(s: String) = s.lowercase().replace(Regex("[^a-z0-9 ]"), "").split(' ').filter { it.length > 2 }.toSet()

    @Test fun poolHasAtLeast500UniqueShortFacts() {
        assertTrue("only ${facts.size} facts", facts.size >= 500)
        assertEquals("duplicate ids", facts.size, facts.map { it.id }.toSet().size)
        assertEquals("duplicate texts", facts.size, facts.map { norm(it.text) }.toSet().size)
        facts.forEach { assertTrue("too long for the card: ${it.id}", it.text.length <= 160) }
        // near-duplicates: no two facts share almost all their words
        val sets = facts.map { norm(it.text) }
        for (i in sets.indices) for (j in i + 1 until sets.size) {
            val a = sets[i]; val b = sets[j]
            val jac = (a intersect b).size.toDouble() / (a union b).size
            assertTrue("near-duplicate: ${facts[i].text} / ${facts[j].text}", jac < 0.6)
        }
    }

    @Test fun photosAreCreditedAndBundled() {
        assertTrue(photos.size >= 40)
        photos.forEach {
            assertTrue(it.id, it.author.isNotBlank() && it.license.isNotBlank() && it.source.startsWith("https://") && it.caption.isNotBlank())
            assertTrue(it.id, File("src/main/assets/${it.assetPath}").exists())
        }
    }

    @Test fun noRepeatsAcrossAFullCycleAndNoBackToBackAcrossReshuffle() {
        val ids = facts.map { it.id }
        val rnd = Random(1)
        val deck = NoRepeatDeck(ids, null, rnd)
        var last: String? = null
        repeat(5) { cycle ->
            val seen = (ids.indices).map { deck.next() }
            assertEquals("cycle $cycle repeated a fact", ids.toSet(), seen.toSet())
            if (last != null) assertNotEquals("first of a new cycle equals last shown", last, seen.first())
            last = seen.last()
        }
    }

    @Test fun reshuffleNeverStartsWithLastEvenForTinyDecks() {
        repeat(200) { seed ->
            val deck = NoRepeatDeck(listOf("a", "b"), null, Random(seed))
            var prev = deck.next()
            repeat(20) { val n = deck.next(); assertNotEquals(prev, n); prev = n }
        }
    }

    @Test fun statePersistsAcrossRestarts() {
        var stored: String? = null
        val shown = ArrayList<String>()
        // Simulate a restart after every few games: a new FactCards built from the stored state each time.
        while (shown.size < facts.size) {
            val cards = FactCards(facts, photos, DeckState.decode(stored), null, persist = { f, _ -> stored = f.encode() }, random = Random(shown.size))
            repeat(minOf(7, facts.size - shown.size)) { shown += cards.draw().fact.id }
        }
        assertEquals("a fact repeated across restarts", facts.size, shown.toSet().size)
        val s = DeckState.decode(stored)!!
        assertEquals(facts.size, s.pos); assertEquals(shown.last(), s.last)
        val next = FactCards(facts, photos, s, null).draw().fact.id
        assertNotEquals(shown.last(), next)
    }

    @Test fun editedPoolKeepsSeenFactsSeen() {
        val deck = NoRepeatDeck(listOf("a", "b", "c", "d"), null, Random(3))
        val first = listOf(deck.next(), deck.next())
        val grown = NoRepeatDeck(listOf("a", "b", "c", "d", "e"), DeckState.decode(deck.state.encode()), Random(4))
        val rest = List(3) { grown.next() }
        assertEquals(setOf("a", "b", "c", "d", "e"), (first + rest).toSet())
    }
}
