package bw.mmele.ui.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Layered-silhouette illustrations. [t] in 0..1 is a slow ambient phase (shimmer, drift, twinkle,
 * and a gentle parallax between layers). All shapes are expressed relative to the canvas size so
 * the scenes fill any aspect ratio.
 */
fun DrawScope.drawScene(scene: Scene, t: Float) {
    when (scene) {
        Scene.OKAVANGO -> okavango(t)
        Scene.CHOBE -> chobe(t)
        Scene.MAKGADIKGADI -> makgadikgadi(t)
        Scene.KALAHARI -> kalahari(t)
        Scene.TSODILO -> tsodilo(t)
    }
}

private val TAU = (2 * PI).toFloat()
private fun wave(t: Float, k: Float = 1f) = sin(t * TAU * k)

// ------------------------------------------------------------------ shared pieces

private fun DrawScope.sky(vararg stops: Pair<Float, Color>, bottom: Float = size.height) {
    drawRect(Brush.verticalGradient(*stops, startY = 0f, endY = bottom))
}

private fun DrawScope.sunDisc(c: Offset, r: Float, core: Color, glow: Color) {
    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0f)), c, r * 5f), r * 5f, c)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), core), c, r), r, c)
}

/** A wavy horizon band from y to the bottom of the canvas. */
private fun DrawScope.ridge(y: Float, amp: Float, freq: Float, phase: Float, color: Color, brush: Brush? = null) {
    val w = size.width
    val p = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, y)
        val steps = 48
        for (i in 0..steps) {
            val x = w * i / steps
            val yy = y + amp * sin(i.toFloat() / steps * TAU * freq + phase) + amp * 0.4f * sin(i.toFloat() / steps * TAU * freq * 2.3f + phase * 1.7f)
            lineTo(x, yy)
        }
        lineTo(w, size.height)
        close()
    }
    if (brush != null) drawPath(p, brush) else drawPath(p, color)
}

private fun DrawScope.stars(count: Int, maxY: Float, t: Float, seed: Int, color: Color = Color.White) {
    val rnd = Random(seed)
    val minDim = size.minDimension
    repeat(count) { i ->
        val x = rnd.nextFloat() * size.width
        val y = rnd.nextFloat().let { it * it } * maxY
        val base = 0.25f + rnd.nextFloat() * 0.75f
        val tw = 0.65f + 0.35f * sin((t * 6f + rnd.nextFloat()) * TAU + i)
        val r = minDim * (0.0012f + rnd.nextFloat() * rnd.nextFloat() * 0.0035f)
        drawCircle(color.copy(alpha = (base * tw).coerceIn(0f, 1f)), r, Offset(x, y))
    }
}

private fun DrawScope.birds(origin: Offset, s: Float, color: Color, n: Int, seed: Int) {
    val rnd = Random(seed)
    repeat(n) {
        val c = origin + Offset(rnd.nextFloat() * s * 10, rnd.nextFloat() * s * 4)
        val w = s * (0.5f + rnd.nextFloat() * 0.5f)
        val p = Path().apply {
            moveTo(c.x - w, c.y - w * 0.3f)
            quadraticTo(c.x - w * 0.4f, c.y - w * 0.55f, c.x, c.y)
            quadraticTo(c.x + w * 0.4f, c.y - w * 0.55f, c.x + w, c.y - w * 0.3f)
        }
        drawPath(p, color, style = Stroke(width = max(1f, w * 0.14f), cap = StrokeCap.Round))
    }
}

/** Draw in a local frame: origin at a ground point, x scaled by [dir]*[s], y by [s] (negative y = up). */
private inline fun DrawScope.local(at: Offset, s: Float, dir: Float = 1f, block: DrawScope.() -> Unit) {
    withTransform({
        translate(at.x, at.y)
        scale(dir * s, s, pivot = Offset.Zero)
    }, block)
}

