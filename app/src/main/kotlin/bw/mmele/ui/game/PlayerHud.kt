package bw.mmele.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bw.mmele.engine.GameState
import bw.mmele.engine.Phase
import bw.mmele.engine.Player
import bw.mmele.ui.board.drawPebble
import bw.mmele.ui.theme.MmeleColors

/** Small pebble glyph for HUDs and buttons. */
@Composable
fun PebbleIcon(player: Player, modifier: Modifier = Modifier.size(28.dp), elevation: Float = 0f) {
    Canvas(modifier) { drawPebble(Offset(size.width / 2, size.height / 2), size.minDimension * 0.36f, player, elevation) }
}

fun playerName(p: Player, config: GameConfig): String = when (config.mode) {
    GameMode.PASS_AND_PLAY -> if (p == Player.ONE) "Obsidian" else "Quartz"
    GameMode.SOLO -> if (p == config.human) "You" else "Computer · ${config.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}"
}

fun formatClock(ms: Long): String {
    val s = (ms + 999) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun PlayerHud(
    player: Player,
    game: GameState,
    config: GameConfig,
    clockMs: Long,
    thinking: Boolean,
    modifier: Modifier = Modifier,
) {
    val active = !game.isGameOver && game.toMove == player
    val borderColor by animateColorAsState(if (active) MmeleColors.Gold else Color.Transparent, tween(300), label = "hudBorder")
    val contentAlpha by animateFloatAsState(if (active || game.isGameOver) 1f else 0.6f, tween(300), label = "hudAlpha")
    val phase = game.phaseOf(player)
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(if (active) MmeleColors.SlateHigh.copy(alpha = 0.92f) else MmeleColors.SlateRaised.copy(alpha = 0.8f), shape)
            .border(1.5.dp, borderColor, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PebbleIcon(player, Modifier.size(40.dp), elevation = if (active && phase == Phase.FLYING) 0.7f else 0f)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(playerName(player, config), style = MaterialTheme.typography.titleMedium, color = MmeleColors.Bone)
            val phaseText = if (game.isGameOver) Phase.GAME_OVER else phase
            Text(
                "${phaseText.setswana} · ${phaseText.english}",
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MmeleColors.GoldBright else MmeleColors.BoneDim,
            )
            if (thinking) {
                LinearProgressIndicator(
                    modifier = Modifier.padding(top = 4.dp).width(96.dp),
                    color = MmeleColors.Gold,
                    trackColor = MmeleColors.SlateRaised,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            val hand = game.handOf(player)
            if (game.rules.cowsPerPlayer > 0 && (hand > 0 || phase == Phase.PLACEMENT)) {
                Text("In hand $hand", style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim)
                HandPips(player, hand, game.rules.cowsPerPlayer)
            } else {
                Text("On board ${game.onBoard(player)}", style = MaterialTheme.typography.labelSmall, color = MmeleColors.BoneDim)
            }
            if (game.rules.millEffect == bw.mmele.engine.MillEffect.SHOOT) {
                Text(
                    "Shot ${game.shotBy(player)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MmeleColors.Bone,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (config.clockMinutes > 0) {
            Spacer(Modifier.width(12.dp))
            val low = clockMs < 20_000
            Text(
                formatClock(clockMs),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Medium),
                color = when {
                    low && active -> MmeleColors.Ochre
                    active -> MmeleColors.GoldBright
                    else -> MmeleColors.BoneDim
                },
            )
        }
    }
}

@Composable
private fun HandPips(player: Player, hand: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 3.dp)) {
        val fill = if (player == Player.ONE) MmeleColors.ObsidianLight else MmeleColors.QuartzMid
        for (i in 0 until total) {
            Canvas(Modifier.size(6.dp)) {
                if (i < hand) drawCircle(fill) else drawCircle(MmeleColors.Bone.copy(alpha = 0.15f), radius = size.minDimension * 0.3f)
            }
        }
    }
}

