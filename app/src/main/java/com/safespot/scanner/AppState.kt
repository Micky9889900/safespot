package com.safespot.scanner

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.documentfile.provider.DocumentFile
import com.safespot.scanner.data.Badge
import com.safespot.scanner.data.Reminder
import com.safespot.scanner.data.Store
import com.safespot.scanner.scan.*
import com.safespot.scanner.vpn.Servers
import com.safespot.scanner.vpn.VpnServer
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.io.File

data class FileHit(val name: String, val sha256: String, val match: String?, val cloud: String?)

/** Single source of UI state. Everything shown comes from real scans or real network results. */
class AppState(private val ctx: Context) {
    val store = Store(ctx)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, t -> CrashReporter.nonFatal("AppState coroutine", t) })

    var tab by mutableStateOf("home")
    var detail by mutableStateOf<AppReport?>(null)
    var showDiag by mutableStateOf(false)

    var result by mutableStateOf<ScanResult?>(null); private set
    var scanning by mutableStateOf(false); private set
    var progress by mutableFloatStateOf(0f); private set
    var currentApp by mutableStateOf(""); private set
    var scanned by mutableIntStateOf(0); private set
    var newBadges by mutableStateOf<List<Badge>>(emptyList())
    var newAppAlerts by mutableStateOf<List<AppReport>>(emptyList())
    var device by mutableStateOf<DeviceHealth?>(null); private set
    var historyTick by mutableIntStateOf(0); private set

    var servers by mutableStateOf<List<VpnServer>>(emptyList()); private set
    var serverSource by mutableStateOf(""); private set
    var loadingServers by mutableStateOf(false); private set
    var selected by mutableStateOf<VpnServer?>(null)

    var dbMsg by mutableStateOf(""); private set
    var dbBusy by mutableStateOf(false); private set
    var iocVersion by mutableStateOf(""); private set
    var mdbVersion by mutableStateOf(""); private set
    var mdbSize by mutableIntStateOf(0); private set
    var iocSize by mutableIntStateOf(0); private set
    var fileHits by mutableStateOf<List<FileHit>>(emptyList()); private set
    var fileScanBusy by mutableStateOf(false); private set
    var cloudBusy by mutableStateOf(false); private set
    var cloudMsg by mutableStateOf(""); private set

    init { refreshDbInfo(); scope.launch { device = withContext(Dispatchers.IO) { DeviceChecks.run(ctx) } }
        scope.launch { newAppAlerts = withContext(Dispatchers.IO) { Reminder.checkNewApps(ctx, false) } } }

    fun refreshDbInfo() = scope.launch(Dispatchers.IO) {
        val i = runCatching { IocDb.load(ctx) }.getOrElse { CrashReporter.nonFatal("IocDb.load", it); File(ctx.filesDir, "ioc.json").delete(); IocDb.load(ctx) }
        val m = runCatching { MalwareDb.load(ctx) }.getOrElse { CrashReporter.nonFatal("MalwareDb.load", it); File(ctx.filesDir, "malware_db.json").delete(); MalwareDb.load(ctx) }
        withContext(Dispatchers.Main) { iocVersion = i.version; iocSize = i.byPackage.size + i.byCert.size; mdbVersion = m.version; mdbSize = m.size }
    }

    fun scan(deep: Boolean, done: () -> Unit = {}) {
        if (scanning) return
        scanning = true; progress = 0f; scanned = 0; tab = "scan"
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                Scanner.scan(ctx, deep) { i, n, name -> scope.launch { progress = i / n.toFloat(); currentApp = name; scanned = i } }
            }
            result = r; device = r.device; newBadges = store.record(r); historyTick++
            ctx.getSharedPreferences("safespot", 0).edit().putLong("rtCheck", System.currentTimeMillis()).apply()
            newAppAlerts = emptyList(); scanning = false; done()
        }
    }

    fun refreshDevice() = scope.launch { device = withContext(Dispatchers.IO) { DeviceChecks.run(ctx) } }

    fun updateDb() { if (dbBusy) return; dbBusy = true
        scope.launch { dbMsg = withContext(Dispatchers.IO) { MalwareDb.update(ctx) }; dbBusy = false; refreshDbInfo() } }

    fun loadServers(force: Boolean = false) {
        if (loadingServers || (servers.isNotEmpty() && !force)) return
        loadingServers = true
        scope.launch {
            val l = withContext(Dispatchers.IO) { Servers.load(ctx) }
            servers = l.servers; serverSource = l.source; loadingServers = false
            // real TCP ping of the 25 fastest-listed servers
            val pinged = withContext(Dispatchers.IO) {
                l.servers.sortedByDescending { it.speedMbps }.take(25).map { s -> async { s.copy(pingMs = Servers.ping(s)) } }.awaitAll() }
            val map = pinged.associateBy { it.id }
            servers = l.servers.map { map[it.id] ?: it }
            if (selected == null) selected = servers.filter { it.pingMs > 0 }.minByOrNull { it.pingMs } ?: servers.firstOrNull()
        }
    }

    /** SHA-256 every .apk in a folder the user picked through SAF. */
    fun scanFolder(uri: Uri) { if (fileScanBusy) return; fileScanBusy = true
        scope.launch {
            fileHits = withContext(Dispatchers.IO) {
                val db = MalwareDb.load(ctx)
                val dir = DocumentFile.fromTreeUri(ctx, uri) ?: return@withContext emptyList()
                dir.listFiles().filter { it.isFile && (it.name ?: "").endsWith(".apk", true) }.mapNotNull { f ->
                    runCatching { ctx.contentResolver.openInputStream(f.uri)!!.use { s ->
                        val md = MessageDigest.getInstance("SHA-256"); val buf = ByteArray(1 shl 16)
                        while (true) { val n = s.read(buf); if (n < 0) break; md.update(buf, 0, n) }
                        val h = md.digest().joinToString("") { "%02X".format(it) }
                        FileHit(f.name ?: "?", h, db.sha256[h], null) } }.getOrNull() }
            }
            fileScanBusy = false
        } }

    /** User-consented cloud lookup of sideloaded app hashes + scanned files. */
    fun cloudCheck() { if (cloudBusy || !Cloud.available) return; cloudBusy = true; cloudMsg = ""
        scope.launch {
            val apps = result?.apps?.filter { it.apkSha256 != null && !it.system && it.source != Source.PLAY } ?: emptyList()
            val (msg, files) = withContext(Dispatchers.IO) {
                val hits = apps.mapNotNull { a -> (Cloud.lookup(a.apkSha256!!) as? Cloud.Hit.Malware)?.let { "${a.label}: ${it.signature}" } }
                val f = fileHits.map { fh -> fh.copy(cloud = when (val h = Cloud.lookup(fh.sha256)) { is Cloud.Hit.Malware -> h.signature; Cloud.Hit.Clean -> "not in MalwareBazaar"; is Cloud.Hit.Error -> "error: ${h.msg}" }) }
                (if (hits.isEmpty()) "Checked ${apps.size} apps: none known to MalwareBazaar." else "MALWARE FOUND:\n" + hits.joinToString("\n")) to f
            }
            cloudMsg = msg; fileHits = files; cloudBusy = false
        } }
}
