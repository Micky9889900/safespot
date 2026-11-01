package com.safespot.scanner.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safespot.scanner.AppState
import com.safespot.scanner.ads.Ads
import com.safespot.scanner.ads.NativeAdCard
import com.safespot.scanner.care.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ToolSpec(val id: String, val title: String, val icon: String, val a: Color, val b: Color, val blurb: String)
object ToolCatalog {
    val care = listOf(
        ToolSpec("junk", "Junk Cleaner", "🧹", Color(0xFFFF9A3C), Color(0xFFE5395B), "Big files, duplicates, old APKs"),
        ToolSpec("battery", "Battery", "🔋", Color(0xFF2BD98B), Color(0xFF0F8A9A), "Real battery stats & drainers"),
        ToolSpec("cooler", "CPU Cooler", "❄️", Color(0xFF3FD5FF), Color(0xFF3A5BFF), "Temperatures & throttling"),
        ToolSpec("booster", "Booster", "🚀", Color(0xFFB98CFF), Color(0xFF5B3AFF), "Free up memory"),
        ToolSpec("apps", "App Manager", "📦", Color(0xFFFFC14D), Color(0xFFFF6A3C), "Sizes, last used, uninstall"),
        ToolSpec("wifi", "Wi-Fi Check", "📶", Color(0xFF4CD7FF), Color(0xFF2B7BFF), "Is this network safe?"),
        ToolSpec("speed", "Speed Test", "⚡", Color(0xFFFFE14D), Color(0xFFFF8A00), "Ping, download, upload"),
        ToolSpec("storage", "Storage", "💾", Color(0xFF7AF0C8), Color(0xFF1C9C7A), "What fills your phone"),
        ToolSpec("perms", "Permissions", "🔐", Color(0xFFFF7AB6), Color(0xFFB0306E), "Who can use camera, mic…"),
        ToolSpec("notif", "Notifications", "🔔", Color(0xFFFFB36B), Color(0xFFE5395B), "Tame noisy apps"))
    fun get(id: String) = care.first { it.id == id }
    private val chain = listOf("junk", "battery", "cooler", "booster", "apps", "storage", "wifi", "speed", "perms", "notif")
    fun next(id: String) = get(chain[(chain.indexOf(id) + 1) % chain.size])
}

/** 3D-style illustrated tile icon: gradient orb with gloss highlight and soft shadow. Pure vector, zero APK size. */
@Composable
fun ToolOrb(spec: ToolSpec, size: androidx.compose.ui.unit.Dp = 52.dp, pulse: Boolean = false, art: String = toolArt(spec.id)) {
    val sc by (if (pulse) rememberInfiniteTransition(label = "orb").animateFloat(1f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s") else remember { mutableFloatStateOf(1f) })
    if (art.isNotEmpty() && artId(art) != 0) { Art(art, Modifier.size(size * sc * 1.12f)); return }
    Box(Modifier.size(size * sc).shadow(10.dp, CircleShape, ambientColor = spec.b, spotColor = spec.b).clip(CircleShape)
        .background(Brush.linearGradient(listOf(spec.a, spec.b))), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) { drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .55f), Color.Transparent), Offset(size.toPx() * .3f, size.toPx() * .25f), size.toPx() * .45f)) }
        Text(spec.icon, fontSize = (size.value * .46f).sp)
    }
}

@Composable
fun ToolTile(spec: ToolSpec, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val h = rememberHaptic()
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(spec.a.copy(alpha = if (C.dark) .28f else .18f), spec.b.copy(alpha = if (C.dark) .14f else .08f))))
        .clickable { h(false); onClick() }.padding(14.dp)) {
        Column { ToolOrb(spec); Spacer(Modifier.height(10.dp))
            Text(spec.title, color = C.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(spec.blurb, color = C.SilverDim, fontSize = 11.sp, maxLines = 2, lineHeight = 14.sp) }
    }
}

/** Shared shell for a tool: hero → animated run → rich result (+ native ad + next tool). */
@Composable
fun ToolHost(s: AppState, id: String) {
    val spec = ToolCatalog.get(id); val act = androidx.activity.compose.LocalActivity.current
    val back = { if (act != null) Ads.onReturnFromTool(act) { s.tool = null } else s.tool = null }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clip(CircleShape).clickable { back() }.padding(horizontal = 10.dp), color = C.Ink, fontSize = 28.sp)
            Text(spec.title, color = C.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f)) {
            when (id) { "junk" -> JunkTool(s); "battery" -> BatteryTool(s); "cooler" -> CoolerTool(s); "booster" -> BoosterTool(s); "apps" -> AppsTool(s)
                "wifi" -> WifiTool(s); "speed" -> SpeedTool(s); "storage" -> StorageTool(s); "perms" -> PermsTool(s); else -> NotifTool(s) }
        }
    }
}

