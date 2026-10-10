package bw.mmele.ui.labels

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import bw.mmele.engine.Phase
import bw.mmele.engine.Variant
import bw.mmele.engine.ai.Difficulty
import bw.mmele.ui.pieces.EggKind
import bw.mmele.ui.pieces.PieceFamily
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.scenes.Scene
import com.tshipiworks.mmele.R

/*
 * Every user-facing name of a model value, as a string resource. The enums keep their English
 * names only for docs and logs; the UI always goes through these.
 */

@StringRes fun Variant.labelRes(): Int = when (this) {
    Variant.CLASSIC -> R.string.variant_classic
    Variant.TRIANGLE -> R.string.variant_triangle
}

@StringRes fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
    Difficulty.EXPERT -> R.string.difficulty_expert
}

@StringRes fun Scene.titleRes(): Int = when (this) {
    Scene.OKAVANGO -> R.string.scene_okavango
    Scene.CHOBE -> R.string.scene_chobe
    Scene.MAKGADIKGADI -> R.string.scene_makgadikgadi
    Scene.KALAHARI -> R.string.scene_kalahari
    Scene.TSODILO -> R.string.scene_tsodilo
}

@StringRes fun Scene.shortRes(): Int = when (this) {
    Scene.OKAVANGO -> R.string.scene_okavango_short
    Scene.CHOBE -> R.string.scene_chobe_short
    Scene.MAKGADIKGADI -> R.string.scene_makgadikgadi_short
    Scene.KALAHARI -> R.string.scene_kalahari_short
    Scene.TSODILO -> R.string.scene_tsodilo_short
}

@StringRes fun Scene.factRes(): Int = when (this) {
    Scene.OKAVANGO -> R.string.scene_okavango_fact
    Scene.CHOBE -> R.string.scene_chobe_fact
    Scene.MAKGADIKGADI -> R.string.scene_makgadikgadi_fact
    Scene.KALAHARI -> R.string.scene_kalahari_fact
    Scene.TSODILO -> R.string.scene_tsodilo_fact
}

@StringRes fun PieceFamily.titleRes(): Int = when (this) {
    PieceFamily.PEBBLES -> R.string.family_pebbles
    PieceFamily.DIAMONDS -> R.string.family_diamonds
    PieceFamily.EGGS -> R.string.family_eggs
}

@StringRes fun PieceFamily.blurbRes(): Int = when (this) {
    PieceFamily.PEBBLES -> R.string.family_pebbles_blurb
    PieceFamily.DIAMONDS -> R.string.family_diamonds_blurb
    PieceFamily.EGGS -> R.string.family_eggs_blurb
}

@StringRes fun EggKind.shortRes(): Int = when (this) {
    EggKind.OSTRICH -> R.string.egg_ostrich
    EggKind.GUINEAFOWL -> R.string.egg_guineafowl
    EggKind.CROWNED_LAPWING -> R.string.egg_lapwing
    EggKind.LILAC_ROLLER -> R.string.egg_roller
    EggKind.KORI_BUSTARD -> R.string.egg_kori
}

@StringRes fun EggKind.fullRes(): Int = when (this) {
    EggKind.OSTRICH -> R.string.egg_ostrich_full
    EggKind.GUINEAFOWL -> R.string.egg_guineafowl_full
    EggKind.CROWNED_LAPWING -> R.string.egg_lapwing_full
    EggKind.LILAC_ROLLER -> R.string.egg_roller_full
    EggKind.KORI_BUSTARD -> R.string.egg_kori_full
}

@StringRes fun PieceStyle.shortRes(): Int = when (this) {
    is PieceStyle.Pebble -> if (dark) R.string.piece_obsidian else R.string.piece_quartz
    is PieceStyle.Diamond -> if (dark) R.string.piece_smoky else R.string.piece_clear
    is PieceStyle.Egg -> kind.shortRes()
}

@StringRes fun PieceStyle.fullRes(): Int = when (this) {
    is PieceStyle.Pebble -> if (dark) R.string.piece_obsidian_full else R.string.piece_quartz_full
    is PieceStyle.Diamond -> if (dark) R.string.piece_smoky_full else R.string.piece_clear_full
    is PieceStyle.Egg -> kind.fullRes()
}

/** Setswana game term for a phase: the same in every UI language (it is the name of the move). */
@StringRes fun Phase.termRes(): Int = when (this) {
    Phase.PLACEMENT -> R.string.phase_place_tn
    Phase.MOVEMENT -> R.string.phase_move_tn
    Phase.FLYING -> R.string.phase_fly_tn
    Phase.GAME_OVER -> R.string.phase_over_tn
}

/** Gloss of the phase in the UI language. */
@StringRes fun Phase.glossRes(): Int = when (this) {
    Phase.PLACEMENT -> R.string.phase_place
    Phase.MOVEMENT -> R.string.phase_move
    Phase.FLYING -> R.string.phase_fly
    Phase.GAME_OVER -> R.string.phase_over
}

@Composable fun Variant.label() = stringResource(labelRes())
@Composable fun Difficulty.label() = stringResource(labelRes())
@Composable fun PieceStyle.shortLabel() = stringResource(shortRes())
@Composable fun PieceStyle.fullLabel() = stringResource(fullRes())
@Composable fun Scene.title() = stringResource(titleRes())
@Composable fun Scene.fact() = stringResource(factRes())
