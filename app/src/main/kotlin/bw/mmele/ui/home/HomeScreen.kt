package bw.mmele.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import bw.mmele.ui.game.PebbleIcon
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
    animate: Boolean = true,
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
                    PebbleIcon(Player.ONE, Modifier.height(34.dp).aspectRatio(1f))
                    Text("Mmele", style = MaterialTheme.typography.displayLarge.copy(shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 4f), 16f)), color = MmeleColors.Bone, modifier = Modifier.padding(horizontal = 10.dp))
                    PebbleIcon(Player.TWO, Modifier.height(34.dp).aspectRatio(1f))
                }
                Text(
                    "MORABARABA · THE CATTLE GAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MmeleColors.Gold,
                )
                Spacer(Modifier.height(14.dp))
                SceneCaption(config.scene)
                Spacer(Modifier.height(18.dp))

                SectionLabel("Board")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VariantCard(
                        title = "Classic",
                        subtitle = "12 cows · mills shoot",
                        variant = Variant.CLASSIC,
                        selected = config.variant == Variant.CLASSIC,
                        animate = animate,
                        modifier = Modifier.weight(1f),
                    ) { onConfigChange(config.copy(variant = Variant.CLASSIC)) }
                    VariantCard(
                        title = "Khutlotharo",
                        subtitle = "Triangle · 4 cows · line wins",
                        variant = Variant.TRIANGLE,
                        selected = config.variant == Variant.TRIANGLE,
                        animate = animate,
                        modifier = Modifier.weight(1f),
                    ) { onConfigChange(config.copy(variant = Variant.TRIANGLE)) }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Mode")
                Segmented(
                    options = listOf("Pass & Play", "Solo vs AI"),
                    selectedIndex = if (config.mode == GameMode.PASS_AND_PLAY) 0 else 1,
                ) { onConfigChange(config.copy(mode = if (it == 0) GameMode.PASS_AND_PLAY else GameMode.SOLO)) }

                if (config.mode == GameMode.SOLO) {
                    Spacer(Modifier.height(12.dp))
                    Segmented(
                        options = listOf("Easy", "Medium", "Hard"),
                        selectedIndex = config.difficulty.ordinal,
                    ) { onConfigChange(config.copy(difficulty = Difficulty.entries[it])) }
                    Spacer(Modifier.height(12.dp))
                    Segmented(
                        options = listOf("Play Obsidian (first)", "Play Quartz"),
                        selectedIndex = if (config.human == Player.ONE) 0 else 1,
                    ) { onConfigChange(config.copy(human = if (it == 0) Player.ONE else Player.TWO)) }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Clock per player")
                val clocks = listOf(0, 3, 5, 10)
                Segmented(
                    options = listOf("Off", "3 min", "5 min", "10 min"),
                    selectedIndex = clocks.indexOf(config.clockMinutes).coerceAtLeast(0),
                ) { onConfigChange(config.copy(clockMinutes = clocks[it])) }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Scene · Botswana")
                ScenePicker(config.scene) { onConfigChange(config.copy(scene = it)) }

                Spacer(Modifier.height(16.dp))
                ToggleRow("Sound", soundOn, onSoundChange)
                ToggleRow("Haptics", hapticsOn, onHapticsChange)

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MmeleColors.Gold, contentColor = MmeleColors.Slate),
                ) { Text("Tshameka · Play", style = MaterialTheme.typography.titleMedium) }

                Spacer(Modifier.height(14.dp))
                // Academy is milestone 2.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .alpha(0.45f)
                        .border(1.dp, MmeleColors.Bone.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Academy", style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
                        Text("Four guided puzzles — coming soon", style = MaterialTheme.typography.labelMedium, color = MmeleColors.BoneDim)
                    }
                    Text("SOON", style = MaterialTheme.typography.labelSmall, color = MmeleColors.Gold)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Mmele (Setswana) is the Morabaraba family of mill games played across Botswana and Southern Africa.",
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val board = if (variant == Variant.CLASSIC) Boards.CLASSIC else Boards.TRIANGLE
    val demo = remember(variant) {
        if (variant == Variant.CLASSIC) {
            listOf(TokenUi(1, Player.ONE, 0), TokenUi(2, Player.ONE, 9), TokenUi(3, Player.TWO, 12), TokenUi(4, Player.TWO, 20), TokenUi(5, Player.ONE, 18))
        } else {
            listOf(TokenUi(1, Player.ONE, 0), TokenUi(2, Player.TWO, 5), TokenUi(3, Player.ONE, 7), TokenUi(4, Player.TWO, 3))
        }
    }
    Column(
        modifier
            .clip(shape)
            .background(if (selected) MmeleColors.SlateHigh else MmeleColors.SlateRaised.copy(alpha = 0.7f))
            .border(if (selected) 2.dp else 1.dp, if (selected) MmeleColors.Gold else MmeleColors.Bone.copy(alpha = 0.15f), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
    ) {
        BoardCanvas(
            board = board,
            tokens = demo,
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
private fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
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
                    .clickable(role = Role.RadioButton) { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) MmeleColors.GoldBright else MmeleColors.Bone, textAlign = TextAlign.Center)
            }
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
        Text(scene.title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MmeleColors.GoldBright)
        Text(scene.fact, style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone)
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
                    .clickable(role = Role.RadioButton) { onSelect(scene) },
            ) {
                SceneBackdrop(scene, SceneMode.FULL, Modifier.height(64.dp).aspectRatio(1.5f), animate = false)
                Text(
                    scene.title.substringBefore(" National").substringBefore(" Delta").substringBefore(" Pans").substringBefore(" Hills"),
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
                .clickable { onSelect(Scene.entries.filter { it != selected }.random()) }
                .height(64.dp + 30.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Surprise\nme", style = MaterialTheme.typography.labelMedium, color = MmeleColors.Bone, textAlign = TextAlign.Center) }
    }
}
