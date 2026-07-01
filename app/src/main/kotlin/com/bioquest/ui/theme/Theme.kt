package com.bioquest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Retro terminal / sci-fi palette.
val TerminalBackground = Color(0xFF05090A)
val TerminalSurface = Color(0xFF0C1414)
val TerminalSurfaceAlt = Color(0xFF10201C)
val PhosphorGreen = Color(0xFF39FF14)
val PhosphorDim = Color(0xFF1E7A2B)
val Amber = Color(0xFFFFB000)
val AlertRed = Color(0xFFFF3B30)
val TerminalText = Color(0xFFB9F5C0)
val TerminalMuted = Color(0xFF5C8A66)

private val BioQuestColors = darkColorScheme(
    primary = PhosphorGreen,
    onPrimary = TerminalBackground,
    secondary = Amber,
    onSecondary = TerminalBackground,
    tertiary = PhosphorDim,
    background = TerminalBackground,
    onBackground = TerminalText,
    surface = TerminalSurface,
    onSurface = TerminalText,
    surfaceVariant = TerminalSurfaceAlt,
    onSurfaceVariant = TerminalMuted,
    error = AlertRed,
    onError = TerminalBackground,
    outline = PhosphorDim,
)

private val Mono = FontFamily.Monospace

private val BioQuestTypography = Typography(
    displaySmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 2.sp),
    titleLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.sp),
    titleMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp),
    bodyLarge = TextStyle(fontFamily = Mono, fontSize = 15.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = Mono, fontSize = 13.sp, letterSpacing = 0.5.sp),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.sp),
)

@Composable
fun BioQuestTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // BioQuest is intentionally always the dark terminal theme.
    MaterialTheme(
        colorScheme = BioQuestColors,
        typography = BioQuestTypography,
        content = content,
    )
}
