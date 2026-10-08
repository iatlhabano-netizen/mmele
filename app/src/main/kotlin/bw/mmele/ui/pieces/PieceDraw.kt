package bw.mmele.ui.pieces

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import bw.mmele.ui.theme.MmeleColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws one cow. [pieceId] seeds per-piece variation (diamond facet layout, egg speckles)
 * so every cow of a style looks unique but deterministic.
 */
fun DrawScope.drawPiece(
    style: PieceStyle,
    center: Offset,
    radius: Float,
    pieceId: Int = 0,
    elevation: Float = 0f,
    alpha: Float = 1f,
    scale: Float = 1f,
) {
    if (alpha <= 0f || scale <= 0f) return
    when (style) {
        is PieceStyle.Pebble -> drawPebbleStyle(style, center, radius, elevation, alpha, scale)
        is PieceStyle.Diamond -> drawDiamondStyle(style, center, radius, pieceId, elevation, alpha, scale)
        is PieceStyle.Egg -> drawEggStyle(style.kind, center, radius, pieceId, elevation, alpha, scale)
    }
}

// ------------------------------------------------------------------ shared shadow

private fun DrawScope.dropShadow(center: Offset, r: Float, elevation: Float, alpha: Float) {
    val shadowCenter = Offset(center.x + r * (0.10f + 0.12f * elevation), center.y + r * (0.18f + 0.30f * elevation))
    val shadowR = r * (1.12f + 0.35f * elevation)
    drawOval(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = (0.55f - 0.22f * elevation) * alpha),
            0.55f to Color.Black.copy(alpha = (0.28f - 0.10f * elevation) * alpha),
            1f to Color.Transparent,
            center = shadowCenter,
            radius = shadowR,
        ),
        topLeft = Offset(shadowCenter.x - shadowR, shadowCenter.y - shadowR * 0.82f),
        size = Size(shadowR * 2, shadowR * 1.64f),
    )
}

private fun bodyOf(center: Offset, radius: Float, elevation: Float, scale: Float): Pair<Offset, Float> {
    val r = radius * scale * (1f + 0.07f * elevation)
    val lift = radius * 0.32f * elevation
    return Offset(center.x, center.y - lift) to r
}

// ------------------------------------------------------------------ pebbles (classic)

private fun DrawScope.drawPebbleStyle(
    style: PieceStyle.Pebble, center: Offset, radius: Float, elevation: Float, alpha: Float, scale: Float,
) {
    val (body, r) = bodyOf(center, radius, elevation, scale)
    dropShadow(center, r, elevation, alpha)
    val (light, mid, dark) = if (style.dark) {
        Triple(MmeleColors.ObsidianLight, MmeleColors.ObsidianMid, MmeleColors.ObsidianDark)
    } else {
        Triple(MmeleColors.QuartzLight, MmeleColors.QuartzMid, MmeleColors.QuartzDark)
    }
    val w = r * 2.06f
    val h = r * 1.94f
    val topLeft = Offset(body.x - w / 2, body.y - h / 2)
    val size = Size(w, h)
    drawOval(
        brush = Brush.radialGradient(0f to light, 0.45f to mid, 1f to dark, center = Offset(body.x - r * 0.38f, body.y - r * 0.42f), radius = r * 1.75f),
        topLeft = topLeft, size = size, alpha = alpha,
    )
    drawOval(
        brush = Brush.radialGradient(0.72f to Color.Transparent, 1f to Color.Black.copy(alpha = if (style.dark) 0.45f else 0.22f), center = body, radius = r * 1.02f),
        topLeft = topLeft, size = size, alpha = alpha,
    )
    if (style.dark) {
        drawArc(Color(0x335E7A8C), 20f, 70f, false, Offset(body.x - r * 0.78f, body.y - r * 0.78f), Size(r * 1.56f, r * 1.56f), style = Stroke(width = r * 0.10f), alpha = alpha)
    }
    val hl = Offset(body.x - r * 0.36f, body.y - r * 0.44f)
    drawOval(
        brush = Brush.radialGradient(0f to Color.White.copy(alpha = if (style.dark) 0.55f else 0.85f), 1f to Color.Transparent, center = hl, radius = r * 0.42f),
        topLeft = Offset(hl.x - r * 0.42f, hl.y - r * 0.30f), size = Size(r * 0.84f, r * 0.60f), alpha = alpha,
    )
}

// ------------------------------------------------------------------ rough diamonds

