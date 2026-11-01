package com.safespot.scanner.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

object Onboard { fun done(c: android.content.Context) = c.getSharedPreferences("ui", 0).getBoolean("onboarded", false)
    fun finish(c: android.content.Context) = c.getSharedPreferences("ui", 0).edit().putBoolean("onboarded", true).apply() }

private data class Page(val spec: ToolSpec, val title: String, val text: String, val art: String = "")
private val pages = listOf(
    Page(ToolSpec("", "", "🛡️", Color(0xFFFF5A6E), Color(0xFF8E1B3A), ""), "Find hidden spyware", "SafeSpot checks your apps against known stalkerware and malware databases, on your phone.", "onb_spyware"),
    Page(ToolSpec("", "", "🌐", Color(0xFF6FA8FF), Color(0xFF2B3BFF), ""), "Free VPN", "Encrypt your connection on public Wi-Fi with free volunteer VPN servers.", "onb_vpn"),
    Page(ToolSpec("", "", "🧹", Color(0xFFFF9A3C), Color(0xFFE5395B), ""), "Phone care", "Junk cleaner, battery, cooler, booster, Wi-Fi check, speed test: all with real readings.", "onb_care"),
    Page(ToolSpec("", "", "🧞", Color(0xFFB98CFF), Color(0xFF5B3AFF), ""), "Meet your genie", "A little helper that gives tips and tells you what to check next.", "onb_genie"))

@Composable
fun Onboarding(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) } // 0 carousel, 1 genie, 2 theme
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        AnimatedContent(step, transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) }, label = "ob") { st ->
            when (st) {
                0 -> Carousel { step = 1 }
                1 -> GeniePicker(true) { step = 2 }
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text("Pick a look", color = C.Ink, fontSize = 26.sp, fontWeight = FontWeight.Black); Body("You can change this any time in Me → Theme.", C.SilverDim, 13)
                    Spacer(Modifier.height(16.dp)); ThemePicker(); Spacer(Modifier.height(20.dp))
                    PrimaryButton("Start using SafeSpot", Modifier.fillMaxWidth(), C.Accent) { Onboard.finish(ctx); onDone() } }
            }
        }
    }
}

@Composable
private fun Carousel(next: () -> Unit) {
    val pager = rememberPagerState { pages.size }; val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row { Spacer(Modifier.weight(1f)); Text("Skip", Modifier.clickable { next() }.padding(8.dp), color = C.SilverDim) }
        HorizontalPager(pager, Modifier.weight(1f)) { i -> val p = pages[i]
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(240.dp).clip(CircleShape).background(Brush.radialGradient(listOf(p.spec.a.copy(alpha = .45f), Color.Transparent))), contentAlignment = Alignment.Center) { if (artId(p.art) != 0) Art(p.art, Modifier.fillMaxSize()) else ToolOrb(p.spec, 140.dp, pulse = true) }
                Spacer(Modifier.height(24.dp)); Text(p.title, color = C.Ink, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp)); Text(p.text, color = C.Silver, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp) } }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) { repeat(pages.size) { i ->
            val w by animateDpAsState(if (pager.currentPage == i) 22.dp else 8.dp, label = "dot")
            Box(Modifier.padding(3.dp).height(8.dp).width(w).clip(CircleShape).background(if (pager.currentPage == i) C.Accent else C.SilverDim.copy(alpha = .4f))) } }
        PrimaryButton(if (pager.currentPage == pages.size - 1) "Choose my genie" else "Next", Modifier.fillMaxWidth(), C.Accent) {
            if (pager.currentPage == pages.size - 1) next() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
    }
}

