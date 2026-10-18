package bw.mmele.ui.ranked

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import bw.mmele.analytics.Events
import bw.mmele.analytics.Tracker
import bw.mmele.online.ranked.Bracket
import bw.mmele.online.ranked.Elo
import bw.mmele.online.ranked.PlayerStanding
import bw.mmele.online.ranked.RankedRepository
import bw.mmele.online.ranked.TournamentInfo
import bw.mmele.online.ranked.TournamentStatus
import bw.mmele.ui.home.Segmented
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** Always shown wherever points appear (virtual points only; see docs/REAL_MONEY.md). */
@Composable
fun PointsDisclaimer() = Text(
    stringResource(R.string.points_disclaimer),
    style = MaterialTheme.typography.labelMedium,
    color = MmeleColors.GoldBright,
    modifier = Modifier.fillMaxWidth().background(MmeleColors.Slate, RoundedCornerShape(12.dp)).padding(10.dp),
)

@Composable
private fun Page(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(MmeleColors.Slate).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onBack) { Text("‹ " + stringResource(R.string.nav_home), color = MmeleColors.BoneDim) }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone)
            PointsDisclaimer()
            content()
        }
    }
}

/** Short anonymous label for a player id (players have no public names). */
fun playerLabel(uid: String, myUid: String?) = if (uid == myUid) "★ You" else "Player " + uid.takeLast(4).uppercase()

@Composable
fun LeaderboardScreen(repo: RankedRepository?, onBack: () -> Unit) {
    Page(stringResource(R.string.leaderboard_title), onBack) {
        if (repo == null) { Text(stringResource(R.string.ranked_offline), color = MmeleColors.BoneDim); return@Page }
        var rows by remember { mutableStateOf<List<PlayerStanding>?>(null) }
        var me by remember { mutableStateOf<PlayerStanding?>(null) }
        var myUid by remember { mutableStateOf<String?>(null) }
        var failed by remember { mutableStateOf(false) }
        LaunchedEffect(repo) {
            runCatching {
                myUid = repo.myUid()
                me = repo.myStanding()
                rows = repo.leaderboard()
            }.onFailure { failed = true }
        }
        val mine = me ?: PlayerStanding(myUid.orEmpty())
        Text(stringResource(R.string.ranked_my_line, mine.rating, mine.points, mine.games), style = MaterialTheme.typography.titleSmall, color = MmeleColors.Bone)
        Text(stringResource(R.string.ranked_how, Elo.STAKE), style = MaterialTheme.typography.bodySmall, color = MmeleColors.BoneDim)
        when {
            failed -> Text(stringResource(R.string.lobby_err_other), color = MmeleColors.BoneDim)
            rows == null -> Text("…", color = MmeleColors.BoneDim)
            rows!!.isEmpty() -> Text(stringResource(R.string.leaderboard_empty), color = MmeleColors.BoneDim)
            else -> rows!!.forEachIndexed { i, p ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${i + 1}.", color = MmeleColors.GoldBright, modifier = Modifier.width(36.dp))
                    Text(playerLabel(p.uid, myUid), color = MmeleColors.Bone, modifier = Modifier.weight(1f))
                    Text("${p.rating}", color = MmeleColors.Bone, modifier = Modifier.width(56.dp))
                    Text(stringResource(R.string.ranked_points_short, p.points), color = MmeleColors.BoneDim, modifier = Modifier.width(80.dp))
                }
            }
        }
    }
}

