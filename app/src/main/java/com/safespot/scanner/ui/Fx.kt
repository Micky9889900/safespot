package com.safespot.scanner.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import kotlin.math.*
import kotlin.random.Random

/** Slowly drifting navy gradient. Drawn in drawBehind so it never recomposes the content. */
fun Modifier.animatedBg(t: State<Float>): Modifier = drawBehind {
    val a = t.value * 2 * PI.toFloat()
    val c1 = Offset(size.width * (.5f + .45f * cos(a)), size.height * (.25f + .2f * sin(a)))
    val c2 = Offset(size.width * (.5f - .45f * cos(a * .7f)), size.height * (.8f - .15f * sin(a * .7f)))
    drawRect(C.bgBrush)
    drawCircle(Brush.radialGradient(listOf(C.Blue.copy(alpha = .22f), Color.Transparent), c1, size.maxDimension * .55f), size.maxDimension * .55f, c1)
    drawCircle(Brush.radialGradient(listOf(C.Red.copy(alpha = .14f), Color.Transparent), c2, size.maxDimension * .5f), size.maxDimension * .5f, c2)
}
@Composable fun rememberBgPhase(): State<Float> = rememberInfiniteTransition(label = "bg").animateFloat(0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "p")

enum class FxKind { SNOW, SPARKLE, SHIELD, CONFETTI }

/** Result-screen particles. Fixed particle count, one Canvas, no per-particle state = no jank. */
@Composable
fun Particles(kind: FxKind, modifier: Modifier = Modifier.fillMaxSize(), durationMs: Int = 4000) {
    val n = when (kind) { FxKind.SNOW -> 40; FxKind.SPARKLE -> 30; FxKind.CONFETTI -> 70; FxKind.SHIELD -> 0 }
    val seeds = remember(kind) { List(n) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    val prog = remember(kind) { Animatable(0f) }
    LaunchedEffect(kind) { prog.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    val palette = listOf(C.Red, C.Amber, C.Green, C.Blue, C.Cyan, Color.White)
    Canvas(modifier) {
        val p = prog.value; if (p >= 1f) return@Canvas
        val fade = if (p > .75f) (1 - p) / .25f else 1f
        when (kind) {
            FxKind.SHIELD -> for (k in 0..2) { val q = ((p * 2.2f) + k / 3f) % 1f
                drawCircle(C.Green.copy(alpha = (1 - q) * .5f * fade), size.minDimension * (.15f + .5f * q), Offset(size.width / 2, size.height * .3f), style = Stroke(6f * (1 - q) + 1)) }
            FxKind.SNOW -> seeds.forEach { s -> val y = ((s[1] + p * (.6f + s[2])) % 1.1f) * size.height; val x = s[0] * size.width + 20 * sin(p * 12 + s[3] * 6)
                val r = 3f + 5f * s[2]; val col = Color(0xFFE0F2FF).copy(alpha = .85f * fade)
                for (arm in 0 until 3) rotate(arm * 60f + p * 90, Offset(x, y)) { drawLine(col, Offset(x - r, y), Offset(x + r, y), 2f) } }
            FxKind.SPARKLE -> seeds.forEach { s -> val tw = (sin((p * 8 + s[3]) * 2 * PI.toFloat() * (0.5f + s[2])) + 1) / 2
                val x = s[0] * size.width; val y = s[1] * size.height * .7f; val r = (4 + 8 * s[2]) * tw; val col = (if (s[3] > .5f) C.Amber else Color.White).copy(alpha = tw * fade)
                drawLine(col, Offset(x - r, y), Offset(x + r, y), 2.5f); drawLine(col, Offset(x, y - r), Offset(x, y + r), 2.5f) }
            FxKind.CONFETTI -> seeds.forEach { s -> val vx = (s[0] - .5f) * 1.6f; val vy = -1.1f - s[1]
                val t = p * 1.6f; val x = size.width * (.5f + vx * t * .5f); val y = size.height * (.35f + (vy * t + 1.6f * t * t) * .35f)
                rotate(p * 720 * (s[2] - .5f), Offset(x, y)) { drawRect(palette[(s[3] * palette.size).toInt() % palette.size].copy(alpha = fade), Offset(x - 5, y - 3), androidx.compose.ui.geometry.Size(10f, 6f)) } }
        }
    }
}

/** Counter that rolls up to its target. */
@Composable
fun AnimatedCount(target: Float, format: (Float) -> String, style: TextStyle, color: Color = C.Ink, ms: Int = 1200) {
    val v by animateFloatAsState(target, tween(ms, easing = FastOutSlowInEasing), label = "count")
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    Text(format(if (started) v else 0f), style = style, color = color)
}

@Composable fun rememberHaptic(): (Boolean) -> Unit { val h = LocalHapticFeedback.current
    return remember(h) { { strong -> runCatching { h.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove) } } } }
