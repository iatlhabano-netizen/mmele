package bw.mmele.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bw.mmele.engine.BoardGraph
import bw.mmele.engine.Player
import bw.mmele.ui.game.LastMove
import bw.mmele.ui.game.MillFlash
import bw.mmele.ui.game.ShotFx
import bw.mmele.ui.game.TokenUi
import bw.mmele.ui.theme.MmeleColors
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.pieces.drawPiece
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Spoken label for one board point, e.g. "Point A, empty, valid move". */
fun pointDescription(label: String, ownerName: String?, selected: Boolean, target: Boolean, shootable: Boolean, movable: Boolean): String =
    buildString {
        append("Point ").append(label).append(", ")
        append(if (ownerName == null) "empty" else "$ownerName cow")
        if (selected) append(", selected")
        if (target) append(", valid move")
        if (shootable) append(", can be shot")
        if (movable) append(", can move")
    }

/** Maps normalised board coordinates to pixels for a given canvas size (any aspect ratio). */
class BoardGeometry(width: Float, height: Float, private val board: BoardGraph) {
    private val minX = board.positions.minOf { it.x }
    private val maxX = board.positions.maxOf { it.x }
    private val minY = board.positions.minOf { it.y }
    private val maxY = board.positions.maxOf { it.y }
    private val minEdge = board.edges.minOf { (a, b) ->
        val p = board.positions[a]; val q = board.positions[b]
        hypot(p.x - q.x, p.y - q.y)
    }
    private val margin = minEdge * 0.62f
    val side: Float = min(width / (maxX - minX + 2 * margin), height / (maxY - minY + 2 * margin))
    private val ox = (width - (maxX - minX) * side) / 2 - minX * side
    private val oy = (height - (maxY - minY) * side) / 2 - minY * side
    val tokenRadius: Float = side * minEdge * 0.36f
    val lineWidth: Float = max(side * 0.0065f, 2f)

    fun px(node: Int): Offset = board.positions[node].let { Offset(ox + it.x * side, oy + it.y * side) }
    fun px(norm: Offset): Offset = Offset(ox + norm.x * side, oy + norm.y * side)
    fun norm(px: Offset): Offset = Offset((px.x - ox) / side, (px.y - oy) / side)

    /** Nearest node within a generous touch radius, or null. */
    fun hit(p: Offset, slack: Float = 1.7f): Int? {
        var best: Int? = null
        var bestD = tokenRadius * slack
        for (i in 0 until board.size) {
            val d = (px(i) - p).getDistance()
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }
}

private class TokenAnim(start: Offset, appearFrom: Float) {
    val pos = Animatable(start, Offset.VectorConverter)
    val appear = Animatable(appearFrom)
}

private data class DragInfo(val from: Int, val pointer: Offset)

private fun bit(mask: Int, i: Int) = mask and (1 shl i) != 0

/**
 * The vector-drawn Mmele board. Stateless with respect to game rules: everything it shows comes
 * from parameters; user input is reported through [onTap], [onDragStart] and [onDrop].
 *
 * @param targets bitmask of nodes to pulse as valid landing spots.
 * @param shootable bitmask of enemy cows that may be shot (ochre crosshair rings).
 * @param draggable bitmask of nodes whose cow may be picked up and dragged.
 * @param flyingPlayers players whose cows hover (flying phase).
 * @param animate false for static renders (screenshots / previews).
 */
@Composable
fun BoardCanvas(
    board: BoardGraph,
    tokens: List<TokenUi>,
    styleOne: PieceStyle = PieceStyle.Obsidian,
    styleTwo: PieceStyle = PieceStyle.Quartz,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    targets: Int = 0,
    subtleTargets: Boolean = false,
    shootable: Int = 0,
    draggable: Int = 0,
    flyingPlayers: Set<Player> = emptySet(),
    lastMove: LastMove? = null,
    millFlash: MillFlash? = null,
    shotFx: ShotFx? = null,
    interactive: Boolean = true,
    animate: Boolean = true,
    drawBackground: Boolean = true,
    onTap: (Int?) -> Unit = {},
    onDragStart: (Int) -> Unit = {},
    onDrop: (from: Int, to: Int?) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val liftPx = with(density) { 35.dp.toPx() }
    val sand = rememberSandBrush()
    val scope = rememberCoroutineScope()

    // ---- per-token animation state
    val anims = remember { HashMap<Int, TokenAnim>() }
    for (t in tokens) {
        key(t.id) {
            val target = board.positions[t.node].let { Offset(it.x, it.y) }
            val anim = remember { TokenAnim(target, if (animate) 0f else 1f) }
            DisposableEffect(t.id) {
                anims[t.id] = anim
                onDispose { anims.remove(t.id) }
            }
            LaunchedEffect(target) {
                if (anim.pos.value != target) {
                    anim.pos.animateTo(target, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow))
                }
            }
            LaunchedEffect(Unit) {
                if (anim.appear.value < 1f) anim.appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
            }
        }
    }

