package bw.mmele.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** "Organic Savanna Minimalism" palette. */
object MmeleColors {
    val Slate = Color(0xFF121417)
    val SlateRaised = Color(0xFF1A1D21)
    val SlateHigh = Color(0xFF23272C)
    val Bone = Color(0xFFE2D9C8)
    // Opaque (was 60% Bone): ≥6:1 on every panel colour, and it stays readable over translucent scrims.
    val BoneDim = Color(0xFFB0A797)
    val Gold = Color(0xFFD9A441)
    val GoldBright = Color(0xFFF2C76B)
    val Danger = Color(0xFFE5735C)
    val Online = Color(0xFF6BCB77)
    val Ochre = Color(0xFFC0563A)
    val Sage = Color(0xFF8FA58A)

    // Pebbles
    val ObsidianLight = Color(0xFF70757D)
    val ObsidianMid = Color(0xFF2B2E33)
    val ObsidianDark = Color(0xFF0A0B0D)
    val QuartzLight = Color(0xFFFFFFFB)
    val QuartzMid = Color(0xFFF0E6D2)
    val QuartzDark = Color(0xFFBFAA86)
}

private val scheme = darkColorScheme(
    primary = MmeleColors.Gold,
    onPrimary = MmeleColors.Slate,
    secondary = MmeleColors.Sage,
    background = MmeleColors.Slate,
    onBackground = MmeleColors.Bone,
    surface = MmeleColors.SlateRaised,
    onSurface = MmeleColors.Bone,
    surfaceVariant = MmeleColors.SlateHigh,
    onSurfaceVariant = MmeleColors.BoneDim,
    error = MmeleColors.Ochre,
)

private val display = FontFamily.Serif

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = 56.sp, letterSpacing = 2.sp),
    headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.3.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.6.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.8.sp),
)

@Composable
fun MmeleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
