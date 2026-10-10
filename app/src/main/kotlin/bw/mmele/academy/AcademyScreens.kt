package bw.mmele.academy

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bw.mmele.engine.Phase
import bw.mmele.engine.Player
import bw.mmele.feedback.FeedbackController
import bw.mmele.ui.board.BoardCanvas
import bw.mmele.ui.common.BodyText
import bw.mmele.ui.common.PageScaffold
import bw.mmele.ui.game.PieceIcon
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.scenes.Scene
import bw.mmele.ui.scenes.SceneBackdrop
import bw.mmele.ui.scenes.SceneMode
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R
import kotlin.math.cos
import kotlin.math.sin

/** Lesson list: done, next (unlocked) and locked lessons, in order. */
@Composable
fun AcademyScreen(lessonsDone: Int, onOpen: (Int) -> Unit, onBack: () -> Unit) {
    val total = Lessons.ALL.size
    PageScaffold(
        title = stringResource(R.string.academy_title),
        onBack = onBack,
        subtitle = stringResource(R.string.academy_subtitle),
    ) {
        Text(
            stringResource(R.string.academy_progress, lessonsDone.coerceAtMost(total), total),
            style = MaterialTheme.typography.labelLarge, color = MmeleColors.Gold,
        )
        Spacer(Modifier.height(8.dp))
        Lessons.ALL.forEachIndexed { i, lesson ->
            val done = i < lessonsDone
            val unlocked = i <= lessonsDone
            LessonRow(i + 1, stringResource(lesson.title), done, unlocked) { onOpen(i) }
        }
        if (lessonsDone >= total) BodyText(stringResource(R.string.lesson_all_done), Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun LessonRow(number: Int, title: String, done: Boolean, unlocked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val status = stringResource(
        when {
            done -> R.string.academy_done
            unlocked -> R.string.academy_start
            else -> R.string.academy_locked
        },
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (unlocked && !done) MmeleColors.Gold.copy(alpha = 0.16f) else MmeleColors.SlateRaised)
            .border(1.dp, if (unlocked && !done) MmeleColors.Gold else MmeleColors.Bone.copy(alpha = 0.12f), shape)
            .then(if (unlocked) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics { stateDescription = status }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(if (done) MmeleColors.Gold else MmeleColors.SlateHigh, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (done) "✓" else number.toString(), color = if (done) MmeleColors.Slate else MmeleColors.Bone, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.academy_lesson_number, number), style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim)
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (unlocked) MmeleColors.Bone else MmeleColors.BoneDim)
        }
        Text(
            if (done) stringResource(R.string.academy_again) else status,
            style = MaterialTheme.typography.labelMedium,
            color = if (unlocked) MmeleColors.GoldBright else MmeleColors.BoneDim,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 120.dp),
        )
    }
}

@Composable
fun LessonRoute(
    vm: LessonViewModel,
    feedback: FeedbackController?,
    hasNext: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.events.collect { feedback?.play(it) } }
    BackHandler(onBack = onBack)
    LessonScreen(
        lesson = vm.lesson, ui = ui,
        onBegin = vm::begin, onHint = vm::showHint, onRestart = vm::restart, onPractice = vm::startPractice,
        onTap = vm::onTap, onDragStart = vm::onDragStart, onDrop = vm::onDrop,
        hasNext = hasNext, onNext = onNext, onBack = onBack,
    )
}