/** Run animation: orbiting rings around the tool orb, real progress when known. */
@Composable
fun RunState(spec: ToolSpec, label: String, progress: Float?) {
    val t by rememberInfiniteTransition(label = "run").animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "r")
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(220.dp)) {
                drawArc(spec.a, t, 100f, false, style = Stroke(10f, cap = StrokeCap.Round))
                drawArc(spec.b, -t * 1.4f, 70f, false, Offset(30f, 30f), androidx.compose.ui.geometry.Size(size.width - 60, size.height - 60), style = Stroke(8f, cap = StrokeCap.Round))
                if (progress != null) drawArc(C.Ink.copy(alpha = .8f), -90f, 360f * progress, false, Offset(60f, 60f), androidx.compose.ui.geometry.Size(size.width - 120, size.height - 120), style = Stroke(6f, cap = StrokeCap.Round))
            }
            ToolOrb(spec, 96.dp, pulse = true)
        }
        Spacer(Modifier.height(16.dp))
        if (progress != null) Text("${(progress * 100).toInt()}%", color = C.Ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Body(label, C.SilverDim, 13)
    }
}

@Composable
fun Hero(spec: ToolSpec, text: String, button: String, onRun: () -> Unit, extra: @Composable ColumnScope.() -> Unit = {}) {
    val h = rememberHaptic()
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(180.dp).clip(CircleShape).background(Brush.radialGradient(listOf(spec.a.copy(alpha = .35f), Color.Transparent))), contentAlignment = Alignment.Center) { ToolOrb(spec, 110.dp, pulse = true) }
        Spacer(Modifier.height(8.dp)); Body(text, C.Silver, 14); Spacer(Modifier.height(16.dp))
        PrimaryButton(button, Modifier.fillMaxWidth(), spec.b) { h(true); onRun() }
        extra()
    }
}

/** Bottom of every result: tips, native ad (separated, labelled), next suggested tool. */
@Composable
fun ResultFooter(s: AppState, id: String, tips: List<String>) {
    val n = ToolCatalog.next(id)
    if (tips.isNotEmpty()) { Section("Tips"); Glass { tips.forEach { Body("• $it", C.Silver, 13) } } }
    NativeAdCard()
    Section("Next suggested")
    Glass(onClick = { s.tool = n.id }) { Row(verticalAlignment = Alignment.CenterVertically) { ToolOrb(n, 44.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(n.title, color = C.Ink, fontWeight = FontWeight.Bold); Body(n.blurb, C.SilverDim, 12) }
        Text("›", color = C.Ink, fontSize = 24.sp) } }
    Spacer(Modifier.height(90.dp))
}

@Composable
fun UsageGate(why: String) {
    val ctx = LocalContext.current
    Glass { Text("Usage access needed", color = C.Amber, fontWeight = FontWeight.Bold)
        Body(why + " Android only shares this if you allow \"Usage access\" for SafeSpot. It stays on your phone.", C.Silver, 13)
        Spacer(Modifier.height(8.dp)); PrimaryButton("Open usage access settings", Modifier.fillMaxWidth(), C.Accent2) {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) }
                .onFailure { runCatching { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } } } }
}

@Composable private fun Kv(k: String, v: String, col: Color = C.Ink) = Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(k, Modifier.weight(1f), color = C.SilverDim, fontSize = 13.sp); Text(v, color = col, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
private val big = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black)
private fun open(ctx: android.content.Context, i: Intent) { runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }

/** Re-reads usage access when returning from Settings. */
@Composable fun rememberUsage(): Boolean { val ctx = LocalContext.current; var ok by remember { mutableStateOf(Usage.hasAccess(ctx)) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) { val o = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) ok = Usage.hasAccess(ctx) }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) } }
    return ok }

