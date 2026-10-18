package bw.mmele.data

import kotlin.random.Random

/** One Botswana fact from res/raw/botswana_facts.json. */
data class BotswanaFact(val id: String, val category: String, val text: String)

/** One photo from assets/photos/photos.json (Wikimedia Commons, free licence; credited on the About screen). */
data class BotswanaPhoto(
    val id: String, val kind: String, val caption: String, val title: String,
    val author: String, val license: String, val licenseUrl: String, val source: String,
) {
    val assetPath get() = "photos/$id.webp"
}

object BotswanaContent {
    @Suppress("UNCHECKED_CAST")
    fun parseFacts(json: String): List<BotswanaFact> =
        ((MiniJson.parse(json) as Map<String, Any?>)["facts"] as List<Map<String, Any?>>).map {
            BotswanaFact(it["id"] as String, it["category"] as String, it["text"] as String)
        }

    @Suppress("UNCHECKED_CAST")
    fun parsePhotos(json: String): List<BotswanaPhoto> = (MiniJson.parse(json) as List<Map<String, Any?>>).map {
        fun f(k: String) = (it[k] as? String).orEmpty()
        BotswanaPhoto(f("id"), f("kind"), f("caption"), f("title"), f("author"), f("license"), f("licenseUrl"), f("source"))
    }
}

/**
 * Persisted position in a shuffled cycle: [order] is a permutation of ids, [pos] how many of them
 * have been shown, [last] the id shown most recently (survives a reshuffle).
 */
data class DeckState(val order: List<String> = emptyList(), val pos: Int = 0, val last: String? = null) {
    fun encode(): String = "$pos|${last.orEmpty()}|${order.joinToString(",")}"

    companion object {
        fun decode(s: String?): DeckState? = runCatching {
            if (s.isNullOrEmpty()) return null
            val (p, l, o) = s.split('|', limit = 3)
            DeckState(if (o.isEmpty()) emptyList() else o.split(','), p.toInt(), l.ifEmpty { null })
        }.getOrNull()
    }
}

/**
 * No-repeat deck: every id is drawn exactly once per cycle, in a random order. Only when the whole
 * cycle has been shown is the deck reshuffled, and the new order never starts with the id shown
 * last. The state is a value ([DeckState]) so the caller can persist it after each draw.
 *
 * If the pool changes between app versions (ids added or removed), [reconcile] drops ids that no
 * longer exist and slots new ids into the unseen part of the current cycle, so nothing already
 * seen comes back early.
 */
class NoRepeatDeck(private val ids: List<String>, state: DeckState?, private val random: Random = Random.Default) {
    init { require(ids.isNotEmpty() && ids.toSet().size == ids.size) { "deck ids must be non-empty and unique" } }

    var state: DeckState = reconcile(state ?: DeckState())
        private set

    private fun reconcile(s: DeckState): DeckState {
        val known = ids.toSet()
        if (s.order.isEmpty()) return DeckState(shuffled(s.last), 0, s.last)
        val seen = s.order.take(s.pos.coerceIn(0, s.order.size)).filter { it in known }
        val unseen = s.order.drop(s.pos.coerceIn(0, s.order.size)).filter { it in known }.toMutableList()
        val missing = ids.filter { it !in s.order }
        for (m in missing) unseen.add(random.nextInt(unseen.size + 1), m)
        return DeckState(seen + unseen, seen.size, s.last?.takeIf { it in known })
    }

    private fun shuffled(avoidFirst: String?): List<String> {
        val o = ids.shuffled(random).toMutableList()
        if (o.size > 1 && o[0] == avoidFirst) { val j = 1 + random.nextInt(o.size - 1); o[0] = o[j].also { o[j] = o[0] } }
        return o
    }

    /** Draws the next id and advances (reshuffling first if the cycle is complete). */
    fun next(): String {
        var s = state
        if (s.pos >= s.order.size) s = DeckState(shuffled(s.last), 0, s.last)
        val id = s.order[s.pos]
        state = DeckState(s.order, s.pos + 1, id)
        return id
    }
}

/** What the game-over card shows: a fact and (when photos are bundled) a captioned photo. */
data class FactCard(val fact: BotswanaFact, val photo: BotswanaPhoto?)

/**
 * Hands out one [FactCard] per finished game. Facts and photos each run their own [NoRepeatDeck];
 * after every draw both states are passed to [persist] so the cycle survives app restarts.
 */
class FactCards(
    facts: List<BotswanaFact>,
    photos: List<BotswanaPhoto>,
    factState: DeckState?,
    photoState: DeckState?,
    private val persist: (facts: DeckState, photos: DeckState?) -> Unit = { _, _ -> },
    random: kotlin.random.Random = kotlin.random.Random.Default,
) {
    private val factById = facts.associateBy { it.id }
    private val photoById = photos.associateBy { it.id }
    private val factDeck = NoRepeatDeck(facts.map { it.id }, factState, random)
    private val photoDeck = photos.takeIf { it.isNotEmpty() }?.let { NoRepeatDeck(it.map { p -> p.id }, photoState, random) }

    @Synchronized
    fun draw(): FactCard {
        val card = FactCard(factById.getValue(factDeck.next()), photoDeck?.next()?.let { photoById.getValue(it) })
        persist(factDeck.state, photoDeck?.state)
        return card
    }
}