private fun DrawScope.acacia(at: Offset, s: Float, color: Color) = local(at, s) {
    val trunk = Path().apply {
        moveTo(-2.5f, 0f); quadraticTo(-4f, -22f, -1.5f, -40f); lineTo(1.5f, -40f); quadraticTo(0f, -22f, 2.5f, 0f); close()
    }
    drawPath(trunk, color)
    drawLine(color, Offset(-1f, -36f), Offset(-22f, -54f), 3f, StrokeCap.Round)
    drawLine(color, Offset(0f, -38f), Offset(20f, -56f), 3f, StrokeCap.Round)
    drawLine(color, Offset(0f, -40f), Offset(3f, -58f), 2.4f, StrokeCap.Round)
    for ((cx, cy, rx) in listOf(Triple(-24f, -58f, 22f), Triple(0f, -62f, 30f), Triple(24f, -59f, 22f), Triple(-8f, -64f, 20f), Triple(12f, -64f, 18f))) {
        drawOval(color, Offset(cx - rx, cy - 6f), Size(rx * 2, 12f))
    }
}

private fun DrawScope.baobab(at: Offset, s: Float, color: Color) = local(at, s) {
    val trunk = Path().apply {
        moveTo(-20f, 0f)
        cubicTo(-16f, -20f, -15f, -40f, -11f, -58f)
        lineTo(11f, -58f)
        cubicTo(15f, -40f, 16f, -20f, 20f, 0f)
        close()
    }
    drawPath(trunk, color)
    val branches = listOf(
        Offset(-9f, -56f) to Offset(-30f, -76f), Offset(-30f, -76f) to Offset(-40f, -80f), Offset(-30f, -76f) to Offset(-32f, -88f),
        Offset(-4f, -57f) to Offset(-10f, -86f), Offset(-10f, -86f) to Offset(-18f, -94f), Offset(-10f, -86f) to Offset(-6f, -98f),
        Offset(4f, -57f) to Offset(10f, -88f), Offset(10f, -88f) to Offset(18f, -97f), Offset(10f, -88f) to Offset(6f, -99f),
        Offset(9f, -56f) to Offset(30f, -74f), Offset(30f, -74f) to Offset(42f, -78f), Offset(30f, -74f) to Offset(33f, -86f),
    )
    branches.forEachIndexed { i, (a, b) -> drawLine(color, a, b, if (i % 3 == 0) 6f else 2.6f, StrokeCap.Round) }
}

private fun DrawScope.elephant(at: Offset, s: Float, dir: Float, color: Color, tusk: Color) = local(at, s, dir) {
    for (x in listOf(22f, 34f, 56f, 67f)) drawRoundRect(color, Offset(x, -32f), Size(10f, 32f), androidx.compose.ui.geometry.CornerRadius(3f))
    drawOval(color, Offset(13f, -64f), Size(64f, 40f))           // body
    drawOval(color, Offset(18f, -70f), Size(40f, 22f))           // back
    drawCircle(color, 15f, Offset(80f, -52f))                    // head
    drawOval(color.copy(alpha = 1f), Offset(58f, -66f), Size(22f, 30f)) // ear
    val trunk = Path().apply { moveTo(90f, -50f); cubicTo(100f, -36f, 96f, -18f, 99f, -6f) }
    drawPath(trunk, color, style = Stroke(width = 8f, cap = StrokeCap.Round))
    drawLine(tusk, Offset(88f, -40f), Offset(97f, -33f), 3f, StrokeCap.Round)
    drawLine(color, Offset(14f, -50f), Offset(9f, -30f), 2.5f, StrokeCap.Round) // tail
}