    // ---- ambient animations
    val infinite = rememberInfiniteTransition(label = "board")
    val pulse by if (animate) {
        infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    } else remember { mutableStateOf(0.6f) }
    val hover by if (animate) {
        infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hover")
    } else remember { mutableStateOf(0.5f) }

    val millProgress = remember { Animatable(1f) }
    LaunchedEffect(millFlash?.id) {
        if (millFlash != null && animate) {
            millProgress.snapTo(0f)
            millProgress.animateTo(1f, tween(1300, easing = LinearEasing))
        }
    }
    val shotProgress = remember { Animatable(1f) }
    LaunchedEffect(shotFx?.id) {
        if (shotFx != null && animate) {
            shotProgress.snapTo(0f)
            shotProgress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
    }

    var drag by remember { mutableStateOf<DragInfo?>(null) }

    val currentTargets by rememberUpdatedState(targets)
    val currentDraggable by rememberUpdatedState(draggable)
    val currentTokens by rememberUpdatedState(tokens)
    val tapCb by rememberUpdatedState(onTap)
    val dragStartCb by rememberUpdatedState(onDragStart)
    val dropCb by rememberUpdatedState(onDrop)

    val input = if (!interactive) Modifier else Modifier.pointerInput(board) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val geo = BoardGeometry(size.width.toFloat(), size.height.toFloat(), board)
            val start = geo.hit(down.position)
            val canDrag = start != null && bit(currentDraggable, start)
            var dragging = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (dragging && start != null) {
                        val lifted = change.position - Offset(0f, liftPx)
                        val to = geo.hit(lifted, 1.9f)?.takeIf { bit(currentTargets, it) }
                        val tokenId = currentTokens.firstOrNull { it.node == start }?.id
                        val anim = tokenId?.let { anims[it] }
                        drag = null
                        if (anim != null) {
                            scope.launch {
                                anim.pos.snapTo(geo.norm(lifted))
                                if (to == null) {
                                    val home = board.positions[start]
                                    anim.pos.animateTo(Offset(home.x, home.y), spring(0.7f, Spring.StiffnessMedium))
                                }
                            }
                        }
                        dropCb(start, to)
                    } else {
                        tapCb(start)
                    }
                    change.consume()
                    break
                }
                if (!dragging && canDrag && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    dragging = true
                    dragStartCb(start!!)
                }
                if (dragging) {
                    drag = DragInfo(start!!, change.position)
                    change.consume()
                }
            }
            if (drag != null) drag = null
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier.onSizeChanged { canvasSize = it }) {
    Canvas(
        Modifier
            .matchParentSize()
            .then(input)
            .semantics { contentDescription = "${board.name} board"; traversalIndex = -1f },
    ) {
        val geo = BoardGeometry(size.width, size.height, board)
        val r = geo.tokenRadius
        if (drawBackground) drawSandBackground(sand)
        drawBoardLines(board, geo)

        // last move markers
        lastMove?.let { lm ->
            lm.from?.let { drawCircle(MmeleColors.Bone.copy(alpha = 0.22f), r * 0.55f, geo.px(it), style = Stroke(geo.lineWidth)) }
            drawCircle(MmeleColors.Gold.copy(alpha = 0.18f), r * 1.35f, geo.px(lm.to))
        }

        // valid landing nodes
        val d = drag
        val dragLifted = d?.let { it.pointer - Offset(0f, liftPx) }
        val hoverNode = dragLifted?.let { geo.hit(it, 1.9f) }?.takeIf { bit(targets, it) }
        for (i in 0 until board.size) {
            if (!bit(targets, i)) continue
            val c = geo.px(i)
            if (subtleTargets) {
                // Placement: every empty node is legal, so keep the hint quiet.
                drawCircle(MmeleColors.Gold.copy(alpha = 0.10f + 0.22f * pulse), r * (0.40f + 0.08f * pulse), c, style = Stroke(geo.lineWidth))
            } else {
                val a = 0.22f + 0.5f * pulse
                drawCircle(MmeleColors.Gold.copy(alpha = a * 0.45f), r * (0.55f + 0.18f * pulse), c)
                drawCircle(MmeleColors.GoldBright.copy(alpha = a), r * (0.55f + 0.18f * pulse), c, style = Stroke(geo.lineWidth * 1.1f))
            }
            if (i == hoverNode) {
                drawCircle(MmeleColors.GoldBright.copy(alpha = 0.9f), r * 1.15f, c, style = Stroke(geo.lineWidth * 1.6f))
            }
        }

        // tokens
        for (t in tokens) {
            if (d != null && t.node == d.from) continue
            val anim = anims[t.id]
            val norm = anim?.pos?.value ?: board.positions[t.node].let { Offset(it.x, it.y) }
            val c = geo.px(norm)
            val appear = anim?.appear?.value ?: 1f
            val flying = t.player in flyingPlayers
            var elev = if (flying) 0.45f + 0.35f * hover else 0f
            if (selected == t.node) elev = max(elev, 0.75f)
            if (selected == t.node) {
                drawCircle(MmeleColors.GoldBright.copy(alpha = 0.85f), r * 1.28f, c, style = Stroke(geo.lineWidth * 1.4f))
            }
            if (bit(shootable, t.node)) drawShootRing(c, r, pulse, geo.lineWidth)
            drawPiece(if (t.player == Player.ONE) styleOne else styleTwo, c, r, pieceId = t.id, elevation = elev, scale = 0.6f + 0.4f * appear, alpha = appear.coerceIn(0f, 1f))
        }

        // shot cow fading out
        if (shotFx != null && shotProgress.value < 1f) {
            val p = shotProgress.value
            val c = geo.px(shotFx.node)
            drawCircle(MmeleColors.Ochre.copy(alpha = 0.6f * (1 - p)), r * (1f + 1.2f * p), c, style = Stroke(geo.lineWidth * 2f))
            drawPiece(if (shotFx.player == Player.ONE) styleOne else styleTwo, c, r, pieceId = shotFx.id, alpha = 1f - p, scale = 1f + 0.25f * p)
        }

        // gold mill trace
        if (millFlash != null && millProgress.value < 1f) drawMillTrace(millFlash, geo, millProgress.value)

        // dragged token on top, 35dp above the finger
        if (d != null && dragLifted != null) {
            val player = tokens.firstOrNull { it.node == d.from }?.player
            if (player != null) drawPiece(if (player == Player.ONE) styleOne else styleTwo, dragLifted, r, pieceId = tokens.firstOrNull { it.node == d.from }?.id ?: 0, elevation = 1f)
        }
    }
    // TalkBack: one focusable, labelled node per point, activated like a tap. Purely semantic,
    // so touches still go to the canvas above.
    if (canvasSize != IntSize.Zero) {
        val geo = BoardGeometry(canvasSize.width.toFloat(), canvasSize.height.toFloat(), board)
        val r = geo.tokenRadius * 1.25f
        val sizeDp = with(density) { (r * 2).toDp() }
        for (i in 0 until board.size) {
            val c = geo.px(i)
            val owner = tokens.firstOrNull { it.node == i }?.player
            val label = pointDescription(
                board.labels[i], owner?.let { if (it == Player.ONE) styleOne.shortName else styleTwo.shortName },
                selected = selected == i, target = bit(targets, i), shootable = bit(shootable, i),
                movable = bit(draggable, i) && selected != i,
            )
            Box(
                Modifier
                    .offset { IntOffset((c.x - r).roundToInt(), (c.y - r).roundToInt()) }
                    .size(sizeDp)
                    .semantics {
                        contentDescription = label
                        role = Role.Button
                        this.selected = selected == i
                        if (interactive) onClick(label = "Choose") { tapCb(i); true } else disabled()
                    },
            )
        }
    }
    }

}

