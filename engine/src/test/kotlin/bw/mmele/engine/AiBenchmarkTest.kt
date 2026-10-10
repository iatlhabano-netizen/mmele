package bw.mmele.engine

import bw.mmele.engine.ai.Ai
import bw.mmele.engine.ai.Difficulty
import bw.mmele.engine.ai.Evaluator
import bw.mmele.engine.ai.SearchAi
import bw.mmele.engine.ai.SearchConfig
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Medium AI benchmark. Reports nodes per second and the depth reached under the time cap, both at
 * full speed and with the cap shrunk by [SLOW_DEVICE_FACTOR] to stand in for a slow phone (a low-end
 * Android device runs this kind of integer search roughly 5-8x slower than a desktop JVM).
 * The report goes to build/reports/ai-benchmark.txt and stdout.
 */
class AiBenchmarkTest {

    /** Realistic positions: states from every phase from Medium-vs-Easy play, across all phases. */
    private fun positions(count: Int, seed: Int): List<GameState> {
        val rnd = Random(seed)
        val out = ArrayList<GameState>()
        val easy = Ai.create(Difficulty.EASY, seed = seed.toLong())
        while (out.size < count) {
            var s = Rules.newGame(RuleSet.CLASSIC)
            while (!s.isGameOver && out.size < count) {
                if (!s.mustShoot && rnd.nextFloat() < 0.12f && Rules.legalActions(s).size > 1) out += s
                s = Rules.apply(s, easy.chooseAction(s))
            }
        }
        return out
    }

    private fun medium(timeMs: Long) =
        SearchAi(SearchConfig(maxDepth = 3, timeLimitMs = timeMs, useTranspositionTable = false, evaluator = Evaluator.MEDIUM), Random(1))

    @Test fun mediumReachesDepthThreeOnSlowDevices() {
        val all = positions(80, 7)
        // Warm the JIT so we measure steady-state speed, as on a phone after the first few moves.
        val warm = medium(10_000)
        all.take(15).forEach { warm.chooseAction(it) }

        // 1) Uncapped: how much work does a full depth-3 search take?
        val ai = medium(60_000)
        var nodes = 0L
        var nanos = 0L
        var maxMs = 0.0
        for (s in all) {
            val t0 = System.nanoTime()
            ai.chooseAction(s)
            val dt = System.nanoTime() - t0
            nanos += dt
            nodes += ai.lastNodes
            maxMs = maxOf(maxMs, dt / 1e6)
        }
        val nps = nodes * 1e9 / nanos
        val avgNodes = nodes / all.size

        // 2) Under the real Medium cap, and under the cap shrunk to emulate a slow device.
        val cap = Ai.defaultTimeMs(Difficulty.MEDIUM)
        val slowCap = cap / SLOW_DEVICE_FACTOR
        fun depths(timeMs: Long): IntArray {
            val capped = medium(timeMs)
            // A search that proved a forced result (or had one legal move) counts as complete.
            return all.map { capped.chooseAction(it); if (capped.lastSearchComplete) 3 else capped.lastDepthCompleted }.toIntArray()
        }
        val atCap = depths(cap)
        val atSlow = depths(slowCap)
        fun share3(d: IntArray) = d.count { it >= 3 } * 100.0 / d.size

        val report = buildString {
            appendLine("Mmele Medium AI benchmark (${all.size} classic positions from opening to endgame)")
            appendLine("nodes/second (depth-3 search, uncapped): ${"%,.0f".format(nps)}")
            appendLine("average nodes per full depth-3 search:   ${"%,d".format(avgNodes)}")
            appendLine("average / worst ms per depth-3 search:   ${"%.1f".format(nanos / 1e6 / all.size)} / ${"%.1f".format(maxMs)}")
            appendLine("depth 3 reached under the ${cap} ms cap:          ${"%.0f".format(share3(atCap))}% (min depth ${atCap.min()})")
            appendLine("depth 3 reached under ${slowCap} ms (${SLOW_DEVICE_FACTOR}x slower device): ${"%.0f".format(share3(atSlow))}% (min depth ${atSlow.min()})")
        }
        println(report)
        File("build/reports").mkdirs()
        File("build/reports/ai-benchmark.txt").writeText(report)

        assertTrue("depth 3 under full cap: $report", share3(atCap) == 100.0)
        assertTrue("depth 3 on a slow device: $report", share3(atSlow) >= 95.0)
    }

    companion object {
        const val SLOW_DEVICE_FACTOR = 8L
    }
}