private fun DrawScope.gemsbok(at: Offset, s: Float, dir: Float, color: Color) = local(at, s, dir) {
    drawOval(color, Offset(10f, -40f), Size(40f, 18f))
    val neck = Path().apply { moveTo(42f, -38f); lineTo(50f, -54f); lineTo(56f, -52f); lineTo(50f, -30f); close() }
    drawPath(neck, color)
    drawOval(color, Offset(50f, -58f), Size(13f, 7f))
    drawLine(color, Offset(52f, -56f), Offset(38f, -92f), 1.8f, StrokeCap.Round)
    drawLine(color, Offset(55f, -56f), Offset(42f, -93f), 1.8f, StrokeCap.Round)
    for ((a, b) in listOf(16f to 15f, 22f to 24f, 42f to 41f, 47f to 49f)) drawLine(color, Offset(a, -26f), Offset(b, 0f), 3.4f, StrokeCap.Round)
    drawLine(color, Offset(11f, -36f), Offset(6f, -18f), 2f, StrokeCap.Round)
}

private fun DrawScope.papyrus(base: Offset, h: Float, lean: Float, color: Color) {
    val top = Offset(base.x + lean, base.y - h)
    drawLine(color, base, top, max(1f, h * 0.012f), StrokeCap.Round)
    val n = 15
    for (i in 0 until n) {
        val a = (-160f + 140f * i / (n - 1)) * PI.toFloat() / 180f
        val len = h * 0.16f
        drawLine(color, top, Offset(top.x + cos(a) * len, top.y + sin(a) * len * 0.8f), max(1f, h * 0.006f), StrokeCap.Round)
    }
}

private fun DrawScope.palm(base: Offset, h: Float, color: Color) {
    val top = Offset(base.x + h * 0.08f, base.y - h)
    val trunk = Path().apply { moveTo(base.x, base.y); quadraticTo(base.x - h * 0.05f, base.y - h * 0.5f, top.x, top.y) }
    drawPath(trunk, color, style = Stroke(width = h * 0.035f, cap = StrokeCap.Round))
    for (i in 0 until 7) {
        val a = (-170f + 160f * i / 6f) * PI.toFloat() / 180f
        val end = Offset(top.x + cos(a) * h * 0.3f, top.y + sin(a) * h * 0.18f + h * 0.08f)
        val ctrl = Offset(top.x + cos(a) * h * 0.18f, top.y - h * 0.06f)
        val p = Path().apply { moveTo(top.x, top.y); quadraticTo(ctrl.x, ctrl.y, end.x, end.y) }
        drawPath(p, color, style = Stroke(width = h * 0.022f, cap = StrokeCap.Round))
    }
}

// ------------------------------------------------------------------ scenes