// =========================================================== Junk
@Composable
fun JunkTool(s: AppState) {
    val ctx = LocalContext.current; val act = androidx.activity.compose.LocalActivity.current; val scope = rememberCoroutineScope(); val spec = ToolCatalog.get("junk"); val h = rememberHaptic()
    var phase by remember { mutableStateOf("hero") }; var label by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<JunkReport?>(null) }; var freedCache by remember { mutableLongStateOf(0L) }
    var deep by remember { mutableStateOf(false) }; var sel by remember { mutableStateOf(setOf<android.net.Uri>()) }
    var confirm by remember { mutableStateOf(false) }; var deleted by remember { mutableLongStateOf(-1L) }
    val usage = rememberUsage()
    val apps = remember(phase, usage) { if (phase == "result" && usage) Apps.list(ctx).filter { (it.cacheBytes ?: 0) > 5 shl 20 }.sortedByDescending { it.cacheBytes }.take(12) else emptyList() }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u ->
        if (u == null) return@rememberLauncherForActivityResult
        runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        phase = "run"; scope.launch {
            freedCache = withContext(Dispatchers.IO) { StorageInfo.clearOwnCache(ctx) }
            report = withContext(Dispatchers.IO) { Junk.scan(ctx, u, deep) { n, name -> label = "$n files · $name" } }
            sel = (report!!.apks + report!!.tempFiles).map { it.uri }.toSet() + report!!.duplicates.flatMap { g -> g.drop(1).map { it.uri } }
            phase = "result"; Ads.markToolRun(); h(true); s.log("🧹", "Junk scan: ${fmtBytes(report!!.reclaimable)} found in ${report!!.folder}")
            s.react(Mood.CHEER, "Found ${fmtBytes(report!!.reclaimable)} to review. Battery check next?")
        }
    }
    when (phase) {
        "hero" -> LazyColumn { item { Hero(spec, "Clears SafeSpot's own cache, then scans a folder you pick (e.g. Download, WhatsApp media, DCIM) for large files, duplicates, old APKs, temp files and empty folders. Nothing is deleted without your confirmation.", "Pick a folder & scan", { pick.launch(null) }) {
            Spacer(Modifier.height(10.dp))
            GhostButton(if (deep) "✓ Deep clean on (full-file duplicate check, deeper folders)" else "Deep clean (watch a short ad)", Modifier.fillMaxWidth(), C.Amber) {
                if (deep) deep = false else if (act != null) Ads.rewarded(act) { ok -> if (ok) deep = true } }
            Spacer(Modifier.height(16.dp)); if (!usage) UsageGate("To show how much cache each app is holding,") } } }
        "run" -> RunState(spec, "Scanning… $label", null)
        else -> { val r = report!!; Box { Particles(FxKind.SPARKLE)
            LazyColumn(contentPadding = PaddingValues(16.dp)) { item { ResultArt("ill_result_cleaned") }
                item { Glass { Text("Reclaimable in ${r.folder}", color = C.SilverDim, fontSize = 13.sp)
                    AnimatedCount(r.reclaimable.toFloat(), { fmtBytes(it.toLong()) }, big)
                    Body("${r.scanned} files scanned · SafeSpot cache cleared: ${fmtBytes(freedCache)}", C.SilverDim, 12) }
                    Spacer(Modifier.height(10.dp))
                    Row { Donut(listOf(r.large.sumOf { it.size }.toFloat() to C.Red, r.dupWaste.toFloat() to C.Amber, r.apks.sumOf { it.size }.toFloat() to C.Blue, r.tempFiles.sumOf { it.size }.toFloat() to C.Cyan),
                        fmtBytes(r.reclaimable), "found", Modifier.size(130.dp))
                        Column(Modifier.padding(start = 12.dp)) { Pill("Large ${r.large.size}", C.Red); Spacer(Modifier.height(4.dp)); Pill("Duplicates ${r.duplicates.size}", C.Amber); Spacer(Modifier.height(4.dp))
                            Pill("APKs ${r.apks.size}", C.Blue); Spacer(Modifier.height(4.dp)); Pill("Temp ${r.tempFiles.size}", C.Cyan); Spacer(Modifier.height(4.dp)); Pill("Empty folders ${r.emptyDirs.size}", C.SilverDim) } } }
                fun group(t: String, l: List<JunkFile>) { if (l.isEmpty()) return; item { Section(t) }
                    items(l.take(60), key = { t + it.uri }) { f -> Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(f.uri in sel, { c -> sel = if (c) sel + f.uri else sel - f.uri }, colors = CheckboxDefaults.colors(checkedColor = C.Accent, uncheckedColor = C.SilverDim))
                        Column(Modifier.weight(1f)) { Text(f.name, color = C.Ink, fontSize = 13.sp, maxLines = 1); Text(f.path, color = C.SilverDim, fontSize = 10.sp, maxLines = 1) }
                        Text(fmtBytes(f.size), color = C.Silver, fontSize = 12.sp) } } }
                group("Old APK installers", r.apks); group("Temp & log files", r.tempFiles); group("Duplicate copies (oldest kept)", r.duplicates.flatMap { it.drop(1) }); group("Large files (check before deleting)", r.large)
                group("Empty folders", r.emptyDirs)
                item { Spacer(Modifier.height(12.dp))
                    val total = (r.apks + r.tempFiles + r.large + r.duplicates.flatten() + r.emptyDirs).distinctBy { it.uri }.filter { it.uri in sel }
                    if (deleted >= 0) Glass { Text("Freed ${fmtBytes(deleted)}", color = C.Green, fontWeight = FontWeight.Bold) }
                    else if (!confirm) PrimaryButton("Delete ${total.size} selected (${fmtBytes(total.sumOf { it.size })})", Modifier.fillMaxWidth(), C.Red, total.isNotEmpty()) { confirm = true }
                    else Glass { Text("Delete ${total.size} items permanently?", color = C.Ink, fontWeight = FontWeight.Bold); Body("This can't be undone.", C.SilverDim, 12); Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { GhostButton("Cancel", Modifier.weight(1f)) { confirm = false }
                            PrimaryButton("Delete", Modifier.weight(1f), C.Red) { h(true); scope.launch { deleted = withContext(Dispatchers.IO) { total.filter { Junk.delete(ctx, it) }.sumOf { it.size } }
                                s.log("🧹", "Deleted ${fmtBytes(deleted)} of junk"); s.react(Mood.CHEER, "Freed ${fmtBytes(deleted)}! ✨"); confirm = false } } } } }
                item { Section("Other apps' cache")
                    if (!usage) UsageGate("To list other apps' cache,") else Glass {
                        Body("Since Android 6, apps can't clear other apps' cache. Tap an app to open its settings → Storage → Clear cache.", C.SilverDim, 12)
                        if (apps.isEmpty()) Body("No app holds more than 5 MB of cache.", C.Silver)
                        apps.forEach { a -> Row(Modifier.fillMaxWidth().clickable { open(ctx, Apps.details(a.pkg)) }.padding(vertical = 6.dp)) { Text(a.label, Modifier.weight(1f), color = C.Ink, fontSize = 13.sp); Text(fmtBytes(a.cacheBytes ?: 0) + " ›", color = C.Amber, fontSize = 13.sp) } } } }
                item { ResultFooter(s, "junk", listOf("Media apps (WhatsApp, Telegram) keep every received file: review their media folder.", "Old APK installers are safe to delete once the app is installed.")) }
            } } }
    }
}

