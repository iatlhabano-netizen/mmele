package bw.mmele.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import bw.mmele.online.Seat
import bw.mmele.online.chat.CHAT_EMOJI
import bw.mmele.online.chat.ChatKind
import bw.mmele.online.chat.ChatMessage
import bw.mmele.online.chat.ChatPolicy
import bw.mmele.online.chat.QuickPhrase
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R

/** Shows a phrase id in the reader's language (Setswana when the app or phone is in tn). */
@Composable
fun phraseText(id: String): String {
    val tn = LocalConfiguration.current.locales[0]?.language == "tn"
    val p = QuickPhrase.byId(id) ?: return id
    return if (tn) p.tn else p.en
}

@Composable
private fun bodyOf(m: ChatMessage) = when (m.kind) { ChatKind.PHRASE -> phraseText(m.body); else -> m.body }

/** Chat button for the online game screen (shows unread count). */
@Composable
fun ChatButton(state: ChatUiState, onIntent: (ChatIntent) -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = { onIntent(ChatIntent.Open(true)) }, modifier = modifier.background(MmeleColors.SlateRaised.copy(alpha = 0.9f), RoundedCornerShape(18.dp))) {
        Text("💬 " + stringResource(R.string.chat_title) + if (state.unread > 0) " (${state.unread})" else "", color = MmeleColors.Bone)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatDialog(state: ChatUiState, mySeat: Seat, onIntent: (ChatIntent) -> Unit) {
    Dialog(onDismissRequest = { onIntent(ChatIntent.Open(false)) }) {
        Column(Modifier.background(MmeleColors.SlateRaised, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.chat_title), style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.chat_mute), style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
                Switch(checked = state.blocks.muted, onCheckedChange = { onIntent(ChatIntent.Mute(it)) })
            }
            val list = rememberLazyListState()
            LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.lastIndex) }
            LazyColumn(Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 240.dp), state = list, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.messages, key = { it.id }) { m -> MessageRow(m, m.seat == mySeat, onIntent) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuickPhrase.entries.forEach { p -> AssistChip(onClick = { onIntent(ChatIntent.Send(ChatKind.PHRASE, p.id)) }, label = { Text(phraseText(p.id)) }) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CHAT_EMOJI.forEach { e -> TextButton(onClick = { onIntent(ChatIntent.Send(ChatKind.EMOJI, e)) }) { Text(e, style = MaterialTheme.typography.titleLarge) } }
            }
            OutlinedTextField(
                value = state.draft,
                onValueChange = { onIntent(ChatIntent.Draft(it)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.chat_type_hint)) },
                supportingText = { Text(stringResource(R.string.chat_remaining, state.remaining), color = if (state.remaining < 0) MmeleColors.GoldBright else MmeleColors.BoneDim) },
                isError = state.draft.length > ChatPolicy.MAX_TEXT,
                trailingIcon = { TextButton(onClick = { onIntent(ChatIntent.Send(ChatKind.TEXT, state.draft)) }) { Text(stringResource(R.string.chat_send)) } },
            )
            Text(stringResource(R.string.chat_rules_note), style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim)
        }
    }
}

@Composable
private fun MessageRow(m: ChatMessage, mine: Boolean, onIntent: (ChatIntent) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            bodyOf(m),
            color = MmeleColors.Bone,
            modifier = Modifier
                .background(if (mine) MmeleColors.Gold.copy(alpha = 0.25f) else MmeleColors.Slate, RoundedCornerShape(12.dp))
                .clickable(enabled = !mine) { menu = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.chat_report)) }, onClick = { menu = false; onIntent(ChatIntent.Report(m)) })
            DropdownMenuItem(text = { Text(stringResource(R.string.chat_block)) }, onClick = { menu = false; onIntent(ChatIntent.Block(m)) })
        }
    }
}
