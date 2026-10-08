package bw.mmele.ui.online

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
                    TextButton(onClick = onBack) { Text("‹ Home", color = MmeleColors.BoneDim) }
                }
                Spacer(Modifier.height(8.dp))
                Text("Play online", style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone)
                Text("Tshameka le tsala · play a friend on another phone", style = MaterialTheme.typography.labelMedium, color = MmeleColors.Gold)
                Spacer(Modifier.height(20.dp))
                if (repo == null) NotConfiguredCard(onBack) else LobbyBody(config, repo, onSession, initialCode)
            }
        }
    }
}

@Composable
private fun NotConfiguredCard(onBack: () -> Unit) {
    Panel {
        Text("Online play not configured yet", style = MaterialTheme.typography.titleLarge, color = MmeleColors.Bone)
        Spacer(Modifier.height(8.dp))
        Text(
            "This copy of Mmele was built without a Firebase project, so it can't reach the internet for games. " +
                "Pass & Play and Solo vs AI work fully offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MmeleColors.BoneDim,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Developer: add app/google-services.json and rebuild — see docs/ONLINE_SETUP.md.",
            style = MaterialTheme.typography.labelMedium,
            color = MmeleColors.GoldBright,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Back to offline play") }
    }
}

@Composable
private fun LobbyBody(config: GameConfig, repo: OnlineGameRepository, onSession: (OnlineSession) -> Unit, initialCode: String) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf(sanitizeRoomCode(initialCode)) }

    fun run(label: String, block: suspend () -> OnlineSession) {
        if (busy != null) return
        busy = label; error = null
        scope.launch {
            try {
                val s = withTimeout(15_000) { block() }
                onSession(s)
            } catch (e: TimeoutCancellationException) {
                error = "Couldn't reach the game server. Check your internet connection and try again."
            } catch (e: RoomNotFoundException) {
                error = "No game with code $code. Check the code with your friend."
            } catch (e: RoomFullException) {
                error = "That game already has two players."
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong."
            } finally {
                busy = null
            }
        }
    }

    Panel {
        Text("Host a game", style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PieceIcon(config.styleOne, Modifier.size(30.dp))
            PieceIcon(config.styleTwo, Modifier.size(30.dp), pieceId = 3)
            Spacer(Modifier.width(10.dp))
            Text(
                (if (config.variant == Variant.CLASSIC) "Classic · 12 cows" else "Khutlotharo · triangle") +
                    " · you play ${config.styleOne.shortName} and move first",
                style = MaterialTheme.typography.labelMedium,
                color = MmeleColors.BoneDim,
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                run("create") {
                    val room = repo.createRoom(RoomConfig(config.variant, 0, config.styleOne.id, config.styleTwo.id))
                    OnlineSession(repo, room, Seat.ONE)
                }
            },
            enabled = busy == null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate),
        ) {
            if (busy == "create") CircularProgressIndicator(Modifier.size(20.dp), color = MmeleColors.Slate, strokeWidth = 2.dp)
            else Text("Create room", style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(Modifier.height(16.dp))
    Panel {
        Text("Join a friend", style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = sanitizeRoomCode(it) },
            placeholder = { Text("ABC234", color = MmeleColors.BoneDim.copy(alpha = 0.5f), style = codeStyle) },
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
            if (busy == "join") CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Join game")
        }
    }
    error?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Danger, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "Variant and pieces come from the host's choices. Your scene stays your own.",
        style = MaterialTheme.typography.labelMedium,
        color = MmeleColors.BoneDim,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private suspend fun joinSession(repo: OnlineGameRepository, code: String): OnlineSession {
    val room = repo.joinRoom(code)
    return OnlineSession(repo, room, Seat.TWO)
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