// =========================================================== Battery
@Composable
fun BatteryTool(s: AppState) {
    val ctx = LocalContext.current; val spec = ToolCatalog.get("battery"); val h = rememberHaptic()
    var phase by remember { mutableStateOf("run") }; var b by remember { mutableStateOf<BatteryInfo?>(null) }
    val usage = rememberUsage()
    LaunchedEffect(Unit) { Battery.record(ctx); delay(1300); b = Battery.read(ctx); phase = "result"; Ads.markToolRun(); h(false); s.log("🔋", "Battery check: ${b?.level ?: "?"}%, ${b?.health}") }
    LaunchedEffect(phase) { while (phase == "result") { delay(3000); b = Battery.read(ctx) } }
    if (phase == "run") { RunState(spec, "Reading battery sensors…", null); return }
    val bi = b!!; val hist = remember { Battery.history(ctx) }
    val drain = remember(usage) { Usage.top(ctx, 86400_000L, limit = 8) }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { ScoreRing(bi.level, 150.dp, "Battery")
            Column(Modifier.padding(start = 12.dp)) { Pill(bi.status, if (bi.status == "Charging") C.Green else C.Silver); Spacer(Modifier.height(6.dp)); Pill("Health: ${bi.health}", if (bi.health == "Good") C.Green else C.Amber)
                if (bi.saverOn) { Spacer(Modifier.height(6.dp)); Pill("Battery Saver on", C.Amber) } } }
            Section("Live readings"); Glass {
                Kv("Temperature", bi.tempC?.let { "%.1f °C".format(it) } ?: "Not reported", bi.tempC?.let { if (it > 40) C.Red else if (it > 35) C.Amber else C.Green } ?: C.Ink)
                Kv("Voltage", bi.voltageMv?.let { "%.2f V".format(it / 1000f) } ?: "Not reported"); Kv("Current", bi.currentMa?.let { "$it mA" } ?: "Not reported by this phone")
                Kv("Charger", bi.plug); Kv("Technology", bi.tech ?: "Not reported"); bi.chargeMah?.let { Kv("Charge left", "$it mAh") }
                bi.cycles?.let { Kv("Charge cycles", "$it") }; bi.timeToFullMin?.let { Kv("Time to full (Android estimate)", "$it min") } } }
        item { Section("Charging history"); Glass { if (hist.size < 3) Body("SafeSpot records a reading each time you open it. Check back later for a graph.", C.SilverDim, 12)
            else { LineChart(hist.map { it.level.toFloat() }, C.Green, fixedMax = 100f); Body("${hist.size} readings since ${fmtAgo(hist.first().t)}", C.SilverDim, 11) } } }
        item { Section("Most used today (screen time)")
            if (!usage) UsageGate("To show which apps you used most today,") else Glass {
                Body("Android doesn't let apps read per-app battery use. Screen time is a good proxy: apps on screen longest usually use the most battery.", C.SilverDim, 12)
                Bars(drain.map { it.label to (it.fgMs / 60_000).toInt() }, C.Amber); Body("minutes on screen, last 24 h", C.SilverDim, 11) } }
        item { Section("Shortcuts"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Battery saver", Modifier.weight(1f)) { open(ctx, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }
            GhostButton("Battery usage", Modifier.weight(1f)) { open(ctx, Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) } }
            Spacer(Modifier.height(8.dp)); GhostButton("Battery optimisation per app", Modifier.fillMaxWidth()) { open(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } }
        item { ResultFooter(s, "battery", listOf("Keep charge between 20 % and 80 % for a longer battery life.", "Heat is the #1 battery killer: avoid charging in the sun or while gaming.", "Lower screen brightness and screen timeout save the most power.")) }
    }
}

