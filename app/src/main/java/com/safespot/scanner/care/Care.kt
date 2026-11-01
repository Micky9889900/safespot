package com.safespot.scanner.care

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** Real device readings for the Phone Care tools. Nothing here invents a number: unknown = null. */
object Usage {
    fun hasAccess(c: Context): Boolean = runCatching {
        val a = c.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) a.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        else @Suppress("DEPRECATION") a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    data class Row(val pkg: String, val label: String, val fgMs: Long, val lastUsed: Long)
    /** Screen time per app over [windowMs]. Empty without usage access. */
    fun top(c: Context, windowMs: Long, byRecent: Boolean = false, limit: Int = 15): List<Row> {
        if (!hasAccess(c)) return emptyList()
        val u = c.getSystemService(UsageStatsManager::class.java); val end = System.currentTimeMillis()
        val pm = c.packageManager
        return u.queryAndAggregateUsageStats(end - windowMs, end).values
            .filter { it.totalTimeInForeground > 60_000 && it.packageName != c.packageName }
            .sortedByDescending { if (byRecent) it.lastTimeUsed else it.totalTimeInForeground }
            .mapNotNull { us -> runCatching { Row(us.packageName, pm.getApplicationLabel(pm.getApplicationInfo(us.packageName, 0)).toString(), us.totalTimeInForeground, us.lastTimeUsed) }.getOrNull() }
            .take(limit)
    }
    fun lastUsedMap(c: Context, windowMs: Long = 90L * 86400_000): Map<String, Long> {
        if (!hasAccess(c)) return emptyMap()
        val end = System.currentTimeMillis()
        return c.getSystemService(UsageStatsManager::class.java).queryAndAggregateUsageStats(end - windowMs, end).mapValues { it.value.lastTimeUsed }
    }
    fun dur(ms: Long): String { val m = ms / 60_000; return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min" }
}

// ---------------------------------------------------------------- Battery
data class BatteryInfo(val level: Int?, val tempC: Float?, val voltageMv: Int?, val health: String, val status: String, val plug: String,
                       val tech: String?, val currentMa: Int?, val chargeMah: Int?, val cycles: Int?, val timeToFullMin: Long?, val saverOn: Boolean)
object Battery {
    fun intent(c: Context): Intent? = runCatching { c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
    fun read(c: Context): BatteryInfo {
        val i = intent(c); val bm = c.getSystemService(BatteryManager::class.java)
        val lvl = i?.let { val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1); if (l >= 0 && s > 0) l * 100 / s else null }
        val t = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }?.div(10f)
        val v = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
        val health = when (i?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"; BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"; BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"; BatteryManager.BATTERY_HEALTH_COLD -> "Cold"; BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"; else -> "Not reported" }
        val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val status = when (st) { BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"; BatteryManager.BATTERY_STATUS_FULL -> "Full"; BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"; else -> "Unknown" }
        val plug = when (i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) { BatteryManager.BATTERY_PLUGGED_AC -> "Wall charger"; BatteryManager.BATTERY_PLUGGED_USB -> "USB"; BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"; else -> "Unplugged" }
        val cur = runCatching { bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrNull()?.takeIf { it != Int.MIN_VALUE && it != 0 }
            ?.let { if (kotlin.math.abs(it) > 20_000) it / 1000 else it } // some devices report mA, most µA
        val ch = runCatching { bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) }.getOrNull()?.takeIf { it > 0 }?.div(1000)
        val cycles = if (Build.VERSION.SDK_INT >= 34) i?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it >= 0 } else null
        val ttf = if (Build.VERSION.SDK_INT >= 28) runCatching { bm.computeChargeTimeRemaining() }.getOrNull()?.takeIf { it > 0 }?.div(60_000) else null
        val saver = runCatching { c.getSystemService(PowerManager::class.java).isPowerSaveMode }.getOrDefault(false)
        return BatteryInfo(lvl, t, v, health, status, plug, i?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY), cur, ch, cycles, ttf, saver)
    }
    data class Sample(val t: Long, val level: Int, val charging: Boolean, val temp: Float?)
    /** Charging history: SafeSpot records a sample whenever it runs (app open / daily check). Android gives no history API. */
    fun record(c: Context) {
        val b = read(c); val lvl = b.level ?: return
        val p = c.getSharedPreferences("battery", 0); val a = runCatching { JSONArray(p.getString("h", "[]")) }.getOrDefault(JSONArray())
        val last = if (a.length() > 0) a.getJSONObject(a.length() - 1) else null
        if (last != null && System.currentTimeMillis() - last.getLong("t") < 10 * 60_000 && last.getInt("l") == lvl) return
        a.put(JSONObject().put("t", System.currentTimeMillis()).put("l", lvl).put("c", b.status == "Charging" || b.status == "Full").put("tp", b.tempC?.toDouble() ?: JSONObject.NULL))
        val trimmed = JSONArray(); val from = maxOf(0, a.length() - 200); for (k in from until a.length()) trimmed.put(a.get(k))
        p.edit().putString("h", trimmed.toString()).apply()
    }
    fun history(c: Context): List<Sample> = runCatching {
        val a = JSONArray(c.getSharedPreferences("battery", 0).getString("h", "[]"))
        (0 until a.length()).map { val o = a.getJSONObject(it); Sample(o.getLong("t"), o.getInt("l"), o.optBoolean("c"), if (o.isNull("tp")) null else o.getDouble("tp").toFloat()) }
    }.getOrDefault(emptyList())
}

