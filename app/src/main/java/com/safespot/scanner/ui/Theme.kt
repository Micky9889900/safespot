package com.safespot.scanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.safespot.scanner.scan.Level
import com.safespot.scanner.scan.Risk

object C {
    val Navy = Color(0xFF0B2A6B); val NavyDeep = Color(0xFF061536); val Bg = Color(0xFF070B16)
    val Silver = Color(0xFFC9D3E3); val SilverDim = Color(0xFF8592A8)
    val Red = Color(0xFFE53950); val Amber = Color(0xFFFFB020); val Green = Color(0xFF2BD98B); val Blue = Color(0xFF4C8DFF); val Cyan = Color(0xFF3FD5FF)
    val Glass = Color(0x1AFFFFFF); val GlassBorder = Color(0x33C9D3E3)
    val bgBrush get() = Brush.verticalGradient(listOf(Navy, NavyDeep, Bg))
    fun score(s: Int) = when { s >= 80 -> Green; s >= 55 -> Amber; else -> Red }
    fun risk(r: Risk) = when (r) { Risk.SAFE -> Green; Risk.LOW -> Blue; Risk.MEDIUM -> Amber; Risk.HIGH -> Red; Risk.CRITICAL -> Color(0xFFFF2D55) }
    fun level(l: Level) = when (l) { Level.GOOD -> Green; Level.WARN -> Amber; Level.BAD -> Red; Level.INFO -> SilverDim }
}

@Composable
fun SafeSpotTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(primary = C.Blue, secondary = C.Silver, tertiary = C.Red, background = C.Bg, surface = C.NavyDeep,
        onPrimary = Color.White, onBackground = Color.White, onSurface = Color.White, error = C.Red),
    content = content)
