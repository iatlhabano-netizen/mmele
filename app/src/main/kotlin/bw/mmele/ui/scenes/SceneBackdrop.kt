package bw.mmele.ui.scenes

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import bw.mmele.ui.board.rememberSandBrush
import bw.mmele.ui.theme.MmeleColors

enum class SceneMode {
    /** Home / game-over: the illustration at full strength. */
    FULL,

    /** Behind the board: blurred (API 31+), heavily scrimmed toward #121417, static (no animation cost). */
    DIMMED,
}

@Composable
fun SceneBackdrop(scene: Scene, mode: SceneMode, modifier: Modifier = Modifier.fillMaxSize(), animate: Boolean = true) {
    val sand = rememberSandBrush()
    val ambient = animate && mode == SceneMode.FULL
    val t by if (ambient) {
        rememberInfiniteTransition(label = "scene").animateFloat(
            0f, 1f, infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
        )
    } else remember { mutableFloatStateOf(0.25f) }

    Box(modifier) {
        val blur = mode == SceneMode.DIMMED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer() // isolate redraws of the ambient animation
                .then(if (blur) Modifier.blur(14.dp, BlurredEdgeTreatment.Rectangle) else Modifier),
        ) { drawScene(scene, t) }
        if (mode == SceneMode.DIMMED) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(MmeleColors.Slate.copy(alpha = if (blur) 0.80f else 0.84f))
                // Extra darkening in the middle where the board sits.
                drawRect(
                    Brush.radialGradient(
                        listOf(MmeleColors.Slate.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.minDimension * 0.62f,
                    ),
                )
                drawRect(sand)
            }
        }
    }
}
