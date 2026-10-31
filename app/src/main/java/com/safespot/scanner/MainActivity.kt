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
        enableEdgeToEdge()
        Vpn.init(this)
        Ads.init(this)
        val state = AppState(applicationContext)
        if (state.store.reminderOn) Reminder.schedule(this, true)
        setContent { SafeSpotTheme { Root(state) } }
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
