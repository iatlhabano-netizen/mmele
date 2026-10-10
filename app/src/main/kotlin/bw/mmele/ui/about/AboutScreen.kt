package bw.mmele.ui.about

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tshipiworks.mmele.R

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bw.mmele.AppInfo
import bw.mmele.online.DeleteResult
import bw.mmele.ui.theme.MmeleColors
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.launch

/** One open-source dependency as shown on the About screen. */
data class LicenceEntry(val name: String, val version: String, val licences: String, val text: String?, val website: String?)

/** Reads the licence list generated at build time by the AboutLibraries Gradle plugin. */
fun loadLicences(context: android.content.Context): List<LicenceEntry> = runCatching {
    Libs.Builder().withContext(context).build().libraries
        .map { lib ->
            LicenceEntry(
                name = lib.name,
                version = lib.artifactVersion.orEmpty(),
                licences = lib.licenses.joinToString { it.name },
                text = lib.licenses.firstOrNull()?.licenseContent,
                website = lib.website ?: lib.licenses.firstOrNull()?.url,
            )
        }
        .sortedBy { it.name.lowercase() }
}.getOrDefault(emptyList())

/**
 * Where the facts in the app come from (mirrors docs/SCENES.md, docs/PIECES.md, docs/RULES.md).
 * Headings are translated; the citations stay as published.
 */
val CREDITS: List<Pair<Int, String>> = listOf(
    R.string.credit_rules to "Mind Sports South Africa, Generally Accepted Rules for Morabaraba; Wikipedia, Morabaraba",
    R.string.variant_triangle to "Board layout from a Motswana player; jump rule as in Tsoro Yematatu (Wikipedia; Ludii, after Matthews 1964)",
    R.string.scene_okavango to "UNESCO World Heritage Centre, news 1159 (whc.unesco.org/en/news/1159)",
    R.string.scene_chobe to "Botswana Tourism Organisation (botswanatourism.co.bw)",
    R.string.scene_makgadikgadi to "NASA Earth Observatory; UNESCO tentative list 5559",
    R.string.scene_kalahari to "Wikipedia, Central Kalahari Game Reserve",
    R.string.scene_tsodilo to "UNESCO World Heritage List 1021 (whc.unesco.org/en/list/1021)",
    R.string.credit_eggs to "Birds of Botswana descriptions: BDI / SABAP1 (thebdi.org) for kori bustard, crowned lapwing, helmeted guineafowl; " +
        "Wikipedia for ostrich; Kruger Park guide and Earth Life for lilac-breasted roller; biodiversityexplorer.info for ranges",
)

@Composable
fun AboutScreen(
    versionName: String,
    versionCode: Int,
    onBack: () -> Unit,
    /** Null when online play isn't configured in this build. */
    onDeleteOnlineData: (suspend () -> DeleteResult)?,
    licences: List<LicenceEntry>? = null,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val libs = licences ?: remember { loadLicences(context) }
    var openLicence by remember { mutableStateOf<LicenceEntry?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteStatus by remember { mutableStateOf<Result<DeleteResult>?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MmeleColors.Slate)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onBack) { Text("‹ " + stringResource(R.string.nav_home), color = MmeleColors.BoneDim) }
                Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.about_version, versionName, versionCode), style = MaterialTheme.typography.labelLarge, color = MmeleColors.Gold)
                Text(AppInfo.DEVELOPER, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.BoneDim)

                Section(stringResource(R.string.about_privacy_terms))
                LinkRow(stringResource(R.string.about_privacy), AppInfo.PRIVACY_URL) { uri.openUri(AppInfo.PRIVACY_URL) }
                LinkRow(stringResource(R.string.about_terms), AppInfo.TERMS_URL) { uri.openUri(AppInfo.TERMS_URL) }
                LinkRow(stringResource(R.string.about_contact), AppInfo.CONTACT_EMAIL) { runCatching { uri.openUri("mailto:${AppInfo.CONTACT_EMAIL}?subject=Mmele") } }

                Section(stringResource(R.string.about_online_data))
                if (onDeleteOnlineData == null) {
                    Body(stringResource(R.string.about_online_off))
                } else {
                    Body(stringResource(R.string.about_online_on))
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = !deleting) {
                        Text(stringResource(if (deleting) R.string.about_deleting else R.string.about_delete), color = MmeleColors.Bone)
                    }
                    deleteStatus?.let { res ->
                        val text = res.fold(
                            { r ->
                                pluralStringResource(R.plurals.about_deleted_rooms, r.roomsDeleted, r.roomsDeleted) + " " +
                                    stringResource(if (r.identityDeleted) R.string.about_id_deleted else R.string.about_id_pending)
                            },
                            { stringResource(R.string.about_delete_failed, it.message ?: stringResource(R.string.about_network_error), AppInfo.CONTACT_EMAIL) },
                        )
                        Body(text)
                    }
                }

                Section(stringResource(R.string.about_credits))
                CREDITS.forEach { (what, src) ->
                    Text(stringResource(what), style = MaterialTheme.typography.titleSmall, color = MmeleColors.Bone)
                    Body(src)
                }
                Text(stringResource(R.string.credit_art), style = MaterialTheme.typography.titleSmall, color = MmeleColors.Bone)
                Body(stringResource(R.string.credit_art_body))
                Text(stringResource(R.string.credit_translation), style = MaterialTheme.typography.titleSmall, color = MmeleColors.Bone)
                Body(stringResource(R.string.credit_translation_body))

                Section(stringResource(R.string.about_licences))
                Body(stringResource(R.string.about_licences_intro))
                if (libs.isEmpty()) Body(stringResource(R.string.about_licences_missing))
                libs.forEach { lib ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.about_read_licence)) { openLicence = lib }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    ) {
                        Text("${lib.name} ${lib.version}".trim(), style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Bone)
                        Text(lib.licences.ifEmpty { stringResource(R.string.about_see_website) }, style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    openLicence?.let { lib ->
        AlertDialog(
            onDismissRequest = { openLicence = null },
            title = { Text(lib.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(lib.text ?: "${lib.licences}\n\n${lib.website.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { openLicence = null }) { Text(stringResource(R.string.common_close)) } },
            dismissButton = lib.website?.let { w -> { TextButton(onClick = { uri.openUri(w) }) { Text(stringResource(R.string.about_website)) } } },
            containerColor = MmeleColors.SlateHigh, titleContentColor = MmeleColors.Bone, textContentColor = MmeleColors.Bone,
        )
    }

    if (confirmDelete && onDeleteOnlineData != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.about_delete_title)) },
            text = { Text(stringResource(R.string.about_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        deleting = true
                        scope.launch {
                            deleteStatus = runCatching { onDeleteOnlineData() }
                            deleting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Danger, contentColor = MmeleColors.Slate),
                ) { Text(stringResource(R.string.about_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
            containerColor = MmeleColors.SlateHigh, titleContentColor = MmeleColors.Bone, textContentColor = MmeleColors.Bone,
        )
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(14.dp))
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.Gold, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Body(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.BoneDim)

@Composable
private fun LinkRow(label: String, detail: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MmeleColors.GoldBright)
        Text(detail, style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
    }
}
