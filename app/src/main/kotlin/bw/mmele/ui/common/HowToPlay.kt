package bw.mmele.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R

/** The five quick-rules pages (title, body). */
val HOW_TO_PAGES: List<Pair<Int, Int>> = listOf(
    R.string.howto_place_title to R.string.howto_place_body,
    R.string.howto_mill_title to R.string.howto_mill_body,
    R.string.howto_move_title to R.string.howto_move_body,
    R.string.howto_fly_title to R.string.howto_fly_body,
    R.string.howto_win_title to R.string.howto_win_body,
)

/** First-launch offer: "New to Morabaraba?" with Show me / Not now. Shown once. */
@Composable
fun HowToPlayOffer(onShow: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.howto_title)) },
        text = { Text(stringResource(R.string.howto_offer)) },
        confirmButton = { TextButton(onClick = onShow) { Text(stringResource(R.string.howto_show)) } },
        dismissButton = { TextButton(onClick = onSkip) { Text(stringResource(R.string.howto_skip)) } },
        containerColor = MmeleColors.SlateHigh, titleContentColor = MmeleColors.Bone, textContentColor = MmeleColors.Bone,
    )
}

/** Quick rules overlay, one short page at a time. */
@Composable
fun HowToPlayDialog(onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val (title, body) = HOW_TO_PAGES[page]
    val last = page == HOW_TO_PAGES.lastIndex
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.howto_page, page + 1, HOW_TO_PAGES.size), style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (last) onDone() else page++ }) {
                Text(stringResource(if (last) R.string.howto_done else R.string.howto_next))
            }
        },
        dismissButton = if (page > 0) { { TextButton(onClick = { page-- }) { Text(stringResource(R.string.common_back)) } } } else null,
        containerColor = MmeleColors.SlateHigh, titleContentColor = MmeleColors.GoldBright, textContentColor = MmeleColors.Bone,
    )
}


