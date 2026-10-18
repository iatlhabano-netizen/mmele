package bw.mmele.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.saveable.rememberSaveable
import bw.mmele.engine.Action
import bw.mmele.engine.IllegalReason

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import bw.mmele.ui.labels.fact
import bw.mmele.ui.labels.title
import com.tshipiworks.mmele.R

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
fun GameRoute(viewModel: GameViewModel, feedback: FeedbackController?, onHome: () -> Unit, onShareCode: (String) -> Unit = {}, factCard: bw.mmele.data.FactCard? = null) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.events.collect { feedback?.play(it) } }
    // Clocks stop while the app is in the background, and the game is saved.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) { viewModel.onHidden() }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_START) { viewModel.onShown() }
    var confirmLeave by remember { mutableStateOf(false) }
    // Ask before abandoning a game that has started; finished or empty games leave straight away.
    val leave = { if (ui.game.isGameOver || ui.game.ply == 0 || ui.online?.waitingForOpponent == true) onHome() else confirmLeave = true }
    BackHandler(onBack = leave)
    var replay by rememberSaveable { mutableStateOf<String?>(null) }
    GameScreenContent(
        ui, viewModel::onIntent, leave, onShareCode = onShareCode, factCard = factCard,
        onReplay = { replay = viewModel.movesSoFar().joinToString(",") { it.encode().toString() } },
    )
    replay?.let { enc ->
        val moves = remember(enc) { enc.split(',').filter { it.isNotEmpty() }.map { Action.decode(it.toInt()) } }
        BackHandler { replay = null }
        ReplayViewer(ui.config, moves, onClose = { replay = null })
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.game_leave_title)) },
            text = { Text(stringResource(if (ui.online != null) R.string.game_leave_online else R.string.game_leave_saved)) },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onHome() }) { Text(stringResource(R.string.game_leave)) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.game_keep_playing)) } },
            containerColor = MmeleColors.SlateHigh,
            titleContentColor = MmeleColors.Bone,
            textContentColor = MmeleColors.Bone,
        )
    }
}

/** Stateless game screen (also used for screenshot tests). */
@Composable
fun GameScreenContent(
    ui: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onHome: () -> Unit,
    animate: Boolean = true,
    onShareCode: (String) -> Unit = {},
    onReplay: () -> Unit = {},
    factCard: bw.mmele.data.FactCard? = null,
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
                    hint = hintMask(ui.hint),
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
                    ui.online?.let { OnlineBar(it, onShareCode, Modifier.padding(bottom = 6.dp), onClaimIdle = { onIntent(GameIntent.ClaimIdle) }) { onIntent(GameIntent.ClaimAbandoned) } }
                    topHud(Modifier)
                    PromptPill(ui, flipped = rotateTop, slot = top, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                    board(Modifier.weight(1f).fillMaxWidth())
                    PromptPill(ui, flipped = false, slot = bottom, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
                    bottomHud(Modifier)
                    NoticePill(ui, onIntent, Modifier.align(Alignment.CenterHorizontally))
                    ControlsRow(ui, onIntent, onHome, onReplay = onReplay)
                }
            } else {
                Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(280.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        ui.online?.let { OnlineBar(it, onShareCode, onClaimIdle = { onIntent(GameIntent.ClaimIdle) }) { onIntent(GameIntent.ClaimAbandoned) } }
                        topHud(Modifier)
                        PromptPill(ui, flipped = false, slot = null, modifier = Modifier.align(Alignment.CenterHorizontally))
                        NoticePill(ui, onIntent, Modifier.align(Alignment.CenterHorizontally))
                        bottomHud(Modifier)
                    }
                    board(Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(132.dp)) { ControlsRow(ui, onIntent, onHome, vertical = true, onReplay = onReplay) }
                }
            }
        }
        ui.online?.takeIf { it.waitingForOpponent }?.let { WaitingCard(it, onShareCode, onHome, Modifier.align(Alignment.Center)) }
        ui.online?.takeIf { it.opponentOffersDraw && !ui.game.isGameOver }?.let {
            DrawOfferBanner(onIntent, Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 128.dp))
        }
        if (ui.showGameOver) GameOverDialog(ui, onIntent, onHome, onReplay, factCard)
    }
}

/** Points the hint marks: the cow and its destination, the point to place on, or the cow to shoot. */
fun hintMask(a: Action?): Int = when (a) {
    null -> 0
    is Action.Place -> 1 shl a.to
    is Action.Move -> (1 shl a.from) or (1 shl a.to)
    is Action.Shoot -> 1 shl a.target
}