/** Stateless lesson screen (also rendered in screenshot tests). */
@Composable
fun LessonScreen(
    lesson: Lesson,
    ui: LessonUiState,
    onBegin: () -> Unit = {},
    onHint: () -> Unit = {},
    onRestart: () -> Unit = {},
    onPractice: () -> Unit = {},
    onTap: (Int?) -> Unit = {},
    onDragStart: (Int) -> Unit = {},
    onDrop: (Int, Int?) -> Unit = { _, _ -> },
    hasNext: Boolean = true,
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    animate: Boolean = true,
) {
    val number = Lessons.ALL.indexOf(lesson) + 1
    Box(Modifier.fillMaxSize()) {
        SceneBackdrop(Scene.KALAHARI, SceneMode.DIMMED, animate = animate)
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val landscape = maxWidth > maxHeight * 1.15f
            val board = @Composable { m: Modifier ->
                BoardCanvas(
                    board = ui.game.board,
                    tokens = ui.tokens,
                    styleOne = PieceStyle.Obsidian,
                    styleTwo = PieceStyle.Quartz,
                    modifier = m,
                    selected = ui.selected,
                    targets = ui.targets,
                    subtleTargets = ui.game.phase == Phase.PLACEMENT,
                    shootable = ui.shootable,
                    draggable = ui.draggable,
                    flyingPlayers = Player.entries.filter { ui.game.result == null && ui.game.phaseOf(it) == Phase.FLYING }.toSet(),
                    lastMove = ui.lastMove,
                    millFlash = ui.millFlash,
                    shotFx = ui.shotFx,
                    interactive = ui.interactive,
                    animate = animate,
                    drawBackground = false,
                    onTap = onTap,
                    onDragStart = onDragStart,
                    onDrop = onDrop,
                )
            }
            val panel = @Composable { m: Modifier ->
                Column(m.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onBack) { Text("‹ " + stringResource(R.string.academy_title), color = MmeleColors.BoneDim) }
                    Text(
                        stringResource(R.string.academy_lesson_number, number) +
                            if (ui.stage == Stage.PRACTICE) " · " + stringResource(R.string.lesson_practice) else "",
                        style = MaterialTheme.typography.labelSmall, color = MmeleColors.Gold,
                    )
                    Text(stringResource(lesson.title), style = MaterialTheme.typography.headlineSmall, color = MmeleColors.Bone, modifier = Modifier.semantics { heading() })
                    if (ui.stage == Stage.LESSON) BodyText(stringResource(lesson.intro))
                    else BodyText(stringResource(R.string.lesson_practice_intro))
                    GoalCard(stringResource(lesson.goal))
                    CoachLine(ui)
                    lesson.hints.take(ui.hintsShown).forEach { h ->
                        Text("💡 " + stringResource(h), style = MaterialTheme.typography.bodyMedium, color = MmeleColors.GoldBright)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (ui.coach == Coach.INTRO) {
                            Button(onClick = onBegin, colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate)) {
                                Text(stringResource(R.string.lesson_begin))
                            }
                        } else if (!ui.solved) {
                            if (ui.stage == Stage.LESSON && ui.hintsShown < lesson.hints.size) {
                                OutlinedButton(onClick = onHint, enabled = !ui.busy) {
                                    Text(stringResource(if (ui.hintsShown == 0) R.string.lesson_hint else R.string.lesson_next_hint))
                                }
                            }
                            TextButton(onClick = onRestart, enabled = !ui.busy) { Text(stringResource(R.string.lesson_restart)) }
                        }
                    }
                }
            }
            if (!landscape) {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    panel(Modifier.fillMaxWidth().weight(0.46f, fill = false))
                    board(Modifier.fillMaxWidth().weight(0.54f))
                }
            } else {
                Row(Modifier.fillMaxSize().padding(12.dp)) {
                    panel(Modifier.weight(1f).fillMaxSize())
                    board(Modifier.weight(1f).fillMaxSize())
                }
            }
        }
        AnimatedVisibility(ui.solved, enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f), modifier = Modifier.align(Alignment.Center)) {
            SuccessCard(lesson, ui, hasNext, onPractice, onNext, onBack, animate)
        }
    }
}

@Composable
private fun GoalCard(goal: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MmeleColors.SlateHigh.copy(alpha = 0.92f), RoundedCornerShape(14.dp))
            .border(1.dp, MmeleColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(stringResource(R.string.lesson_goal_label).uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.Gold)
        Text(goal, style = MaterialTheme.typography.bodyLarge, color = MmeleColors.Bone)
    }
}

@Composable
private fun CoachLine(ui: LessonUiState) {
    val res = when (ui.coach) {
        Coach.INTRO -> null
        Coach.YOUR_TURN -> R.string.lesson_your_turn
        Coach.SHOOT_NOW -> R.string.lesson_shoot_now
        Coach.GOOD -> R.string.lesson_good
        Coach.WRONG -> R.string.lesson_wrong
        Coach.WRONG_PUNISHED -> R.string.lesson_wrong_punished
        Coach.SOLVED -> null
    } ?: return
    val bad = ui.coach == Coach.WRONG || ui.coach == Coach.WRONG_PUNISHED
    Text(
        stringResource(res),
        style = MaterialTheme.typography.labelLarge,
        color = if (bad) MmeleColors.Slate else MmeleColors.Bone,
        modifier = Modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .background(if (bad) MmeleColors.Danger else MmeleColors.SlateHigh.copy(alpha = 0.9f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun SuccessCard(
    lesson: Lesson, ui: LessonUiState, hasNext: Boolean,
    onPractice: () -> Unit, onNext: () -> Unit, onBack: () -> Unit, animate: Boolean,
) {
    Column(
        Modifier
            .padding(24.dp)
            .widthIn(max = 420.dp)
            .background(MmeleColors.SlateRaised, RoundedCornerShape(24.dp))
            .border(1.5.dp, MmeleColors.Gold, RoundedCornerShape(24.dp))
            .padding(22.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Burst(animate)
        Text(stringResource(R.string.lesson_solved_title), style = MaterialTheme.typography.headlineMedium, color = MmeleColors.GoldBright)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (ui.readyForPractice) lesson.success else R.string.lesson_practice_done),
            style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Bone, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        if (ui.readyForPractice) {
            Button(onClick = onPractice, colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate)) {
                Text(stringResource(R.string.lesson_practice_cta))
            }
        } else {
            if (hasNext) {
                Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate)) {
                    Text(stringResource(R.string.lesson_next))
                }
            }
            TextButton(onClick = onBack) { Text(stringResource(R.string.lesson_back_to_academy), color = MmeleColors.Bone) }
        }
    }
}

/** A small ring of gold cows bursting outwards: the lesson's celebration. */
@Composable
private fun Burst(animate: Boolean) {
    val anim = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(900)) }
    val t = anim.value
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            for (i in 0 until 12) {
                val a = i * (Math.PI * 2 / 12)
                val r = size.minDimension * 0.42f * t
                drawCircle(
                    if (i % 2 == 0) MmeleColors.GoldBright else MmeleColors.Gold.copy(alpha = 0.7f),
                    radius = size.minDimension * 0.04f,
                    center = Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat()),
                )
            }
        }
        PieceIcon(PieceStyle.Obsidian, Modifier.size(40.dp), elevation = 0.8f)
    }
}
