package com.safespot.scanner.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safespot.scanner.AppState
import com.safespot.scanner.ads.Ads
import com.safespot.scanner.data.Store
import com.safespot.scanner.scan.*
import com.safespot.scanner.vpn.Vpn
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(s: AppState) {
    val r = s.result; val st = s.store; s.historyTick
    val last = st.history().lastOrNull()
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { HomeHero(s) }
        item { StatusChips(s) }
        if (s.newAppAlerts.isNotEmpty()) item { Spacer(Modifier.height(12.dp))
            Glass(onClick = { s.detail = s.newAppAlerts.first() }) { Text("⚠ New/updated app needs a look", color = C.Amber, fontWeight = FontWeight.Bold)
                Body(s.newAppAlerts.joinToString { "${it.label} (${it.risk.label})" }) } }
        item {
            Section("Overview")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(r?.apps?.size?.toString() ?: last?.apps?.toString() ?: "--", "Apps scanned", C.Ink, Modifier.weight(1f))
                StatCard(r?.risky?.size?.toString() ?: last?.risky?.toString() ?: "--", "Risky apps", if ((r?.risky?.size ?: last?.risky ?: 0) > 0) C.Amber else C.Green, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(r?.permsExposed?.toString() ?: "--", "Sensitive permissions", C.Blue, Modifier.weight(1f))
                StatCard(if (st.daysProtected == 0) "--" else "${st.daysProtected}", "Days protected", C.Cyan, Modifier.weight(1f))
            }
        }
        item {
            Section("Antivirus")
            Glass {
                val th = r?.threats ?: emptyList()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (r == null) "Not scanned yet" else if (th.isEmpty()) "No known threats" else "${th.size} threat(s) found", color = if (th.isEmpty()) C.Green else C.Red, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Body("Stalkerware DB: ${s.iocSize} indicators · ${s.iocVersion}", C.SilverDim, 12)
                        Body("Malware hash DB: ${s.mdbSize} entries · ${s.mdbVersion}", C.SilverDim, 12)
                    }
                    GhostButton(if (s.dbBusy) "…" else "Update") { s.updateDb() }
                }
                th.forEach { a -> Spacer(Modifier.height(8.dp)); ThreatRow(a) { s.detail = a } }
            }
        }
        item { Recommended(s) }
        item { Section("Tools"); HomeGrid(s) }
        item { Section("Tip of the day"); Glass { Body("💡 " + Store.tipOfDay(), C.Ink) } }
        item { Timeline(s) }
        item {
            Section("Achievements")
            val un = st.unlocked
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(st.badges.sortedByDescending { it.id in un }) { b ->
                Glass(Modifier.width(96.dp), pad = 10.dp) { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(b.icon, fontSize = 30.sp, color = if (b.id in un) C.Ink else C.SilverDim.copy(alpha = .4f), modifier = Modifier.graphicsLayerAlpha(if (b.id in un) 1f else .35f))
                    Text(b.title, color = if (b.id in un) C.Ink else C.SilverDim, fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } } } }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun ThreatRow(a: AppReport, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Glass(onClick = onClick, pad = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(a.label, color = C.Ink, fontWeight = FontWeight.Bold); Body(a.malwareMatch ?: a.iocMatch ?: a.findings.firstOrNull()?.title ?: "", C.Red, 12) }
            GhostButton("Uninstall", color = C.Red) { uninstall(ctx, a.pkg) }
        }
    }
}

fun uninstall(ctx: android.content.Context, pkg: String) = ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
fun appSettings(ctx: android.content.Context, pkg: String) = ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

