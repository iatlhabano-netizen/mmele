package bw.mmele.ui.online

import androidx.compose.ui.res.stringResource
import bw.mmele.ui.labels.shortLabel
import com.tshipiworks.mmele.R

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bw.mmele.engine.Variant
import bw.mmele.online.OnlineGameRepository
import bw.mmele.online.OnlineSession
import bw.mmele.online.ROOM_CODE_ALPHABET
import bw.mmele.online.ROOM_CODE_LENGTH
import bw.mmele.online.RoomConfig
import bw.mmele.online.RoomFullException
import bw.mmele.online.RoomNotFoundException
import bw.mmele.online.Seat
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.PieceIcon
import bw.mmele.ui.scenes.SceneBackdrop
import bw.mmele.ui.scenes.SceneMode
import bw.mmele.ui.theme.MmeleColors
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Keep only characters a room code can contain (typed or pasted, any case). */
fun sanitizeRoomCode(raw: String): String =
    raw.uppercase().filter { it in ROOM_CODE_ALPHABET }.take(ROOM_CODE_LENGTH)

/**
 * Create or join an online room. [repo] is null when this build has no Firebase project —
 * the screen then explains that, and offline modes are unaffected.
 */
@Composable
fun OnlineLobbyScreen(
    config: GameConfig,
    repo: OnlineGameRepository?,
    onSession: (OnlineSession) -> Unit,
    onBack: () -> Unit,
    animate: Boolean = true,
    initialCode: String = "",
) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize()) {
        SceneBackdrop(config.scene, SceneMode.DIMMED, animate = animate)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 520.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("‹ " + stringResource(R.string.nav_home), color = MmeleColors.BoneDim) }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.mode_online), style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone)
                Text(stringResource(R.string.lobby_tagline), style = MaterialTheme.typography.labelMedium, color = MmeleColors.Gold)
                Spacer(Modifier.height(20.dp))
                if (repo == null) NotConfiguredCard(onBack) else LobbyBody(config, repo, onSession, initialCode)
            }
        }
    }
}

@Composable
private fun NotConfiguredCard(onBack: () -> Unit) {
    Panel {
        Text(stringResource(R.string.lobby_off_title), style = MaterialTheme.typography.titleLarge, color = MmeleColors.Bone)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.lobby_off_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MmeleColors.BoneDim,
        )
        if (com.tshipiworks.mmele.BuildConfig.DEBUG) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.lobby_off_dev),
                style = MaterialTheme.typography.labelMedium,
                color = MmeleColors.GoldBright,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate),
        ) { Text(stringResource(R.string.lobby_off_back)) }
    }
}

@Composable
private fun LobbyBody(config: GameConfig, repo: OnlineGameRepository, onSession: (OnlineSession) -> Unit, initialCode: String) {
    var ranked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<LobbyError?>(null) }
    var code by remember { mutableStateOf(sanitizeRoomCode(initialCode)) }

    fun run(label: String, block: suspend () -> OnlineSession) {
        if (busy != null) return
        busy = label; error = null
        scope.launch {
            try {
                val s = withTimeout(15_000) { block() }
                onSession(s)
            } catch (e: TimeoutCancellationException) {
                error = LobbyError.Network
            } catch (e: RoomNotFoundException) {
                error = LobbyError.NoRoom(code)
            } catch (e: RoomFullException) {
                error = LobbyError.Full
            } catch (e: bw.mmele.online.RoomBlockedException) {
                error = LobbyError.Other(null)
            } catch (e: Exception) {
                error = LobbyError.Other(e.message)
            } finally {
                busy = null
            }
        }
    }

    Panel {
        Text(stringResource(R.string.lobby_host), style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PieceIcon(config.styleOne, Modifier.size(30.dp))
            PieceIcon(config.styleTwo, Modifier.size(30.dp), pieceId = 3)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(if (config.variant == Variant.CLASSIC) R.string.lobby_host_classic else R.string.lobby_host_triangle, config.styleOne.shortLabel()),
                style = MaterialTheme.typography.labelMedium,
                color = MmeleColors.BoneDim,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ranked_toggle), style = MaterialTheme.typography.labelLarge, color = MmeleColors.Bone)
                Text(stringResource(R.string.points_disclaimer_short), style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim)
            }
            androidx.compose.material3.Switch(checked = ranked, onCheckedChange = { ranked = it })
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                run("create") {
                    val room = repo.createRoom(RoomConfig(config.variant, 0, config.styleOne.id, config.styleTwo.id, ranked = ranked))
                    bw.mmele.analytics.Tracker.log(bw.mmele.analytics.Events.ROOM_CREATED, "ranked" to ranked)
                    OnlineSession(repo, room, Seat.ONE)
                }
            },
            enabled = busy == null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate),
        ) {
            if (busy == "create") CircularProgressIndicator(Modifier.size(20.dp), color = MmeleColors.Slate, strokeWidth = 2.dp)
            else Text(stringResource(R.string.lobby_create), style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(Modifier.height(16.dp))
    Panel {
        Text(stringResource(R.string.lobby_join_title), style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = sanitizeRoomCode(it) },
            placeholder = { Text("ABC234", color = MmeleColors.BoneDim, style = codeStyle) },
            label = { Text(stringResource(R.string.lobby_code_label)) },
            singleLine = true,
            textStyle = codeStyle.copy(color = MmeleColors.GoldBright),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (code.length == ROOM_CODE_LENGTH) run("join") { joinSession(repo, code) } }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MmeleColors.Gold, unfocusedBorderColor = MmeleColors.Bone.copy(alpha = 0.3f), cursorColor = MmeleColors.Gold),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { run("join") { joinSession(repo, code) } },
            enabled = busy == null && code.length == ROOM_CODE_LENGTH,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (busy == "join") CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.lobby_join))
        }
    }
    error?.let { err ->
        Spacer(Modifier.height(12.dp))
        val msg = when (err) {
            LobbyError.Network -> stringResource(R.string.lobby_err_network)
            is LobbyError.NoRoom -> stringResource(R.string.lobby_err_no_room, err.code)
            LobbyError.Full -> stringResource(R.string.lobby_err_full)
            is LobbyError.Other -> err.detail ?: stringResource(R.string.lobby_err_other)
        }
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Danger, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(16.dp))
    Text(
        stringResource(R.string.lobby_note),
        style = MaterialTheme.typography.labelMedium,
        color = MmeleColors.BoneDim,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private sealed interface LobbyError {
    data object Network : LobbyError
    data class NoRoom(val code: String) : LobbyError
    data object Full : LobbyError
    data class Other(val detail: String?) : LobbyError
}

private suspend fun joinSession(repo: OnlineGameRepository, code: String): OnlineSession {
    val room = repo.joinRoom(code)
    bw.mmele.analytics.Tracker.log(bw.mmele.analytics.Events.ROOM_JOINED, "ranked" to room.config.ranked)
    return OnlineSession(repo, room, repo.lastJoinSeat)
}

private val codeStyle = TextStyle(fontSize = 26.sp, letterSpacing = 6.sp, textAlign = TextAlign.Center)

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MmeleColors.SlateRaised.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
            .border(1.dp, MmeleColors.Bone.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.Top,
    ) { content() }
}
