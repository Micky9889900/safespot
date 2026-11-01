package com.safespot.scanner

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.safespot.scanner.ui.*
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ToolsSmokeTest {
    private fun render(theme: String = "navy", content: @androidx.compose.runtime.Composable (AppState) -> Unit) {
        Themes.selected = theme
        val c = Robolectric.buildActivity(ComponentActivity::class.java).setup(); val a = c.get()
        val s = AppState(a.applicationContext)
        a.setContent { SafeSpotTheme { content(s) } }
        repeat(30) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200)) }
        assertFalse(a.isFinishing); c.pause().stop().destroy()
    }
    @Test fun everyToolRenders() { ToolCatalog.care.forEach { t -> println("tool ${t.id}"); render { ToolHost(it, t.id) } } }
    @Test fun everyToolRendersLight() { ToolCatalog.care.forEach { t -> render("light") { ToolHost(it, t.id) } } }
    @Test fun homeMeOnboardingRender() { Themes.all.forEach { th -> render(th.id) { HomeScreen(it) } }; render { MeScreen(it) }; render("rose") { Onboarding {} } }
    @Test fun genieOverlayAllGenders() { listOf("male", "female").forEach { g -> GenieProfile.gender = g; render { GenieOverlay(it) } } }
    @Test fun genieFallbackResolves() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        listOf("male", "female").forEach { g -> Mood.entries.forEach { m -> val (id, _) = GenieProfile.pose(ctx, g, m); org.junit.Assert.assertTrue("$g $m", id != 0) } }
    }
}