private fun DrawScope.drawBoardLines(board: BoardGraph, geo: BoardGeometry) {
    val w = geo.lineWidth
    val shadow = Color.Black.copy(alpha = 0.55f)
    for ((a, b) in board.edges) {
        drawLine(shadow, geo.px(a) + Offset(0f, w * 0.9f), geo.px(b) + Offset(0f, w * 0.9f), w * 1.3f, StrokeCap.Round)
    }
    for ((a, b) in board.edges) {
        drawLine(MmeleColors.Bone.copy(alpha = 0.86f), geo.px(a), geo.px(b), w, StrokeCap.Round)
    }
    val r = geo.tokenRadius
    for (i in 0 until board.size) {
        val c = geo.px(i)
        drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.46f, c)
        drawCircle(MmeleColors.Bone.copy(alpha = 0.16f), r * 0.46f, c, style = Stroke(w * 0.8f))
        drawCircle(MmeleColors.Bone, r * 0.17f, c)
    }
}

private fun DrawScope.drawShootRing(c: Offset, r: Float, pulse: Float, w: Float) {
    val color = MmeleColors.Ochre.copy(alpha = 0.55f + 0.45f * pulse)
    val rr = r * (1.3f + 0.08f * pulse)
    drawCircle(color, rr, c, style = Stroke(w * 1.5f))
    val tick = r * 0.35f
    drawLine(color, Offset(c.x, c.y - rr - tick * 0.2f), Offset(c.x, c.y - rr + tick), w * 1.4f, StrokeCap.Round)
    drawLine(color, Offset(c.x, c.y + rr + tick * 0.2f), Offset(c.x, c.y + rr - tick), w * 1.4f, StrokeCap.Round)
    drawLine(color, Offset(c.x - rr - tick * 0.2f, c.y), Offset(c.x - rr + tick, c.y), w * 1.4f, StrokeCap.Round)
    drawLine(color, Offset(c.x + rr + tick * 0.2f, c.y), Offset(c.x + rr - tick, c.y), w * 1.4f, StrokeCap.Round)
}