@androidx.annotation.StringRes
fun IllegalReason.textRes(): Int = when (this) {
    IllegalReason.GAME_OVER -> R.string.illegal_game_over
    IllegalReason.MUST_SHOOT_FIRST -> R.string.illegal_must_shoot
    IllegalReason.SHOOT_ENEMY_ONLY -> R.string.illegal_shoot_enemy
    IllegalReason.PROTECTED_IN_MILL -> R.string.illegal_protected
    IllegalReason.PLACE_FIRST -> R.string.illegal_place_first
    IllegalReason.OCCUPIED -> R.string.illegal_occupied
    IllegalReason.NOT_YOUR_COW -> R.string.illegal_not_your_cow
    IllegalReason.DESTINATION_OCCUPIED -> R.string.illegal_destination_occupied
    IllegalReason.SLIDE_ONLY -> R.string.illegal_slide_only
    IllegalReason.STEP_OR_JUMP -> R.string.illegal_step_or_jump
    IllegalReason.COW_IS_STUCK -> R.string.illegal_stuck
    IllegalReason.NO_IMMEDIATE_REMILL -> R.string.illegal_remill
}

@Composable
fun hintText(a: Action, board: bw.mmele.engine.BoardGraph): String = when (a) {
    is Action.Place -> stringResource(R.string.game_hint_place, board.labels[a.to])
    is Action.Move -> stringResource(R.string.game_hint_move, board.labels[a.from], board.labels[a.to])
    is Action.Shoot -> stringResource(R.string.game_hint_shoot, board.labels[a.target])
}

/**
 * Toast-style pill under the board: why a move was refused (fades after a few seconds), or the
 * hint's suggestion. Spoken by TalkBack.
 */
