package com.safespot.scanner.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun Glass(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, pad: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.clip(shape)
        .background(if (C.dark) Brush.linearGradient(listOf(C.Glass.copy(alpha = C.Glass.alpha * 1.5f), C.Glass.copy(alpha = C.Glass.alpha * .5f))) else Brush.linearGradient(listOf(C.Glass, C.Glass)))
        .border(1.dp, Brush.linearGradient(listOf(C.GlassBorder, C.GlassBorder.copy(alpha = .05f))), shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(pad), content = content)
}

@Composable fun Title(t: String, modifier: Modifier = Modifier) = Text(t, modifier, color = C.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
@Composable fun Section(t: String) = Text(t.uppercase(), Modifier.padding(top = 18.dp, bottom = 8.dp), color = C.SilverDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
@Composable fun Body(t: String, color: Color = C.Silver, size: Int = 14) = Text(t, color = color, fontSize = size.sp, lineHeight = (size + 6).sp)

@Composable
fun Pill(text: String, color: Color) = Text(text, Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .18f)).padding(horizontal = 10.dp, vertical = 3.dp),
    color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, color: Color = C.Red, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(if (enabled) Brush.horizontalGradient(listOf(color, color.copy(alpha = .7f))) else SolidColor(C.SilverDim.copy(alpha = .3f)))
        .clickable(enabled) { onClick() }.padding(vertical = 14.dp, horizontal = 18.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = C.Silver, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, color.copy(alpha = .5f), RoundedCornerShape(12.dp)).clickable { onClick() }
        .padding(vertical = 9.dp, horizontal = 14.dp), contentAlignment = Alignment.Center) { Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

/** Animated security score ring. */
@Composable
fun ScoreRing(score: Int?, size: Dp = 200.dp, label: String = "Security score") {
    val target = (score ?: 0) / 100f
    val anim by animateFloatAsState(target, tween(1400, easing = FastOutSlowInEasing), label = "ring")
    val col = if (score == null) C.SilverDim else C.score(score)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = this.size.minDimension * .085f
            val inset = sw / 2
            val arc = Size(this.size.width - sw, this.size.height - sw)
            drawArc(C.SilverDim.copy(alpha = .2f), 135f, 270f, false, Offset(inset, inset), arc, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(col.copy(alpha = .6f), col, col)), 135f, 270f * anim, false, Offset(inset, inset), arc, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (score == null) "--" else "${(anim * 100).toInt()}", color = C.Ink, fontSize = (size.value * .26f).sp, fontWeight = FontWeight.Black)
            Text(label, color = C.SilverDim, fontSize = 12.sp)
            if (score != null) Text(when { score >= 80 -> "Protected"; score >= 55 -> "Needs attention"; else -> "At risk" }, color = col, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) = Glass(modifier, pad = 14.dp) {
    Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
    Text(label, color = C.SilverDim, fontSize = 12.sp)
}

/** Radar sweep animation used while scanning. */
@Composable
fun Radar(progress: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "radar")
    val ang by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "a")
    val blips = remember { List(9) { Pair((it * 41 % 360).toFloat(), .25f + (it * 37 % 70) / 100f) } }
    Canvas(modifier) {
        val r = size.minDimension / 2; val c = center
        for (i in 1..4) drawCircle(C.Cyan.copy(alpha = .18f), r * i / 4, c, style = Stroke(1.5f))
        drawLine(C.Cyan.copy(alpha = .15f), Offset(c.x - r, c.y), Offset(c.x + r, c.y)); drawLine(C.Cyan.copy(alpha = .15f), Offset(c.x, c.y - r), Offset(c.x, c.y + r))
        rotate(ang, c) { drawArc(Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, C.Cyan.copy(alpha = .55f)), c), 270f, 90f, true, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r)) }
        blips.take((progress * blips.size).toInt()).forEach { (a, d) ->
            val rad = Math.toRadians(a.toDouble()); val fade = 1f - (((ang - a + 360) % 360) / 360f)
            drawCircle(C.Cyan.copy(alpha = .3f + .7f * fade), 5f, Offset(c.x + (cos(rad) * r * d).toFloat(), c.y + (sin(rad) * r * d).toFloat()))
        }
        drawArc(C.Green, -90f, 360f * progress, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(6f, cap = StrokeCap.Round))
    }
}