// =========================================================== Cooler
@Composable
fun CoolerTool(s: AppState) {
    val ctx = LocalContext.current; val spec = ToolCatalog.get("cooler"); val scope = rememberCoroutineScope(); val h = rememberHaptic()
    var phase by remember { mutableStateOf("run") }; var t0 by remember { mutableStateOf<ThermalInfo?>(null) }; var now by remember { mutableStateOf<ThermalInfo?>(null) }
    var timer by remember { mutableIntStateOf(0) }; var start by remember { mutableStateOf<Float?>(null) }
    val usage = rememberUsage()
    LaunchedEffect(Unit) { t0 = withContext(Dispatchers.IO) { Thermal.read(ctx) }; now = t0; delay(1500); phase = "result"; Ads.markToolRun(); s.log("❄️", "Cooler: battery ${t0?.batteryC ?: "?"} °C") }
    LaunchedEffect(timer) { if (timer > 0) { delay(1000); now = withContext(Dispatchers.IO) { Thermal.read(ctx) }; timer--
        if (timer == 0) { h(true); val d = (start ?: 0f) - (now?.batteryC ?: 0f); s.log("❄️", "Cool-down: %+.1f °C".format(-d)); s.react(Mood.CHEER, if (d > 0) "Your phone is %.1f °C cooler now ❄️".format(d) else "Temperature held steady. Close heavy apps for more.") } } }
    if (phase == "run") { RunState(spec, "Reading thermal sensors…", null); return }
    val ti = now!!; val heavy = remember(usage) { Usage.top(ctx, 3600_000L, byRecent = true, limit = 8) }
    Box { if (timer == 0 && start != null) Particles(FxKind.SNOW)
    LazyColumn(contentPadding = PaddingValues(16.dp)) { item { ResultArt("ill_result_cooled") }
        item { Glass { Text("Battery temperature", color = C.SilverDim, fontSize = 13.sp)
            AnimatedCount(ti.batteryC ?: 0f, { if (ti.batteryC == null) "--" else "%.1f °C".format(it) }, big, ti.batteryC?.let { if (it > 40) C.Red else if (it > 35) C.Amber else C.Cyan } ?: C.Ink)
            Kv("CPU temperature", ti.cpuC?.let { "%.1f °C".format(it) } ?: "Hidden by Android on this phone"); Kv("Thermal status", ti.status, if (ti.statusLevel >= 2) C.Red else if (ti.statusLevel == 1) C.Amber else C.Green)
            ti.headroom?.let { Kv("Thermal headroom (10 s)", "${(it * 100).toInt()} % of limit", if (it > .9f) C.Red else C.Green) }
            if (start != null) Kv("Change since timer start", "%+.1f °C".format((ti.batteryC ?: 0f) - start!!), C.Cyan) }
            Spacer(Modifier.height(10.dp))
            if (timer > 0) Glass { Text("Cooling down… ${timer / 60}:%02d".format(timer % 60), color = C.Cyan, fontWeight = FontWeight.Bold); Body("Leave the phone idle and out of its case. The real temperature is re-read every second.", C.SilverDim, 12) }
            else PrimaryButton("Start 3-min cool-down timer", Modifier.fillMaxWidth(), C.Cyan) { h(true); start = ti.batteryC; timer = 180 } }
        item { Section("Recently used apps (close the heavy ones)")
            if (!usage) UsageGate("To list recently active apps,") else Glass {
                Body("Android doesn't let apps close other apps. Tap one to open its settings, then use Force stop.", C.SilverDim, 12)
                heavy.forEach { a -> Row(Modifier.fillMaxWidth().clickable { open(ctx, Apps.details(a.pkg)) }.padding(vertical = 6.dp)) { Text(a.label, Modifier.weight(1f), color = C.Ink, fontSize = 13.sp); Text("Force stop ›", color = C.Amber, fontSize = 12.sp) } } } }
        item { ResultFooter(s, "cooler", listOf("Remove the case while charging or gaming.", "Avoid direct sun and car dashboards.", "Lower brightness, turn off 5G/hotspot if not needed.", "A phone above 45 °C throttles itself to protect the battery.")) }
    } }
}

// =========================================================== Booster
@Composable
fun BoosterTool(s: AppState) {
    val ctx = LocalContext.current; val spec = ToolCatalog.get("booster"); val h = rememberHaptic()
    var phase by remember { mutableStateOf("run") }; var before by remember { mutableStateOf<MemInfo?>(null) }; var after by remember { mutableStateOf<MemInfo?>(null) }; var asked by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { before = Memory.read(ctx)
        val pkgs = withContext(Dispatchers.IO) { ctx.packageManager.getInstalledApplications(0).filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }.map { it.packageName } }
        asked = withContext(Dispatchers.IO) { Memory.killBackground(ctx, pkgs) }; System.gc(); delay(1800); after = Memory.read(ctx); phase = "result"; Ads.markToolRun(); h(true)
        val freed = (after!!.avail - before!!.avail).coerceAtLeast(0); s.log("🚀", "Booster: ${fmtBytes(freed)} memory freed"); s.react(Mood.CHEER, if (freed > 0) "Freed ${fmtBytes(freed)} of memory 🚀" else "Memory was already in good shape!") }
    if (phase == "run") { RunState(spec, "Asking Android to trim background apps…", null); return }
    val b = before!!; val a = after!!; val freed = (a.avail - b.avail).coerceAtLeast(0)
    Box { Particles(FxKind.CONFETTI)
    LazyColumn(contentPadding = PaddingValues(16.dp)) { item { ResultArt("ill_result_boosted") }
        item { Glass { Text("Memory freed", color = C.SilverDim, fontSize = 13.sp); AnimatedCount(freed.toFloat(), { fmtBytes(it.toLong()) }, big, C.Green)
            Kv("Available before", fmtBytes(b.avail)); Kv("Available after", fmtBytes(a.avail), C.Green); Kv("Total RAM", fmtBytes(a.total)); Kv("In use now", "${a.pct} %")
            if (a.low) Kv("Status", "Android reports LOW memory", C.Red) }
            Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("${b.pct}%", "Used before", C.Amber, Modifier.weight(1f)); StatCard("${a.pct}%", "Used after", C.Green, Modifier.weight(1f)) }
            Spacer(Modifier.height(10.dp)); Glass { Body(if (Memory.killWorks) "Asked Android to stop the background processes of $asked apps. Android restarts some services when needed: that's normal."
                else "On Android 14+, Android no longer lets apps stop other apps' processes; it manages memory itself. The number above is the real change in free memory.", C.SilverDim, 12) } }
        item { ResultFooter(s, "booster", listOf("Android manages RAM well: unused RAM is wasted RAM.", "If one app is slow, Force stop it from App Manager.", "Restarting the phone once a week clears everything.")) }
    } }
}