private fun DrawScope.drawDiamondStyle(
    style: PieceStyle.Diamond, center: Offset, radius: Float, pieceId: Int, elevation: Float, alpha: Float, scale: Float,
) {
    val rnd = Random(pieceId * 2654435761L xor 0xD1A10D01L)
    val sizeJitter = 0.88f + rnd.nextFloat() * 0.18f // keep within ~12% of nominal so nodes stay clear
    val (body, r0) = bodyOf(center, radius, elevation, scale * sizeJitter)
    val r = r0
    dropShadow(center, r, elevation, alpha)
    val (core, mid, edge, sparkle) = if (style.dark) {
        listOf(Color(0xFF5A3E1E), Color(0xFFB8945A), Color(0xFF2A1A0C), Color(0xFFFFE6A0))
    } else {
        listOf(Color(0xFFE8F4FF), Color(0xFFB8D4F0), Color(0xFF6A88A8), Color(0xFFFFFFFF))
    }
    // irregular octahedron-ish silhouette: 6–8 vertices with jittered angles
    val n = 6 + rnd.nextInt(3)
    val verts = Array(n) { i ->
        val a = -PI.toFloat() / 2f + i * 2f * PI.toFloat() / n + (rnd.nextFloat() - 0.5f) * 0.35f
        val rad = r * (0.78f + rnd.nextFloat() * 0.32f) * if (i % (n / 2) == 0) 1.05f else 0.92f
        Offset(body.x + cos(a) * rad, body.y + sin(a) * rad * 1.05f)
    }
    val outline = Path().apply {
        moveTo(verts[0].x, verts[0].y)
        for (i in 1 until n) lineTo(verts[i].x, verts[i].y)
        close()
    }
    drawPath(outline, Brush.linearGradient(listOf(core, mid, edge), start = body - Offset(r, r), end = body + Offset(r, r)), alpha = alpha)
    // facets: triangles from a slightly off-centre apex
    val apex = Offset(body.x + (rnd.nextFloat() - 0.5f) * r * 0.3f, body.y - r * (0.15f + rnd.nextFloat() * 0.2f))
    for (i in verts.indices) {
        val a = verts[i]; val b = verts[(i + 1) % n]
        val face = Path().apply { moveTo(apex.x, apex.y); lineTo(a.x, a.y); lineTo(b.x, b.y); close() }
        val shade = if (i % 2 == 0) Color.White.copy(alpha = 0.18f * alpha) else Color.Black.copy(alpha = 0.22f * alpha)
        drawPath(face, shade)
        drawPath(face, Color.White.copy(alpha = 0.35f * alpha), style = Stroke(width = r * 0.025f))
    }
    // translucent rim + specular flash
    drawPath(outline, Color.White.copy(alpha = 0.35f * alpha), style = Stroke(width = r * 0.06f))
    drawCircle(Brush.radialGradient(listOf(sparkle.copy(alpha = 0.9f * alpha), Color.Transparent), apex - Offset(r * 0.1f, r * 0.1f), r * 0.35f), r * 0.28f, apex - Offset(r * 0.05f, r * 0.1f))
    // tiny rainbow glints
    if (!style.dark) {
        for (k in 0 until 3) {
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            val p = Offset(body.x + cos(a) * r * 0.45f, body.y + sin(a) * r * 0.4f)
            drawCircle(listOf(Color(0x88FF88AA), Color(0x8888FFCC), Color(0x8888AAFF))[k].copy(alpha = 0.55f * alpha), r * 0.05f, p)
        }
    }
}

// ------------------------------------------------------------------ eggs

private fun eggPath(body: Offset, rx: Float, ry: Float): Path = Path().apply {
    // Real egg profile: narrower, rounder point at the top, broad blunt base; widest below centre.
    val top = Offset(body.x, body.y - ry)
    val bottom = Offset(body.x, body.y + ry)
    val waistY = body.y + ry * 0.18f
    moveTo(top.x, top.y)
    cubicTo(body.x + rx * 0.58f, top.y, body.x + rx, body.y - ry * 0.42f, body.x + rx, waistY)
    cubicTo(body.x + rx, body.y + ry * 0.66f, body.x + rx * 0.56f, bottom.y, bottom.x, bottom.y)
    cubicTo(body.x - rx * 0.56f, bottom.y, body.x - rx, body.y + ry * 0.66f, body.x - rx, waistY)
    cubicTo(body.x - rx, body.y - ry * 0.42f, body.x - rx * 0.58f, top.y, top.x, top.y)
    close()
}

