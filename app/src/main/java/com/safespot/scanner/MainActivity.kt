package com.safespot.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.safespot.scanner.ads.Ads
import com.safespot.scanner.ads.BannerAd
import com.safespot.scanner.data.Reminder
import com.safespot.scanner.ui.*
import com.safespot.scanner.vpn.Vpn

/** Where crash reports are emailed (placeholder until the final address is confirmed). */
const val CRASH_EMAIL = "safespot.crashreports@gmail.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }
        val crash = CrashReporter.pending(this)
        if (crash != null) { setContent { SafeSpotTheme { CrashScreen(crash) { CrashReporter.clear(this); recreate() } } }; return }
        try { Vpn.init(this) } catch (t: Throwable) { CrashReporter.nonFatal("Vpn.init", t) }
        try { Ads.init(this) } catch (t: Throwable) { CrashReporter.nonFatal("Ads.init", t) }
        runCatching { Themes.selected = Themes.pref(this); GenieProfile.load(this); com.safespot.scanner.care.Battery.record(this) }
        val state = AppState(applicationContext)
        try { if (state.store.reminderOn) Reminder.schedule(this, true) } catch (t: Throwable) { CrashReporter.nonFatal("Reminder.schedule", t) }
        setContent { SafeSpotTheme { Root(state) } }
    }
}

@androidx.compose.runtime.Composable
fun CrashScreen(report: String, onContinue: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().background(C.bgBrush).systemBarsPadding().padding(16.dp)) {
        com.safespot.scanner.ui.Art("ill_crash", Modifier.size(140.dp).align(androidx.compose.ui.Alignment.CenterHorizontally))
        Text("Crash report", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text("SafeSpot closed unexpectedly last time. Please tap Send report (or Copy and email it to $CRASH_EMAIL) so we can fix it.", color = C.SilverDim, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({
                val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("SafeSpot crash", report))
                android.widget.Toast.makeText(ctx, "Copied", android.widget.Toast.LENGTH_SHORT).show()
            }) { Text("Copy") }
            Button({
                val i = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"))
                    .putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(CRASH_EMAIL))
                    .putExtra(android.content.Intent.EXTRA_SUBJECT, "SafeSpot crash ${com.safespot.scanner.BuildConfig.VERSION_NAME}")
                    .putExtra(android.content.Intent.EXTRA_TEXT, report)
                runCatching { ctx.startActivity(android.content.Intent.createChooser(i, "Send report")) }
                    .onFailure { android.widget.Toast.makeText(ctx, "No email app found. Tap Copy instead.", android.widget.Toast.LENGTH_LONG).show() }
            }) { Text("Send report") }
            OutlinedButton(onContinue) { Text("Continue", color = Color.White) }
        }
        Text(report, color = Color.White, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.padding(top = 12.dp).weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()))
    }
}

private val tabs = listOf(Triple("home", "Home", Icons.Filled.Home), Triple("scan", "Scan", Icons.Filled.Search), Triple("vpn", "VPN", Icons.Filled.VpnKey),
    Triple("tools", "Tools", Icons.Filled.Build), Triple("me", "Me", Icons.Filled.Person))

@androidx.compose.runtime.Composable
fun Root(s: AppState) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var onboarded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Onboard.done(ctx)) }
    val phase = rememberBgPhase()
    if (!onboarded) { Box(Modifier.fillMaxSize().animatedBg(phase)) { Onboarding { onboarded = true } }; return }
    val act = androidx.activity.compose.LocalActivity.current
    BackHandler(s.detail != null || s.showDiag || s.tool != null || s.tab != "home") { when {
        s.detail != null -> s.detail = null; s.showDiag -> s.showDiag = false
        s.tool != null -> if (act != null) Ads.onReturnFromTool(act) { s.tool = null } else s.tool = null
        s.tab == "about" -> s.tab = "me"; else -> s.tab = "home" } }
    Box(Modifier.fillMaxSize().animatedBg(phase)) {
    val bgName = when { s.tab == "scan" && s.detail == null && s.tool == null -> "bg_scan"; s.tab == "vpn" -> "bg_vpn_map"
        com.safespot.scanner.ui.BgHint.result -> "bg_result"; s.tab == "tools" && s.tool == null -> "bg_tools"
        else -> "bg_home_" + when (C.pal.id) { "navy" -> "dark"; else -> C.pal.id } }
    com.safespot.scanner.ui.Art(bgName, Modifier.fillMaxSize(), androidx.compose.ui.layout.ContentScale.Crop, alpha = if (bgName == "bg_vpn_map") .55f else .85f)
    // Readability scrim: light/frost art is bright, so tint toward the palette background.
    val lightBg = C.pal.id == "light" || C.pal.id == "frost"
    Box(Modifier.fillMaxSize().background(C.pal.bottom.copy(alpha = if (lightBg) .55f else .35f)))
    Scaffold(containerColor = Color.Transparent,
        bottomBar = { Column {
            // Banner only on Home and tool lists, never on a tool's result screen (those carry one native ad).
            if (s.tool == null && s.detail == null && (s.tab == "home" || s.tab == "tools")) BannerAd()
            NavigationBar(containerColor = C.NavyDeep.copy(alpha = .96f)) {
                tabs.forEach { (k, l, ic: ImageVector) -> val sel = (s.tab == k || (k == "me" && s.tab == "about")) && s.detail == null && !s.showDiag && s.tool == null
                    val sc by androidx.compose.animation.core.animateFloatAsState(if (sel) 1.25f else 1f, androidx.compose.animation.core.spring(.4f), label = "nav")
                    NavigationBarItem(sel, { s.tab = k; s.detail = null; s.showDiag = false; s.tool = null },
                    { val gm = Modifier.size(30.dp).graphicsLayer { scaleX = sc; scaleY = sc; translationY = if (sel) -4f else 0f }
                      com.safespot.scanner.ui.Art("ic_nav_" + k + if (sel) "_active" else "", gm) { Icon(ic, l, gm) } }, label = { Text(l) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = C.Ink, selectedTextColor = C.Ink,
                        indicatorColor = C.Accent.copy(alpha = .3f), unselectedIconColor = C.SilverDim, unselectedTextColor = C.SilverDim)) }
            } } }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val key = when { s.detail != null -> "detail"; s.showDiag -> "diag"; s.tool != null -> "tool:" + s.tool; else -> s.tab }
            androidx.compose.animation.AnimatedContent(key, transitionSpec = { (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.scaleIn(initialScale = .97f)) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) }, label = "screen") { k ->
                when {
                    k == "detail" -> s.detail?.let { d -> AppDetail(d) { s.detail = null } }
                    k == "diag" -> Diagnostics(s) { s.showDiag = false }
                    k.startsWith("tool:") -> ToolHost(s, k.removePrefix("tool:"))
                    else -> when (k) { "home" -> HomeScreen(s); "scan" -> ScanScreen(s); "vpn" -> VpnScreen(s); "tools" -> ToolsScreen(s); "me" -> MeScreen(s); else -> AboutScreen() }
                }
            }
            GenieOverlay(s)
        }
    }
    }
}
