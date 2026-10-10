package bw.mmele.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bw.mmele.data.StatKey
import bw.mmele.data.Tally
import bw.mmele.engine.Variant
import bw.mmele.ui.common.BodyText
import bw.mmele.ui.common.PageScaffold
import bw.mmele.ui.common.SectionHeading
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.labels.label
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R

/** Win / loss / draw counts per mode, difficulty and board. Kept on the phone only. */
@Composable
fun StatsScreen(stats: Map<StatKey, Tally>, onBack: () -> Unit, onReset: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    PageScaffold(title = stringResource(R.string.stats_title), onBack = onBack, subtitle = stringResource(R.string.stats_subtitle)) {
        val total = stats.values.sumOf { it.games }
        if (total == 0) BodyText(stringResource(R.string.stats_empty))
        for (variant in Variant.entries) {
            SectionHeading(variant.label())
            HeaderRow()
            StatKey.ALL.filter { it.variant == variant }.forEach { key -> StatRow(key, stats[key] ?: Tally()) }
        }
        BodyText(stringResource(R.string.stats_pass_and_play_note), Modifier.padding(top = 10.dp))
        if (total > 0) {
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.stats_reset), color = MmeleColors.Bone)
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.stats_reset_title)) },
            text = { Text(stringResource(R.string.stats_reset_body)) },
            confirmButton = { TextButton(onClick = { confirm = false; onReset() }) { Text(stringResource(R.string.stats_reset_confirm)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.common_cancel)) } },
            containerColor = MmeleColors.SlateHigh, titleContentColor = MmeleColors.Bone, textContentColor = MmeleColors.Bone,
        )
    }
}

@Composable
private fun HeaderRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("", Modifier.weight(1f))
        for (h in listOf(R.string.stats_won, R.string.stats_lost, R.string.stats_drawn)) {
            Text(stringResource(h), Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun StatRow(key: StatKey, t: Tally) {
    val name = when (key.mode) {
        GameMode.SOLO -> stringResource(R.string.stats_row_computer, key.difficulty!!.label())
        GameMode.PASS_AND_PLAY -> stringResource(R.string.mode_pass_and_play)
        GameMode.ONLINE -> stringResource(R.string.mode_online)
    }
    val streak = if (t.bestStreak > 0) stringResource(R.string.stats_streak, t.streak, t.bestStreak) else null
    val spoken = stringResource(R.string.stats_row_spoken, name, t.wins, t.losses, t.draws) + (streak?.let { ". $it" } ?: "")
    Column(
        Modifier
            .fillMaxWidth()
            .background(MmeleColors.SlateRaised, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (t.games > 0) MmeleColors.Bone else MmeleColors.BoneDim)
        for ((v, c) in listOf(t.wins to MmeleColors.GoldBright, t.losses to MmeleColors.Bone, t.draws to MmeleColors.Bone)) {
            Text(v.toString(), Modifier.width(56.dp), style = MaterialTheme.typography.titleMedium, color = if (v > 0) c else MmeleColors.BoneDim, textAlign = TextAlign.End)
        }
    }
    streak?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MmeleColors.Gold, modifier = Modifier.padding(top = 2.dp)) }
    }
}