/** Energetic gold trace: a bright head races along the line, leaving a glow that fades out. */
private fun DrawScope.drawMillTrace(flash: MillFlash, geo: BoardGeometry, p: Float) {
    val trace = (p / 0.4f).coerceIn(0f, 1f)
    val fade = if (p < 0.55f) 1f else (1f - (p - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val w = geo.lineWidth
    for (line in flash.lines) {
        val a = geo.px(line.first())
        val b = geo.px(line.last())
        val head = lerp(a, b, FastOutSlowInEasing.transform(trace))
        drawLine(MmeleColors.Gold.copy(alpha = 0.18f * fade), a, head, w * 9f, StrokeCap.Round)
        drawLine(MmeleColors.Gold.copy(alpha = 0.35f * fade), a, head, w * 4.5f, StrokeCap.Round)
        drawLine(MmeleColors.GoldBright.copy(alpha = 0.95f * fade), a, head, w * 1.8f, StrokeCap.Round)
        if (trace < 1f) drawCircle(Color.White.copy(alpha = 0.9f), w * 2.6f, head)
        for (n in line) {
            val c = geo.px(n)
            val reached = (c - a).getDistance() <= (head - a).getDistance() + 1f
            if (reached) drawCircle(MmeleColors.GoldBright.copy(alpha = 0.55f * fade), geo.tokenRadius * 1.25f, c, style = Stroke(w * 1.6f))
        }
    }
}
