package bw.mmele.online

import bw.mmele.engine.Action
import bw.mmele.engine.Variant

/**
 * The map shape stored under `rooms/{code}` in Realtime Database. Kept dependency-free so the
 * security rules, the in-memory tests and the Firebase transport all agree on one layout:
 *
 * ```
 * rooms/{code}:
 *   hostUid, guestUid, createdAt, updatedAt, phase, variant, clockMinutes,
 *   styleOne, styleTwo,
 *   connected/{one|two}: bool,
 *   leftAt/{one|two}: millis,
 *   resigned: "one"|"two"|null,
 *   offers/{draw|rematch}/{one|two}: bool,
 *   moves/{index}: { by: "one"|"two", m: "P|node" | "M|from|to" | "S|node" }
 * ```
 */
object Wire {
    const val PHASE_LOBBY = "lobby"
    const val PHASE_PLAYING = "playing"
    const val PHASE_FINISHED = "finished"
    const val PHASE_ABANDONED = "abandoned"

    fun phaseOf(name: String?) = when (name) {
        PHASE_PLAYING -> RoomPhase.PLAYING
        PHASE_FINISHED -> RoomPhase.FINISHED
        PHASE_ABANDONED -> RoomPhase.ABANDONED
        else -> RoomPhase.LOBBY
    }

    fun phaseName(phase: RoomPhase) = when (phase) {
        RoomPhase.LOBBY -> PHASE_LOBBY
        RoomPhase.PLAYING -> PHASE_PLAYING
        RoomPhase.FINISHED -> PHASE_FINISHED
        RoomPhase.ABANDONED -> PHASE_ABANDONED
    }

    fun seatName(seat: Seat) = if (seat == Seat.ONE) "one" else "two"
    fun seatOf(name: String?) = if (name == "two") Seat.TWO else Seat.ONE

    fun encodeAction(seat: Seat, action: Action): String {
        val who = seatName(seat)
        return when (action) {
            is Action.Place -> "$who|P|${action.to}"
            is Action.Move -> "$who|M|${action.from}|${action.to}"
            is Action.Shoot -> "$who|S|${action.target}"
        }
    }

    fun decodeMove(index: Int, raw: String): MoveRecord? {
        val p = raw.split('|')
        if (p.size < 3) return null
        val seat = if (p[0] == "two") Seat.TWO else if (p[0] == "one") Seat.ONE else return null
        val action = when (p[1]) {
            "P" -> p.getOrNull(2)?.toIntOrNull()?.let { Action.Place(it) }
            "M" -> {
                val from = p.getOrNull(2)?.toIntOrNull()
                val to = p.getOrNull(3)?.toIntOrNull()
                if (from != null && to != null) Action.Move(from, to) else null
            }
            "S" -> p.getOrNull(2)?.toIntOrNull()?.let { Action.Shoot(it) }
            else -> null
        } ?: return null
        return MoveRecord(index, seat, action)
    }

    fun variantOf(name: String?) = if (name == "triangle") Variant.TRIANGLE else Variant.CLASSIC
    fun variantName(v: Variant) = if (v == Variant.TRIANGLE) "triangle" else "classic"