// =========================================================== App Manager
@Composable
fun AppsTool(s: AppState) {
    val ctx = LocalContext.current; val spec = ToolCatalog.get("apps"); val usage = rememberUsage()
    var list by remember { mutableStateOf<List<AppEntry>?>(null) }; var sort by remember { mutableStateOf("size") }; var sel by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(usage) { list = withContext(Dispatchers.IO) { Apps.list(ctx) }; Ads.markToolRun() }
    val l = list ?: run { RunState(spec, "Measuring apps…", null); return }
    val sorted = when (sort) { "unused" -> l.sortedBy { it.lastUsed ?: 0 }; "name" -> l.sortedBy { it.label.lowercase() }; else -> l }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("${l.size}", "Your apps", C.Ink, Modifier.weight(1f)); StatCard(fmtBytes(l.sumOf { it.total }), "Total size", C.Amber, Modifier.weight(1f)) }
            if (!usage) { Spacer(Modifier.height(10.dp)); UsageGate("For data/cache sizes and last-used dates,") }
            Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("size" to "Biggest", "unused" to "Least used", "name" to "A–Z").forEach { (k, t) ->
                Text(t, Modifier.clip(RoundedCornerShape(50)).background(if (sort == k) C.Accent2 else C.Glass).clickable { sort = k }.padding(horizontal = 14.dp, vertical = 6.dp), color = if (sort == k) Color.White else C.Ink, fontSize = 13.sp) } }
            Spacer(Modifier.height(8.dp)) }
        items(sorted, key = { it.pkg }) { a -> Row(Modifier.fillMaxWidth().clickable { open(ctx, Apps.details(a.pkg)) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(a.pkg in sel, { c -> sel = if (c) sel + a.pkg else sel - a.pkg }, colors = CheckboxDefaults.colors(checkedColor = C.Accent, uncheckedColor = C.SilverDim))
            Column(Modifier.weight(1f)) { Text(a.label, color = C.Ink, fontSize = 14.sp, maxLines = 1); Text(if (usage) "Last used ${a.lastUsed?.let { fmtAgo(it) } ?: "90+ days ago"}" else "Installed ${fmtAgo(a.installed)}", color = C.SilverDim, fontSize = 11.sp) }
            Text(fmtBytes(a.total), color = C.Silver, fontSize = 12.sp) } }
        item { Spacer(Modifier.height(10.dp)); PrimaryButton("Uninstall ${sel.size} selected", Modifier.fillMaxWidth(), C.Red, sel.isNotEmpty()) {
            sel.forEach { open(ctx, Apps.uninstall(it)) }; s.log("📦", "Uninstall requested for ${sel.size} apps"); sel = emptySet() }
            Body("Android asks you to confirm each uninstall.", C.SilverDim, 11) }
        item { ResultFooter(s, "apps", listOf("Apps you haven't opened in months are safe candidates to remove.", "Games and social apps usually hold the most data.")) }
    }
}