@Composable
private fun NoticePill(ui: GameUiState, onIntent: (GameIntent) -> Unit, modifier: Modifier = Modifier) {
    val illegal = ui.illegal
    LaunchedEffect(illegal?.id) {
        if (illegal != null) { kotlinx.coroutines.delay(3_500); onIntent(GameIntent.DismissIllegal) }
    }
    val text = when {
        illegal != null -> stringResource(illegal.reason.textRes())
        ui.hint != null -> hintText(ui.hint, ui.game.board)
        else -> null
    }
    AnimatedVisibility(text != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Text(
            text.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = if (illegal != null) MmeleColors.Slate else MmeleColors.Bone,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .background(if (illegal != null) MmeleColors.Bone else MmeleColors.SlateHigh.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                .border(1.dp, MmeleColors.Bone.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Contextual instruction for the player to move. [slot] = the side this pill sits next to (null = any). */
@Composable
private fun PromptPill(ui: GameUiState, flipped: Boolean, slot: Player?, modifier: Modifier = Modifier) {
    val game = ui.game
    val show = !game.isGameOver && (slot == null || game.toMove == slot) && (ui.isHumanTurn || ui.aiThinking || ui.online != null)
    val text = promptText(ui)
    // Spoken by TalkBack whenever it changes (only one pill is shown at a time).
    Box(modifier.height(40.dp).semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
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

@Composable
fun promptText(ui: GameUiState): String {
    val g = ui.game
    if (g.isGameOver) return ""
    if (!ui.isHumanTurn) return when {
        ui.aiThinking -> stringResource(R.string.prompt_ai_thinking)
        ui.online != null && !ui.online.waitingForOpponent -> stringResource(R.string.prompt_opponent_turn)
        else -> ""
    }
    val text = if (g.mustShoot) {
        val enemy = g.maskOf(g.toMove.opponent)
        val allProtected = Rules.shootableMask(g) == enemy && Rules.cowsInMills(g.board, enemy) == enemy
        stringResource(if (allProtected) R.string.prompt_shoot_any else R.string.prompt_shoot)
    } else when (g.phase) {
        Phase.PLACEMENT -> g.handOf(g.toMove).let { pluralStringResource(R.plurals.prompt_place, it, it) }
        Phase.MOVEMENT -> stringResource(if (ui.selected != null) R.string.prompt_move_to else R.string.prompt_move)
        Phase.FLYING -> stringResource(R.string.prompt_fly)
        Phase.GAME_OVER -> ""
    }
    return if (ui.config.mode == GameMode.PASS_AND_PLAY) stringResource(R.string.prompt_who, playerName(g.toMove, ui.config), text) else text
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ControlsRow(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit, vertical: Boolean = false, onReplay: () -> Unit = {}) {
    val content = @Composable {
        TextButton(onClick = onHome) { Text(stringResource(R.string.nav_home), color = MmeleColors.BoneDim) }
        val online = ui.online
        if (online == null && !ui.game.isGameOver) {
            OutlinedButton(onClick = { onIntent(GameIntent.Hint) }, enabled = ui.canHint && !ui.hinting) {
                Text(stringResource(if (ui.hinting) R.string.game_hint_thinking else R.string.game_hint))
            }
        }
        if (online == null) {
            OutlinedButton(onClick = { onIntent(GameIntent.Undo) }, enabled = ui.canUndo) { Text(stringResource(R.string.game_undo)) }
        } else if (!ui.game.isGameOver) {
            OutlinedButton(onClick = { onIntent(GameIntent.OfferDraw) }, enabled = !online.iOfferedDraw && !online.waitingForOpponent) {
                Text(stringResource(if (online.iOfferedDraw) R.string.game_draw_offered else R.string.game_offer_draw))
            }
            var confirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            OutlinedButton(onClick = { if (confirm) onIntent(GameIntent.Resign) else confirm = true }, enabled = !online.waitingForOpponent) {
                Text(stringResource(if (confirm) R.string.game_confirm else R.string.game_resign), color = if (confirm) MmeleColors.Danger else androidx.compose.ui.graphics.Color.Unspecified)
            }
        }
        if (ui.game.isGameOver) {
            OutlinedButton(onClick = onReplay) { Text(stringResource(R.string.game_replay)) }
            OutlinedButton(onClick = { onIntent(GameIntent.Rematch) }, enabled = online?.iOfferedRematch != true) { Text(rematchLabel(ui)) }
        }
    }
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    } else {
        // Wraps onto a second line at large font sizes instead of clipping.
        androidx.compose.foundation.layout.FlowRow(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) { content() }
    }
}

@Composable
fun resultTitle(ui: GameUiState): String = when (val r = ui.game.result) {
    is GameResult.Win -> when (ui.config.mode) {
        GameMode.SOLO -> stringResource(if (r.winner == ui.config.human) R.string.result_you_win else R.string.result_computer_wins)
        GameMode.PASS_AND_PLAY -> stringResource(R.string.result_side_wins, playerName(r.winner, ui.config))
        GameMode.ONLINE -> stringResource(if (r.winner == ui.config.human) R.string.result_you_win else R.string.result_opponent_wins)
    }
    is GameResult.Draw -> stringResource(R.string.result_draw)
    null -> ""
}

@Composable
fun resultReason(ui: GameUiState): String = when (val r = ui.game.result) {
    is GameResult.Win -> stringResource(
        when (r.reason) {
            WinReason.REDUCED_TO_TWO -> R.string.reason_two
            WinReason.BLOCKADE -> R.string.reason_blockade
            WinReason.LINE_OF_THREE -> R.string.reason_line
            WinReason.TIMEOUT -> R.string.reason_timeout
            WinReason.RESIGNATION -> R.string.reason_resigned
            WinReason.ABANDONED -> R.string.reason_abandoned
        },
    )
    is GameResult.Draw -> when (r.reason) {
        DrawReason.FLYING_NO_SHOT -> stringResource(R.string.reason_flying_draw)
        DrawReason.NO_PROGRESS -> {
            val each = ui.game.rules.idleDrawPlies / 2
            if (ui.game.rules.millEffect == MillEffect.WIN) pluralStringResource(R.plurals.reason_no_line, each, each)
            else pluralStringResource(R.plurals.reason_no_shot, each, each)
        }
        DrawReason.AGREEMENT -> stringResource(R.string.reason_agreed)
    }
    null -> ""
}

@Composable
private fun GameOverDialog(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit, onReplay: () -> Unit = {}, factCard: bw.mmele.data.FactCard? = null) {
    Dialog(onDismissRequest = { onIntent(GameIntent.DismissGameOver) }) { GameOverCard(ui, onIntent, onHome, onReplay = onReplay, factCard = factCard) }
}

/** Game-over card: the scene at full strength with its caption, result, and rematch. */
@Composable
fun GameOverCard(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier, onReplay: () -> Unit = {}, factCard: bw.mmele.data.FactCard? = null) {
        Column(modifier.then(
            Modifier
                .background(MmeleColors.SlateRaised, RoundedCornerShape(24.dp))
                .border(1.dp, MmeleColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (factCard == null) SceneHeader(ui.config.scene) else bw.mmele.ui.facts.FactCardHeader(factCard, ui.config.scene)
            GameOverBody(ui, onIntent, onHome, onReplay)
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
            Text(scene.title().uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.GoldBright)
            Text(scene.fact(), style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun GameOverBody(ui: GameUiState, onIntent: (GameIntent) -> Unit, onHome: () -> Unit, onReplay: () -> Unit) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val r = ui.game.result
            if (r is GameResult.Win) PieceIcon(ui.config.styleOf(r.winner), Modifier.width(64.dp).height(64.dp), elevation = 0.8f)
            Spacer(Modifier.height(8.dp))
            Text(resultTitle(ui), style = MaterialTheme.typography.headlineMedium, color = MmeleColors.Bone)
            Spacer(Modifier.height(6.dp))
            Text(resultReason(ui), style = MaterialTheme.typography.bodyMedium, color = MmeleColors.BoneDim, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.result_moves, ui.game.ply),
                style = MaterialTheme.typography.labelMedium,
                color = MmeleColors.BoneDim,
            )
            Spacer(Modifier.height(20.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onHome) { Text(stringResource(R.string.nav_home)) }
                TextButton(onClick = { onIntent(GameIntent.DismissGameOver) }) { Text(stringResource(R.string.result_view_board)) }
                TextButton(onClick = { onIntent(GameIntent.DismissGameOver); onReplay() }) { Text(stringResource(R.string.game_replay)) }
                androidx.compose.material3.Button(onClick = { onIntent(GameIntent.Rematch) }, enabled = ui.online?.iOfferedRematch != true) { Text(rematchLabel(ui)) }
            }
        }
    }


@Composable
fun rematchLabel(ui: GameUiState): String {
    val o = ui.online ?: return stringResource(R.string.game_rematch)
    return stringResource(
        when {
            o.iOfferedRematch -> R.string.game_waiting
            o.opponentOffersRematch -> R.string.game_accept_rematch
            else -> R.string.game_rematch
        },
    )
}

/** Room code, connection dot, and transient messages for online games. */
@Composable
private fun OnlineBar(o: OnlineUi, onShareCode: (String) -> Unit, modifier: Modifier = Modifier, onClaimIdle: () -> Unit = {}, onClaim: () -> Unit = {}) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MmeleColors.SlateHigh.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (dot, label) = when {
                o.waitingForOpponent -> MmeleColors.Gold to stringResource(R.string.online_waiting)
                o.opponentConnected -> MmeleColors.Online to stringResource(R.string.online_connected)
                o.abandonSecondsLeft != null && o.abandonSecondsLeft > 0 -> MmeleColors.Danger to stringResource(R.string.online_disconnected_countdown, o.abandonSecondsLeft)
                else -> MmeleColors.Danger to stringResource(R.string.online_disconnected)
            }
            androidx.compose.foundation.Canvas(Modifier.width(10.dp).height(10.dp)) { drawCircle(dot) }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone, modifier = Modifier.weight(1f))
            if (o.syncing) Text(stringResource(R.string.online_sending), style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim, modifier = Modifier.padding(end = 8.dp))
            Text(
                o.code,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = MmeleColors.GoldBright,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = stringResource(R.string.online_share_action)) { onShareCode(o.code) }.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (o.abandonSecondsLeft == 0) {
            ClaimRow(onClaim)
        } else if (o.idleClaimSecondsLeft == 0) {
            ClaimRow(onClaimIdle, R.string.online_idle_claim)
        }
        o.myIdleSecondsLeft?.let { s ->
            Text(
                stringResource(R.string.online_idle_warning, "%d:%02d".format(s / 60, s % 60)),
                style = MaterialTheme.typography.labelLarge,
                color = MmeleColors.Slate,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .background(MmeleColors.Gold, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        o.message?.let { msg ->
            Text(msg.text(), style = MaterialTheme.typography.labelMedium, color = MmeleColors.GoldBright, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }
}

@Composable
private fun ClaimRow(onClaim: () -> Unit, @androidx.annotation.StringRes why: Int = R.string.online_away_too_long) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, start = 4.dp)) {
        Text(
            stringResource(why),
            style = MaterialTheme.typography.labelMedium,
            color = MmeleColors.Bone,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClaim) { Text(stringResource(R.string.online_claim_win)) }
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
        Text(stringResource(R.string.online_room_code), style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim)
        Spacer(Modifier.height(6.dp))
        Text(
            o.code.chunked(3).joinToString(" "),
            style = MaterialTheme.typography.displaySmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(6f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = MmeleColors.GoldBright,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.online_send_code), style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Bone, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        androidx.compose.material3.Button(onClick = { onShareCode(o.code) }) { Text(stringResource(R.string.online_share_code)) }
        TextButton(onClick = onHome) { Text(stringResource(R.string.common_cancel), color = MmeleColors.BoneDim) }
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
        Text(stringResource(R.string.online_offers_draw), style = MaterialTheme.typography.labelLarge, color = MmeleColors.Bone)
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { onIntent(GameIntent.OfferDraw) }) { Text(stringResource(R.string.online_accept)) }
    }
}