private fun DrawScope.drawEggStyle(
    kind: EggKind, center: Offset, radius: Float, pieceId: Int, elevation: Float, alpha: Float, scale: Float,
) {
    val rnd = Random(pieceId * 0x9E3779B9L xor kind.ordinal.toLong())
    // Eggs are a bit taller than wide; keep the long axis inside ~1.15 * radius.
    val sizeJitter = 0.94f + rnd.nextFloat() * 0.08f
    val (body, r) = bodyOf(center, radius, elevation, scale * sizeJitter)
    val rx = r * 0.84f
    val ry = r * 1.06f
    dropShadow(center, r, elevation, alpha)
    val path = eggPath(body, rx, ry)

    val (base, mid, shade) = when (kind) {
        EggKind.OSTRICH -> Triple(Color(0xFFF6EBD0), Color(0xFFE8D7B0), Color(0xFFC9B48A))
        EggKind.GUINEAFOWL -> Triple(Color(0xFFF0E0C0), Color(0xFFE0CFA8), Color(0xFFC4AE82))
        EggKind.CROWNED_LAPWING -> Triple(Color(0xFFC4A45A), Color(0xFF9A7A3A), Color(0xFF5A4020))
        EggKind.LILAC_ROLLER -> Triple(Color(0xFFFFFFF8), Color(0xFFF2F0E8), Color(0xFFC8C4B8))
        EggKind.KORI_BUSTARD -> Triple(Color(0xFFB8A56A), Color(0xFF7A6A3A), Color(0xFF3A2E18))
    }
    drawPath(path, Brush.radialGradient(0f to base, 0.55f to mid, 1f to shade, center = Offset(body.x - rx * 0.3f, body.y - ry * 0.35f), radius = ry * 1.6f), alpha = alpha)

    clipPath(path) {
        when (kind) {
            EggKind.OSTRICH -> {
                // South African ostrich eggs are cream/ivory with grouped sunken pores → a pitted look.
                repeat(55) {
                    val a = rnd.nextFloat() * 2f * PI.toFloat()
                    val d = rnd.nextFloat().let { it * it } * ry * 0.95f
                    val p = Offset(body.x + cos(a) * d * 0.75f, body.y + sin(a) * d)
                    drawCircle(Color(0xFF8A7048).copy(alpha = 0.35f * alpha), r * (0.02f + rnd.nextFloat() * 0.035f), p)
                    drawCircle(Color(0x22000000), r * 0.015f, p + Offset(r * 0.01f, r * 0.01f))
                }
            }
            EggKind.GUINEAFOWL -> {
                // Cream/buff with fine brown speckles; thick-shelled look via a soft rim later.
                repeat(90) {
                    val a = rnd.nextFloat() * 2f * PI.toFloat()
                    val d = rnd.nextFloat() * ry * 0.95f
                    val p = Offset(body.x + cos(a) * d * 0.7f, body.y + sin(a) * d)
                    drawCircle(Color(0xFF6A4A28).copy(alpha = (0.45f + rnd.nextFloat() * 0.4f) * alpha), r * (0.012f + rnd.nextFloat() * 0.025f), p)
                }
            }
            EggKind.CROWNED_LAPWING -> {
                // Olive / mustard base with bold black and grey blotches.
                repeat(14) {
                    val a = rnd.nextFloat() * 2f * PI.toFloat()
                    val d = rnd.nextFloat() * ry * 0.7f
                    val p = Offset(body.x + cos(a) * d * 0.65f, body.y + sin(a) * d)
                    val bw = r * (0.18f + rnd.nextFloat() * 0.28f)
                    val bh = r * (0.12f + rnd.nextFloat() * 0.22f)
                    val col = if (rnd.nextBoolean()) Color(0xFF1A1208) else Color(0xFF5A5A4A)
                    drawOval(col.copy(alpha = 0.7f * alpha), Offset(p.x - bw / 2, p.y - bh / 2), Size(bw, bh))
                }
            }
            EggKind.LILAC_ROLLER -> {
                // Plain glossy white — cavity nesters lay unmarked eggs. Just a soft sheen.
            }
            EggKind.KORI_BUSTARD -> {
                // Pale olive-brown with darker olive / brown streaks and blotches.
                repeat(18) {
                    val a = rnd.nextFloat() * 2f * PI.toFloat()
                    val d = rnd.nextFloat() * ry * 0.75f
                    val p = Offset(body.x + cos(a) * d * 0.65f, body.y + sin(a) * d)
                    val col = if (rnd.nextFloat() < 0.5f) Color(0xFF3A2E14) else Color(0xFF5A6A3A)
                    if (rnd.nextBoolean()) {
                        drawOval(col.copy(alpha = 0.65f * alpha), Offset(p.x - r * 0.15f, p.y - r * 0.08f), Size(r * (0.2f + rnd.nextFloat() * 0.3f), r * (0.1f + rnd.nextFloat() * 0.18f)))
                    } else {
                        drawLine(col.copy(alpha = 0.7f * alpha), p - Offset(r * 0.15f, 0f), p + Offset(r * 0.2f, r * 0.05f), r * 0.04f, StrokeCap.Round)
                    }
                }
            }
        }
        // Specular highlight (glossier for the roller)
        val gloss = if (kind == EggKind.LILAC_ROLLER) 0.9f else 0.55f
        val hl = Offset(body.x - rx * 0.32f, body.y - ry * 0.4f)
        drawOval(
            Brush.radialGradient(0f to Color.White.copy(alpha = gloss * alpha), 1f to Color.Transparent, center = hl, radius = r * 0.45f),
            Offset(hl.x - r * 0.35f, hl.y - r * 0.22f), Size(r * 0.7f, r * 0.44f),
        )
    }
    // Rim / thick-shell cue for guineafowl; soft edge for the rest.
    val rimAlpha = when (kind) {
        EggKind.GUINEAFOWL -> 0.55f
        EggKind.LILAC_ROLLER -> 0.25f
        else -> 0.35f
    }
    drawPath(path, Color.Black.copy(alpha = rimAlpha * alpha), style = Stroke(width = r * if (kind == EggKind.GUINEAFOWL) 0.07f else 0.04f))
}
