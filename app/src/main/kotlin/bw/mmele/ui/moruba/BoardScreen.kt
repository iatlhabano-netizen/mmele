package bw.mmele.ui.moruba

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bw.mmele.data.FactCard
import bw.mmele.engine.Player
import bw.mmele.engine.ai.Difficulty
import bw.mmele.engine.moruba.MorubaEngine
import bw.mmele.feedback.FeedbackController
import bw.mmele.ui.facts.FactCardHeader
import bw.mmele.ui.home.Segmented
import bw.mmele.ui.labels.label
import bw.mmele.ui.scenes.Scene
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

private val Wood = Color(0xFF6B4423)
private val WoodLight = Color(0xFF8E5E34)
private val PitDeep = Color(0xFF2A170B)
private val PitRim = Color(0xFFA9733F)
private val StoneColours = listOf(Color(0xFFD9CBB0), Color(0xFFB9A88A), Color(0xFF9C8E78), Color(0xFFE8DCC4))

/** Setup for a Moruba game: holes per row, stones per pit, opponent. */
@Composable
fun MorubaSetupScreen(setup: MorubaSetup, onChange: (MorubaSetup) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(MmeleColors.Slate)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onBack) { Text("‹ " + stringResource(R.string.nav_home), color = MmeleColors.BoneDim) }
                Text(stringResource(R.string.moruba_title), style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone)
                Text(stringResource(R.string.moruba_rules_short), style = MaterialTheme.typography.bodyMedium, color = MmeleColors.BoneDim)
                Label(stringResource(R.string.moruba_opponent))
                Segmented(listOf(stringResource(R.string.mode_solo), stringResource(R.string.mode_pass_and_play)), if (setup.vsComputer) 0 else 1) {
                    onChange(setup.copy(vsComputer = it == 0))
                }
                if (setup.vsComputer) {
                    val levels = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)
                    Segmented(levels.map { it.label() }, levels.indexOf(setup.difficulty).coerceAtLeast(0)) { onChange(setup.copy(difficulty = levels[it])) }
                }
                Label(stringResource(R.string.moruba_holes))
                Segmented(listOf("6", "8", "10"), listOf(6, 8, 10).indexOf(setup.holes)) { onChange(setup.copy(holes = listOf(6, 8, 10)[it])) }
                Label(stringResource(R.string.moruba_stones))
                Segmented(listOf("2", "3", "4"), setup.stones - 2) { onChange(setup.copy(stones = it + 2)) }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate),
                ) { Text(stringResource(R.string.home_play), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable private fun Label(t: String) = Text(t.uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.GoldBright)

@Composable
fun MorubaRoute(vm: MorubaViewModel, feedback: FeedbackController?, onHome: () -> Unit, factCard: FactCard?) {
    val ui by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.effects.collect { feedback?.play(it.event, it.rate) } }
    BackHandler(onBack = onHome)
    MorubaScreenContent(ui, vm::onIntent, onHome, factCard)
}

@Composable
fun MorubaScreenContent(ui: MorubaUiState, onIntent: (MorubaIntent) -> Unit, onHome: () -> Unit, factCard: FactCard? = null) {
    Box(Modifier.fillMaxSize().background(MmeleColors.Slate)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onHome) { Text("‹ " + stringResource(R.string.nav_home), color = MmeleColors.BoneDim) }
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.moruba_title), style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
            }
            SideHeader(ui, Player.TWO)
            Spacer(Modifier.weight(1f))
            MorubaBoard(ui, onTap = { onIntent(MorubaIntent.Tap(it)) }, modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth())
            Spacer(Modifier.weight(1f))
            SideHeader(ui, Player.ONE)
            Spacer(Modifier.height(8.dp))
            Text(status(ui), style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Bone, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
        }
        if (ui.showGameOver) {
            Dialog(onDismissRequest = { onIntent(MorubaIntent.DismissGameOver) }) { MorubaGameOverCard(ui, onIntent, onHome, factCard) }
        }
    }
}

@Composable
private fun sideName(ui: MorubaUiState, p: Player) = when {
    ui.setup.vsComputer && p == Player.TWO -> stringResource(R.string.moruba_computer)
    ui.setup.vsComputer -> stringResource(R.string.moruba_you)
    p == Player.ONE -> stringResource(R.string.moruba_south)
    else -> stringResource(R.string.moruba_north)
}

@Composable
private fun SideHeader(ui: MorubaUiState, p: Player) {
    val stones = MorubaEngine.loop(ui.game.holes, p).sumOf { ui.pits[it] }
    val toMove = ui.game.toMove == p && !ui.game.isGameOver
    Text(
        "${sideName(ui, p)} · " + stringResource(R.string.moruba_stones_on_board, stones),
        style = MaterialTheme.typography.labelLarge,
        color = if (toMove) MmeleColors.GoldBright else MmeleColors.BoneDim,
    )
}

@Composable
private fun status(ui: MorubaUiState): String = when {
    ui.game.isGameOver -> winnerTitle(ui)
    ui.animating -> stringResource(R.string.moruba_sowing, ui.inHand)
    ui.aiThinking -> stringResource(R.string.moruba_thinking)
    ui.setup.vsComputer -> stringResource(R.string.moruba_your_turn)
    else -> stringResource(R.string.moruba_side_turn, sideName(ui, ui.game.toMove))
}

@Composable
private fun winnerTitle(ui: MorubaUiState): String {
    val w = ui.game.winner ?: return ""
    return when {
        ui.setup.vsComputer && w == Player.ONE -> stringResource(R.string.result_you_win)
        ui.setup.vsComputer -> stringResource(R.string.result_computer_wins)
        else -> stringResource(R.string.result_side_wins, sideName(ui, w))
    }
}