// ---------------------------------------------------------------- Thermal
data class ThermalInfo(val batteryC: Float?, val cpuC: Float?, val status: String, val statusLevel: Int, val headroom: Float?)
object Thermal {
    @Volatile private var zones: List<File>? = null
    private fun line(f: File) = runCatching { f.bufferedReader().use { it.readLine() } }.getOrNull()
    /** Hottest readable CPU/SoC zone. Most Android 10+ phones block this: returns null, never a guess. */
    fun cpu(): Float? {
        val zs = zones ?: (File("/sys/class/thermal").listFiles { _, n -> n.startsWith("thermal_zone") }?.filter { z ->
            line(File(z, "type"))?.lowercase(Locale.US)?.let { "cpu" in it || "soc" in it || "tsens" in it || "x86_pkg" in it } == true } ?: emptyList()).also { zones = it }
        return zs.mapNotNull { line(File(it, "temp"))?.trim()?.toFloatOrNull()?.let { v -> if (v > 1000) v / 1000 else v }?.takeIf { v -> v in 1f..130f } }.maxOrNull()
    }
    fun read(c: Context): ThermalInfo {
        val pm = c.getSystemService(PowerManager::class.java)
        val lvl = if (Build.VERSION.SDK_INT >= 29) runCatching { pm.currentThermalStatus }.getOrDefault(-1) else -1
        val st = when (lvl) { 0 -> "No throttling"; 1 -> "Light throttling"; 2 -> "Moderate throttling"; 3 -> "Severe throttling"; 4 -> "Critical"; 5 -> "Emergency"; 6 -> "Shutdown imminent"; else -> "Not reported by this phone" }
        val hr = if (Build.VERSION.SDK_INT >= 30) runCatching { pm.getThermalHeadroom(10) }.getOrNull()?.takeIf { !it.isNaN() } else null
        return ThermalInfo(Battery.read(c).tempC, cpu(), st, lvl, hr)
    }
}

// ---------------------------------------------------------------- Memory
data class MemInfo(val total: Long, val avail: Long, val low: Boolean, val threshold: Long) { val used get() = total - avail; val pct get() = (used * 100 / maxOf(1, total)).toInt() }
object Memory {
    fun read(c: Context): MemInfo { val mi = ActivityManager.MemoryInfo(); c.getSystemService(ActivityManager::class.java).getMemoryInfo(mi); return MemInfo(mi.totalMem, mi.availMem, mi.lowMemory, mi.threshold) }
    /** Android 14+ only lets an app kill its own processes, so this mostly helps on Android 13 and older. Returns packages asked. */
    fun killBackground(c: Context, pkgs: List<String>): Int {
        val am = c.getSystemService(ActivityManager::class.java); var n = 0
        for (p in pkgs) if (p != c.packageName) runCatching { am.killBackgroundProcesses(p); n++ }
        return n
    }
    val killWorks get() = Build.VERSION.SDK_INT < 34
}

