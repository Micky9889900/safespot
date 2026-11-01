package com.safespot.scanner

import com.safespot.scanner.scan.*
import com.safespot.scanner.vpn.Servers
import com.safespot.scanner.vpn.Vpn
import com.safespot.scanner.vpn.VpnServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScanSpeedTest {
    private val ctx get() = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun sha(s: String, alg: String = "SHA-256") = Scanner.hex(MessageDigest.getInstance(alg).digest(s.toByteArray()))

    @Test fun matcherBenchmark300Apps() {
        val ioc = Dbs.ioc(ctx); val mdb = Dbs.mdb(ctx)
        val knownPkg = ioc.byPackage.keys.first(); val knownHash = mdb.sha256.keys.first()
        val apps = (0 until 300).map { i -> Triple("com.example.app$i", listOf(sha("c$i")), listOf(sha("c$i", "SHA-1"))) }
        repeat(3) { apps.forEach { (p, a, b) -> Matcher.match(p, a, b, sha(p), ioc, mdb) } } // warm-up
        val t = System.nanoTime()
        var hits = 0
        repeat(10) { apps.forEach { (p, a, b) -> if (Matcher.match(p, a, b, null, ioc, mdb).let { it.first != null || it.second != null }) hits++ } }
        val perScanMs = (System.nanoTime() - t) / 1e6 / 10
        println("BENCH matcher: 300 apps in %.3f ms per scan (ioc=${ioc.byPackage.size + ioc.byCert.size}, mdb=${mdb.size})".format(perScanMs))
        assertEquals(0, hits)
        assertNotNull(Matcher.match(knownPkg, emptyList(), emptyList(), null, ioc, mdb).first)
        assertNotNull(Matcher.match("x.y", emptyList(), emptyList(), knownHash, ioc, mdb).second)
        assertTrue("matching must be well under 50 ms for 300 apps", perScanMs < 50)
    }
    @Test fun dbsCachedInMemory() { val a = Dbs.mdb(ctx); assertSame(a, Dbs.mdb(ctx)) }
    @Test fun quickAndDeepScanRun() = runBlocking {
        var calls = 0
        val t = System.nanoTime(); val q = Scanner.scan(ctx, false) { calls++ }; val qMs = (System.nanoTime() - t) / 1e6
        val t2 = System.nanoTime(); val d = Scanner.scan(ctx, true) { }; val dMs = (System.nanoTime() - t2) / 1e6
        println("BENCH scan (Robolectric, ${q.apps.size}/${d.apps.size} apps): quick=%.0f ms deep=%.0f ms, progress callbacks=$calls".format(qMs, dMs))
        assertTrue(q.apps.all { it.apkSha256 == null })
    }
    @Test fun vpnConfigPatchedAndRanked() {
        val cfg = "client\nproto tcp\nremote 1.2.3.4 443\nresolv-retry infinite\nconnect-retry 5 300\n"
        val p = Vpn.patchConfig(cfg, false)
        assertFalse(p.contains("infinite")); assertTrue(p.contains("connect-timeout 6")); assertTrue(p.contains("server-poll-timeout 6"))
        val s = { id: String, proto: String, sp: Double, ping: Int -> VpnServer(id, "openvpn", "h", 443, proto, "X", "XX", "", "vpngate", sp, 0, ping) }
        val r = Servers.rank(listOf(s("slowdead", "tcp", 300.0, -2), s("udp", "udp", 50.0, -1), s("tcp", "tcp", 50.0, -1), s("measured", "tcp", 10.0, 40)))
        assertEquals(listOf("measured", "udp", "tcp", "slowdead"), r.map { it.id })
        val bundled = Servers.parseJson(java.io.File("../pages/vpn/servers.json").readText())
        println("servers.json entries=${bundled.size}"); assertTrue(bundled.size in 10..60)
    }
}
