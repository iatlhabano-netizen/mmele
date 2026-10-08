package bw.mmele.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import bw.mmele.engine.Player
import bw.mmele.ui.theme.MmeleColors

/**
 * Draws an organic pebble token: soft drop shadow, radial-gradient body lit from the upper left,
 * darkened rim and a specular highlight. [elevation] 0..1 lifts the pebble and widens/softens
 * its shadow (used for selection, flying hover and dragging).
 */
fun DrawScope.drawPebble(
    center: Offset,
    radius: Float,
    player: Player,
    elevation: Float = 0f,
    alpha: Float = 1f,
    scale: Float = 1f,
) {
    if (alpha <= 0f || scale <= 0f) return
    val r = radius * scale * (1f + 0.07f * elevation)
    val lift = radius * 0.32f * elevation
    val body = Offset(center.x, center.y - lift)

    // Shadow: grows, drifts and softens with elevation.
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

    val (light, mid, dark) = if (player == Player.ONE) {
        Triple(MmeleColors.ObsidianLight, MmeleColors.ObsidianMid, MmeleColors.ObsidianDark)
    } else {
        Triple(MmeleColors.QuartzLight, MmeleColors.QuartzMid, MmeleColors.QuartzDark)
    }
    // Pebbles are very slightly oval.
    val w = r * 2.06f
    val h = r * 1.94f
    val topLeft = Offset(body.x - w / 2, body.y - h / 2)
    val size = Size(w, h)
    drawOval(
        brush = Brush.radialGradient(
            0f to light,
            0.45f to mid,
            1f to dark,
            center = Offset(body.x - r * 0.38f, body.y - r * 0.42f),
            radius = r * 1.75f,
        ),
        topLeft = topLeft,
        size = size,
        alpha = alpha,
    )
    // Rim occlusion
    drawOval(
        brush = Brush.radialGradient(
            0.72f to Color.Transparent,
            1f to Color.Black.copy(alpha = if (player == Player.ONE) 0.45f else 0.22f),
            center = body,
            radius = r * 1.02f,
        ),
        topLeft = topLeft,
        size = size,
        alpha = alpha,
    )
    if (player == Player.ONE) {
        // Obsidian: a cool glassy sheen arc on the lower right.
        drawArc(
            color = Color(0x335E7A8C),
            startAngle = 20f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(body.x - r * 0.78f, body.y - r * 0.78f),
            size = Size(r * 1.56f, r * 1.56f),
            style = Stroke(width = r * 0.10f),
            alpha = alpha,
        )
    }
    // Specular highlight
    val hl = Offset(body.x - r * 0.36f, body.y - r * 0.44f)
    drawOval(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = if (player == Player.ONE) 0.55f else 0.85f),
            1f to Color.Transparent,
            center = hl,
            radius = r * 0.42f,
        ),
        topLeft = Offset(hl.x - r * 0.42f, hl.y - r * 0.30f),
        size = Size(r * 0.84f, r * 0.60f),
        alpha = alpha,
    )
}
