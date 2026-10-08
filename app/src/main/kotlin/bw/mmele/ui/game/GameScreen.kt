package bw.mmele.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bw.mmele.engine.GameResult
import bw.mmele.engine.MillEffect
import bw.mmele.engine.Phase
import bw.mmele.engine.Player
import bw.mmele.engine.Rules
import bw.mmele.engine.WinReason
import bw.mmele.engine.DrawReason
import bw.mmele.feedback.FeedbackController
import bw.mmele.ui.board.BoardCanvas
import bw.mmele.ui.scenes.SceneBackdrop
import bw.mmele.ui.scenes.SceneMode
import bw.mmele.ui.theme.MmeleColors

/** Stateful entry point: wires the ViewModel and feedback to the stateless content. */
@Composable
fun GameRoute(viewModel: GameViewModel, feedback: FeedbackController?, onHome: () -> Unit, onShareCode: (String) -> Unit = {}) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.events.collect { feedback?.play(it) } }
    BackHandler(onBack = onHome)
    GameScreenContent(ui, viewModel::onIntent, onHome, onShareCode = onShareCode)
}

/** Stateless game screen (also used for screenshot tests). */
@Composable
fun GameScreenContent(
    ui: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onHome: () -> Unit,
    animate: Boolean = true,
    onShareCode: (String) -> Unit = {},
) {
    val config = ui.config
    val bottom = if (config.mode == GameMode.PASS_AND_PLAY) Player.ONE else config.human
    val top = bottom.opponent
    val rotateTop = config.mode == GameMode.PASS_AND_PLAY
    val game = ui.game

    Box(Modifier.fillMaxSize()) {
        SceneBackdrop(config.scene, SceneMode.DIMMED, animate = animate)
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val landscape = maxWidth > maxHeight * 1.15f
            val board = @Composable { m: Modifier ->
                BoardCanvas(
                    board = game.board,
                    tokens = ui.tokens,
                    styleOne = config.styleOne,
                    styleTwo = config.styleTwo,
                    modifier = m,
                    selected = ui.selected,
                    targets = ui.targets,
                    subtleTargets = game.phase == Phase.PLACEMENT,
                    shootable = ui.shootable,
                    draggable = ui.draggable,
                    flyingPlayers = Player.entries.filter { !game.isGameOver && game.phaseOf(it) == Phase.FLYING }.toSet(),
                    lastMove = ui.lastMove,
                    millFlash = ui.millFlash,
                    shotFx = ui.shotFx,
                    interactive = ui.isHumanTurn,
                    animate = animate,
                    drawBackground = false,
                    onTap = { onIntent(GameIntent.NodeTapped(it)) },
                    onDragStart = { onIntent(GameIntent.DragStarted(it)) },
                    onDrop = { from, to -> onIntent(GameIntent.Dropped(from, to)) },
                )
            }
            val topHud = @Composable { m: Modifier ->
                PlayerHud(top, game, config, ui.clockOf(top), ui.aiThinking && game.toMove == top, m.then(if (rotateTop) Modifier.rotate(180f) else Modifier))
            }
            val bottomHud = @Composable { m: Modifier ->
                PlayerHud(bottom, game, config, ui.clockOf(bottom), ui.aiThinking && game.toMove == bottom, m)
            }
            if (!landscape) {
                Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    ui.online?.let { OnlineBar(it, onShareCode, Modifier.padding(bottom = 6.dp)) { onIntent(GameIntent.ClaimAbandoned) } }
                    topHud(Modifier)
                    PromptPill(ui, flipped = rotateTop, slot = top, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                    board(Modifier.weight(1f).fillMaxWidth())
                    PromptPill(ui, flipped = false, slot = bottom, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
                    bottomHud(Modifier)
                    ControlsRow(ui, onIntent, onHome)
                }
            } else {
                Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(280.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        ui.online?.let { OnlineBar(it, onShareCode) { onIntent(GameIntent.ClaimAbandoned) } }
                        topHud(Modifier)
                        PromptPill(ui, flipped = false, slot = null, modifier = Modifier.align(Alignment.CenterHorizontally))
                        bottomHud(Modifier)
                    }
                    board(Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(120.dp)) { ControlsRow(ui, onIntent, onHome, vertical = true) }
                }
            }
        }
        ui.online?.takeIf { it.waitingForOpponent }?.let { WaitingCard(it, onShareCode, onHome, Modifier.align(Alignment.Center)) }
        ui.online?.takeIf { it.opponentOffersDraw && !ui.game.isGameOver }?.let {
            DrawOfferBanner(onIntent, Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 128.dp))
        }
        if (ui.showGameOver) GameOverDialog(ui, onIntent, onHome)
    }
}

