package bw.mmele.online

import bw.mmele.engine.Action
import bw.mmele.engine.Boards
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Plays a scripted game through [RoomRules] and writes every Firebase write the app would make
 * to `firebase/fixtures/kotlin-payloads.json`. `firebase/payloads.test.mjs` replays that file
 * against the emulator with the real security rules, proving client and rules agree.
 */
class PayloadFixtureTest {
    private fun c(l: String) = Boards.CLASSIC.labels.indexOf(l)

    @Test fun exportScriptedGame() {
        val steps = mutableListOf<Pair<String, Map<String, Any?>>>() // actor -> update
        var room = RoomRules.create("MMERE2", RoomConfig(styleOne = "diamond-smoky", styleTwo = "diamond-clear"), 0)
        val create = RtdbPayloads.create(room, "\$HOST")
        room = (RoomRules.join(room, 1) as SubmitResult.Accepted).room
        steps += "guest" to RtdbPayloads.join("\$GUEST")

        fun play(seat: Seat, action: Action) {
            val r = RoomRules.submit(room, seat, action, 2)
            assertTrue("$action rejected: $r", r is SubmitResult.Accepted)
            val after = (r as SubmitResult.Accepted).room
            steps += (if (seat == Seat.ONE) "host" else "guest") to RtdbPayloads.move(room, after, seat, action)
            room = after
        }
        // Host builds a mill on the top row and shoots.
        play(Seat.ONE, Action.Place(c("a7")))
        play(Seat.TWO, Action.Place(c("a1")))
        play(Seat.ONE, Action.Place(c("d7")))
        play(Seat.TWO, Action.Place(c("g1")))
        // Guest offers a draw; the next move withdraws it.
        room = (RoomRules.offerDraw(room, Seat.TWO, 3) as SubmitResult.Accepted).room.also { steps += "guest" to RtdbPayloads.offerDraw(it, Seat.TWO) }
        play(Seat.ONE, Action.Place(c("g7")))
        play(Seat.ONE, Action.Shoot(c("a1")))
        play(Seat.TWO, Action.Place(c("d1")))
        // Guest steps away and back.
        steps += "guest" to RtdbPayloads.disconnect(Seat.TWO)
        steps += "guest" to RtdbPayloads.reconnect(Seat.TWO)
        // Host resigns, both agree to a rematch, and the new game starts.
        room = (RoomRules.resign(room, Seat.ONE, 4) as SubmitResult.Accepted).room.also { steps += "host" to RtdbPayloads.resign(Seat.ONE) }
        room = (RoomRules.offerRematch(room, Seat.ONE, 5) as SubmitResult.Accepted).room.also { steps += "host" to RtdbPayloads.offerRematch(it, Seat.ONE) }
        room = (RoomRules.offerRematch(room, Seat.TWO, 6) as SubmitResult.Accepted).room.also { steps += "guest" to RtdbPayloads.offerRematch(it, Seat.TWO) }
        play(Seat.ONE, Action.Place(c("d6")))
        play(Seat.TWO, Action.Place(c("d2")))
        // Agreed draw.
        room = (RoomRules.offerDraw(room, Seat.ONE, 7) as SubmitResult.Accepted).room.also { steps += "host" to RtdbPayloads.offerDraw(it, Seat.ONE) }
        room = (RoomRules.offerDraw(room, Seat.TWO, 8) as SubmitResult.Accepted).room.also { steps += "guest" to RtdbPayloads.offerDraw(it, Seat.TWO) }

        val json = buildString {
            append("{\n  \"code\": \"MMERE2\",\n  \"create\": ").append(toJson(create)).append(",\n  \"steps\": [\n")
            steps.forEachIndexed { i, (actor, update) ->
                append("    {\"actor\": \"").append(actor).append("\", \"update\": ").append(toJson(update)).append("}")
                append(if (i < steps.lastIndex) ",\n" else "\n")
            }
            append("  ],\n  \"expectFinal\": {\"phase\": \"finished\", \"moveCount\": 2}\n}\n")
        }
        val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
        File(root, "firebase/fixtures").mkdirs()
        File(root, "firebase/fixtures/kotlin-payloads.json").writeText(json)
    }

    private fun toJson(v: Any?): String = when (v) {
        null -> "null"
        is String -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Number, is Boolean -> v.toString()
        is Map<*, *> -> v.entries.joinToString(", ", "{", "}") { (k, x) -> toJson(k.toString()) + ": " + toJson(x) }
        else -> error("unsupported $v")
    }
}
