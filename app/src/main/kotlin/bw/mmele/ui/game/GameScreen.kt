package bw.mmele.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import bw.mmele.ui.board.SandBackground
import bw.mmele.ui.theme.MmeleColors

/** Stateful entry point: wires the ViewModel and feedback to the stateless content. */
@Composable
fun GameRoute(viewModel: GameViewModel, feedback: FeedbackController?, onHome: () -> Unit) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.events.collect { feedback?.play(it) } }
    BackHandler(onBack = onHome)
    GameScreenContent(ui, viewModel::onIntent, onHome)
}

/** Stateless game screen (also used for screenshot tests). */
@Composable
fun GameScreenContent(
    ui: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onHome: () -> Unit,
    animate: Boolean = true,
) {
    val config = ui.config
    val bottom = if (config.mode == GameMode.SOLO) config.human else Player.ONE
    val top = bottom.opponent
    val rotateTop = config.mode == GameMode.PASS_AND_PLAY
    val game = ui.game

    Box(Modifier.fillMaxSize()) {
        SandBackground()
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val landscape = maxWidth > maxHeight * 1.15f
            val board = @Composable { m: Modifier ->
                BoardCanvas(
                    board = game.board,
                    tokens = ui.tokens,
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
                        topHud(Modifier)
                        PromptPill(ui, flipped = false, slot = null, modifier = Modifier.align(Alignment.CenterHorizontally))
                        bottomHud(Modifier)
                    }
                    board(Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(120.dp)) { ControlsRow(ui, onIntent, onHome, vertical = true) }
                }
            }
        }
        if (ui.showGameOver) GameOverDialog(ui, onIntent, onHome)
    }
}

/** Contextual instruction for the player to move. [slot] = the side this pill sits next to (null = any). */
@Composable
private fun PromptPill(ui: GameUiState, flipped: Boolean, slot: Player?, modifier: Modifier = Modifier) {
    val game = ui.game
    val show = !game.isGameOver && (slot == null || game.toMove == slot) && (ui.isHumanTurn || ui.aiThinking)
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
    if (!ui.isHumanTurn) return if (ui.aiThinking) "Computer is thinking…" else ""
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
        OutlinedButton(onClick = { onIntent(GameIntent.Undo) }, enabled = ui.canUndo) { Text("Undo") }
        if (ui.game.isGameOver) {
            OutlinedButton(onClick = { onIntent(GameIntent.Rematch) }) { Text("Rematch") }
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
    }
    is GameResult.Draw -> when (r.reason) {
        DrawReason.FLYING_NO_SHOT -> "Ten moves each with a player on three cows and no shot."
        DrawReason.NO_PROGRESS -> if (ui.game.rules.millEffect == MillEffect.WIN) "Twenty moves each without a line." else "Fifty moves each without a shot."
    }
    null -> ""
}

@Composable
private fun GameOverDialog(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit) {
    Dialog(onDismissRequest = { onIntent(GameIntent.DismissGameOver) }) {
        Column(
            Modifier
                .background(MmeleColors.SlateRaised, RoundedCornerShape(24.dp))
                .border(1.dp, MmeleColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val r = ui.game.result
            if (r is GameResult.Win) PebbleIcon(r.winner, Modifier.width(64.dp).height(64.dp), elevation = 0.8f)
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
                androidx.compose.material3.Button(onClick = { onIntent(GameIntent.Rematch) }) { Text("Rematch") }
            }
        }
    }
}