/** Pit centres on a unit-less grid: column c, row r (row 0 at the top). */
private data class Geometry(val cell: Float, val ox: Float, val oy: Float, val holes: Int) {
    fun centre(pit: Int) = Offset(ox + (pit % holes + 0.5f) * cell, oy + (pit / holes + 0.5f) * cell + if (pit / holes >= 2) cell * 0.25f else 0f)
    val radius get() = cell * 0.42f
}

@Composable
fun MorubaBoard(ui: MorubaUiState, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val h = ui.game.holes
    val measurer = rememberTextMeasurer()
    // Deterministic per-pit stone offsets: same pit + same stone index always lands on the same spot.
    val offsets = remember(h) {
        List(4 * h) { pit ->
            val r = Random(pit * 7919 + 17)
            List(MAX_DRAWN) {
                var o: Offset
                do { o = Offset(r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1) } while (o.x * o.x + o.y * o.y > 1f)
                o
            }
        }
    }
    val desc = stringResource(R.string.moruba_board_desc)
    Canvas(
        modifier
            .aspectRatio(h / 4.25f + 0.02f)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MmeleColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .semantics { contentDescription = desc }
            .pointerInput(ui.game, ui.animating) {
                detectTapGestures { pos ->
                    val g = geometry(size.width.toFloat(), size.height.toFloat(), h)
                    val pit = (0 until 4 * h).minByOrNull { val c = g.centre(it); hypot(c.x - pos.x, c.y - pos.y) } ?: return@detectTapGestures
                    val c = g.centre(pit)
                    if (hypot(c.x - pos.x, c.y - pos.y) <= g.cell * 0.55f) onTap(pit)
                }
            },
    ) {
        drawRect(Brush.verticalGradient(listOf(WoodLight, Wood, WoodLight, Wood)))
        val g = geometry(size.width, size.height, h)
        // groove between the two players' halves
        drawLine(PitDeep.copy(alpha = 0.6f), Offset(g.ox, g.oy + 2.125f * g.cell), Offset(size.width - g.ox, g.oy + 2.125f * g.cell), strokeWidth = g.cell * 0.05f)
        for (pit in 0 until 4 * h) {
            val c = g.centre(pit)
            drawPit(c, g.radius, highlighted = ui.canTap(pit), active = pit == ui.active, captured = pit in ui.lastCapture)
            val n = ui.pits[pit]
            val stoneR = g.radius * 0.2f
            val spread = g.radius * 0.78f - stoneR // keeps every stone inside the pit
            for (k in 0 until min(n, MAX_DRAWN)) {
                val o = offsets[pit][k]
                val p = Offset(c.x + o.x * spread, c.y + o.y * spread)
                drawStone(p, stoneR, StoneColours[(pit + k) % StoneColours.size])
            }
            if (n > 0) {
                val badge = Offset(c.x + g.radius * 0.72f, c.y - g.radius * 0.72f)
                drawCircle(MmeleColors.Slate.copy(alpha = 0.85f), g.cell * 0.15f, badge)
                val t = measurer.measure(n.toString(), TextStyle(color = MmeleColors.Bone, fontSize = (g.cell * 0.16f / density).sp, fontWeight = FontWeight.Bold))
                drawText(t, topLeft = Offset(badge.x - t.size.width / 2f, badge.y - t.size.height / 2f))
            }
        }
    }
}

private const val MAX_DRAWN = 14

private fun geometry(w: Float, hgt: Float, holes: Int): Geometry {
    val cell = min(w / holes, hgt / 4.25f)
    return Geometry(cell, (w - cell * holes) / 2, (hgt - cell * 4.25f) / 2, holes)
}

private fun DrawScope.drawPit(c: Offset, r: Float, highlighted: Boolean, active: Boolean, captured: Boolean) {
    // carved bowl: dark centre, lighter rim
    drawCircle(Brush.radialGradient(listOf(PitDeep, Color(0xFF4A2C15), PitRim), center = Offset(c.x, c.y + r * 0.12f), radius = r), r, c)
    drawCircle(Color.Black.copy(alpha = 0.25f), r, c, style = Stroke(r * 0.06f))
    if (highlighted) drawCircle(MmeleColors.Gold.copy(alpha = 0.55f), r * 1.04f, c, style = Stroke(r * 0.08f))
    if (active) drawCircle(MmeleColors.GoldBright, r * 1.04f, c, style = Stroke(r * 0.12f))
    if (captured) drawCircle(Color(0xFFE0603A), r * 1.04f, c, style = Stroke(r * 0.12f))
}

private fun DrawScope.drawStone(p: Offset, r: Float, colour: Color) {
    drawCircle(Color.Black.copy(alpha = 0.35f), r, Offset(p.x + r * 0.15f, p.y + r * 0.2f))
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), colour, colour.copy(alpha = 1f).darken()), center = Offset(p.x - r * 0.35f, p.y - r * 0.35f), radius = r * 1.4f), r, p)
}

private fun Color.darken() = Color(red * 0.6f, green * 0.6f, blue * 0.6f, alpha)

@Composable
private fun MorubaGameOverCard(ui: MorubaUiState, onIntent: (MorubaIntent) -> Unit, onHome: () -> Unit, factCard: FactCard?) {
    Column(
        Modifier
            .background(MmeleColors.SlateRaised, RoundedCornerShape(24.dp))
            .border(1.dp, MmeleColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (factCard != null) FactCardHeader(factCard, Scene.KALAHARI)
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(winnerTitle(ui), style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone)
            Text(stringResource(R.string.result_moves, ui.turns), style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onHome) { Text(stringResource(R.string.nav_home), color = MmeleColors.Bone) }
                Button(onClick = { onIntent(MorubaIntent.Rematch) }, colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate)) {
                    Text(stringResource(R.string.game_rematch))
                }
            }
        }
    }
}
