package bw.mmele.ui.board

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import bw.mmele.ui.theme.MmeleColors
import kotlin.random.Random

/** A small tileable "sand grain" noise bitmap, generated once (deterministic). */
object SandTexture {
    val bitmap: ImageBitmap by lazy { generate(160, 7) }

    private fun generate(size: Int, seed: Int): ImageBitmap {
        val rnd = Random(seed)
        val px = IntArray(size * size)
        for (i in px.indices) {
            val r = rnd.nextFloat()
            px[i] = when {
                r < 0.012f -> argb(34, 0, 0, 0)                       // dark speck
                r < 0.030f -> argb(26, 0xF0, 0xE2, 0xC6)              // bright grain
                else -> argb((rnd.nextFloat() * 10).toInt(), 0xE2, 0xD9, 0xC8) // fine warm noise
            }
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, size, 0, 0, size, size)
        return bmp.asImageBitmap()
    }

    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
}

/** Deep slate with sand grain and a faint warm centre glow. */
fun DrawScope.drawSandBackground(brush: ShaderBrush) {
    drawRect(MmeleColors.Slate)
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0x14E2C79A), Color.Transparent),
            center = Offset(size.width / 2, size.height * 0.45f),
            radius = maxOf(size.width, size.height) * 0.7f,
        ),
    )
    drawRect(brush)
}

@Composable
fun rememberSandBrush(): ShaderBrush = remember {
    ShaderBrush(ImageShader(SandTexture.bitmap, TileMode.Repeated, TileMode.Repeated))
}

@Composable
fun SandBackground(modifier: Modifier = Modifier.fillMaxSize()) {
    val brush = rememberSandBrush()
    Canvas(modifier) { drawSandBackground(brush) }
}
