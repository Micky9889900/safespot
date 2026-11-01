package com.safespot.scanner.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safespot.scanner.AppState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Genie poses. Art: res/drawable-nodpi/genie_{male|female}_{pose}.png. Missing poses fall back to *_idle + Compose effects. */
enum class Mood { IDLE, HAPPY, WORRIED, CHEER, SLEEP, POINT }

object GenieProfile {
    var gender by mutableStateOf("male"); var name by mutableStateOf("Jinn"); var chosen by mutableStateOf(false)
    fun defaultName(g: String) = if (g == "female") "Jinny" else "Jinn"
    fun load(c: android.content.Context) { val p = c.getSharedPreferences("genie", 0)
        gender = p.getString("gender", "male") ?: "male"; name = p.getString("name", defaultName(gender)) ?: defaultName(gender); chosen = p.getBoolean("chosen", false) }
    fun save(c: android.content.Context, g: String, n: String) { gender = g; name = n.trim().ifBlank { defaultName(g) }.take(16); chosen = true
        c.getSharedPreferences("genie", 0).edit().putString("gender", gender).putString("name", name).putBoolean("chosen", true).apply() }
    /** Resolves a pose drawable at runtime so new PNGs work without code changes. Returns (resId, isRealPose). */
    fun pose(c: android.content.Context, g: String, m: Mood): Pair<Int, Boolean> {
        val r = c.resources; fun id(n: String) = r.getIdentifier(n, "drawable", c.packageName)
        val exact = id("genie_${g}_${m.name.lowercase()}")
        return if (exact != 0) exact to true else id("genie_${g}_idle") to (m == Mood.IDLE)
    }
}

/** Picks the genie's mood + tip from real app state (never claims something that did not happen). */
fun genieFor(s: AppState, idleMs: Long): Pair<Mood, String> {
    s.genieEvent?.let { return it }
    val r = s.result
    return when {
        s.scanning -> Mood.POINT to "Scanning your apps… ${(s.progress * 100).toInt()}%"
        idleMs > 45_000 -> Mood.SLEEP to "Zzz… tap me when you need me"
        r != null && r.risky.isNotEmpty() -> Mood.WORRIED to "I found ${r.risky.size} risky app${if (r.risky.size > 1) "s" else ""}. Tap Scan to review!"
        s.tool != null -> Mood.POINT to toolTip(s.tool!!)
        s.tab == "vpn" -> Mood.IDLE to "Public Wi-Fi? Turn on the VPN first."
        s.tab == "tools" -> Mood.POINT to "Try Phone Care: Junk Cleaner is a good start."
        r == null && s.store.lastScan == 0L -> Mood.POINT to "Tap Scan to check for spyware!"
        (r?.score ?: 0) >= 80 -> Mood.HAPPY to "Score ${r!!.score}: looking safe! Try the Junk Cleaner next."
        else -> Mood.IDLE to "Tip: scan again after installing new apps."
    }
}
private fun toolTip(t: String) = when (t) {
    "junk" -> "Pick a folder and I'll find big files, duplicates and old APKs."
    "battery" -> "These are real battery readings from Android."
    "cooler" -> "Close heavy apps and I'll time the cool-down."
    "booster" -> "Let's see how much memory we can free."
    "wifi" -> "I'll check if this Wi-Fi is safe."
    "speed" -> "Measuring with Cloudflare's speed servers."
    else -> "Tap Run to start."
}

