package com.safespot.scanner.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safespot.scanner.AppState
import com.safespot.scanner.BuildConfig
import com.safespot.scanner.data.Reminder
import com.safespot.scanner.scan.Cloud
import com.safespot.scanner.vpn.Vpn
import com.safespot.scanner.vpn.VpnServer
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun ToolsScreen(s: AppState) {
    val ctx = LocalContext.current
    val dh = s.device
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u -> u?.let { s.scanFolder(it) } }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> s.store.reminderOn = ok; Reminder.schedule(ctx, ok) }
    var reminder by remember { mutableStateOf(s.store.reminderOn) }
    var cloud by remember { mutableStateOf(s.store.cloudLookupConsent) }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Title("Tools"); Section("Phone Care")
            ToolCatalog.care.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { t -> ToolTile(t, Modifier.weight(1f).height(130.dp)) { s.tool = t.id } } }; Spacer(Modifier.height(12.dp)) }
            Section("Device health")
            if (dh == null) Body("Checking…")
        }
        items(dh?.checks ?: emptyList()) { c -> Glass(pad = 12.dp, onClick = c.settingsAction?.let { a -> { runCatching { ctx.startActivity(Intent(a)) } } }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(C.level(c.level)))
                Text("  " + c.title, Modifier.weight(1f), color = C.Ink, fontWeight = FontWeight.SemiBold)
                Text(c.value, color = C.level(c.level), fontSize = 12.sp)
            }
            if (c.advice.isNotBlank() && c.level != com.safespot.scanner.scan.Level.GOOD) Body(c.advice + if (c.settingsAction != null) "  ›" else "", C.SilverDim, 12)
        }; Spacer(Modifier.height(8.dp)) }
        item {
            GhostButton("Re-check device") { s.refreshDevice() }
            Section("Shortcuts")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Accessibility") { ctx.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                GhostButton("Notification access") { ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Device admins") { runCatching { ctx.startActivity(Intent().setClassName("com.android.settings", "com.android.settings.DeviceAdminSettings")) }.onFailure { ctx.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)) } }
                GhostButton("Security") { ctx.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)) }
            }
            Section("Databases")
            Glass {
                Body("Stalkerware indicators: ${s.iocSize} (${s.iocVersion})", C.Ink)
                Body("Malware hash DB: ${s.mdbSize} entries (${s.mdbVersion})", C.Ink)
                val chk = ctx.getSharedPreferences("db", 0).getLong("checked", 0)
                Body("Last update check: ${if (chk == 0L) "never" else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(chk))}", C.SilverDim, 12)
                Spacer(Modifier.height(8.dp)); PrimaryButton(if (s.dbBusy) "Updating…" else "Update databases", Modifier.fillMaxWidth(), C.Blue) { s.updateDb() }
                if (s.dbMsg.isNotBlank()) Body(s.dbMsg, C.SilverDim, 12)
            }
            Section("Scan APK files")
            Glass {
                Body("Pick a folder (for example Download). SafeSpot computes the SHA-256 of every .apk file and checks it against the malware DB.", C.Silver, 13)
                Spacer(Modifier.height(8.dp)); PrimaryButton(if (s.fileScanBusy) "Scanning…" else "Choose folder", Modifier.fillMaxWidth(), C.Accent2) { folder.launch(null) }
                s.fileHits.forEach { f -> Spacer(Modifier.height(6.dp)); Text(f.name, color = C.Ink, fontSize = 13.sp)
                    Body((f.match?.let { "⚠ MALWARE: $it" } ?: "Not in local DB") + (f.cloud?.let { " · Cloud: $it" } ?: ""), if (f.match != null) C.Red else C.Green, 12) }
                if (s.fileHits.isEmpty() && !s.fileScanBusy) Body("No files scanned yet.", C.SilverDim, 12)
            }
            Section("Cloud lookup (optional)")
            Glass {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Body("Send SHA-256 hashes (never files) of sideloaded apps to MalwareBazaar by abuse.ch", C.Ink, 13)
                    Spacer(Modifier.weight(1f))
                    Switch(cloud, { cloud = it; s.store.cloudLookupConsent = it }, enabled = Cloud.available)
                }
                if (!Cloud.available) Body("Not available in this build (no abuse.ch API key configured).", C.SilverDim, 12)
                else if (cloud) { PrimaryButton(if (s.cloudBusy) "Checking…" else "Check now", Modifier.fillMaxWidth(), C.Blue, enabled = s.result != null || s.fileHits.isNotEmpty()) { s.cloudCheck() }
                    if (s.cloudMsg.isNotBlank()) Body(s.cloudMsg, if (s.cloudMsg.startsWith("MALWARE")) C.Red else C.Green, 12) }
            }
            Section("Protection")
            Glass {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Body("Daily check & reminder", C.Ink); Body("Once a day: checks newly installed/updated apps and reminds you to keep your streak.", C.SilverDim, 12) }
                    Switch(reminder, { on -> reminder = on
                        if (on && Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) else { s.store.reminderOn = on; Reminder.schedule(ctx, on) } })
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun VpnScreen(s: AppState) {
    val ctx = androidx.activity.compose.LocalActivity.current!!
    LaunchedEffect(Unit) { s.loadServers() }
    var picking by remember { mutableStateOf(false) }
    var safeDns by remember { mutableStateOf(s.store.safeDns) }
    var err by remember { mutableStateOf<String?>(null) }
    val prep = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) startVpn(s, ctx, safeDns) else err = "VPN permission was not granted." }
    fun toggle() {
        if (Vpn.state == Vpn.State.CONNECTED || Vpn.state == Vpn.State.CONNECTING) { Vpn.disconnect(ctx); return }
        if (s.servers.isEmpty()) { err = "No servers yet. Check your internet."; s.loadServers(true); return }
        err = null
        val i = VpnService.prepare(ctx)
        if (i != null) prep.launch(i) else startVpn(s, ctx, safeDns)
    }
    if (picking) { ServerPicker(s) { picking = false }; return }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Vpn.state) { while (Vpn.state == Vpn.State.CONNECTED) { now = SystemClock.elapsedRealtime(); delay(1000) } }
    LazyColumn(contentPadding = PaddingValues(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Title("VPN", Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            ConnectButton(Vpn.state) { toggle() }
            Spacer(Modifier.height(10.dp))
            val (txt, col) = when (Vpn.state) { Vpn.State.CONNECTED -> "Connected" to C.Green; Vpn.State.CONNECTING -> "Connecting…" to C.Amber; Vpn.State.ERROR -> "Failed" to C.Red; else -> "Not connected" to C.SilverDim }
            Text(txt, color = col, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (Vpn.state == Vpn.State.CONNECTED && Vpn.connectedAt > 0) { val sec = (now - Vpn.connectedAt) / 1000
                Text("%02d:%02d:%02d".format(sec / 3600, sec / 60 % 60, sec % 60), color = C.Ink, fontSize = 28.sp, fontWeight = FontWeight.Black) }
            if (Vpn.detail.isNotBlank() && Vpn.state != Vpn.State.CONNECTED) Body(Vpn.detail, C.SilverDim, 11)
            err?.let { Body(it, C.Red, 12) }
            Spacer(Modifier.height(16.dp))
            Glass(Modifier.fillMaxWidth(), onClick = { if (Vpn.state == Vpn.State.OFF || Vpn.state == Vpn.State.ERROR) picking = true }) {
                val sv = s.selected
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (s.autoServer && Vpn.state == Vpn.State.OFF) "⚡" else sv?.flag ?: "🌐", fontSize = 30.sp)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(if (s.autoServer && Vpn.state == Vpn.State.OFF && s.servers.isNotEmpty()) "Fastest server (auto)" else sv?.country ?: if (s.loadingServers) "Loading servers…" else "No servers available", color = C.Ink, fontWeight = FontWeight.Bold)
                        Body(sv?.let { "${it.host} · ${it.proto.uppercase()} ${it.port} · ${it.provider}" } ?: s.serverSource, C.SilverDim, 12)
                    }
                    sv?.let { PingText(it.pingMs) }; Text("  ›", color = C.Silver, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("↓ " + fmtBytes(Vpn.bytesIn), "Downloaded", C.Cyan, Modifier.weight(1f))
                StatCard("↑ " + fmtBytes(Vpn.bytesOut), "Uploaded", C.Red, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Glass(Modifier.fillMaxWidth()) {
                Row { Text("Live speed", Modifier.weight(1f), color = C.Ink, fontWeight = FontWeight.SemiBold)
                    Text("↓ ${fmtBytes((Vpn.speedDown.lastOrNull() ?: 0f).toLong())}/s  ↑ ${fmtBytes((Vpn.speedUp.lastOrNull() ?: 0f).toLong())}/s", color = C.SilverDim, fontSize = 12.sp) }
                Spacer(Modifier.height(8.dp))
                LineChart(Vpn.speedDown.toList(), C.Cyan, second = Vpn.speedUp.toList(), secondColor = C.Red)
            }
            Spacer(Modifier.height(12.dp))
            Glass(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Body("Malware-blocking DNS", C.Ink); Body("Uses Quad9 (9.9.9.9), which refuses to resolve known malicious domains. Applies on next connect.", C.SilverDim, 12) }
                    Switch(safeDns, { safeDns = it; s.store.safeDns = it })
                }
            }
            Spacer(Modifier.height(12.dp))
            Glass(Modifier.fillMaxWidth()) {
                Text("About these servers", color = C.Amber, fontWeight = FontWeight.SemiBold)
                Body("Free servers are run by volunteers of the VPN Gate Academic Experiment (University of Tsukuba, Japan). Speed and uptime vary and operators may log connections. Don't use them for banking or anything sensitive. SafeSpot shows no ads inside the VPN and doesn't touch your traffic.", C.Silver, 12)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

fun startVpn(s: AppState, ctx: android.content.Context, safeDns: Boolean) {
    val ranked = s.rankedServers
    val manual = s.selected?.takeIf { !s.autoServer }
    Vpn.connectSmart(ctx, if (manual != null) listOf(manual) + ranked.filter { it.id != manual.id } else ranked, safeDns, probe = manual == null) { s.selected = it }
    s.store.unlockVpn()
}

@Composable fun PingText(ms: Int) = Text(if (ms == -2) "no reply" else if (ms < 0) "—" else "$ms ms", color = when { ms < 0 -> C.SilverDim; ms < 150 -> C.Green; ms < 350 -> C.Amber; else -> C.Red }, fontSize = 12.sp)

@Composable
fun ConnectButton(state: Vpn.State, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "c")
    val pulse by t.animateFloat(1f, 1.18f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "p")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "s")
    val col = when (state) { Vpn.State.CONNECTED -> C.Green; Vpn.State.CONNECTING -> C.Amber; Vpn.State.ERROR -> C.Red; else -> C.Blue }
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().scale(if (state == Vpn.State.CONNECTED) pulse else 1f)) { drawCircle(col.copy(alpha = .12f)); drawCircle(col.copy(alpha = .25f), size.minDimension * .4f) }
        if (state == Vpn.State.CONNECTING) Canvas(Modifier.size(170.dp)) { drawArc(col, spin, 90f, false, style = Stroke(8f)) }
        val art = when (state) { Vpn.State.CONNECTED -> "btn_vpn_on"; Vpn.State.CONNECTING -> "btn_vpn_connecting"; else -> "btn_vpn_off" }
        if (artId(art) != 0) { Art(art, Modifier.size(170.dp).scale(if (state == Vpn.State.CONNECTING) .96f + .04f * pulse else 1f).clip(CircleShape).clickable { onClick() })
            Text(if (state == Vpn.State.CONNECTED) "TAP TO STOP" else if (state == Vpn.State.CONNECTING) "CONNECTING…" else "TAP TO CONNECT", Modifier.align(Alignment.BottomCenter), color = col, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        } else
        Box(Modifier.size(140.dp).clip(CircleShape).background(Brush.radialGradient(listOf(col, col.copy(alpha = .55f), C.NavyDeep))).clickable { onClick() }, contentAlignment = Alignment.Center) {
            Text(if (state == Vpn.State.CONNECTED) "STOP" else if (state == Vpn.State.CONNECTING) "CANCEL" else "CONNECT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
fun ServerPicker(s: AppState, close: () -> Unit) {
    val byCountry = s.servers.groupBy { it.country }.toList().sortedBy { it.first }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { GhostButton("← Back") { close() }; Spacer(Modifier.weight(1f)); GhostButton(if (s.loadingServers) "…" else "Refresh") { s.loadServers(true) } }
            Spacer(Modifier.height(8.dp)); Title("Choose a location"); Body("${s.servers.size} servers · ${s.serverSource}", C.SilverDim, 12); Spacer(Modifier.height(8.dp))
            Glass(pad = 12.dp, onClick = { s.setAuto(true); close() }) { Row(verticalAlignment = Alignment.CenterVertically) {
                Art("ic_server_auto", Modifier.size(36.dp)) { Text("⚡", fontSize = 22.sp) }; Column(Modifier.weight(1f).padding(start = 10.dp)) { Text("Fastest server (auto)", color = C.Ink, fontWeight = FontWeight.Bold)
                    Body("Tests the best 8 servers at once and connects to the first that answers. Switches automatically if one stalls.", C.SilverDim, 11) }
                if (s.autoServer) Text("✓", color = C.Green, fontSize = 18.sp) } }
            Spacer(Modifier.height(6.dp)) }
        byCountry.forEach { (country, list) ->
            item { Section("${list.first().flag}  $country (${list.size})") }
            items(list.sortedWith(compareBy<VpnServer> { if (it.pingMs < 0) Int.MAX_VALUE else it.pingMs }.thenByDescending { it.speedMbps }).take(6), key = { it.id }) { sv ->
                Glass(pad = 12.dp, onClick = { s.selected = sv; s.setAuto(false); close() }) { Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(sv.host, color = C.Ink, fontSize = 13.sp); Body("${sv.proto.uppercase()} ${sv.port}" + if (sv.speedMbps > 0) " · %.0f Mbps listed".format(sv.speedMbps) else "", C.SilverDim, 11) }
                    PingText(sv.pingMs) } }
                Spacer(Modifier.height(6.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    fun open(u: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
    LazyColumn(contentPadding = PaddingValues(16.dp)) { item {
        Title("About SafeSpot"); Body("Version ${BuildConfig.VERSION_NAME}", C.SilverDim, 12)
        Section("What SafeSpot does")
        Glass { Body("SafeSpot checks the apps on your phone for known stalkerware (by package name and signing certificate), known malware (by APK SHA-256), and warning signs such as accessibility or device-admin abuse, hidden icons and sideloading. Results are indicators, not proof. No app can guarantee a phone is spyware-free; if you feel unsafe, contact a support organisation and consider a factory reset.", C.Ink, 13) }
        Section("Privacy")
        Glass { Body("Scans run on the device. App lists are never uploaded. The optional cloud lookup sends only file hashes, and only if you turn it on. Ads (Google AdMob) appear only in SafeSpot's screens, never in VPN traffic.", C.Ink, 13) }
        Section("Credits & licences")
        Glass(onClick = { open("https://github.com/AssoEchap/stalkerware-indicators") }) { Text("Stalkerware indicators", color = C.Ink, fontWeight = FontWeight.SemiBold)
            Body("© Echap and contributors, CC BY 4.0. github.com/AssoEchap/stalkerware-indicators", C.SilverDim, 12) }
        Spacer(Modifier.height(8.dp))
        Glass(onClick = { open("https://www.vpngate.net/") }) { Text("VPN Gate", color = C.Ink, fontWeight = FontWeight.SemiBold)
            Body("Free relay servers from the VPN Gate Academic Experiment Service, University of Tsukuba. vpngate.net", C.SilverDim, 12) }
        Spacer(Modifier.height(8.dp))
        Glass(onClick = { open("https://github.com/schwabe/ics-openvpn") }) { Text("ics-openvpn / OpenVPN", color = C.Ink, fontWeight = FontWeight.SemiBold)
            Body("© Arne Schwabe, GPL-2.0. OpenVPN © OpenVPN Inc. OpenSSL, LZ4, LZO under their own licences.", C.SilverDim, 12) }
        Spacer(Modifier.height(8.dp))
        Glass(onClick = { open("https://bazaar.abuse.ch/") }) { Text("MalwareBazaar (optional)", color = C.Ink, fontWeight = FontWeight.SemiBold); Body("abuse.ch, CC0.", C.SilverDim, 12) }
        Spacer(Modifier.height(8.dp))
        Glass(onClick = { open("https://quad9.net/") }) { Text("Quad9 DNS", color = C.Ink, fontWeight = FontWeight.SemiBold); Body("Malware-blocking DNS resolver, quad9.net", C.SilverDim, 12) }
        Section("Source code")
        Glass { Body("SafeSpot is free software under the GNU GPL v3. You can get the source code, change it and share it under the same licence.", C.Ink, 13) }
        Spacer(Modifier.height(80.dp))
    } }
}
