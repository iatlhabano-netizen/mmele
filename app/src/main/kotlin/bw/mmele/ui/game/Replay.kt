package bw.mmele.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bw.mmele.engine.Action
import bw.mmele.engine.GameState
import bw.mmele.engine.RuleSet
import bw.mmele.engine.Rules
import bw.mmele.ui.board.BoardCanvas
import bw.mmele.ui.scenes.SceneBackdrop
import bw.mmele.ui.scenes.SceneMode
import bw.mmele.ui.theme.MmeleColors
import com.tshipiworks.mmele.R

/** Every position of a game, from the empty board to after the last action. Pure, for tests. */
object ReplayModel {
    fun positions(config: GameConfig, moves: List<Action>): List<GameState> {
        val out = ArrayList<GameState>(moves.size + 1)
        var g = Rules.newGame(RuleSet.of(config.variant))
        out += g
        for (a in moves) {
            if (g.isGameOver || !Rules.isLegal(g, a)) break
            g = Rules.apply(g, a)
            out += g
        }
        return out
    }

    /** Board tokens for a position (ids by point: replay jumps, it doesn't animate slides). */
    fun tokens(g: GameState): List<TokenUi> =
        (0 until g.board.size).mapNotNull { n -> g.ownerAt(n)?.let { TokenUi(n + 1, it, n) } }
}

/** Step through a finished (or current) game move by move. */
@Composable
fun ReplayViewer(config: GameConfig, moves: List<Action>, onClose: () -> Unit, animate: Boolean = true, startAt: Int? = null) {
    val positions = remember(moves) { ReplayModel.positions(config, moves) }
    val last = positions.lastIndex
    var i by rememberSaveable { mutableIntStateOf(startAt ?: last) }
    val g = positions[i.coerceIn(0, last)]
    val action = if (i > 0) moves[i - 1] else null
    val mover = if (i > 0) positions[i - 1].toMove else null
    Box(Modifier.fillMaxSize().background(MmeleColors.Slate)) {
        SceneBackdrop(config.scene, SceneMode.DIMMED, animate = animate)
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.replay_title), style = MaterialTheme.typography.headlineSmall, color = MmeleColors.Bone, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text(stringResource(R.string.common_close), color = MmeleColors.GoldBright) }
            }
            Text(
                stringResource(R.string.replay_position, i, last),
                style = MaterialTheme.typography.labelLarge, color = MmeleColors.Gold,
            )
            val board = g.board
            val what = when {
                action == null || mover == null -> stringResource(R.string.replay_start_position)
                else -> {
                    val who = playerName(mover, config)
                    when (action) {
                        is Action.Place -> stringResource(R.string.replay_action_place, who, board.labels[action.to])
                        is Action.Move -> stringResource(R.string.replay_action_move, who, board.labels[action.from], board.labels[action.to])
                        is Action.Shoot -> stringResource(R.string.replay_action_shoot, who, board.labels[action.target])
                    }
                }
            }
            Text(
                what, style = MaterialTheme.typography.bodyMedium, color = MmeleColors.Bone, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
            BoardCanvas(
                board = board,
                tokens = ReplayModel.tokens(g),
                styleOne = config.styleOne,
                styleTwo = config.styleTwo,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                lastMove = when (action) {
                    is Action.Place -> LastMove(null, action.to)
                    is Action.Move -> LastMove(action.from, action.to)
                    else -> null
                },
                interactive = false,
                animate = false,
                drawBackground = false,
            )
            Slider(
                value = i.toFloat(),
                onValueChange = { i = it.toInt() },
                valueRange = 0f..maxOf(1, last).toFloat(),
                steps = maxOf(0, last - 1),
                enabled = last > 0,
                colors = SliderDefaults.colors(thumbColor = MmeleColors.Gold, activeTrackColor = MmeleColors.Gold),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StepButton("⏮", stringResource(R.string.replay_start), i > 0) { i = 0 }
                StepButton("◀", stringResource(R.string.replay_back), i > 0) { i-- }
                StepButton("▶", stringResource(R.string.replay_forward), i < last) { i++ }
                StepButton("⏭", stringResource(R.string.replay_end), i < last) { i = last }
            }
        }
    }
}

@Composable
private fun StepButton(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = label }) {
        Text(glyph, style = MaterialTheme.typography.titleLarge, color = if (enabled) MmeleColors.Bone else MmeleColors.BoneDim)
    }
}

