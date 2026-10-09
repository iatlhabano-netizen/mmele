package bw.mmele.ui.about

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
                licences = lib.licenses.joinToString { it.name }.ifEmpty { "See website" },
                text = lib.licenses.firstOrNull()?.licenseContent,
                website = lib.website ?: lib.licenses.firstOrNull()?.url,
            )
        }
        .sortedBy { it.name.lowercase() }
}.getOrDefault(emptyList())

/** Where the facts in the app come from (mirrors docs/SCENES.md, docs/PIECES.md, docs/RULES.md). */
val CREDITS: List<Pair<String, String>> = listOf(
    "Morabaraba rules" to "Mind Sports South Africa, Generally Accepted Rules for Morabaraba; Wikipedia, Morabaraba",
    "Khutlotharo" to "Board layout from a Motswana player; jump rule as in Tsoro Yematatu (Wikipedia; Ludii, after Matthews 1964)",
    "Okavango Delta" to "UNESCO World Heritage Centre, news 1159 (whc.unesco.org/en/news/1159)",
    "Chobe National Park" to "Botswana Tourism Organisation (botswanatourism.co.bw)",
    "Makgadikgadi Pans" to "NASA Earth Observatory; UNESCO tentative list 5559",
    "Central Kalahari" to "Wikipedia, Central Kalahari Game Reserve",
    "Tsodilo Hills" to "UNESCO World Heritage List 1021 (whc.unesco.org/en/list/1021)",
    "Bird eggs" to "Birds of Botswana descriptions: BDI / SABAP1 (thebdi.org) for kori bustard, crowned lapwing, helmeted guineafowl; " +
        "Wikipedia for ostrich; Kruger Park guide and Earth Life for lilac-breasted roller; biodiversityexplorer.info for ranges",
    "Art and sound" to "Every board, scene, piece and sound is drawn or synthesised in code for Mmele. No third-party images, fonts or samples.",
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
    var deleteStatus by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MmeleColors.Slate)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onBack) { Text("‹ Home", color = MmeleColors.BoneDim) }
                Text("About Mmele", style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone, modifier = Modifier.semantics { heading() })
                Text("Version $versionName ($versionCode)", style = MaterialTheme.typography.labelLarge, color = MmeleColors.Gold)
                Text(AppInfo.DEVELOPER, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.BoneDim)

                Section("Privacy and terms")
                LinkRow("Privacy policy", AppInfo.PRIVACY_URL) { uri.openUri(AppInfo.PRIVACY_URL) }
                LinkRow("Terms of use", AppInfo.TERMS_URL) { uri.openUri(AppInfo.TERMS_URL) }
                LinkRow("Contact", AppInfo.CONTACT_EMAIL) { runCatching { uri.openUri("mailto:${AppInfo.CONTACT_EMAIL}?subject=Mmele") } }

                Section("Your online data")
                if (onDeleteOnlineData == null) {
                    Body("Online play isn't switched on in this version, so Mmele has no online data about you. Offline games never leave your phone.")
                } else {
                    Body(
                        "Online play uses a random anonymous ID and stores your game rooms on Google Firebase. Finished rooms are deleted when " +
                            "the host leaves, and anything idle for 30 days is removed at the next monthly clean-up. You can delete everything now:",
                    )
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = !deleting) {
                        Text(if (deleting) "Deleting…" else "Delete my online data", color = MmeleColors.Bone)
                    }
                    deleteStatus?.let { Body(it) }
                }

                Section("Credits and sources")
                CREDITS.forEach { (what, src) ->
                    Text(what, style = MaterialTheme.typography.titleSmall, color = MmeleColors.Bone)
                    Body(src)
                }

                Section("Open-source licences")
                Body("Mmele is built with these open-source libraries. Tap one to read its licence.")
                if (libs.isEmpty()) Body("Licence list unavailable in this build.")
                libs.forEach { lib ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClickLabel = "read licence") { openLicence = lib }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    ) {
                        Text("${lib.name} ${lib.version}".trim(), style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Bone)
                        Text(lib.licences, style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
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
            confirmButton = { TextButton(onClick = { openLicence = null }) { Text("Close") } },
            dismissButton = lib.website?.let { w -> { TextButton(onClick = { uri.openUri(w) }) { Text("Website") } } },
            containerColor = MmeleColors.SlateHigh, titleContentColor = MmeleColors.Bone, textContentColor = MmeleColors.Bone,
        )
    }

    if (confirmDelete && onDeleteOnlineData != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your online data?") },
            text = { Text("This deletes every online room you created or joined from this phone, and your anonymous online ID. Games in progress will end. Offline play is not affected.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        deleting = true
                        scope.launch {
                            deleteStatus = runCatching { onDeleteOnlineData() }.fold(
                                { r ->
                                    "Done: ${r.roomsDeleted} room${if (r.roomsDeleted == 1) "" else "s"} deleted. " +
                                        if (r.identityDeleted) "Your anonymous ID has been deleted." else "Your anonymous ID was removed from this phone and will be erased from the server within 60 days."
                                },
                                { "Couldn't reach the server (${it.message ?: "network error"}). Check your connection and try again, or email ${AppInfo.CONTACT_EMAIL}." },
                            )
                            deleting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Danger, contentColor = MmeleColors.Slate),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
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