private fun DrawScope.okavango(t: Float) {
    val w = size.width; val h = size.height
    val horizon = h * 0.52f
    sky(0f to Color(0xFF2A1B3D), 0.35f to Color(0xFF7A3B52), 0.7f to Color(0xFFE0784A), 1f to Color(0xFFF6C26B), bottom = horizon)
    val sun = Offset(w * 0.62f, horizon - h * 0.035f)
    sunDisc(sun, size.minDimension * 0.075f, Color(0xFFFFC46B), Color(0xFFFF9A4A))
    birds(Offset(w * 0.18f + wave(t) * w * 0.02f, h * 0.18f), size.minDimension * 0.022f, Color(0xFF2A1B2E), 6, 3)
    // distant tree line: palms + bush
    ridge(horizon - h * 0.012f, h * 0.006f, 5f, 0.8f, Color(0xFF2B1A26))
    for (i in 0 until 7) palm(Offset(w * (0.04f + i * 0.15f) + (i % 2) * w * 0.03f, horizon), h * (0.07f + (i % 3) * 0.02f), Color(0xFF2B1A26))
    // water with reflection
    drawRect(Brush.verticalGradient(listOf(Color(0xFFE58C55), Color(0xFF6B3A4E), Color(0xFF1E1A2C)), startY = horizon, endY = h), Offset(0f, horizon), Size(w, h - horizon))
    drawRect(Brush.verticalGradient(listOf(Color(0x66FFD38A), Color.Transparent), startY = horizon, endY = horizon + h * 0.25f),
        Offset(sun.x - w * 0.06f, horizon), Size(w * 0.12f, h * 0.25f))
    // shimmer lines
    val rnd = Random(11)
    repeat(40) { i ->
        val y = horizon + (h - horizon) * (rnd.nextFloat().let { it * it })
        val x = rnd.nextFloat() * w + wave(t, 1f) * w * 0.01f * (if (i % 2 == 0) 1 else -1)
        val len = w * (0.02f + rnd.nextFloat() * 0.08f)
        drawLine(Color(0xFFFFD9A0).copy(alpha = 0.12f + 0.25f * rnd.nextFloat()), Offset(x, y), Offset(x + len, y), max(1f, h * 0.0015f))
    }
    // reed islands floating on the open water, each with a fringe of papyrus
    val island = Color(0xFF1C1520)
    for ((cx, cy, iw) in listOf(Triple(0.15f, 0.07f, 0.34f), Triple(0.78f, 0.11f, 0.42f), Triple(0.42f, 0.04f, 0.22f))) {
        val y = horizon + h * cy
        drawOval(island, Offset(w * cx - w * iw / 2, y - h * 0.008f), Size(w * iw, h * 0.022f))
        val n = (iw * 40).toInt()
        for (k in 0 until n) {
            val x = w * cx - w * iw * 0.45f + w * iw * 0.9f * k / n
            papyrus(Offset(x, y), h * (0.035f + 0.02f * ((k * 7) % 5) / 4f), (k % 3 - 1) * h * 0.004f, island)
        }
        // soft reflection
        drawOval(Color(0x221C1520), Offset(w * cx - w * iw / 2, y + h * 0.01f), Size(w * iw, h * 0.03f))
    }
    // mokoro with a poler, gliding along a sunlit channel
    val mx = w * 0.5f + wave(t) * w * 0.02f
    val my = horizon + h * 0.27f
    drawOval(Brush.radialGradient(listOf(Color(0x55FFD38A), Color.Transparent), Offset(mx, my), w * 0.3f), Offset(mx - w * 0.3f, my - h * 0.02f), Size(w * 0.6f, h * 0.04f))
    local(Offset(mx, my), size.minDimension * 0.0042f) {
        val c = Color(0xFF120C14)
        val hull = Path().apply { moveTo(-60f, -4f); quadraticTo(0f, 8f, 60f, -4f); quadraticTo(0f, 2f, -60f, -4f); close() }
        drawPath(hull, c)
        drawLine(c, Offset(18f, -2f), Offset(20f, -34f), 5f, StrokeCap.Round)
        drawCircle(c, 4.5f, Offset(20.5f, -39f))
        drawLine(c, Offset(20f, -28f), Offset(30f, -36f), 2.5f, StrokeCap.Round)
        drawLine(c, Offset(6f, 22f), Offset(40f, -64f), 1.8f, StrokeCap.Round)
        drawLine(c, Offset(-30f, -4f), Offset(-29f, -18f), 4f, StrokeCap.Round)
        drawCircle(c, 3.6f, Offset(-29f, -22f))
        drawLine(Color(0x44120C14), Offset(-50f, 6f), Offset(50f, 6f), 3f)
        drawLine(Color(0x33120C14), Offset(19f, 4f), Offset(21f, 26f), 4f)
    }
    // foreground papyrus clusters
    val fg = Color(0xFF0E0B12)
    for (i in 0 until 9) papyrus(Offset(w * (0.0f + i * 0.025f), h), h * (0.26f + (i % 3) * 0.05f), (i - 4) * w * 0.012f + wave(t, 1f) * w * 0.004f, fg)
    for (i in 0 until 8) papyrus(Offset(w * (0.82f + i * 0.025f), h), h * (0.22f + (i % 3) * 0.05f), (i - 3) * w * 0.012f - wave(t, 1f) * w * 0.004f, fg)
}

