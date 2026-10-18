package bw.mmele.ui.home

import androidx.compose.ui.res.stringResource
import bw.mmele.AppLanguage
import bw.mmele.ui.game.GameRecord
import bw.mmele.ui.labels.blurbRes
import bw.mmele.ui.labels.fact
import bw.mmele.ui.labels.label
import bw.mmele.ui.labels.shortLabel
import bw.mmele.ui.labels.shortRes
import bw.mmele.ui.labels.title
import bw.mmele.ui.labels.titleRes
import com.tshipiworks.mmele.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bw.mmele.engine.Boards
import bw.mmele.engine.Player
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.board.BoardCanvas
import bw.mmele.ui.scenes.Scene
import bw.mmele.ui.scenes.SceneBackdrop
import bw.mmele.ui.scenes.SceneMode
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.game.PieceIcon
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.pieces.PieceFamily
import bw.mmele.ui.game.TokenUi
import bw.mmele.ui.theme.MmeleColors

@Composable
fun HomeScreen(
    config: GameConfig,
    onConfigChange: (GameConfig) -> Unit,
    soundOn: Boolean,
    hapticsOn: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onPlayOnline: () -> Unit = {},
    onAbout: () -> Unit = {},
    onlineConfigured: Boolean = true,
    animate: Boolean = true,
    /** In-progress offline game that can be continued, if any. */
    savedGame: GameRecord? = null,
    onContinue: () -> Unit = {},
    lessonsDone: Int = 0,
    lessonsTotal: Int = 5,
    onAcademy: () -> Unit = {},
    onStats: () -> Unit = {},
    onMoruba: () -> Unit = {},
    onHowToPlay: () -> Unit = {},
    language: AppLanguage = AppLanguage.SYSTEM,
    onLanguage: (AppLanguage) -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        // Botswana scene at full strength, with a gradient that only darkens where controls sit.
        SceneBackdrop(config.scene, SceneMode.FULL, animate = animate)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.30f to MmeleColors.Slate.copy(alpha = 0.10f),
                    0.55f to MmeleColors.Slate.copy(alpha = 0.72f),
                    1f to MmeleColors.Slate.copy(alpha = 0.92f),
                ),
            ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PieceIcon(config.styleOne, Modifier.height(34.dp).aspectRatio(1f))
                    Text("Mmele", style = MaterialTheme.typography.displayLarge.copy(shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 4f), 16f)), color = MmeleColors.Bone, modifier = Modifier.padding(horizontal = 10.dp))
                    PieceIcon(config.styleTwo, Modifier.height(34.dp).aspectRatio(1f), pieceId = 7)
                }
                Text(
                    stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.labelSmall,
                    color = MmeleColors.Gold,
                )
                Spacer(Modifier.height(14.dp))
                SceneCaption(config.scene)
                Spacer(Modifier.height(18.dp))
                if (savedGame != null) {
                    ContinueCard(savedGame, onContinue)
                    Spacer(Modifier.height(18.dp))
                }

                SectionLabel(stringResource(R.string.home_section_board))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VariantCard(
                        title = stringResource(R.string.variant_classic),
                        subtitle = stringResource(R.string.variant_classic_sub),
                        variant = Variant.CLASSIC,
                        selected = config.variant == Variant.CLASSIC,
                        animate = animate,
                        styleOne = config.styleOne, styleTwo = config.styleTwo,
                        modifier = Modifier.weight(1f),
                    ) { onConfigChange(config.copy(variant = Variant.CLASSIC)) }
                    VariantCard(
                        title = stringResource(R.string.variant_triangle),
                        subtitle = stringResource(R.string.variant_triangle_sub),
                        variant = Variant.TRIANGLE,
                        selected = config.variant == Variant.TRIANGLE,
                        animate = animate,
                        styleOne = config.styleOne, styleTwo = config.styleTwo,
                        modifier = Modifier.weight(1f),
                    ) { onConfigChange(config.copy(variant = Variant.TRIANGLE)) }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel(stringResource(R.string.home_section_mode))
                // Order matters: vs Computer (default), online with a friend, then same-phone.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeOption(stringResource(R.string.mode_solo), stringResource(R.string.mode_solo_sub), config.mode == GameMode.SOLO) {
                        onConfigChange(config.copy(mode = GameMode.SOLO))
                    }
                    ModeOption(
                        stringResource(R.string.mode_online),
                        stringResource(if (onlineConfigured) R.string.mode_online_sub else R.string.mode_online_sub_off),
                        config.mode == GameMode.ONLINE,
                    ) {
                        onConfigChange(config.copy(mode = GameMode.ONLINE))
                    }
                    ModeOption(stringResource(R.string.mode_pass_and_play), stringResource(R.string.mode_pass_and_play_sub), config.mode == GameMode.PASS_AND_PLAY) {
                        onConfigChange(config.copy(mode = GameMode.PASS_AND_PLAY))
                    }
                }

                if (config.mode == GameMode.SOLO) {
                    Spacer(Modifier.height(12.dp))
                    Segmented(
                        options = Difficulty.entries.map { it.label() },
                        selectedIndex = config.difficulty.ordinal,
                    ) { onConfigChange(config.copy(difficulty = Difficulty.entries[it])) }
                    Spacer(Modifier.height(12.dp))
                    Segmented(
                        options = listOf(
                            stringResource(R.string.home_play_side_first, config.styleOne.shortLabel()),
                            stringResource(R.string.home_play_side, config.styleTwo.shortLabel()),
                        ),
                        selectedIndex = if (config.human == Player.ONE) 0 else 1,
                    ) { onConfigChange(config.copy(human = if (it == 0) Player.ONE else Player.TWO)) }
                }

                Spacer(Modifier.height(20.dp))
                if (config.mode != GameMode.ONLINE) {
                    SectionLabel(stringResource(R.string.home_section_clock))
                    val clocks = listOf(0, 3, 5, 10)
                    Segmented(
                        options = clocks.map { if (it == 0) stringResource(R.string.clock_off) else stringResource(R.string.clock_minutes, it) },
                        selectedIndex = clocks.indexOf(config.clockMinutes).coerceAtLeast(0),
                    ) { onConfigChange(config.copy(clockMinutes = clocks[it])) }
                    Spacer(Modifier.height(20.dp))
                }
                SectionLabel(stringResource(R.string.home_section_pieces))
                PieceSetPicker(config) { onConfigChange(it) }
                Spacer(Modifier.height(20.dp))
                SectionLabel(stringResource(R.string.home_section_scene))
                ScenePicker(config.scene) { onConfigChange(config.copy(scene = it)) }

                Spacer(Modifier.height(16.dp))
                ToggleRow(stringResource(R.string.home_sound), soundOn, onSoundChange)
                ToggleRow(stringResource(R.string.home_haptics), hapticsOn, onHapticsChange)
                Spacer(Modifier.height(12.dp))
                SectionLabel(stringResource(R.string.home_section_language))
                Segmented(
                    options = AppLanguage.entries.map { stringResource(it.labelRes) },
                    selectedIndex = language.ordinal,
                ) { onLanguage(AppLanguage.entries[it]) }

                Spacer(Modifier.height(20.dp))
                val online = config.mode == GameMode.ONLINE
                Button(
                    onClick = if (online) onPlayOnline else onPlay,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate),
                ) { Text(stringResource(if (online) R.string.home_find_room else R.string.home_play), style = MaterialTheme.typography.titleMedium) }

                Spacer(Modifier.height(14.dp))
                NavCard(
                    title = stringResource(R.string.home_academy),
                    subtitle = stringResource(R.string.home_academy_sub, lessonsDone.coerceAtMost(lessonsTotal), lessonsTotal),
                    onClick = onAcademy,
                )
                Spacer(Modifier.height(8.dp))
                NavCard(stringResource(R.string.home_moruba), stringResource(R.string.home_moruba_sub), onMoruba)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavCard(stringResource(R.string.home_how_to_play), null, onHowToPlay, Modifier.weight(1f))
                    NavCard(stringResource(R.string.home_stats), null, onStats, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.TextButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_about), style = MaterialTheme.typography.labelLarge, color = MmeleColors.GoldBright)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.home_footer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MmeleColors.BoneDim,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MmeleColors.BoneDim,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp),
    )
}