/** Boy/girl genie + name. Used in onboarding and in Me. */
@Composable
fun GeniePicker(onboarding: Boolean, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var g by remember { mutableStateOf(GenieProfile.gender) }
    var name by remember { mutableStateOf(if (GenieProfile.chosen) GenieProfile.name else GenieProfile.defaultName(g)) }
    val h = rememberHaptic()
    Column(Modifier.fillMaxWidth().then(if (onboarding) Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp) else Modifier)) {
        if (onboarding) { Text("Choose your genie", color = C.Ink, fontSize = 26.sp, fontWeight = FontWeight.Black); Body("Your genie gives tips and cheers you on.", C.SilverDim, 13); Spacer(Modifier.height(16.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf("male" to "Boy", "female" to "Girl").forEach { (k, l) ->
            val selected = g == k; val sc by animateFloatAsState(if (selected) 1.04f else .96f, spring(.5f), label = "g")
            val (res, _) = remember(k) { GenieProfile.pose(ctx, k, Mood.IDLE) }
            Column(Modifier.weight(1f).graphicsLayer { scaleX = sc; scaleY = sc }.clip(RoundedCornerShape(22.dp)).background(if (selected) C.Accent.copy(alpha = .18f) else C.Glass)
                .border(2.dp, if (selected) C.Accent else C.GlassBorder, RoundedCornerShape(22.dp))
                .clickable { h(false); if (name == GenieProfile.defaultName(g)) name = GenieProfile.defaultName(k); g = k }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (res != 0) Image(painterResource(res), "$l genie", Modifier.height(170.dp))
                Text(l, color = C.Ink, fontWeight = FontWeight.Bold) } } }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(name, { name = it.take(16) }, Modifier.fillMaxWidth(), label = { Text("Genie's name") }, singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = C.Ink, unfocusedTextColor = C.Ink, focusedBorderColor = C.Accent, unfocusedBorderColor = C.GlassBorder, focusedLabelColor = C.Accent, unfocusedLabelColor = C.SilverDim, cursorColor = C.Accent))
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (onboarding) "Next" else "Save genie", Modifier.fillMaxWidth(), C.Accent) { h(true); GenieProfile.save(ctx, g, name)
            if (g == "female" && onboarding && Themes.selected == "navy") { Themes.selected = "rose"; Themes.save(ctx, "rose") }; onDone() }
    }
}

@Composable
fun ThemePicker() {
    val ctx = LocalContext.current
    val opts = Themes.all.map { it.id to it } + listOf(Themes.SYSTEM to null)
    opts.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { (id, p) ->
        val pal = p ?: Themes.navy; val sel = Themes.selected == id
        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).border(2.dp, if (sel) C.Accent else C.GlassBorder, RoundedCornerShape(18.dp)).clickable { Themes.selected = id; Themes.save(ctx, id) }) {
            Box(Modifier.fillMaxWidth().height(70.dp).background(if (p == null) Brush.horizontalGradient(listOf(Themes.navy.top, Themes.light.top)) else Brush.verticalGradient(listOf(pal.top, pal.bottom)))) {
                Row(Modifier.align(Alignment.BottomStart).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf(pal.accent, pal.accent2, pal.green).forEach { Box(Modifier.size(14.dp).clip(CircleShape).background(it)) } }
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp, 18.dp).clip(RoundedCornerShape(6.dp)).background(pal.glass.copy(alpha = maxOf(pal.glass.alpha, .3f)))) }
            Text((if (sel) "✓ " else "") + (p?.name ?: "Follow system"), Modifier.padding(10.dp), color = C.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) } }
        if (row.size == 1) Spacer(Modifier.weight(1f)) }; Spacer(Modifier.height(10.dp)) }
}

/** "Me" tab: genie, theme, then the About content. */
@Composable
fun MeScreen(s: com.safespot.scanner.AppState) {
    var editGenie by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Title("Me & Settings")
        Section("Your genie")
        Glass { if (editGenie) GeniePicker(false) { editGenie = false } else Row(verticalAlignment = Alignment.CenterVertically) {
            val ctx = LocalContext.current; val (res, _) = remember(GenieProfile.gender) { GenieProfile.pose(ctx, GenieProfile.gender, Mood.HAPPY) }
            if (res != 0) Image(painterResource(res), null, Modifier.height(80.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(GenieProfile.name, color = C.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp); Body(if (GenieProfile.gender == "female") "Girl genie" else "Boy genie", C.SilverDim, 12) }
            GhostButton("Change") { editGenie = true } } }
        Section("Theme"); ThemePicker()
        Section("About")
        GhostButton("About SafeSpot, privacy & licences", Modifier.fillMaxWidth()) { s.tab = "about" }
        Spacer(Modifier.height(90.dp))
    }
}
