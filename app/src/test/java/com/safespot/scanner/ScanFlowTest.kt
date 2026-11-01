package com.safespot.scanner

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.BatteryManager
import android.os.Looper
import com.safespot.scanner.scan.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/** Regression for "stuck at 100 % checking apps" (1.2.2): the scan must always end in a result. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScanFlowTest {
    private val app get() = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()

    @Before fun setUp() {
        val pm = shadowOf(app.packageManager)
        listOf("com.example.good", "com.example.broken").forEach { n ->
            pm.installPackage(PackageInfo().apply { packageName = n; versionName = "1"
                applicationInfo = ApplicationInfo().apply { packageName = n; sourceDir = "/nonexistent/$n.apk"; flags = 0 } }) }
        // Real phones always have a sticky battery broadcast; Robolectric doesn't unless we send one.
        app.sendStickyBroadcast(Intent(Intent.ACTION_BATTERY_CHANGED).putExtra(BatteryManager.EXTRA_LEVEL, 57)
            .putExtra(BatteryManager.EXTRA_TEMPERATURE, 312).putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD))
    }
    @After fun tearDown() { Scanner.faultInjector = null; Scanner.deviceChecksOverride = null }

    @Test fun deviceChecksWithBatteryDoNotThrow() {
        val dh = DeviceChecks.run(app)
        val b = dh.checks.firstOrNull { it.title == "Battery" }
        assertNotNull("battery check missing (format crash?)", b); assertTrue(b!!.value.startsWith("57% ·"))
    }

    @Test fun scanSurvivesThrowingAppAndHangingDeviceChecks() = runBlocking {
        Scanner.faultInjector = { if (it == "com.example.broken") throw SecurityException("boom") }
        Scanner.deviceChecksOverride = { Thread.sleep(60_000); DeviceChecks.fallback() } // simulates a hung system/network call
        val t = System.nanoTime(); var last: Scanner.Progress? = null
        val r = Scanner.scan(app, deep = true) { last = it }
        val ms = (System.nanoTime() - t) / 1_000_000
        assertTrue("took $ms ms", ms < Scanner.DEVICE_TIMEOUT_MS + 3000)
        assertTrue(r.apps.any { it.pkg == "com.example.good" })
        assertFalse(r.apps.any { it.pkg == "com.example.broken" })
        assertEquals(last!!.total, last!!.done)
    }

    @Test fun appStateScanAlwaysReachesResult() {
        Scanner.faultInjector = { if (it == "com.example.broken") throw IllegalStateException("bad entry") }
        Scanner.deviceChecksOverride = { throw RuntimeException("no network") }
        val s = AppState(app)
        var doneCalled = false
        s.scan(false) { doneCalled = true }
        val end = System.currentTimeMillis() + 15_000
        while ((s.scanning || !doneCalled) && System.currentTimeMillis() < end) { shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS); Thread.sleep(20) }
        assertFalse("still scanning", s.scanning)
        assertTrue(doneCalled)
        assertNotNull(s.result); assertTrue(s.result!!.apps.any { it.pkg == "com.example.good" })
    }
}