private fun DrawScope.chobe(t: Float) {
    val w = size.width; val h = size.height
    val horizon = h * 0.5f
    sky(0f to Color(0xFF3B4A6B), 0.5f to Color(0xFFC08A5B), 1f to Color(0xFFF0C27A), bottom = horizon)
    sunDisc(Offset(w * 0.25f, horizon - h * 0.09f), size.minDimension * 0.05f, Color(0xFFFFD58A), Color(0xFFFFB45A))
    // clouds drifting
    val drift = wave(t) * w * 0.03f
    for ((x, y, rw) in listOf(Triple(0.6f, 0.14f, 0.24f), Triple(0.15f, 0.22f, 0.18f), Triple(0.82f, 0.28f, 0.16f))) {
        drawOval(Color(0x33FFE6C0), Offset(w * x + drift - w * rw / 2, h * y), Size(w * rw, h * 0.025f))
    }
    // far bank (Namibian floodplain) with acacias
    ridge(horizon - h * 0.02f, h * 0.008f, 3f, 1.1f, Color(0xFF4A3A3A))
    for (i in 0 until 6) acacia(Offset(w * (0.08f + i * 0.17f), horizon - h * 0.012f), size.minDimension * 0.0018f * (0.9f + (i % 2) * 0.3f), Color(0xFF3A2C2E))
    // river
    val riverTop = horizon + h * 0.04f
    drawRect(Color(0xFF6E5A54), Offset(0f, horizon - h * 0.005f), Size(w, h * 0.05f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFFDDA56E), Color(0xFF5B5E78)), startY = riverTop, endY = riverTop + h * 0.18f), Offset(0f, riverTop), Size(w, h * 0.18f))
    val rnd = Random(5)
    repeat(30) {
        val y = riverTop + rnd.nextFloat() * h * 0.18f
        val x = rnd.nextFloat() * w + wave(t) * w * 0.012f
        drawLine(Color(0x55FFE2B0), Offset(x, y), Offset(x + w * (0.03f + rnd.nextFloat() * 0.06f), y), max(1f, h * 0.0014f))
    }
    // near bank
    val bankY = riverTop + h * 0.16f
    ridge(bankY, h * 0.01f, 1.5f, 0.3f, Color(0xFF2A2024), Brush.verticalGradient(listOf(Color(0xFF3A2C28), Color(0xFF141012)), startY = bankY, endY = h))
    // elephants (adults + calf), with faint reflections
    val el = Color(0xFF1A1416)
    val s = size.minDimension * 0.0024f
    val shift = wave(t) * w * 0.006f
    elephant(Offset(w * 0.06f + shift, bankY + h * 0.012f), s * 1.05f, 1f, el, Color(0xFFD8CBB0))
    elephant(Offset(w * 0.37f + shift, bankY + h * 0.008f), s * 0.6f, 1f, el, Color(0xFFD8CBB0))
    elephant(Offset(w * 0.95f - shift, bankY + h * 0.016f), s * 1.15f, -1f, el, Color(0xFFD8CBB0))
    elephant(Offset(w * 0.6f, riverTop + h * 0.012f), s * 0.42f, -1f, Color(0xFF3B3034), Color(0x66D8CBB0))
    birds(Offset(w * 0.55f, h * 0.2f), size.minDimension * 0.02f, Color(0xFF2E2630), 5, 8)
}