@Composable
fun ScanScreen(s: AppState) {
    val act = androidx.activity.compose.LocalActivity.current!!
    if (s.scanning) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Radar(s.progress, Modifier.size(260.dp))
            Spacer(Modifier.height(24.dp))
            Text("${(s.progress * 100).toInt()}%", color = C.Ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Body("Checking app ${s.scanned}: ${s.currentApp}", C.Silver, 13)
            Body("Signatures · permissions · accessibility · admins · install source", C.SilverDim, 12)
        }
        return
    }
    val r = s.result
    if (r == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Radar(0f, Modifier.size(220.dp)); Spacer(Modifier.height(24.dp))
            PrimaryButton("Quick scan", Modifier.fillMaxWidth()) { s.scan(false) { Ads.interstitial(act) } }
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Deep scan (watch an ad)", Modifier.fillMaxWidth(), C.Blue) { Ads.rewarded(act) { ok -> if (ok) s.scan(true) { Ads.interstitial(act) } } }
            Spacer(Modifier.height(10.dp))
            Body("Deep scan also hashes every installed APK (SHA-256) and checks it against the malware database.", C.SilverDim, 12)
        }
        return
    }
    var filter by remember { mutableStateOf("risky") }
    val list = when (filter) {
        "risky" -> r.apps.filter { it.risk >= Risk.LOW }
        "hidden" -> r.apps.filter { !it.hasLauncher && !it.system }
        "access" -> r.apps.filter { it.accessibilityOn || it.deviceAdmin || it.notifListener }
        "sideload" -> r.apps.filter { it.source == Source.SIDELOAD || it.source == Source.UNKNOWN }
        else -> r.apps
    }.sortedByDescending { it.score }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(r.score, 120.dp, "Score")
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Title(if (r.threats.isNotEmpty()) "${r.threats.size} threat(s)" else if (r.risky.isNotEmpty()) "${r.risky.size} app(s) to review" else "Looking good")
                    Body("${r.apps.size} apps · ${if (r.deep) "deep" else "quick"} scan · ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(r.time))}", C.SilverDim, 12)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { GhostButton("Diagnostics") { s.showDiag = true }; GhostButton("Rescan") { s.scan(r.deep) { Ads.interstitial(act) } } }
                }
            }
            if (!r.deep) { Spacer(Modifier.height(10.dp)); PrimaryButton("Unlock deep scan (watch an ad)", Modifier.fillMaxWidth(), C.Blue) { Ads.rewarded(act) { ok -> if (ok) s.scan(true) { Ads.interstitial(act) } } } }
            if (s.newBadges.isNotEmpty()) { Spacer(Modifier.height(10.dp)); Glass { Text("🏅 Achievement unlocked", color = C.Amber, fontWeight = FontWeight.Bold); Body(s.newBadges.joinToString { "${it.icon} ${it.title}" }, C.Ink) } }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("risky" to "Risky", "access" to "Access", "hidden" to "Hidden", "sideload" to "Sideloaded", "all" to "All").forEach { (k, l) ->
                    Box(Modifier.weight(1f)) { if (filter == k) Pill(l, C.Cyan) else GhostButton(l, Modifier.fillMaxWidth()) { filter = k } } }
            }
            Spacer(Modifier.height(8.dp))
            if (list.isEmpty()) Body("Nothing in this category. 👍", C.Green)
        }
        items(list, key = { it.pkg }) { a -> AppRow(a) { s.detail = a }; Spacer(Modifier.height(8.dp)) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun AppRow(a: AppReport, onClick: () -> Unit) = Glass(onClick = onClick, pad = 12.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(a.label, color = C.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(a.pkg, color = C.SilverDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (a.findings.isNotEmpty()) Text(a.findings.take(2).joinToString(" · ") { it.title }, color = C.Silver, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) { Pill(a.risk.label, C.risk(a.risk)); Text("${a.score}", color = C.SilverDim, fontSize = 11.sp) }
    }
}

@Composable
fun AppDetail(a: AppReport, back: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            GhostButton("← Back") { back() }; Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Title(a.label); Body(a.pkg, C.SilverDim, 12); Body("Version ${a.versionName ?: "?"}", C.SilverDim, 12) }
                ScoreRing(100 - a.score.coerceIn(0, 100), 96.dp, "Trust")
            }
            Spacer(Modifier.height(8.dp)); Pill("Risk: ${a.risk.label}", C.risk(a.risk))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Uninstall", Modifier.weight(1f)) { uninstall(ctx, a.pkg) }
                PrimaryButton("App settings / Disable", Modifier.weight(1f), C.Accent2) { appSettings(ctx, a.pkg) }
            }
            if (a.accessibilityOn || a.deviceAdmin || a.notifListener) { Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (a.accessibilityOn) GhostButton("Accessibility", color = C.Amber) { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    if (a.deviceAdmin) GhostButton("Device admins", color = C.Amber) { ctx.startActivity(Intent().setClassName("com.android.settings", "com.android.settings.DeviceAdminSettings")) }
                    if (a.notifListener) GhostButton("Notification access", color = C.Amber) { ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
                } }
            if (a.deviceAdmin) Body("Device-admin apps must be deactivated under Device admins before they can be uninstalled.", C.SilverDim, 12)
            Section("Why")
            if (a.findings.isEmpty()) Body("No warning signs found.", C.Green)
        }
        items(a.findings) { f -> Glass(pad = 12.dp) { Row { Text(f.title, Modifier.weight(1f), color = C.Ink, fontWeight = FontWeight.SemiBold); Text("+${f.points}", color = C.Amber) }; Body(f.detail, C.Silver, 13) }; Spacer(Modifier.height(8.dp)) }
        item {
            Section("Facts")
            Glass {
                val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
                listOf("Install source" to "${a.source.label}${a.installer?.let { " ($it)" } ?: ""}", "Launcher icon" to if (a.hasLauncher) "Yes" else "No (hidden)",
                    "Installed" to df.format(Date(a.firstInstall)), "Updated" to df.format(Date(a.lastUpdate)),
                    "Signing cert SHA-256" to (a.certSha256.firstOrNull() ?: "?"), "APK SHA-256" to (a.apkSha256 ?: "not hashed (quick scan)")).forEach { (k, v) ->
                    Text(k, color = C.SilverDim, fontSize = 11.sp); Text(v, color = C.Ink, fontSize = 13.sp); Spacer(Modifier.height(6.dp)) }
            }
            Section("Permissions granted (${a.grantedPerms.size})")
            Glass { a.grantedPerms.sortedByDescending { Permissions.map[it]?.weight ?: 0 }.forEach { p -> val info = Permissions.map[p]
                Text(p.substringAfterLast('.'), color = if ((info?.weight ?: 0) >= 8) C.Amber else C.Ink, fontSize = 13.sp)
                info?.let { Body(it.explain, C.SilverDim, 11) }; Spacer(Modifier.height(4.dp)) }
                if (a.grantedPerms.isEmpty()) Body("None") }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun Diagnostics(s: AppState, back: () -> Unit) {
    val r = s.result; s.historyTick
    val h = s.store.history()
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            GhostButton("← Back") { back() }; Spacer(Modifier.height(12.dp)); Title("Diagnostics")
            Section("Risk breakdown")
            Glass {
                if (r == null) Body("Run a scan to see the breakdown.") else Row(verticalAlignment = Alignment.CenterVertically) {
                    val parts = Risk.values().map { lv -> r.apps.count { it.risk == lv }.toFloat() to C.risk(lv) }
                    Donut(parts, "${r.apps.size}", "apps")
                    Column(Modifier.padding(start = 16.dp)) { Risk.values().forEach { lv -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).padding(1.dp)) { androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawCircle(C.risk(lv)) } }
                        Text("  ${lv.label}: ${r.apps.count { it.risk == lv }}", color = C.Ink, fontSize = 13.sp) } } }
                }
            }
            Section("Apps per sensitive permission")
            Glass { if (r == null) Body("Run a scan first.") else Bars(PermCategory.values().map { c -> c.label to r.apps.count { c in it.categories && !it.system } }.sortedByDescending { it.second }) }
            Body("User-installed apps only.", C.SilverDim, 11)
            Section("Score history")
            Glass {
                if (h.isEmpty()) Body("No scans yet.") else {
                    LineChart(h.map { it.score.toFloat() }, C.Green, fixedMax = 100f)
                    Body("${h.size} scans · best ${h.maxOf { it.score }} · latest ${h.last().score}", C.SilverDim, 12)
                }
            }
            Section("Scan log")
        }
        items(h.reversed().take(20)) { e -> Row(Modifier.padding(vertical = 4.dp)) {
            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(e.time)), Modifier.weight(1f), color = C.Silver, fontSize = 12.sp)
            Text("${if (e.deep) "deep" else "quick"} · ${e.apps} apps · ${e.risky} risky · ", color = C.SilverDim, fontSize = 12.sp)
            Text("${e.score}", color = C.score(e.score), fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
