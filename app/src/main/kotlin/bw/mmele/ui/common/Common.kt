package bw.mmele.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R

/** Plain scrolling page with a "‹ Home" back link and a heading, used by About, Academy and Stats. */
@Composable
fun PageScaffold(
    title: String,
    onBack: () -> Unit,
    backLabel: String = stringResource(R.string.nav_home),
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(MmeleColors.Slate)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onBack) { Text("‹ $backLabel", color = MmeleColors.BoneDim) }
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone, modifier = Modifier.semantics { heading() })
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.BoneDim) }
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionHeading(title: String) {
    Spacer(Modifier.height(14.dp))
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.Gold, modifier = Modifier.semantics { heading() })
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.BoneDim, modifier = modifier.padding(vertical = 1.dp))