private fun DrawScope.makgadikgadi(t: Float) {
    val w = size.width; val h = size.height
    val horizon = h * 0.62f
    sky(0f to Color(0xFF060A1A), 0.6f to Color(0xFF0E1B3A), 1f to Color(0xFF2B3B63), bottom = horizon)
    // Milky Way band
    withTransform({ rotate(-28f, Offset(w / 2, horizon * 0.5f)) }) {
        drawOval(Brush.verticalGradient(listOf(Color.Transparent, Color(0x22B9C6FF), Color(0x33E6DCFF), Color(0x22B9C6FF), Color.Transparent),
            startY = horizon * 0.5f - h * 0.08f, endY = horizon * 0.5f + h * 0.08f),
            Offset(-w * 0.3f, horizon * 0.5f - h * 0.08f), Size(w * 1.6f, h * 0.16f))
    }
    stars(220, horizon, t, 42)
    stars(120, horizon * 0.8f, t, 7, Color(0xFFDCE4FF))
    // crescent moon
    val moon = Offset(w * 0.8f, h * 0.12f)
    val mr = size.minDimension * 0.035f
    drawCircle(Brush.radialGradient(listOf(Color(0x44DCE6FF), Color.Transparent), moon, mr * 4), mr * 4, moon)
    drawCircle(Color(0xFFF2F0E2), mr, moon)
    drawCircle(Color(0xFF09122A), mr * 0.92f, moon + Offset(mr * 0.42f, -mr * 0.12f))
    // salt pan: pale, flat, faint polygon crust in perspective
    drawRect(Brush.verticalGradient(listOf(Color(0xFF8B97B5), Color(0xFF4E5A7A), Color(0xFF232B42)), startY = horizon, endY = h), Offset(0f, horizon), Size(w, h - horizon))
    // irregular polygonal salt crust, compressed toward the horizon (perspective)
    val crack = Color(0x30E8EEFF)
    val rnd = Random(9)
    val rows = 11
    val cols = 9
    val pts = Array(rows) { r ->
        val f = (r + 1f) / rows
        val y = horizon + (h - horizon) * f * f
        val spread = 0.25f + 0.9f * f
        Array(cols + 1) { c ->
            val u = (c.toFloat() / cols - 0.5f) * 2f * spread + 0.5f
            Offset(w * u + (rnd.nextFloat() - 0.5f) * w * 0.09f * f, y + (rnd.nextFloat() - 0.5f) * (h - horizon) * 0.05f * f)
        }
    }
    for (r in 0 until rows) for (c in 0 until cols) {
        val a = pts[r][c]
        val sw = max(1f, h * 0.0012f * (1 + r / 4f))
        drawLine(crack, a, pts[r][c + 1], sw)
        if (r + 1 < rows) drawLine(crack, a, pts[r + 1][if ((r + c) % 2 == 0) c else c + 1], sw)
    }
    // baobabs on the horizon, Nxai-style group
    val tree = Color(0xFF070A14)
    baobab(Offset(w * 0.2f, horizon + h * 0.006f), size.minDimension * 0.0042f, tree)
    baobab(Offset(w * 0.33f, horizon + h * 0.003f), size.minDimension * 0.0026f, tree)
    baobab(Offset(w * 0.74f, horizon + h * 0.004f), size.minDimension * 0.0034f, tree)
    // shooting star (cheap ambient)
    val sp = (t * 3f) % 1f
    if (sp < 0.12f) {
        val a = Offset(w * (0.2f + sp * 3f), h * (0.08f + sp * 1.2f))
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), a - Offset(w * 0.08f, h * 0.04f), a), a - Offset(w * 0.08f, h * 0.04f), a, 2f)
    }
}