// =========================================================== Wi-Fi
@Composable
fun WifiTool(s: AppState) {
    val ctx = LocalContext.current; val spec = ToolCatalog.get("wifi"); var w by remember { mutableStateOf<WifiCheck?>(null) }
    LaunchedEffect(Unit) { delay(1200); w = withContext(Dispatchers.IO) { Wifi.check(ctx) }; Ads.markToolRun()
        w?.let { s.log("📶", "Wi-Fi check: ${it.security ?: if (it.onWifi) "Wi-Fi" else "not on Wi-Fi"}"); if (it.open == true && !it.vpn) s.react(Mood.WORRIED, "This Wi-Fi has no password. Turn on the VPN!") } }
    val x = w ?: run { RunState(spec, "Checking this network…", null); return }
    val score = if (!x.onWifi) null else listOf(x.open == false, x.validated, !x.captive, x.vpn || x.privateDns == true).count { it } * 25
    Box { if (score != null && score >= 75) Particles(FxKind.SHIELD)
    LazyColumn(contentPadding = PaddingValues(16.dp)) { item { ResultArt(if (score != null && score >= 75) "ill_result_safe" else "") }
        item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ScoreRing(score, 170.dp, "Network safety") }
            if (!x.onWifi) Glass { Body("You're not on Wi-Fi right now. Mobile data is encrypted between you and your carrier.", C.Silver) }
            Section("Checks"); Glass {
                Kv("Encryption", x.security ?: if (Build.VERSION.SDK_INT < 31) "Needs Android 12+" else "Not reported", when (x.open) { true -> C.Red; false -> C.Green; else -> C.Ink })
                Kv("Internet verified", if (x.validated) "Yes" else "No", if (x.validated) C.Green else C.Amber); Kv("Captive portal / login page", if (x.captive) "Yes" else "No", if (x.captive) C.Amber else C.Green)
                Kv("VPN active", if (x.vpn) "Yes" else "No", if (x.vpn) C.Green else C.Amber); Kv("Private DNS", when (x.privateDns) { true -> "On"; false -> "Off"; else -> "Unknown" }, if (x.privateDns == true) C.Green else C.Ink)
                Kv("Metered", if (x.metered) "Yes" else "No"); x.freqMhz?.let { Kv("Band", if (it > 5900) "6 GHz" else if (it > 4900) "5 GHz" else "2.4 GHz") }
                x.linkMbps?.let { Kv("Link speed", "$it Mbps") }; x.signal?.let { Kv("Signal", "$it dBm", if (it > -67) C.Green else if (it > -80) C.Amber else C.Red) } }
            if (x.open == true && !x.vpn) { Spacer(Modifier.height(10.dp)); Glass { Text("⚠ Open network", color = C.Red, fontWeight = FontWeight.Bold); Body("Anyone nearby can see unencrypted traffic. Turn on SafeSpot VPN before logging in anywhere.", C.Silver, 13)
                Spacer(Modifier.height(8.dp)); PrimaryButton("Turn on VPN", Modifier.fillMaxWidth(), C.Accent) { s.tool = null; s.tab = "vpn" } } } }
        item { ResultFooter(s, "wifi", listOf("Prefer WPA3 or WPA2 networks; avoid WEP and open Wi-Fi.", "Turn on Private DNS (Settings → Network) for encrypted DNS everywhere.")) }
    } }
}

// =========================================================== Speed
@Composable
fun SpeedTool(s: AppState) {
    val spec = ToolCatalog.get("speed"); val scope = rememberCoroutineScope(); val h = rememberHaptic()
    var phase by remember { mutableStateOf("hero") }; var stage by remember { mutableStateOf("") }; var prog by remember { mutableFloatStateOf(0f) }; var r by remember { mutableStateOf<SpeedResult?>(null) }
    when (phase) {
        "hero" -> Hero(spec, "Measures ping, download and upload with Cloudflare's public speed test (about 30 MB of data).", "Start speed test", { phase = "run"
            scope.launch { r = withContext(Dispatchers.IO) { Speed.run { st, p -> stage = st; prog = p } }; phase = "result"; Ads.markToolRun(); h(true)
                r?.downMbps?.let { s.log("⚡", "Speed: %.0f↓ / %.0f↑ Mbps".format(it, r?.upMbps ?: 0.0)) } } })
        "run" -> RunState(spec, stage, prog)
        else -> { val x = r!!; LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item { if (x.error != null) Glass { Text("Test failed", color = C.Red, fontWeight = FontWeight.Bold); Body(x.error, C.Silver) }
                else { Glass { Text("Download", color = C.SilverDim, fontSize = 13.sp); AnimatedCount((x.downMbps ?: 0.0).toFloat(), { "%.1f Mbps".format(it) }, big, C.Green) }
                    Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("%.1f".format(x.upMbps ?: 0.0), "Upload Mbps", C.Blue, Modifier.weight(1f)); StatCard(x.pingMs?.let { "$it ms" } ?: "--", "Ping", C.Amber, Modifier.weight(1f)) }
                    Spacer(Modifier.height(10.dp)); Glass { Body(when { (x.downMbps ?: 0.0) >= 25 -> "Great for 4K video and video calls."; (x.downMbps ?: 0.0) >= 5 -> "Fine for HD video and calls."; else -> "Slow: expect buffering." }, C.Silver) } }
                Spacer(Modifier.height(10.dp)); GhostButton("Run again", Modifier.fillMaxWidth()) { phase = "hero" } }
            item { ResultFooter(s, "speed", listOf("Results vary with distance to the router and how busy the network is.", "Speeds over the VPN depend on the volunteer server you picked.")) } } }
    }
}

