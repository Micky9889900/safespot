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
    /** Open Phone Care tool id (junk, battery, cooler, booster, apps, wifi, perms, notif, speed, storage) or null. */
    var tool by mutableStateOf<String?>(null)
    /** Transient genie reaction after a real event (mood + text), cleared by the overlay after a few seconds. */
    var genieEvent by mutableStateOf<Pair<com.safespot.scanner.ui.Mood, String>?>(null)
    fun react(m: com.safespot.scanner.ui.Mood, text: String) { genieEvent = m to text }
    /** Activity timeline (real events only), newest first. */
    fun log(icon: String, text: String) { val p = ctx.getSharedPreferences("activity", 0)
        val a = runCatching { org.json.JSONArray(p.getString("a", "[]")) }.getOrDefault(org.json.JSONArray())
        a.put(org.json.JSONObject().put("t", System.currentTimeMillis()).put("i", icon).put("x", text))
        val keep = org.json.JSONArray(); for (k in maxOf(0, a.length() - 30) until a.length()) keep.put(a.get(k))
        p.edit().putString("a", keep.toString()).apply(); activityTick++ }
    var activityTick by mutableIntStateOf(0); private set
    fun activity(): List<Triple<Long, String, String>> = runCatching { val a = org.json.JSONArray(ctx.getSharedPreferences("activity", 0).getString("a", "[]"))
        (0 until a.length()).map { val o = a.getJSONObject(it); Triple(o.getLong("t"), o.getString("i"), o.getString("x")) }.reversed() }.getOrDefault(emptyList())

    var result by mutableStateOf<ScanResult?>(null); private set
    var scanning by mutableStateOf(false); private set
    var progress by mutableFloatStateOf(0f); private set
    var currentApp by mutableStateOf(""); private set
    var scanned by mutableIntStateOf(0); private set
    var etaSec by mutableIntStateOf(-1); private set
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
                Scanner.scan(ctx, deep) { pr -> scope.launch { progress = if (pr.total == 0) 1f else pr.done / pr.total.toFloat(); currentApp = pr.label; scanned = pr.done; etaSec = pr.etaSec } }
            }
            result = r; device = r.device; newBadges = store.record(r); historyTick++
            ctx.getSharedPreferences("safespot", 0).edit().putLong("rtCheck", System.currentTimeMillis()).apply()
            newAppAlerts = emptyList(); scanning = false
            log("🛡", "Scan: score ${r.score}, ${r.risky.size} risky of ${r.apps.size} apps")
            react(if (r.risky.isEmpty()) com.safespot.scanner.ui.Mood.CHEER else com.safespot.scanner.ui.Mood.WORRIED, if (r.risky.isEmpty()) "All clear! Score ${r.score}. Try the Junk Cleaner next." else "I found ${r.risky.size} app(s) to review. Check them first.")
            done()
        }
    }

    fun refreshDevice() = scope.launch { device = withContext(Dispatchers.IO) { DeviceChecks.run(ctx) } }

    fun updateDb() { if (dbBusy) return; dbBusy = true
        scope.launch { dbMsg = withContext(Dispatchers.IO) { MalwareDb.update(ctx) }; dbBusy = false; refreshDbInfo() } }

    /** true = "Fastest server" auto mode (default). */
    var autoServer by mutableStateOf(ctx.getSharedPreferences("vpn", 0).getBoolean("auto", true))
    fun setAuto(v: Boolean) { autoServer = v; ctx.getSharedPreferences("vpn", 0).edit().putBoolean("auto", v).apply() }
    val rankedServers get() = Servers.rank(servers)

    /** Cached list shows instantly; network refresh + parallel 1.5 s probes happen in the background. */
    fun loadServers(force: Boolean = false) {
        if (loadingServers || (servers.isNotEmpty() && !force)) return
        loadingServers = true
        scope.launch {
            if (servers.isEmpty()) withContext(Dispatchers.IO) { Servers.loadCached(ctx) }?.let { servers = it.servers; serverSource = it.source }
            val remote = withContext(Dispatchers.IO) { Servers.loadRemote(ctx) }
            if (remote != null) { servers = remote.servers; serverSource = remote.source } else if (servers.isEmpty()) serverSource = "Offline: no server list"
            loadingServers = false
            val pinged = withContext(Dispatchers.IO) { Servers.rank(servers).take(25).map { sv -> async { sv.copy(pingMs = Servers.ping(sv)) } }.awaitAll() }
            val map = pinged.associateBy { it.id }
            servers = servers.map { map[it.id] ?: it }
            if (selected == null) selected = Servers.rank(servers).firstOrNull()
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