private fun DrawScope.kalahari(t: Float) {
    val w = size.width; val h = size.height
    val horizon = h * 0.48f
    sky(0f to Color(0xFF5F86A8), 0.55f to Color(0xFFE8B98A), 1f to Color(0xFFF7D9A0), bottom = horizon)
    sunDisc(Offset(w * 0.78f, horizon - h * 0.12f), size.minDimension * 0.045f, Color(0xFFFFF0C0), Color(0xFFFFD27A))
    val par = wave(t) * w * 0.01f
    ridge(horizon, h * 0.02f, 1.6f, 0.4f + par / w, Color(0xFFC9895A))
    ridge(horizon + h * 0.07f, h * 0.035f, 1.2f, 2.2f, Color(0xFFB8673C))
    acacia(Offset(w * 0.72f + par, horizon + h * 0.075f), size.minDimension * 0.0045f, Color(0xFF3A2218))
    ridge(horizon + h * 0.18f, h * 0.04f, 0.9f, 4.1f, Color(0xFF9A4E2C))
    // gemsbok herd on the middle dune
    val g = Color(0xFF2A160F)
    val s = size.minDimension * 0.0036f
    gemsbok(Offset(w * 0.18f + par * 1.5f, horizon + h * 0.2f), s, 1f, g)
    gemsbok(Offset(w * 0.33f + par * 1.5f, horizon + h * 0.215f), s * 0.85f, 1f, g)
    gemsbok(Offset(w * 0.5f + par * 1.5f, horizon + h * 0.205f), s * 0.95f, -1f, g)
    ridge(horizon + h * 0.32f, h * 0.03f, 0.7f, 1.3f, Color(0xFF7A3A22),
        Brush.verticalGradient(listOf(Color(0xFF7A3A22), Color(0xFF3C1C12)), startY = horizon + h * 0.3f, endY = h))
    // grass tufts in the foreground
    val rnd = Random(4)
    repeat(26) {
        val x = rnd.nextFloat() * w
        val y = horizon + h * (0.38f + rnd.nextFloat() * 0.5f)
        val gh = h * (0.015f + rnd.nextFloat() * 0.025f)
        for (k in -2..2) drawLine(Color(0x99D9A866), Offset(x, y), Offset(x + k * gh * 0.25f + wave(t, 1f) * gh * 0.1f, y - gh), max(1f, gh * 0.06f))
    }
    acacia(Offset(w * 0.08f, h * 1.01f), size.minDimension * 0.007f, Color(0xFF1E100A))
}