@Composable
fun GenieOverlay(s: AppState) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("genie", 0) }
    var minimized by remember { mutableStateOf(prefs.getBoolean("min", false)) }
    var off by remember { mutableStateOf(IntOffset(prefs.getInt("x", -1), prefs.getInt("y", -1))) }
    var box by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(5000); now = System.currentTimeMillis() } }
    LaunchedEffect(s.tab, s.tool, s.scanning, s.genieEvent) { lastTouch = System.currentTimeMillis() }
    LaunchedEffect(s.genieEvent) { if (s.genieEvent != null) { delay(6000); s.genieEvent = null } }
    val (mood, rawTip) = genieFor(s, now - lastTouch)
    val tip = "${GenieProfile.name} says: $rawTip"
    var showTip by remember { mutableStateOf(true) }
    LaunchedEffect(tip) { showTip = true; delay(7000); showTip = false }
    val d = LocalDensity.current
    val sizePx = with(d) { (if (minimized) 44.dp else 84.dp).roundToPx() }

    val inf = rememberInfiniteTransition(label = "genie")
    val bob by inf.animateFloat(-6f, 6f, infiniteRepeatable(tween(if (mood == Mood.SLEEP) 3200 else 1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val blink by inf.animateFloat(1f, 1f, infiniteRepeatable(keyframes { durationMillis = 4200; 1f at 3900; .1f at 4000; 1f at 4100 }), label = "blink")
    val cheerScale by animateFloatAsState(if (mood == Mood.CHEER) 1.15f else 1f, spring(dampingRatio = .35f), label = "cheer")

    Box(Modifier.fillMaxSize().onSizeChanged { box = it }) {
        if (box.width == 0) return@Box
        val x = (if (off.x < 0) box.width - sizePx - with(d) { 12.dp.roundToPx() } else off.x).coerceIn(0, maxOf(0, box.width - sizePx))
        val y = (if (off.y < 0) (box.height * .62f).toInt() else off.y).coerceIn(0, maxOf(0, box.height - sizePx))
        Column(Modifier.offset { IntOffset(x, y) }.width(with(d) { sizePx.toDp() }).pointerInput(sizePx) {
            detectDragGestures(onDragEnd = { prefs.edit().putInt("x", off.x).putInt("y", off.y).apply() }) { ch, drag ->
                ch.consume(); lastTouch = System.currentTimeMillis()
                off = IntOffset((x + drag.x).roundToInt().coerceIn(0, box.width - sizePx), (y + drag.y).roundToInt().coerceIn(0, box.height - sizePx)) }
        }, horizontalAlignment = Alignment.CenterHorizontally) {
            val (resId, real) = remember(GenieProfile.gender, mood) { GenieProfile.pose(ctx, GenieProfile.gender, mood) }
            Box(contentAlignment = Alignment.Center) {
                // Fallback expression effects when only the idle art exists.
                Art("fx_genie_aura", Modifier.size(with(d) { (sizePx * 1.35f).toDp() }), alpha = .8f)
                if (!real) GenieAura(mood, Modifier.size(with(d) { (sizePx * 1.3f).toDp() }))
                Image(painterResource(resId), "${GenieProfile.name}, your SafeSpot genie (${mood.name.lowercase()})",
                    Modifier.size(with(d) { sizePx.toDp() }).graphicsLayer {
                        translationY = bob + if (!real && mood == Mood.CHEER) -8f * kotlin.math.abs(kotlin.math.sin(bob / 3f)) else 0f
                        rotationZ = if (!real) when (mood) { Mood.WORRIED -> bob * .5f; Mood.SLEEP -> 8f; Mood.POINT -> -6f; else -> 0f } else 0f
                        alpha = if (!real && mood == Mood.SLEEP) .8f else 1f
                        scaleX = cheerScale; scaleY = cheerScale * (if (mood == Mood.SLEEP) 1f else (.94f + .06f * blink)) }
                        .clickable { lastTouch = System.currentTimeMillis(); if (minimized) { minimized = false; prefs.edit().putBoolean("min", false).apply() } else showTip = !showTip })
            }
            if (!minimized) Text("–", Modifier.clip(CircleShape).background(Color.Black.copy(alpha = .35f)).clickable { minimized = true; showTip = false; prefs.edit().putBoolean("min", true).apply() }
                .padding(horizontal = 10.dp), color = C.Silver, fontSize = 12.sp)
        }
        // Bubble placed to the side with more room; never overlaps the genie itself.
        AnimatedVisibility(showTip && !minimized, enter = fadeIn() + scaleIn(initialScale = .8f), exit = fadeOut(),
            modifier = Modifier.offset { val left = x > box.width / 2; IntOffset(if (left) maxOf(0, x - with(d) { 210.dp.roundToPx() }) else x + sizePx, maxOf(0, y - with(d) { 8.dp.roundToPx() })) }) {
            Text(tip, Modifier.widthIn(max = 200.dp).clip(RoundedCornerShape(14.dp)).background(C.NavyDeep.copy(alpha = .97f)).border(1.dp, C.GlassBorder, RoundedCornerShape(14.dp))
                .clickable { showTip = false }.padding(10.dp), color = C.Ink, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

/** Mood glow + particles drawn behind the idle art (zzz for sleep, sparkles for cheer, "!" for worried, arrow for point). */
@Composable
private fun GenieAura(mood: Mood, modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "aura").animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "t")
    val glow = when (mood) { Mood.HAPPY -> C.Green; Mood.WORRIED -> C.Amber; Mood.CHEER -> C.Cyan; Mood.SLEEP -> C.Blue; Mood.POINT -> C.Accent; Mood.IDLE -> C.Blue }
    androidx.compose.foundation.Canvas(modifier) {
        val c = center
        drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(glow.copy(alpha = .35f + .1f * kotlin.math.sin(t * 6.28f)), Color.Transparent), c, size.minDimension / 2), size.minDimension / 2, c)
        val paint = android.graphics.Paint().apply { isAntiAlias = true; textSize = size.minDimension * .16f; isFakeBoldText = true; color = android.graphics.Color.WHITE }
        when (mood) {
            Mood.SLEEP -> for (k in 0..2) { val q = (t + k / 3f) % 1f; paint.alpha = ((1 - q) * 255).toInt(); paint.textSize = size.minDimension * (.1f + .08f * q)
                drawContext.canvas.nativeCanvas.drawText("z", size.width * (.72f + .12f * q), size.height * (.3f - .25f * q), paint) }
            Mood.WORRIED -> { paint.color = android.graphics.Color.rgb(255, 176, 32); paint.alpha = (180 + 75 * kotlin.math.sin(t * 12.56f)).toInt().coerceIn(0, 255)
                drawContext.canvas.nativeCanvas.drawText("!", size.width * .8f, size.height * .22f, paint) }
            Mood.CHEER, Mood.HAPPY -> for (k in 0..4) { val a = (t + k / 5f) * 6.28f; val r = size.minDimension * .45f
                val x = c.x + r * kotlin.math.cos(a); val y = c.y + r * kotlin.math.sin(a) * .8f; val sz = 6f + 4f * kotlin.math.sin(a * 2)
                drawLine(Color.White.copy(alpha = .9f), androidx.compose.ui.geometry.Offset(x - sz, y), androidx.compose.ui.geometry.Offset(x + sz, y), 2.5f)
                drawLine(Color.White.copy(alpha = .9f), androidx.compose.ui.geometry.Offset(x, y - sz), androidx.compose.ui.geometry.Offset(x, y + sz), 2.5f) }
            Mood.POINT -> { paint.color = android.graphics.Color.WHITE; drawContext.canvas.nativeCanvas.drawText("👉", size.width * (.0f + .05f * kotlin.math.sin(t * 12.56f)), size.height * .55f, paint) }
            Mood.IDLE -> {}
        }
    }
}