@Composable
private fun VariantCard(
    title: String,
    subtitle: String,
    variant: Variant,
    selected: Boolean,
    animate: Boolean,
    styleOne: PieceStyle,
    styleTwo: PieceStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val board = if (variant == Variant.CLASSIC) Boards.CLASSIC else Boards.TRIANGLE
    val demo = remember(variant) {
        if (variant == Variant.CLASSIC) {
            listOf(TokenUi(1, Player.ONE, 0), TokenUi(2, Player.ONE, 9), TokenUi(3, Player.TWO, 12), TokenUi(4, Player.TWO, 20), TokenUi(5, Player.ONE, 18))
        } else {
            // 7-point board: A, CL, C, CR, BL, BM, BR.
            listOf(TokenUi(1, Player.ONE, 0), TokenUi(2, Player.TWO, 2), TokenUi(3, Player.ONE, 4), TokenUi(4, Player.TWO, 6))
        }
    }
    Column(
        modifier
            .clip(shape)
            .background(if (selected) MmeleColors.SlateHigh else MmeleColors.SlateRaised.copy(alpha = 0.7f))
            .border(if (selected) 2.dp else 1.dp, if (selected) MmeleColors.Gold else MmeleColors.Bone.copy(alpha = 0.15f), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
    ) {
        BoardCanvas(
            board = board,
            tokens = demo,
            styleOne = styleOne,
            styleTwo = styleTwo,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            interactive = false,
            animate = animate,
            drawBackground = false,
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MmeleColors.Bone)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = if (selected) MmeleColors.GoldBright else MmeleColors.BoneDim)
    }
}