/** Contextual instruction for the player to move. [slot] = the side this pill sits next to (null = any). */
@Composable
private fun PromptPill(ui: GameUiState, flipped: Boolean, slot: Player?, modifier: Modifier = Modifier) {
    val game = ui.game
    val show = !game.isGameOver && (slot == null || game.toMove == slot) && (ui.isHumanTurn || ui.aiThinking || ui.online != null)
    val text = promptText(ui)
    Box(modifier.height(40.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(targetState = if (show) text else "", transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "prompt") { t ->
            if (t.isNotEmpty()) {
                val shoot = game.mustShoot && ui.isHumanTurn
                Text(
                    t,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (shoot) MmeleColors.Slate else MmeleColors.Bone,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .then(if (flipped) Modifier.rotate(180f) else Modifier)
                        .background(if (shoot) MmeleColors.Gold else MmeleColors.SlateHigh.copy(alpha = 0.9f), RoundedCornerShape(50))
                        .border(1.dp, MmeleColors.Gold.copy(alpha = if (shoot) 0f else 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

fun promptText(ui: GameUiState): String {
    val g = ui.game
    if (g.isGameOver) return ""
    if (!ui.isHumanTurn) return when {
        ui.aiThinking -> "Computer is thinking…"
        ui.online != null && !ui.online.waitingForOpponent -> "Opponent's turn"
        else -> ""
    }
    val who = if (ui.config.mode == GameMode.PASS_AND_PLAY) "${playerName(g.toMove, ui.config)}: " else ""
    if (g.mustShoot) {
        val enemy = g.maskOf(g.toMove.opponent)
        val allProtected = Rules.shootableMask(g) == enemy && Rules.cowsInMills(g.board, enemy) == enemy
        return who + if (allProtected) "Mill! All their cows are in mills — shoot any one" else "Mill! Tap a glowing cow to shoot it"
    }
    return who + when (g.phase) {
        Phase.PLACEMENT -> "Place a cow · ${g.handOf(g.toMove)} in hand"
        Phase.MOVEMENT -> if (ui.selected != null) "Move to a glowing point" else "Tap or drag a cow to move it"
        Phase.FLYING -> "Three cows left — fly to any empty point"
        Phase.GAME_OVER -> ""
    }
}

@Composable
private fun ControlsRow(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit, vertical: Boolean = false) {
    val content = @Composable {
        TextButton(onClick = onHome) { Text("Home", color = MmeleColors.BoneDim) }
        val online = ui.online
        if (online == null) {
            OutlinedButton(onClick = { onIntent(GameIntent.Undo) }, enabled = ui.canUndo) { Text("Undo") }
        } else if (!ui.game.isGameOver) {
            OutlinedButton(onClick = { onIntent(GameIntent.OfferDraw) }, enabled = !online.iOfferedDraw && !online.waitingForOpponent) {
                Text(if (online.iOfferedDraw) "Draw offered" else "Offer draw")
            }
            var confirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            OutlinedButton(onClick = { if (confirm) onIntent(GameIntent.Resign) else confirm = true }, enabled = !online.waitingForOpponent) {
                Text(if (confirm) "Tap to confirm" else "Resign", color = if (confirm) MmeleColors.Danger else androidx.compose.ui.graphics.Color.Unspecified)
            }
        }
        if (ui.game.isGameOver) {
            OutlinedButton(onClick = { onIntent(GameIntent.Rematch) }, enabled = online?.iOfferedRematch != true) { Text(rematchLabel(ui)) }
        }
    }
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

fun resultTitle(ui: GameUiState): String = when (val r = ui.game.result) {
    is GameResult.Win -> when (ui.config.mode) {
        GameMode.SOLO -> if (r.winner == ui.config.human) "You win!" else "The computer wins"
        GameMode.PASS_AND_PLAY -> "${playerName(r.winner, ui.config)} wins!"
        GameMode.ONLINE -> if (r.winner == ui.config.human) "You win!" else "Opponent wins"
    }
    is GameResult.Draw -> "A draw"
    null -> ""
}

fun resultReason(ui: GameUiState): String = when (val r = ui.game.result) {
    is GameResult.Win -> when (r.reason) {
        WinReason.REDUCED_TO_TWO -> "Reduced to two cows — no more mills possible."
        WinReason.BLOCKADE -> "Go tswala — locked in with no legal move."
        WinReason.LINE_OF_THREE -> "Three in a line."
        WinReason.TIMEOUT -> "Out of time."
        WinReason.RESIGNATION -> "Resigned."
        WinReason.ABANDONED -> "Left the game and did not return."
    }
    is GameResult.Draw -> when (r.reason) {
        DrawReason.FLYING_NO_SHOT -> "Ten moves each with a player on three cows and no shot."
        DrawReason.NO_PROGRESS -> if (ui.game.rules.millEffect == MillEffect.WIN) "Twenty moves each without a line." else "Fifty moves each without a shot."
        DrawReason.AGREEMENT -> "Both players agreed to a draw."
    }
    null -> ""
}

@Composable
private fun GameOverDialog(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit) {
    Dialog(onDismissRequest = { onIntent(GameIntent.DismissGameOver) }) { GameOverCard(ui, onIntent, onHome) }
}

/** Game-over card: the scene at full strength with its caption, result, and rematch. */
@Composable
fun GameOverCard(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
        Column(modifier.then(
            Modifier
                .background(MmeleColors.SlateRaised, RoundedCornerShape(24.dp))
                .border(1.dp, MmeleColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SceneHeader(ui.config.scene)
            GameOverBody(ui, onIntent, onHome)
        }
}

@Composable
private fun SceneHeader(scene: bw.mmele.ui.scenes.Scene) {
    Box(Modifier.fillMaxWidth().height(170.dp)) {
        SceneBackdrop(scene, SceneMode.FULL, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, MmeleColors.SlateRaised.copy(alpha = 0.95f)))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(scene.title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.GoldBright)
            Text(scene.fact, style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone)
        }
    }
}

@Composable
private fun GameOverBody(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val r = ui.game.result
            if (r is GameResult.Win) PieceIcon(ui.config.styleOf(r.winner), Modifier.width(64.dp).height(64.dp), elevation = 0.8f)
            Spacer(Modifier.height(8.dp))
            Text(resultTitle(ui), style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone)
            Spacer(Modifier.height(6.dp))
            Text(resultReason(ui), style = MaterialTheme.typography.bodyMedium, color = MmeleColors.BoneDim, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Moves played: ${ui.game.ply}",
                style = MaterialTheme.typography.labelMedium,
                color = MmeleColors.BoneDim,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onHome) { Text("Home") }
                TextButton(onClick = { onIntent(GameIntent.DismissGameOver) }) { Text("View board") }
                androidx.compose.material3.Button(onClick = { onIntent(GameIntent.Rematch) }, enabled = ui.online?.iOfferedRematch != true) { Text(rematchLabel(ui)) }
            }
        }
    }


fun rematchLabel(ui: GameUiState): String {
    val o = ui.online ?: return "Rematch"
    return when {
        o.iOfferedRematch -> "Waiting…"
        o.opponentOffersRematch -> "Accept rematch"
        else -> "Rematch"
    }
}

/** Room code, connection dot, and transient messages for online games. */
@Composable
private fun OnlineBar(o: OnlineUi, onShareCode: (String) -> Unit, modifier: Modifier = Modifier, onClaim: () -> Unit = {}) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MmeleColors.SlateHigh.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (dot, label) = when {
                o.waitingForOpponent -> MmeleColors.Gold to "Waiting for opponent"
                o.opponentConnected -> MmeleColors.Online to "Opponent connected"
                o.abandonSecondsLeft != null && o.abandonSecondsLeft > 0 -> MmeleColors.Danger to "Opponent disconnected · ${o.abandonSecondsLeft}s"
                else -> MmeleColors.Danger to "Opponent disconnected"
            }
            androidx.compose.foundation.Canvas(Modifier.width(10.dp).height(10.dp)) { drawCircle(dot) }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone, modifier = Modifier.weight(1f))
            if (o.syncing) Text("sending…", style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim, modifier = Modifier.padding(end = 8.dp))
            Text(
                o.code,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = MmeleColors.GoldBright,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onShareCode(o.code) }.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (o.abandonSecondsLeft == 0) {
            ClaimRow(onClaim)
        }
        o.message?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MmeleColors.GoldBright, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }
}

@Composable
private fun ClaimRow(onClaim: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, start = 4.dp)) {
        Text(
            "Your opponent has been away too long.",
            style = MaterialTheme.typography.labelMedium,
            color = MmeleColors.Bone,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClaim) { Text("Claim win") }
    }
}

/** Shown to the host until somebody joins. */
@Composable
private fun WaitingCard(o: OnlineUi, onShareCode: (String) -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(24.dp)
            .background(MmeleColors.SlateRaised.copy(alpha = 0.97f), RoundedCornerShape(24.dp))
            .border(1.dp, MmeleColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ROOM CODE", style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim)
        Spacer(Modifier.height(6.dp))
        Text(
            o.code.chunked(3).joinToString(" "),
            style = MaterialTheme.typography.displaySmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(6f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = MmeleColors.GoldBright,
        )
        Spacer(Modifier.height(8.dp))
        Text("Send this code to a friend. The game starts as soon as they join.", style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Bone, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        androidx.compose.material3.Button(onClick = { onShareCode(o.code) }) { Text("Share code") }
        TextButton(onClick = onHome) { Text("Cancel", color = MmeleColors.BoneDim) }
    }
}

@Composable
private fun DrawOfferBanner(onIntent: (GameIntent) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(MmeleColors.SlateHigh, RoundedCornerShape(50))
            .border(1.dp, MmeleColors.Gold.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Opponent offers a draw", style = MaterialTheme.typography.labelLarge, color = MmeleColors.Bone)
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { onIntent(GameIntent.OfferDraw) }) { Text("Accept") }
    }
}
