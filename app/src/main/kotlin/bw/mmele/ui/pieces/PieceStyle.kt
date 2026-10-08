package bw.mmele.ui.pieces

import androidx.compose.ui.graphics.Color
import bw.mmele.engine.Player

/**
 * How a side's cows look. All variants are drawn procedurally on Canvas (no photos).
 * Player ONE picks a "dark-ish" style and Player TWO a "light-ish" one (or any strong
 * contrast pairing) so the two sides stay immediately distinguishable.
 */
enum class PieceFamily(val title: String, val blurb: String) {
    PEBBLES("Classic pebbles", "Obsidian and river-quartz stones"),
    DIAMONDS("Rough diamonds", "Botswana's famed uncut crystals"),
    EGGS("Bird eggs", "Eggs of birds found in Botswana"),
}

/** A concrete look for one side. */
sealed class PieceStyle {
    abstract val family: PieceFamily
    abstract val shortName: String
    abstract val fullName: String
    /** Rough luminance 0..1 used to keep sides contrasting. */
    abstract val luminance: Float
    /** Accent colour for HUD chips / pip fills. */
    abstract val accent: Color
    abstract val isDark: Boolean
    /** Stable id stored in online rooms so both phones draw the same set. */
    val id: String get() = when (this) {
        is Pebble -> if (dark) "obsidian" else "quartz"
        is Diamond -> if (dark) "diamond-smoky" else "diamond-clear"
        is Egg -> "egg-" + kind.name.lowercase()
    }

    data class Pebble(val dark: Boolean) : PieceStyle() {
        override val family = PieceFamily.PEBBLES
        override val shortName = if (dark) "Obsidian" else "Quartz"
        override val fullName = if (dark) "Obsidian pebble" else "River-quartz pebble"
        override val luminance = if (dark) 0.18f else 0.88f
        override val accent = if (dark) Color(0xFF70757D) else Color(0xFFF0E6D2)
        override val isDark = dark
    }

    data class Diamond(val dark: Boolean) : PieceStyle() {
        override val family = PieceFamily.DIAMONDS
        override val shortName = if (dark) "Smoky" else "Clear"
        override val fullName = if (dark) "Smoky rough diamond" else "Clear rough diamond"
        override val luminance = if (dark) 0.28f else 0.92f
        override val accent = if (dark) Color(0xFFB8945A) else Color(0xFFE8F4FF)
        override val isDark = dark
    }

    data class Egg(val kind: EggKind) : PieceStyle() {
        override val family = PieceFamily.EGGS
        override val shortName = kind.shortName
        override val fullName = kind.fullName
        override val luminance = kind.luminance
        override val accent = kind.accent
        override val isDark = kind.luminance < 0.55f
    }

    companion object {
        val Obsidian = Pebble(true)
        val Quartz = Pebble(false)
        val SmokyDiamond = Diamond(true)
        val ClearDiamond = Diamond(false)

        val all: List<PieceStyle> = listOf(
            Obsidian, Quartz, SmokyDiamond, ClearDiamond,
        ) + EggKind.entries.map { Egg(it) }

        fun fromId(id: String?): PieceStyle? = all.firstOrNull { it.id == id }

        fun defaultFor(player: Player): PieceStyle = if (player == Player.ONE) Obsidian else Quartz

        /** True when the two styles are easy to tell apart (luminance gap or different families). */
        fun contrastOk(a: PieceStyle, b: PieceStyle): Boolean {
            if (a == b) return false
            if (kotlin.math.abs(a.luminance - b.luminance) >= 0.28f) return true
            // Same-family eggs that share a similar tone (e.g. ostrich + roller) need a hard veto.
            return a.family != b.family && kotlin.math.abs(a.luminance - b.luminance) >= 0.18f
        }
    }
}

/**
 * Real eggs of birds that occur in Botswana. Only eggs whose appearance is confirmed by
 * natural-history sources are included — see docs/PIECES.md.
 */
enum class EggKind(
    val shortName: String,
    val fullName: String,
    val bird: String,
    val luminance: Float,
    val accent: Color,
) {
    OSTRICH("Ostrich", "Ostrich egg", "Struthio camelus", 0.86f, Color(0xFFF2E6C8)),
    GUINEAFOWL("Guineafowl", "Helmeted guineafowl egg", "Numida meleagris", 0.72f, Color(0xFFE8D5B0)),
    CROWNED_LAPWING("Lapwing", "Crowned lapwing egg", "Vanellus coronatus", 0.42f, Color(0xFF9A7A3A)),
    LILAC_ROLLER("Roller", "Lilac-breasted roller egg", "Coracias caudatus", 0.95f, Color(0xFFF8F8F4)),
    KORI_BUSTARD("Kori", "Kori bustard egg", "Ardeotis kori", 0.38f, Color(0xFF7A6A3A)),
}
