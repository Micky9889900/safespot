package com.safespot.scanner

import android.os.Looper
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StartupTest {
    @Test fun appAndMainActivityStart() {
        val c = Robolectric.buildActivity(MainActivity::class.java).setup()
        repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(100) }
        val a = c.get()
        val nf = File(a.filesDir, "nonfatal.txt")
        if (nf.exists()) println("NONFATAL:\n" + nf.readText())
        assertFalse(a.isFinishing)
        c.pause().stop().destroy()
    }
    @Test fun crashScreenShowsAfterCrash() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        CrashReporter.install(ctx)
        val h = Thread.getDefaultUncaughtExceptionHandler()!!
        val prev = Thread.currentThread()
        // simulate a crash without killing the test JVM: call the handler with no chained previous handler effect
        runCatching { h.uncaughtException(prev, IllegalStateException("boom-test")) }
        val txt = CrashReporter.pending(ctx)
        org.junit.Assert.assertTrue(txt != null && txt.contains("boom-test"))
        val c = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(c.get().isFinishing)
        CrashReporter.clear(ctx)
    }
    @Test fun bundledDbsParse() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        println("ioc=" + com.safespot.scanner.scan.IocDb.load(ctx).byPackage.size + " mdb=" + com.safespot.scanner.scan.MalwareDb.load(ctx).size)
    }
}
