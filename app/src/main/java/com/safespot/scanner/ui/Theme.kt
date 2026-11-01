package com.safespot.scanner.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.safespot.scanner.scan.Level
import com.safespot.scanner.scan.Risk

/** One full colour scheme. ink/inkSoft/inkDim are text colours chosen for >= 4.5:1 contrast on that theme's cards. */
data class Palette(
    val id: String, val name: String, val dark: Boolean,
    val top: Color, val mid: Color, val bottom: Color,     // background gradient
    val bar: Color,                                         // nav bar / bubble surface
    val glass: Color, val glassBorder: Color,               // card tint
    val ink: Color, val inkSoft: Color, val inkDim: Color,  // text
    val accent: Color, val accent2: Color,                  // primary actions / charts
    val red: Color = Color(0xFFE53950), val amber: Color = Color(0xFFFFB020), val green: Color = Color(0xFF2BD98B),
    val blue: Color = Color(0xFF4C8DFF), val cyan: Color = Color(0xFF3FD5FF),
)

object Themes {
    val navy = Palette("navy", "Midnight Navy", true, Color(0xFF0B2A6B), Color(0xFF061536), Color(0xFF070B16), Color(0xFF061536),
        Color(0x1AFFFFFF), Color(0x33C9D3E3), Color.White, Color(0xFFC9D3E3), Color(0xFF8D9AB0), Color(0xFFE53950), Color(0xFF4C8DFF))
    val light = Palette("light", "Pure Light", false, Color(0xFFF4F7FC), Color(0xFFEDF1F8), Color(0xFFFFFFFF), Color(0xFFFFFFFF),
        Color(0xCCFFFFFF), Color(0x220B2A6B), Color(0xFF0E1B33), Color(0xFF33405A), Color(0xFF5A6680), Color(0xFFD12C43), Color(0xFF2563EB),
        red = Color(0xFFC81E36), amber = Color(0xFFB86E00), green = Color(0xFF0F8A55), blue = Color(0xFF2563EB), cyan = Color(0xFF0A7FA6))
    val amoled = Palette("amoled", "AMOLED Black", true, Color.Black, Color.Black, Color.Black, Color(0xFF050505),
        Color(0x14FFFFFF), Color(0x26FFFFFF), Color.White, Color(0xFFCFCFCF), Color(0xFF8E8E8E), Color(0xFFE53950), Color(0xFF4C8DFF))
    val frost = Palette("frost", "Frost Blue", true, Color(0xFF0E4D7A), Color(0xFF0A2F52), Color(0xFF061A30), Color(0xFF0A2F52),
        Color(0x1FBFE9FF), Color(0x4D9EDCFF), Color.White, Color(0xFFD3ECFA), Color(0xFF93B6CC), Color(0xFF3FD5FF), Color(0xFF7AA7FF))
    val rose = Palette("rose", "Rose Pink", true, Color(0xFF6B1B4A), Color(0xFF3D0F2E), Color(0xFF1C0716), Color(0xFF3D0F2E),
        Color(0x1FFFD6EC), Color(0x40FFB3D9), Color.White, Color(0xFFF6D6E8), Color(0xFFC293AE), Color(0xFFFF4FA3), Color(0xFFB98CFF))
    val emerald = Palette("emerald", "Emerald", true, Color(0xFF0B5A45), Color(0xFF07382C), Color(0xFF041C17), Color(0xFF07382C),
        Color(0x1FCFFFEF), Color(0x4D8EF0CC), Color.White, Color(0xFFD1F2E6), Color(0xFF8EBDAB), Color(0xFF2BD98B), Color(0xFF3FD5FF))
    val all = listOf(navy, light, amoled, frost, rose, emerald)
    const val SYSTEM = "system"
    fun pref(c: Context) = c.getSharedPreferences("ui", 0).getString("theme", "navy") ?: "navy"
    fun save(c: Context, id: String) = c.getSharedPreferences("ui", 0).edit().putString("theme", id).apply()
    var selected by mutableStateOf("navy")
    fun resolve(id: String, systemDark: Boolean) = if (id == SYSTEM) (if (systemDark) navy else light) else all.firstOrNull { it.id == id } ?: navy
}

/** Colour accessors. Backed by Compose state so a theme change recomposes every screen. */
object C {
    var pal by mutableStateOf(Themes.navy)
    val dark get() = pal.dark
    val Navy get() = pal.top; val NavyDeep get() = pal.bar; val Bg get() = pal.bottom
    val Ink get() = pal.ink; val Silver get() = pal.inkSoft; val SilverDim get() = pal.inkDim
    val Red get() = pal.red; val Amber get() = pal.amber; val Green get() = pal.green; val Blue get() = pal.blue; val Cyan get() = pal.cyan
    val Accent get() = pal.accent; val Accent2 get() = pal.accent2
    val Glass get() = pal.glass; val GlassBorder get() = pal.glassBorder
    val bgBrush get() = Brush.verticalGradient(listOf(pal.top, pal.mid, pal.bottom))
    fun score(s: Int) = when { s >= 80 -> Green; s >= 55 -> Amber; else -> Red }
    fun risk(r: Risk) = when (r) { Risk.SAFE -> Green; Risk.LOW -> Blue; Risk.MEDIUM -> Amber; Risk.HIGH -> Red; Risk.CRITICAL -> Color(0xFFE0103A) }
    fun level(l: Level) = when (l) { Level.GOOD -> Green; Level.WARN -> Amber; Level.BAD -> Red; Level.INFO -> SilverDim }
}

@Composable private fun ac(c: Color) = animateColorAsState(c, tween(450), label = "theme").value

@Composable
fun SafeSpotTheme(content: @Composable () -> Unit) {
    val t = Themes.resolve(Themes.selected, isSystemInDarkTheme())
    // Smooth crossfade: every colour animates to the new theme.
    val p = t.copy(top = ac(t.top), mid = ac(t.mid), bottom = ac(t.bottom), bar = ac(t.bar), glass = ac(t.glass), glassBorder = ac(t.glassBorder),
        ink = ac(t.ink), inkSoft = ac(t.inkSoft), inkDim = ac(t.inkDim), accent = ac(t.accent), accent2 = ac(t.accent2),
        red = ac(t.red), amber = ac(t.amber), green = ac(t.green), blue = ac(t.blue), cyan = ac(t.cyan))
    C.pal = p
    val scheme = if (t.dark) darkColorScheme(primary = p.accent2, secondary = p.inkSoft, tertiary = p.accent, background = p.bottom, surface = p.bar,
        onPrimary = Color.White, onBackground = p.ink, onSurface = p.ink, error = p.red)
    else lightColorScheme(primary = p.accent2, secondary = p.inkSoft, tertiary = p.accent, background = p.bottom, surface = p.bar,
        onPrimary = Color.White, onBackground = p.ink, onSurface = p.ink, error = p.red)
    MaterialTheme(colorScheme = scheme, content = content)
}
