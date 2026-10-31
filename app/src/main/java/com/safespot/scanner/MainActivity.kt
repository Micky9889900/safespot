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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }
        val crash = CrashReporter.pending(this)
        if (crash != null) { setContent { SafeSpotTheme { CrashScreen(crash) { CrashReporter.clear(this); recreate() } } }; return }
        try { Vpn.init(this) } catch (t: Throwable) { CrashReporter.nonFatal("Vpn.init", t) }
        try { Ads.init(this) } catch (t: Throwable) { CrashReporter.nonFatal("Ads.init", t) }
        val state = AppState(applicationContext)
        try { if (state.store.reminderOn) Reminder.schedule(this, true) } catch (t: Throwable) { CrashReporter.nonFatal("Reminder.schedule", t) }
        setContent { SafeSpotTheme { Root(state) } }
    }
}

@androidx.compose.runtime.Composable
fun CrashScreen(report: String, onContinue: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().background(C.bgBrush).systemBarsPadding().padding(16.dp)) {
        Text("Crash report", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text("SafeSpot closed unexpectedly last time. Please tap Copy and send this to sipofdiamond@gmail.com so we can fix it.", color = C.SilverDim, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({
                val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("SafeSpot crash", report))
                android.widget.Toast.makeText(ctx, "Copied", android.widget.Toast.LENGTH_SHORT).show()
            }) { Text("Copy") }
            OutlinedButton(onContinue) { Text("Continue", color = Color.White) }
        }
        Text(report, color = Color.White, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.padding(top = 12.dp).weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()))
    }
}

private val tabs = listOf(Triple("home", "Home", Icons.Filled.Home), Triple("scan", "Scan", Icons.Filled.Search), Triple("vpn", "VPN", Icons.Filled.VpnKey),
    Triple("tools", "Tools", Icons.Filled.Build), Triple("about", "About", Icons.Filled.Info))

@androidx.compose.runtime.Composable
fun Root(s: AppState) {
    BackHandler(s.detail != null || s.showDiag || s.tab != "home") { when { s.detail != null -> s.detail = null; s.showDiag -> s.showDiag = false; else -> s.tab = "home" } }
    Scaffold(containerColor = Color.Transparent, modifier = Modifier.background(C.bgBrush),
        bottomBar = { Column {
            BannerAd()
            NavigationBar(containerColor = C.NavyDeep.copy(alpha = .96f)) {
                tabs.forEach { (k, l, ic: ImageVector) -> NavigationBarItem(s.tab == k && s.detail == null && !s.showDiag, { s.tab = k; s.detail = null; s.showDiag = false },
                    { Icon(ic, l) }, label = { Text(l) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, selectedTextColor = Color.White,
                        indicatorColor = C.Red.copy(alpha = .35f), unselectedIconColor = C.SilverDim, unselectedTextColor = C.SilverDim)) }
            } } }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val d = s.detail
            when {
                d != null -> AppDetail(d) { s.detail = null }
                s.showDiag -> Diagnostics(s) { s.showDiag = false }
                else -> when (s.tab) { "home" -> HomeScreen(s); "scan" -> ScanScreen(s); "vpn" -> VpnScreen(s); "tools" -> ToolsScreen(s); else -> AboutScreen() }
            }
        }
    }
}