// ---------------------------------------------------------------- Apps / storage
data class AppEntry(val pkg: String, val label: String, val system: Boolean, val appBytes: Long?, val dataBytes: Long?, val cacheBytes: Long?, val lastUsed: Long?, val installed: Long, val updated: Long) {
    val total get() = (appBytes ?: 0) + (dataBytes ?: 0) + (cacheBytes ?: 0)
}
object Apps {
    @Suppress("DEPRECATION")
    @android.annotation.SuppressLint("NewApi") fun list(c: Context, includeSystem: Boolean = false): List<AppEntry> {
        val pm = c.packageManager; val usage = Usage.hasAccess(c)
        val ssm = if (Build.VERSION.SDK_INT >= 26) c.getSystemService(StorageStatsManager::class.java) else null
        val last = Usage.lastUsedMap(c)
        return pm.getInstalledApplications(0).filter { includeSystem || it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 }
            .filter { it.packageName != c.packageName }
            .map { ai ->
                val st = if (usage && ssm != null && Build.VERSION.SDK_INT >= 26) runCatching { ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, ai.packageName, Process.myUserHandle()) }.getOrNull() else null
                val pi = runCatching { pm.getPackageInfo(ai.packageName, 0) }.getOrNull()
                AppEntry(ai.packageName, pm.getApplicationLabel(ai).toString(), ai.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    st?.appBytes ?: runCatching { File(ai.sourceDir).length() }.getOrNull(), st?.dataBytes?.let { d -> d - (st.cacheBytes) }, st?.cacheBytes,
                    last[ai.packageName]?.takeIf { it > 0 }, pi?.firstInstallTime ?: 0, pi?.lastUpdateTime ?: 0)
            }.sortedByDescending { it.total }
    }
    fun details(pkg: String) = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun uninstall(pkg: String) = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun notifSettings(pkg: String) = (if (Build.VERSION.SDK_INT >= 26) Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, pkg) else details(pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

data class StorageBreakdown(val total: Long, val free: Long, val images: Long?, val video: Long?, val audio: Long?, val apps: Long?, val ownCache: Long) { val used get() = total - free }
object StorageInfo {
    fun sizeOf(f: File?): Long = if (f == null) 0 else if (f.isFile) f.length() else f.listFiles()?.sumOf { sizeOf(it) } ?: 0
    fun deleteContents(f: File?): Long { var n = 0L; f?.listFiles()?.forEach { k -> n += if (k.isDirectory) deleteContents(k).also { k.delete() } else { val l = k.length(); if (k.delete()) l else 0 } }; return n }
    fun ownCache(c: Context) = sizeOf(c.cacheDir) + sizeOf(c.codeCacheDir) + sizeOf(c.externalCacheDir)
    fun clearOwnCache(c: Context) = deleteContents(c.cacheDir) + deleteContents(c.codeCacheDir) + deleteContents(c.externalCacheDir)
    fun read(c: Context, appsTotal: Long?): StorageBreakdown {
        val st = StatFs(Environment.getDataDirectory().path)
        var img: Long? = null; var vid: Long? = null; var aud: Long? = null
        if (Build.VERSION.SDK_INT >= 26 && Usage.hasAccess(c)) runCatching {
            val e = c.getSystemService(StorageStatsManager::class.java).queryExternalStatsForUser(StorageManager.UUID_DEFAULT, Process.myUserHandle())
            img = e.imageBytes; vid = e.videoBytes; aud = e.audioBytes
        }
        return StorageBreakdown(st.totalBytes, st.availableBytes, img, vid, aud, appsTotal, ownCache(c))
    }
}

// ---------------------------------------------------------------- Junk (SAF folder the user picks)
data class JunkFile(val uri: Uri, val name: String, val path: String, val size: Long, val modified: Long, val kind: String)
data class JunkReport(val folder: String, val scanned: Int, val large: List<JunkFile>, val duplicates: List<List<JunkFile>>, val apks: List<JunkFile>, val emptyDirs: List<JunkFile>, val tempFiles: List<JunkFile>) {
    val dupWaste get() = duplicates.sumOf { g -> g.drop(1).sumOf { it.size } }
    val reclaimable get() = large.sumOf { it.size } + dupWaste + apks.sumOf { it.size } + tempFiles.sumOf { it.size }
}
object Junk {
    private val TEMP = Regex("""(?i).*\.(tmp|temp|log|bak|old|crdownload|part|thumbdata\d*)$|^\.?thumbs?\.db$""")
    fun scan(c: Context, tree: Uri, deep: Boolean, progress: (Int, String) -> Unit): JunkReport {
        val root = DocumentFile.fromTreeUri(c, tree) ?: return JunkReport("?", 0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val files = ArrayList<JunkFile>(); val empty = ArrayList<JunkFile>(); var n = 0
        val maxDepth = if (deep) 12 else 6
        fun walk(d: DocumentFile, path: String, depth: Int): Boolean { // returns true if dir is empty
            val kids = runCatching { d.listFiles() }.getOrDefault(emptyArray())
            if (kids.isEmpty()) return true
            var allEmpty = true
            for (k in kids) {
                val name = k.name ?: continue
                if (k.isDirectory) { if (depth < maxDepth && walk(k, "$path/$name", depth + 1)) empty += JunkFile(k.uri, name, "$path/$name", 0, k.lastModified(), "folder") else allEmpty = false }
                else { allEmpty = false; n++; if (n % 50 == 0) progress(n, name); files += JunkFile(k.uri, name, "$path/$name", k.length(), k.lastModified(), "file") }
            }
            return allEmpty
        }
        walk(root, root.name ?: "", 0)
        val large = files.filter { it.size >= (if (deep) 50L else 100L) * 1024 * 1024 }.sortedByDescending { it.size }
        val apks = files.filter { it.name.endsWith(".apk", true) || it.name.endsWith(".xapk", true) || it.name.endsWith(".apks", true) }
        val temp = files.filter { TEMP.matches(it.name) }
        val dups = files.filter { it.size >= 64 * 1024 }.groupBy { it.size }.values.filter { it.size > 1 }
            .flatMap { same -> same.groupBy { hash(c, it.uri, deep) }.filterKeys { it != null }.values.filter { it.size > 1 } }
            .map { g -> g.sortedBy { it.modified } }.sortedByDescending { g -> g.first().size * (g.size - 1) }
        return JunkReport(root.name ?: "folder", n, large, dups, apks, empty.sortedBy { it.path }, temp)
    }
    /** Quick mode hashes the first+last 1 MB (plus size match), deep mode hashes the whole file. */
    private fun hash(c: Context, u: Uri, full: Boolean): String? = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        c.contentResolver.openInputStream(u)!!.use { ins ->
            val buf = ByteArray(1 shl 16); var read = 0L
            while (true) { val r = ins.read(buf); if (r < 0) break; md.update(buf, 0, r); read += r; if (!full && read >= (1 shl 20)) break }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
    fun delete(c: Context, f: JunkFile): Boolean = runCatching { android.provider.DocumentsContract.deleteDocument(c.contentResolver, f.uri) }.getOrDefault(false)
}

// ---------------------------------------------------------------- Wi-Fi security
data class WifiCheck(val onWifi: Boolean, val security: String?, val open: Boolean?, val validated: Boolean, val captive: Boolean, val vpn: Boolean, val privateDns: Boolean?, val metered: Boolean, val freqMhz: Int?, val linkMbps: Int?, val signal: Int?)
object Wifi {
    fun check(c: Context): WifiCheck {
        val cm = c.getSystemService(ConnectivityManager::class.java)
        val n = cm.activeNetwork; val caps = n?.let { cm.getNetworkCapabilities(it) }
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val vpn = cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true }
        var sec: String? = null; var open: Boolean? = null; var freq: Int? = null; var link: Int? = null; var rssi: Int? = null
        val wi = if (Build.VERSION.SDK_INT >= 29) caps?.transportInfo as? WifiInfo else null
        if (wi != null) {
            freq = wi.frequency.takeIf { it > 0 }; link = wi.linkSpeed.takeIf { it > 0 }; rssi = wi.rssi.takeIf { it > -127 }
            if (Build.VERSION.SDK_INT >= 31) {
                val t = wi.currentSecurityType
                sec = when (t) { WifiInfo.SECURITY_TYPE_OPEN -> "Open (no password)"; WifiInfo.SECURITY_TYPE_OWE -> "Enhanced Open (OWE)"; WifiInfo.SECURITY_TYPE_WEP -> "WEP (broken)"
                    WifiInfo.SECURITY_TYPE_PSK -> "WPA2-Personal"; WifiInfo.SECURITY_TYPE_SAE -> "WPA3-Personal"; WifiInfo.SECURITY_TYPE_EAP, WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE, WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT -> "WPA2/3-Enterprise"
                    WifiInfo.SECURITY_TYPE_UNKNOWN -> null; else -> "Other ($t)" }
                open = when (t) { WifiInfo.SECURITY_TYPE_OPEN, WifiInfo.SECURITY_TYPE_WEP -> true; WifiInfo.SECURITY_TYPE_UNKNOWN -> null; else -> false }
            }
        }
        val pdns = if (Build.VERSION.SDK_INT >= 28) n?.let { cm.getLinkProperties(it)?.isPrivateDnsActive } else null
        return WifiCheck(onWifi, sec, open, caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true, vpn, pdns,
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true, freq, link, rssi)
    }
}

// ---------------------------------------------------------------- Permissions by category
object Perms {
    val groups = linkedMapOf(
        "Location" to listOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION"),
        "Camera" to listOf("android.permission.CAMERA"), "Microphone" to listOf("android.permission.RECORD_AUDIO"),
        "Contacts" to listOf("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"),
        "SMS" to listOf("android.permission.READ_SMS", "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS"),
        "Phone & call logs" to listOf("android.permission.READ_CALL_LOG", "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE"),
        "Photos & media" to listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_EXTERNAL_STORAGE"),
        "Calendar" to listOf("android.permission.READ_CALENDAR"), "Body sensors" to listOf("android.permission.BODY_SENSORS"),
        "Nearby devices" to listOf("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT", "android.permission.NEARBY_WIFI_DEVICES"),
        "Notifications" to listOf("android.permission.POST_NOTIFICATIONS"))
    data class Holder(val pkg: String, val label: String)
    /** Apps that currently HOLD (granted) each group. */
    @Suppress("DEPRECATION")
    fun byGroup(c: Context): Map<String, List<Holder>> {
        val pm = c.packageManager
        val pkgs: List<PackageInfo> = runCatching { pm.getInstalledPackages(PackageManager.GET_PERMISSIONS) }.getOrDefault(emptyList())
        val out = LinkedHashMap<String, MutableList<Holder>>(); groups.keys.forEach { out[it] = mutableListOf() }
        for (p in pkgs) {
            val ai = p.applicationInfo ?: continue
            if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0 && ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0) continue
            if (p.packageName == c.packageName) continue
            val req = p.requestedPermissions ?: continue; val fl = p.requestedPermissionsFlags ?: continue
            val granted = req.indices.filter { fl[it] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }.map { req[it] }.toSet()
            val label = pm.getApplicationLabel(ai).toString()
            groups.forEach { (g, ps) -> if (ps.any { it in granted }) out[g]!!.add(Holder(p.packageName, label)) }
        }
        return out.mapValues { it.value.sortedBy { h -> h.label.lowercase() } }
    }
}

// ---------------------------------------------------------------- Speed test (Cloudflare public endpoints)
data class SpeedResult(val pingMs: Long?, val downMbps: Double?, val upMbps: Double?, val error: String?)
object Speed {
    private fun ping(): Long? = runCatching {
        (1..5).map {
            val t = System.nanoTime(); val c = URL("https://speed.cloudflare.com/__down?bytes=0").openConnection() as HttpURLConnection
            c.connectTimeout = 5000; c.readTimeout = 5000; c.inputStream.use { it.readBytes() }; c.disconnect(); (System.nanoTime() - t) / 1_000_000
        }.sorted()[1]
    }.getOrNull()
    fun run(progress: (String, Float) -> Unit): SpeedResult = try {
        progress("Ping", 0.05f); val p = ping()
        progress("Download", 0.2f)
        val bytes = 25_000_000L; val c = URL("https://speed.cloudflare.com/__down?bytes=$bytes").openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 15000
        val t0 = System.nanoTime(); var got = 0L
        c.inputStream.use { ins -> val b = ByteArray(1 shl 16); while (true) { val r = ins.read(b); if (r < 0) break; got += r; progress("Download", .2f + .5f * got / bytes); if ((System.nanoTime() - t0) > 15_000_000_000) break } }
        val down = got * 8 / ((System.nanoTime() - t0) / 1e9) / 1e6
        progress("Upload", 0.75f)
        val upBytes = 5_000_000; val u = URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
        u.requestMethod = "POST"; u.doOutput = true; u.setFixedLengthStreamingMode(upBytes); u.connectTimeout = 8000; u.readTimeout = 20000
        val t1 = System.nanoTime(); u.outputStream.use { o -> val b = ByteArray(1 shl 16); var sent = 0; while (sent < upBytes) { val k = minOf(b.size, upBytes - sent); o.write(b, 0, k); sent += k; progress("Upload", .75f + .25f * sent / upBytes) } }
        u.responseCode; val up = upBytes * 8 / ((System.nanoTime() - t1) / 1e9) / 1e6; u.disconnect()
        SpeedResult(p, down, up, null)
    } catch (e: Exception) { SpeedResult(null, null, null, "${e.javaClass.simpleName}: check your connection") }
}