private fun DrawScope.tsodilo(t: Float) {
    val w = size.width; val h = size.height
    val horizon = h * 0.55f
    sky(0f to Color(0xFF2D5560), 0.55f to Color(0xFFB98A7A), 1f to Color(0xFFF0C9A0), bottom = horizon)
    sunDisc(Offset(w * 0.16f, horizon - h * 0.06f), size.minDimension * 0.04f, Color(0xFFFFE2B0), Color(0xFFFFB27A))
    // The "Male", "Female" and "Child" hills rising from the Kalahari sand.
    fun hill(x0: Float, x1: Float, top: Float, color: Color) {
        val shade = Brush.linearGradient(listOf(color.copy(red = (color.red * 1.35f).coerceAtMost(1f), green = (color.green * 1.25f).coerceAtMost(1f)), color, color.copy(red = color.red * 0.7f, green = color.green * 0.7f, blue = color.blue * 0.75f)),
            start = Offset(w * x0, top), end = Offset(w * x1, horizon))
        val p = Path().apply {
            moveTo(w * x0, horizon)
            cubicTo(w * (x0 + 0.04f), horizon - (horizon - top) * 0.7f, w * (x0 + 0.08f), top, w * ((x0 + x1) / 2), top)
            cubicTo(w * (x1 - 0.1f), top + h * 0.01f, w * (x1 - 0.05f), horizon - (horizon - top) * 0.5f, w * x1, horizon)
            close()
        }
        drawPath(p, shade)
        // quartzite bedding lines
        for (k in 1..4) {
            val yy = top + (horizon - top) * k / 5f
            drawLine(Color(0x22000000), Offset(w * (x0 + 0.03f * k), yy), Offset(w * (x1 - 0.02f * k), yy + h * 0.01f), max(1f, h * 0.002f))
        }
    }
    val par = wave(t) * w * 0.006f
    withTransform({ translate(par, 0f) }) {
        hill(0.28f, 0.95f, h * 0.24f, Color(0xFF5E4048))
        hill(0.02f, 0.4f, h * 0.36f, Color(0xFF6E4A4A))
        hill(0.85f, 1.1f, h * 0.44f, Color(0xFF6A4646))
    }
    ridge(horizon, h * 0.006f, 4f, 0.2f, Color(0xFFB98A62))
    for (i in 0 until 5) acacia(Offset(w * (0.1f + i * 0.2f), horizon + h * 0.01f), size.minDimension * 0.0016f, Color(0xFF4A3028))
    // foreground rock panel with ochre rock-art motifs
    val panelTop = horizon + h * 0.12f
    val rock = Path().apply {
        moveTo(0f, h); lineTo(0f, panelTop + h * 0.04f)
        cubicTo(w * 0.2f, panelTop - h * 0.02f, w * 0.55f, panelTop + h * 0.02f, w * 0.8f, panelTop - h * 0.01f)
        cubicTo(w * 0.9f, panelTop, w, panelTop + h * 0.03f, w, panelTop + h * 0.05f)
        lineTo(w, h); close()
    }
    drawPath(rock, Brush.verticalGradient(listOf(Color(0xFF8A6A5A), Color(0xFF5A4038), Color(0xFF2E201C)), startY = panelTop, endY = h))
    // rock grain
    val rnd = Random(21)
    repeat(60) {
        val x = rnd.nextFloat() * w; val y = panelTop + h * 0.04f + rnd.nextFloat() * (h - panelTop)
        drawLine(Color(0x22000000), Offset(x, y), Offset(x + w * 0.05f * rnd.nextFloat(), y + h * 0.006f), 1.5f)
    }
    val ochre = Color(0xFFB0432A).copy(alpha = 0.85f)
    val u = size.minDimension * 0.0045f
    // eland
    local(Offset(w * 0.3f, panelTop + h * 0.16f), u) {
        drawOval(ochre, Offset(-14f, -14f), Size(28f, 12f))
        drawLine(ochre, Offset(12f, -10f), Offset(19f, -18f), 3f, StrokeCap.Round)
        drawOval(ochre, Offset(17f, -21f), Size(7f, 4f))
        drawLine(ochre, Offset(20f, -21f), Offset(22f, -27f), 1.2f, StrokeCap.Round)
        for (x in listOf(-10f, -6f, 6f, 10f)) drawLine(ochre, Offset(x, -4f), Offset(x, 6f), 1.8f, StrokeCap.Round)
        drawLine(ochre, Offset(-14f, -9f), Offset(-17f, -2f), 1.2f, StrokeCap.Round)
    }
    // rhino
    local(Offset(w * 0.62f, panelTop + h * 0.2f), u * 0.9f) {
        drawOval(ochre, Offset(-16f, -16f), Size(32f, 15f))
        drawOval(ochre, Offset(12f, -14f), Size(10f, 8f))
        drawLine(ochre, Offset(20f, -13f), Offset(23f, -19f), 1.8f, StrokeCap.Round)
        for (x in listOf(-11f, -6f, 6f, 11f)) drawLine(ochre, Offset(x, -4f), Offset(x, 5f), 2.6f, StrokeCap.Round)
    }
    // human figures
    for (k in 0 until 4) local(Offset(w * (0.42f + k * 0.035f), panelTop + h * 0.3f), u * 0.8f) {
        drawCircle(ochre, 2.2f, Offset(0f, -22f))
        drawLine(ochre, Offset(0f, -20f), Offset(0f, -8f), 1.6f, StrokeCap.Round)
        drawLine(ochre, Offset(0f, -8f), Offset(-4f, 0f), 1.4f, StrokeCap.Round)
        drawLine(ochre, Offset(0f, -8f), Offset(4f, 0f), 1.4f, StrokeCap.Round)
        drawLine(ochre, Offset(0f, -17f), Offset(5f, -12f + k), 1.2f, StrokeCap.Round)
        drawLine(ochre, Offset(0f, -17f), Offset(-5f, -13f), 1.2f, StrokeCap.Round)
    }
    // geometric "ladder" motif
    local(Offset(w * 0.82f, panelTop + h * 0.3f), u) {
        drawLine(ochre, Offset(-6f, 0f), Offset(-6f, -30f), 1.6f)
        drawLine(ochre, Offset(6f, 0f), Offset(6f, -30f), 1.6f)
        for (k in 0..5) drawLine(ochre, Offset(-6f, -k * 6f), Offset(6f, -k * 6f), 1.4f)
    }
}
