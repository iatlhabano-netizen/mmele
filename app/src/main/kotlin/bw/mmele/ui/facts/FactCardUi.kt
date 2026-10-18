package bw.mmele.ui.facts

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bw.mmele.data.BotswanaPhoto
import bw.mmele.data.FactCard
import bw.mmele.ui.scenes.Scene
import bw.mmele.ui.scenes.SceneBackdrop
import bw.mmele.ui.scenes.SceneMode
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes a bundled photo off the main thread; null while loading or if it fails. */
@Composable
fun rememberPhoto(photo: BotswanaPhoto?): ImageBitmap? {
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, photo?.id) {
        value = photo?.let { p ->
            withContext(Dispatchers.IO) {
                runCatching { context.assets.open(p.assetPath).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
            }
        }
    }
    return bmp
}

/**
 * Top of the game-over card: a captioned Botswana photo (or the drawn scene when no photo is
 * bundled), then "Did you know?" with this game's fact. Photo credits are on the About screen.
 */
@Composable
fun FactCardHeader(card: FactCard, fallback: Scene) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            val img = rememberPhoto(card.photo)
            if (img != null) {
                Image(img, contentDescription = card.photo?.caption, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                SceneBackdrop(fallback, SceneMode.FULL, Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, MmeleColors.SlateRaised.copy(alpha = 0.95f)))))
            card.photo?.let { p ->
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(p.caption, style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone)
                    Text("© ${p.author.take(60)} · ${p.license}", style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim, maxLines = 1)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
            Text(stringResource(R.string.fact_did_you_know).uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.GoldBright)
            Text(card.fact.text, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Bone)
        }
    }
}