@Composable
fun TournamentsScreen(repo: RankedRepository?, onBack: () -> Unit, onPlayRoom: (String) -> Unit) {
    Page(stringResource(R.string.tournaments_title), onBack) {
        if (repo == null) { Text(stringResource(R.string.ranked_offline), color = MmeleColors.BoneDim); return@Page }
        val scope = rememberCoroutineScope()
        var myUid by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(repo) { myUid = runCatching { repo.myUid() }.getOrNull() }
        val list by remember(repo) { repo.tournaments().catch { emit(emptyList()) } }.collectAsState(initial = null)
        var name by remember { mutableStateOf("") }
        var sizeIdx by remember { mutableIntStateOf(0) }
        var status by remember { mutableStateOf<String?>(null) }
        val sizes = listOf(4, 8, 16)
        val (p1, p2) = Bracket.prizes(sizes[sizeIdx])
        Text(stringResource(R.string.tournaments_create), style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
        OutlinedTextField(name, { name = it.take(40) }, label = { Text(stringResource(R.string.tournaments_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Segmented(sizes.map { stringResource(R.string.tournaments_players, it) }, sizeIdx) { sizeIdx = it }
        Text(stringResource(R.string.tournaments_entry_line, Bracket.ENTRY, p1, p2), style = MaterialTheme.typography.bodySmall, color = MmeleColors.BoneDim)
        Button(onClick = {
            scope.launch {
                status = repo.createTournament(name, sizes[sizeIdx]).fold({ null }, { it.message })
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate)) { Text(stringResource(R.string.tournaments_create_button)) }
        status?.let { Text(it, color = MmeleColors.GoldBright) }
        Spacer(Modifier.padding(4.dp))
        when (val l = list) {
            null -> Text("…", color = MmeleColors.BoneDim)
            else -> if (l.isEmpty()) Text(stringResource(R.string.tournaments_empty), color = MmeleColors.BoneDim)
            else l.forEach { t -> TournamentCard(t, myUid, onJoin = {
                scope.launch { repo.join(t.id).onSuccess { Tracker.log(Events.TOURNAMENT_JOINED, "size" to t.size) }.onFailure { status = it.message } }
            }, onLeave = { scope.launch { repo.leave(t.id) } }, onPlayRoom = onPlayRoom) }
        }
    }
}

@Composable
private fun TournamentCard(t: TournamentInfo, myUid: String?, onJoin: () -> Unit, onLeave: () -> Unit, onPlayRoom: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MmeleColors.SlateRaised, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val statusText = stringResource(when (t.status) { TournamentStatus.OPEN -> R.string.tournaments_open; TournamentStatus.RUNNING -> R.string.tournaments_running; TournamentStatus.FINISHED -> R.string.tournaments_finished })
        Text("${t.name} · ${t.entrants.size}/${t.size} · $statusText", style = MaterialTheme.typography.titleSmall, color = MmeleColors.Bone)
        val inIt = myUid != null && myUid in t.entrants
        t.rejected[myUid]?.let { Text(stringResource(R.string.tournaments_rejected, it), color = MmeleColors.GoldBright) }
        if (t.status == TournamentStatus.OPEN) {
            if (inIt) OutlinedButton(onClick = onLeave) { Text(stringResource(R.string.tournaments_leave)) }
            else Button(onClick = onJoin) { Text(stringResource(R.string.tournaments_join, t.entry)) }
        }
        t.bracket?.let { b ->
            for (r in 1..b.rounds) {
                Text(stringResource(R.string.tournaments_round, r), style = MaterialTheme.typography.labelLarge, color = MmeleColors.GoldBright)
                b.matches.filter { it.round == r }.forEach { m ->
                    val line = "${m.one?.let { playerLabel(it, myUid) } ?: "?"}  v  ${m.two?.let { playerLabel(it, myUid) } ?: "?"}" + (m.winner?.let { "  →  " + playerLabel(it, myUid) } ?: "")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(line, color = MmeleColors.Bone, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        val room = t.rooms[m.id]
                        if (room != null && m.winner == null && (m.one == myUid || m.two == myUid)) TextButton(onClick = { onPlayRoom(room) }) { Text(stringResource(R.string.tournaments_play)) }
                    }
                }
            }
            if (b.isFinished) {
                Text(stringResource(R.string.tournaments_standings), style = MaterialTheme.typography.labelLarge, color = MmeleColors.GoldBright)
                b.standings().forEach { (place, uid) -> Text("$place. ${playerLabel(uid, myUid)}", color = MmeleColors.Bone, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
