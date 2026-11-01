package com.safespot.scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safespot.scanner.AppState
import com.safespot.scanner.care.*
import com.safespot.scanner.vpn.Vpn
import java.util.Calendar

fun Modifier.graphicsLayerAlpha(a: Float) = this.alpha(a)

@Composable
fun HomeHero(s: AppState) {
    val ctx = LocalContext.current; val r = s.result; val last = s.store.history().lastOrNull()
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greet = when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    val (res, _) = remember(GenieProfile.gender) { GenieProfile.pose(ctx, GenieProfile.gender, Mood.IDLE) }
    Glass(pad = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greet, color = C.SilverDim, fontSize = 13.sp)
                Text("${GenieProfile.name} is watching over your phone", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                Spacer(Modifier.height(6.dp))
                if (s.store.streak > 0) Pill("🔥 ${s.store.streak}-day streak", C.Amber)
            }
            if (res != 0) androidx.compose.foundation.Image(painterResource(res), null, Modifier.height(96.dp))
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ScoreRing(r?.score ?: last?.score, 190.dp) }
        Body(if (s.store.lastScan == 0L) "No scan yet: run your first scan to get a score." else "Last scan ${fmtAgo(s.store.lastScan)}", C.SilverDim, 12)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(if (s.scanning) "Scanning…" else "Scan now", Modifier.fillMaxWidth(), C.Accent) { s.scan(false) }
    }
}

@Composable
fun StatusChips(s: AppState) {
    val ctx = LocalContext.current
    val bat = remember { Battery.read(ctx) }; val mem = remember { Memory.read(ctx) }; val st = remember { StorageInfo.read(ctx, null) }
    val chips = listOf(
        Triple("🔋 ${bat.level ?: "--"}%", if ((bat.level ?: 100) < 20) C.Red else C.Green, "battery"),
        Triple("🌡 ${bat.tempC?.let { "%.0f°C".format(it) } ?: "--"}", if ((bat.tempC ?: 0f) > 40) C.Red else C.Cyan, "cooler"),
        Triple("🧠 RAM ${mem.pct}%", if (mem.pct > 85) C.Amber else C.Blue, "booster"),
        Triple("💾 ${fmtBytes(st.free)} free", if (st.free * 10 < st.total) C.Red else C.Green, "storage"),
        Triple(if (Vpn.state == Vpn.State.CONNECTED) "🔒 VPN on" else "🔓 VPN off", if (Vpn.state == Vpn.State.CONNECTED) C.Green else C.SilverDim, "vpn"))
    Spacer(Modifier.height(12.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(chips) { (t, col, go) ->
        Text(t, Modifier.clip(RoundedCornerShape(50)).background(col.copy(alpha = .16f)).clickable { if (go == "vpn") s.tab = "vpn" else s.tool = go }.padding(horizontal = 14.dp, vertical = 8.dp),
            color = C.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) } }
}

/** Big tile grid on Home: Scan / Antivirus / VPN + all Phone Care tools. */
@Composable
fun HomeGrid(s: AppState) {
    val core = listOf(ToolSpec("#scan", "Spyware Scan", "🛡️", Color(0xFFFF5A6E), Color(0xFF8E1B3A), "Find stalkerware & risky apps"),
        ToolSpec("#deep", "Antivirus", "🦠", Color(0xFF3CE8A0), Color(0xFF117A5A), "Deep scan vs malware database"),
        ToolSpec("#vpn", "Free VPN", "🌐", Color(0xFF6FA8FF), Color(0xFF2B3BFF), "Encrypt public Wi-Fi"))
    val all = core + ToolCatalog.care.filter { it.id != "notif" }
    all.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { t ->
        ToolTile(t, Modifier.weight(1f).height(138.dp)) { when (t.id) { "#scan" -> s.scan(false); "#deep" -> s.scan(true); "#vpn" -> s.tab = "vpn"; else -> s.tool = t.id } } }
        if (row.size == 1) Spacer(Modifier.weight(1f)) }; Spacer(Modifier.height(12.dp)) }
}

/** One recommendation driven by real device state. */
@Composable
fun Recommended(s: AppState) {
    val ctx = LocalContext.current
    val rec = remember(s.historyTick, s.activityTick) {
        val b = Battery.read(ctx); val st = StorageInfo.read(ctx, null); val m = Memory.read(ctx); val r = s.result
        when {
            r != null && r.risky.isNotEmpty() -> Triple("Review ${r.risky.size} risky app(s)", "Your last scan flagged them.", "#scan")
            s.store.lastScan == 0L || System.currentTimeMillis() - s.store.lastScan > 3 * 86400_000L -> Triple("Time for a spyware scan", if (s.store.lastScan == 0L) "You haven't scanned yet." else "Last scan ${fmtAgo(s.store.lastScan)}.", "#scan")
            (b.tempC ?: 0f) > 39 -> Triple("Your phone is warm (%.0f °C)".format(b.tempC), "Open the CPU Cooler for tips and a cool-down timer.", "cooler")
            st.free * 7 < st.total -> Triple("Storage almost full", "Only ${fmtBytes(st.free)} free. Find junk files.", "junk")
            (b.level ?: 100) < 25 && b.status != "Charging" -> Triple("Battery at ${b.level}%", "See what's using it.", "battery")
            m.pct > 85 -> Triple("Memory ${m.pct}% used", "Try the Booster.", "booster")
            else -> Triple("Check your Wi-Fi", "Make sure the network you're on is encrypted.", "wifi")
        }
    }
    Section("Recommended for you")
    Glass(onClick = { if (rec.third == "#scan") s.scan(false) else s.tool = rec.third }) { Row(verticalAlignment = Alignment.CenterVertically) {
        Text("✨", fontSize = 30.sp); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(rec.first, color = C.Ink, fontWeight = FontWeight.Bold); Body(rec.second, C.SilverDim, 12) }
        Text("›", color = C.Ink, fontSize = 24.sp) } }
}

@Composable
fun Timeline(s: AppState) {
    s.activityTick
    val a = remember(s.activityTick) { s.activity().take(6) }
    if (a.isEmpty()) return
    Section("Recent activity")
    Glass { a.forEachIndexed { i, (t, ic, x) -> Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(28.dp).clip(CircleShape).background(C.Accent2.copy(alpha = .2f)), contentAlignment = Alignment.Center) { Text(ic, fontSize = 14.sp) }
            if (i < a.size - 1) Box(Modifier.width(2.dp).height(18.dp).background(C.SilverDim.copy(alpha = .3f))) }
        Column(Modifier.padding(start = 10.dp)) { Text(x, color = C.Ink, fontSize = 13.sp); Text(fmtAgo(t), color = C.SilverDim, fontSize = 11.sp) } } } }
}