@Composable
internal fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MmeleColors.SlateRaised.copy(alpha = 0.85f))
            .border(1.dp, MmeleColors.Bone.copy(alpha = 0.12f), shape)
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) MmeleColors.Gold.copy(alpha = 0.18f) else Color.Transparent)
                    .border(if (sel) 1.dp else 0.dp, if (sel) MmeleColors.Gold.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(10.dp))
                    .selectable(selected = sel, role = Role.RadioButton) { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) MmeleColors.GoldBright else MmeleColors.Bone, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ModeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) MmeleColors.Gold.copy(alpha = 0.18f) else MmeleColors.SlateRaised.copy(alpha = 0.85f))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) MmeleColors.Gold.copy(alpha = 0.8f) else MmeleColors.Bone.copy(alpha = 0.12f), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .border(2.dp, if (selected) MmeleColors.GoldBright else MmeleColors.BoneDim, RoundedCornerShape(50))
                .padding(4.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) MmeleColors.GoldBright else Color.Transparent),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) MmeleColors.GoldBright else MmeleColors.Bone)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MmeleColors.Bone, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MmeleColors.Gold, checkedThumbColor = MmeleColors.Slate),
        )
    }
}

@Composable
private fun SceneCaption(scene: Scene) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MmeleColors.Slate.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .border(1.dp, MmeleColors.Bone.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(scene.title().uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.GoldBright)
        Text(scene.fact(), style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone)
    }
}

@Composable
private fun ScenePicker(selected: Scene, onSelect: (Scene) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Scene.entries.forEach { scene ->
            val sel = scene == selected
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier
                    .clip(shape)
                    .border(if (sel) 2.dp else 1.dp, if (sel) MmeleColors.Gold else MmeleColors.Bone.copy(alpha = 0.15f), shape)
                    .selectable(selected = sel, role = Role.RadioButton) { onSelect(scene) },
            ) {
                SceneBackdrop(scene, SceneMode.FULL, Modifier.height(64.dp).aspectRatio(1.5f), animate = false)
                Text(
                    stringResource(scene.shortRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sel) MmeleColors.GoldBright else MmeleColors.Bone,
                    modifier = Modifier.background(MmeleColors.SlateRaised).fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    maxLines = 1,
                )
            }
        }
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .clip(shape)
                .border(1.dp, MmeleColors.Bone.copy(alpha = 0.15f), shape)
                .background(MmeleColors.SlateRaised)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.home_surprise_action)) { onSelect(Scene.entries.filter { it != selected }.random()) }
                .height(64.dp + 30.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.home_surprise), style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone, textAlign = TextAlign.Center) }
    }
}