    /** Flatten a snapshot the way the database stores it. */
    fun toMap(room: RoomSnapshot, hostUid: String, guestUid: String?): Map<String, Any?> = mapOf(
        "hostUid" to hostUid,
        "guestUid" to guestUid,
        "createdAt" to room.updatedAtMs,
        "updatedAt" to room.updatedAtMs,
        "phase" to phaseName(room.phase),
        "variant" to variantName(room.config.variant),
        "clockMinutes" to room.config.clockMinutes,
        "styleOne" to room.config.styleOne,
        "styleTwo" to room.config.styleTwo,
        "ranked" to room.config.ranked.takeIf { it },
        "connected" to mapOf("one" to (Seat.ONE in room.connected), "two" to (Seat.TWO in room.connected)),
        "leftAt" to room.leftAtMs.entries.associate { seatName(it.key) to it.value },
        "resigned" to room.resigned?.let { seatName(it) },
        "offers" to mapOf(
            "draw" to mapOf("one" to room.offers.any { it.kind == OfferKind.DRAW && it.seat == Seat.ONE }, "two" to room.offers.any { it.kind == OfferKind.DRAW && it.seat == Seat.TWO }),
            "rematch" to mapOf("one" to room.offers.any { it.kind == OfferKind.REMATCH && it.seat == Seat.ONE }, "two" to room.offers.any { it.kind == OfferKind.REMATCH && it.seat == Seat.TWO }),
        ),
        "moves" to room.moves.associate { it.ply.toString() to mapOf("by" to seatName(it.seat), "m" to encodeAction(it.seat, it.action).substringAfter('|')) }.ifEmpty { null },
        "moveCount" to room.moves.size,
        "lastMoveAt" to room.lastMoveAtMs.takeIf { it > 0 },
        "toMove" to RoomRules.toMove(room)?.let { seatName(it) },
    )

    /** Parse a database value back. Unknown shapes return null rather than throwing. */
    fun fromMap(code: String, value: Map<*, *>): RoomSnapshot? {
        val variant = variantOf(value["variant"] as? String)
        val config = RoomConfig(
            variant = variant,
            clockMinutes = (value["clockMinutes"] as? Number)?.toInt() ?: 0,
            styleOne = value["styleOne"] as? String ?: "obsidian",
            styleTwo = value["styleTwo"] as? String ?: "quartz",
            ranked = value["ranked"] == true,
        )
        val connected = buildSet {
            val c = value["connected"] as? Map<*, *>
            if (c?.get("one") == true) add(Seat.ONE)
            if (c?.get("two") == true) add(Seat.TWO)
        }
        val left = buildMap {
            val l = value["leftAt"] as? Map<*, *> ?: emptyMap<Any, Any>()
            for ((k, v) in l) {
                val seat = if (k == "two") Seat.TWO else if (k == "one") Seat.ONE else continue
                (v as? Number)?.toLong()?.let { put(seat, it) }
            }
        }
        // RTDB returns integer-keyed maps as lists when keys are dense; accept both shapes.
        // Each move is {by, m}; RTDB returns dense integer keys as a List.
        val entries: List<Pair<Any?, Any?>> = when (val m = value["moves"]) {
            is Map<*, *> -> m.entries.map { it.key to it.value }
            is List<*> -> m.mapIndexed { i, v -> i.toString() to v }
            else -> emptyList()
        }
        val moves = entries.map { (k, v) ->
            val idx = (k as? String)?.toIntOrNull() ?: return null
            val node = v as? Map<*, *> ?: return null
            decodeMove(idx, "${node["by"]}|${node["m"]}") ?: return null
        }.sortedBy { it.ply }
        if (moves.indices.any { moves[it].ply != it }) return null
        val offers = buildList {
            val o = value["offers"] as? Map<*, *>
            for (kind in OfferKind.entries) {
                val m = o?.get(kind.name.lowercase()) as? Map<*, *>
                if (m?.get("one") == true) add(Offer(Seat.ONE, kind))
                if (m?.get("two") == true) add(Offer(Seat.TWO, kind))
            }
        }
        return RoomSnapshot(
            code = code,
            config = config,
            phase = phaseOf(value["phase"] as? String),
            moves = moves,
            connected = connected,
            resigned = (value["resigned"] as? String)?.let { if (it == "two") Seat.TWO else if (it == "one") Seat.ONE else null },
            offers = offers,
            updatedAtMs = (value["updatedAt"] as? Number)?.toLong() ?: 0,
            leftAtMs = left,
            lastMoveAtMs = (value["lastMoveAt"] as? Number)?.toLong() ?: 0,
        )
    }
}