/** Donut of risk levels. */
@Composable
fun Donut(parts: List<Pair<Float, Color>>, center: String, sub: String, modifier: Modifier = Modifier.size(150.dp)) {
    val anim by animateFloatAsState(1f, tween(1000), label = "d")
    val total = max(parts.sumOf { it.first.toDouble() }.toFloat(), 1f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.minDimension * .16f; var start = -90f
            val tl = Offset(sw / 2, sw / 2); val s = Size(size.width - sw, size.height - sw)
            drawArc(C.SilverDim.copy(alpha = .2f), 0f, 360f, false, tl, s, style = Stroke(sw))
            parts.forEach { (v, col) -> val sweep = 360f * v / total * anim; if (sweep > 0) drawArc(col, start, max(sweep - 1.5f, .5f), false, tl, s, style = Stroke(sw)); start += sweep }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(center, color = C.Ink, fontSize = 24.sp, fontWeight = FontWeight.Black); Text(sub, color = C.SilverDim, fontSize = 11.sp) }
    }
}

/** Horizontal bars. */
@Composable
fun Bars(items: List<Pair<String, Int>>, color: Color = C.Blue) {
    val mx = max(items.maxOfOrNull { it.second } ?: 1, 1)
    items.forEach { (l, v) ->
        val a by animateFloatAsState(v.toFloat() / mx, tween(900), label = l)
        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(l, Modifier.width(118.dp), color = C.Silver, fontSize = 12.sp, maxLines = 1)
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(C.SilverDim.copy(alpha = .15f))) {
                Box(Modifier.fillMaxWidth(a).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(color.copy(alpha = .6f), color))))
            }
            Text("$v", Modifier.width(34.dp).padding(start = 6.dp), color = C.Ink, fontSize = 12.sp)
        }
    }
}

/** Line chart (score timeline, speed graph). */
@Composable
fun LineChart(values: List<Float>, color: Color, modifier: Modifier = Modifier.fillMaxWidth().height(120.dp), fixedMax: Float? = null, second: List<Float>? = null, secondColor: Color = C.Red) {
    Canvas(modifier) {
        for (i in 0..3) drawLine(C.SilverDim.copy(alpha = .15f), Offset(0f, size.height * i / 3), Offset(size.width, size.height * i / 3))
        val mx = fixedMax ?: max((values + (second ?: emptyList())).maxOrNull() ?: 1f, 1f)
        fun draw(v: List<Float>, col: Color, fill: Boolean) {
            if (v.size < 2) { v.firstOrNull()?.let { drawCircle(col, 6f, Offset(size.width / 2, size.height * (1 - it / mx))) }; return }
            val step = size.width / (v.size - 1)
            val p = Path(); v.forEachIndexed { i, y -> val pt = Offset(i * step, size.height * (1 - y / mx)); if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y) }
            if (fill) { val f = Path().apply { addPath(p); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                drawPath(f, Brush.verticalGradient(listOf(col.copy(alpha = .35f), Color.Transparent))) }
            drawPath(p, col, style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        draw(values, color, true); second?.let { draw(it, secondColor, false) }
    }
}

fun fmtBytes(b: Long): String = when { b < 1024 -> "$b B"; b < 1 shl 20 -> "%.1f KB".format(b / 1024f); b < 1 shl 30 -> "%.1f MB".format(b / 1048576f); else -> "%.2f GB".format(b / 1073741824f) }
fun fmtAgo(t: Long): String { if (t == 0L) return "never"; val m = (System.currentTimeMillis() - t) / 60000
    return when { m < 1 -> "just now"; m < 60 -> "$m min ago"; m < 1440 -> "${m / 60} h ago"; else -> "${m / 1440} d ago" } }