// =========================================================== Storage
@Composable
fun StorageTool(s: AppState) {
    val ctx = LocalContext.current; val spec = ToolCatalog.get("storage"); val usage = rememberUsage(); var b by remember { mutableStateOf<StorageBreakdown?>(null) }
    LaunchedEffect(usage) { b = withContext(Dispatchers.IO) { StorageInfo.read(ctx, if (usage) Apps.list(ctx, true).sumOf { it.total } else null) }; Ads.markToolRun() }
    val x = b ?: run { RunState(spec, "Measuring storage…", null); return }
    val known = (x.images ?: 0) + (x.video ?: 0) + (x.audio ?: 0) + (x.apps ?: 0); val other = (x.used - known).coerceAtLeast(0)
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Donut(listOf((x.apps ?: 0).toFloat() to C.Blue, (x.images ?: 0).toFloat() to C.Amber, (x.video ?: 0).toFloat() to C.Red, (x.audio ?: 0).toFloat() to C.Cyan, other.toFloat() to C.SilverDim, x.free.toFloat() to C.Green.copy(alpha = .35f)),
                "${(x.used * 100 / maxOf(1, x.total))}%", "used", Modifier.size(190.dp)) }
            Section("Breakdown"); Glass { Kv("Total", fmtBytes(x.total)); Kv("Free", fmtBytes(x.free), C.Green)
                if (usage) { Kv("Apps & app data", fmtBytes(x.apps ?: 0), C.Blue); Kv("Photos", fmtBytes(x.images ?: 0), C.Amber); Kv("Videos", fmtBytes(x.video ?: 0), C.Red); Kv("Audio", fmtBytes(x.audio ?: 0), C.Cyan); Kv("System & other", fmtBytes(other)) }
                else Body("Allow usage access to see apps / photos / videos / audio.", C.SilverDim, 12) }
            if (!usage) { Spacer(Modifier.height(10.dp)); UsageGate("For the category breakdown,") }
            Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { GhostButton("Junk Cleaner", Modifier.weight(1f)) { s.tool = "junk" }; GhostButton("Storage settings", Modifier.weight(1f)) { open(ctx, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) } } }
        item { ResultFooter(s, "storage", listOf("Videos are usually the biggest: back them up and remove local copies.", "Keep at least 10 % free so Android stays fast.")) }
    }
}

// =========================================================== Permissions
@Composable
fun PermsTool(s: AppState) {
    val ctx = LocalContext.current; val spec = ToolCatalog.get("perms"); var m by remember { mutableStateOf<Map<String, List<Perms.Holder>>?>(null) }; var open by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { m = withContext(Dispatchers.IO) { Perms.byGroup(ctx) }; Ads.markToolRun() }
    val x = m ?: run { RunState(spec, "Reading app permissions…", null); return }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { Bars(x.filter { it.value.isNotEmpty() }.map { it.key to it.value.size }, C.Accent); Body("apps currently allowed, per permission", C.SilverDim, 11); Spacer(Modifier.height(10.dp)) }
        x.forEach { (g, l) -> item(key = g) { Glass(onClick = { open = if (open == g) null else g }) {
            Row { Text(g, Modifier.weight(1f), color = C.Ink, fontWeight = FontWeight.SemiBold); Pill("${l.size}", if (l.size > 5) C.Amber else C.Blue) }
            AnimatedVisibility(open == g) { Column { l.forEach { h -> Text("${h.label}  ›", Modifier.fillMaxWidth().clickable { open(ctx, Apps.details(h.pkg)) }.padding(vertical = 6.dp), color = C.Silver, fontSize = 13.sp) } } } }
            Spacer(Modifier.height(8.dp)) } }
        item { GhostButton("Android permission manager", Modifier.fillMaxWidth()) { runCatching { ctx.startActivity(Intent("android.intent.action.MANAGE_PERMISSIONS")) }.onFailure { open(ctx, Intent(Settings.ACTION_PRIVACY_SETTINGS)) } }
            ResultFooter(s, "perms", listOf("Only messaging/camera apps need Camera and Microphone.", "Use 'Only while using the app' for Location.")) }
    }
}

// =========================================================== Notifications
@Composable
fun NotifTool(s: AppState) {
    val ctx = LocalContext.current; val spec = ToolCatalog.get("notif"); var m by remember { mutableStateOf<List<Perms.Holder>?>(null) }
    LaunchedEffect(Unit) { m = withContext(Dispatchers.IO) { Perms.byGroup(ctx)["Notifications"] ?: emptyList() }; Ads.markToolRun() }
    val x = m ?: run { RunState(spec, "Finding apps that can notify you…", null); return }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { Glass { Text("${x.size}", style = big, color = C.Ink); Body(if (Build.VERSION.SDK_INT >= 33) "apps are allowed to send you notifications. Tap one to choose which notifications it can send." else "Notification permission is per-app on Android 13+. Tap an app to manage its notifications.", C.Silver, 13) }; Spacer(Modifier.height(8.dp)) }
        items(x, key = { it.pkg }) { a -> Row(Modifier.fillMaxWidth().clickable { open(ctx, Apps.notifSettings(a.pkg)) }.padding(vertical = 8.dp)) { Text(a.label, Modifier.weight(1f), color = C.Ink, fontSize = 14.sp); Text("Manage ›", color = C.Accent2, fontSize = 12.sp) } }
        item { ResultFooter(s, "notif", listOf("Turn off 'promotions' channels but keep messages.", "Fewer wake-ups = longer battery life.")) }
    }
}


@Composable
fun ResultArt(name: String) { if (name.isEmpty() || artId(name) == 0) return
    val a by animateFloatAsState(1f, tween(600), label = "ra")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Art(name, Modifier.size(150.dp).graphicsLayer { scaleX = .8f + .2f * a; scaleY = .8f + .2f * a; alpha = a }) } }