@Composable
private fun PieceSetPicker(config: GameConfig, onChange: (GameConfig) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PieceFamily.entries.forEach { family ->
            val selected = config.styleOne.family == family
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (selected) MmeleColors.Gold.copy(alpha = 0.12f) else MmeleColors.SlateRaised.copy(alpha = 0.85f))
                    .border(if (selected) 1.5.dp else 1.dp, if (selected) MmeleColors.Gold else MmeleColors.Bone.copy(alpha = 0.12f), shape)
                    .selectable(selected = selected, role = Role.RadioButton) {
                        val (one, two) = defaultPair(family)
                        onChange(config.copy(styleOne = one, styleTwo = two))
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (one, two) = if (selected) config.styleOne to config.styleTwo else defaultPair(family)
                PieceIcon(one, Modifier.size(36.dp))
                Spacer(Modifier.width(6.dp))
                PieceIcon(two, Modifier.size(36.dp), pieceId = 4)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(family.titleRes()), style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
                    Text(stringResource(family.blurbRes()), style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
                }
            }
            if (selected && family == PieceFamily.EGGS) {
                EggSidePicker(stringResource(R.string.home_dark_side), config.styleOne, dark = true) { onChange(config.copy(styleOne = it)) }
                EggSidePicker(stringResource(R.string.home_light_side), config.styleTwo, dark = false) { onChange(config.copy(styleTwo = it)) }
            }
        }
    }
}

private fun defaultPair(family: PieceFamily): Pair<PieceStyle, PieceStyle> = when (family) {
    PieceFamily.PEBBLES -> PieceStyle.Obsidian to PieceStyle.Quartz
    PieceFamily.DIAMONDS -> PieceStyle.SmokyDiamond to PieceStyle.ClearDiamond
    PieceFamily.EGGS -> PieceStyle.Egg(bw.mmele.ui.pieces.EggKind.KORI_BUSTARD) to PieceStyle.Egg(bw.mmele.ui.pieces.EggKind.OSTRICH)
}

@Composable
private fun EggSidePicker(label: String, current: PieceStyle, dark: Boolean, onPick: (PieceStyle) -> Unit) {
    val options = bw.mmele.ui.pieces.EggKind.entries.filter { (it.luminance < 0.55f) == dark }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim, modifier = Modifier.width(72.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { kind ->
                val style = PieceStyle.Egg(kind)
                val sel = current == style
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) MmeleColors.Gold.copy(alpha = 0.2f) else Color.Transparent)
                        .selectable(selected = sel, role = Role.RadioButton) { onPick(style) }
                        .padding(6.dp),
                ) {
                    PieceIcon(style, Modifier.size(32.dp), pieceId = kind.ordinal + 2)
                    Text(stringResource(kind.shortRes()), style = MaterialTheme.typography.labelSmall, color = if (sel) MmeleColors.GoldBright else MmeleColors.Bone)
                }
            }
        }
    }
}

@Composable
private fun NavCard(title: String, subtitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MmeleColors.SlateRaised.copy(alpha = 0.85f))
            .border(1.dp, MmeleColors.Gold.copy(alpha = 0.45f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim) }
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MmeleColors.Gold)
    }
}

@Composable
private fun ContinueCard(rec: GameRecord, onContinue: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val mode = when (rec.config.mode) {
        GameMode.SOLO -> stringResource(R.string.hud_computer, rec.config.difficulty.label())
        GameMode.PASS_AND_PLAY -> stringResource(R.string.mode_pass_and_play)
        GameMode.ONLINE -> stringResource(R.string.mode_online)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MmeleColors.Gold)
            .clickable(role = Role.Button, onClick = onContinue)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PieceIcon(rec.config.styleOne, Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.titleMedium, color = MmeleColors.Slate)
            Text(
                stringResource(R.string.home_continue_detail, rec.config.variant.label(), mode, rec.actions.size),
                style = MaterialTheme.typography.labelMedium,
                color = MmeleColors.Slate,
            )
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MmeleColors.Slate)
    }
}
