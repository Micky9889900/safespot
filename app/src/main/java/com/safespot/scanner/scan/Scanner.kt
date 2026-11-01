package com.safespot.scanner.scan

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.io.File
import java.security.MessageDigest
import com.safespot.scanner.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject

object Scanner {
    private val PLAY = setOf("com.android.vending", "com.google.android.feedback")
    private val STORES = setOf("com.sec.android.app.samsungapps", "com.amazon.venezia", "com.huawei.appmarket", "com.xiaomi.market",
        "com.xiaomi.mipicks", "com.heytap.market", "com.oppo.market", "com.bbk.appstore", "org.fdroid.fdroid", "com.uptodown", "com.apkpure.aegon", "com.aurora.store")
    private val INSTALLERS = setOf("com.google.android.packageinstaller", "com.android.packageinstaller", "com.android.shell",
        "com.google.android.apps.nbu.files", "com.android.chrome")
    private val SYSTEM_NAMES = setOf("system service", "system services", "android system", "system update", "update service", "google services",
        "google play services", "wifi service", "wi-fi service", "settings", "sync service", "device health", "system", "android services", "security service")

    private val HEXC = "0123456789ABCDEF".toCharArray()
    fun hex(b: ByteArray): String { val o = CharArray(b.size * 2); for (i in b.indices) { val v = b[i].toInt() and 0xFF; o[2 * i] = HEXC[v ushr 4]; o[2 * i + 1] = HEXC[v and 15] }; return String(o) }
    private fun digest(alg: String, b: ByteArray) = hex(MessageDigest.getInstance(alg).digest(b))

    /** Streams the file with a 1 MB buffer; [onBytes] reports progress for the ETA. */
    fun sha256File(path: String, onBytes: (Long) -> Unit = {}): String? = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        File(path).inputStream().use { s -> val buf = ByteArray(1 shl 20); while (true) { val n = s.read(buf); if (n < 0) break; md.update(buf, 0, n); onBytes(n.toLong()) } }
        hex(md.digest())
    }.getOrNull()

    fun enabledAccessibility(ctx: Context): Set<String> =
        (Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "")
            .split(':').mapNotNull { ComponentName.unflattenFromString(it)?.packageName }.toSet()

    fun notificationListeners(ctx: Context): Set<String> =
        (Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: "")
            .split(':').mapNotNull { ComponentName.unflattenFromString(it)?.packageName }.toSet()

    fun deviceAdmins(ctx: Context): Set<String> =
        ctx.getSystemService(DevicePolicyManager::class.java)?.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()

    @Suppress("DEPRECATION")
    private fun packages(pm: PackageManager): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS or (if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
        return if (Build.VERSION.SDK_INT >= 33) pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong())) else pm.getInstalledPackages(flags)
    }

    @Suppress("DEPRECATION")
    private fun certs(p: PackageInfo): List<ByteArray> =
        if (Build.VERSION.SDK_INT >= 28) p.signingInfo?.let { si -> (if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory)?.map { it.toByteArray() } } ?: emptyList()
        else p.signatures?.map { it.toByteArray() } ?: emptyList()

    @Suppress("DEPRECATION")
    private fun installer(pm: PackageManager, pkg: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(pkg).installingPackageName else pm.getInstallerPackageName(pkg)
    }.getOrNull()

    /** Progress snapshot (throttled to ~20/s by the caller side). */
    data class Progress(val done: Int, val total: Int, val label: String, val etaSec: Int = -1)

    /**
     * On-device scan.
     * Quick: user + updated-system apps, NO APK hashing (package name, signing cert, permissions, flags only).
     * Deep: all apps, plus APK SHA-256 for user/updated apps, cached by (pkg, versionCode, lastUpdateTime, size).
     * Work runs on Dispatchers.Default in parallel; [progress] is invoked at most every 50 ms.
     */
    suspend fun scan(ctx: Context, deep: Boolean, progress: (Progress) -> Unit): ScanResult = coroutineScope {
        val t0 = System.nanoTime()
        val pm = ctx.packageManager
        val ioc = Dbs.ioc(ctx); val mdb = Dbs.mdb(ctx)
        val acc = enabledAccessibility(ctx); val nl = notificationListeners(ctx); val adm = deviceAdmins(ctx)
        val launchable = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).mapTo(HashSet()) { it.activityInfo.packageName }
        val own = ctx.packageName
        val all = packages(pm).filter { it.packageName != own && it.applicationInfo != null }
        val list = all.filter { deep || !isPristineSystem(it.applicationInfo!!) }
        val t1 = System.nanoTime()
        val done = AtomicInteger(0); val current = AtomicReference("")
        val cache = if (deep) HashCache(ctx) else null
        val toHash = if (deep) list.filter { !isPristineSystem(it.applicationInfo!!) && cache!!.get(it) == null } else emptyList()
        val totalBytes = toHash.sumOf { runCatching { File(it.applicationInfo!!.sourceDir).length() }.getOrDefault(0L) }
        val hashed = AtomicLong(0); val tHash = System.nanoTime()
        val ticker = launch { while (isActive) {
            val d = done.get()
            val eta = if (deep && totalBytes > 0) { val h = hashed.get(); val el = (System.nanoTime() - tHash) / 1e9
                if (h > 0 && el > 0.5) ((totalBytes - h) / (h / el)).toInt() else -1 } else -1
            progress(Progress(d, list.size, current.get(), eta)); delay(50) } }
        // Hash (deep only) with a bounded pool: I/O-bound, 3 parallel streams is the sweet spot on flash.
        val hashes = java.util.concurrent.ConcurrentHashMap<String, String>()
        if (deep) {
            list.forEach { p -> cache!!.get(p)?.let { hashes[p.packageName] = it } }
            val sem = Semaphore(3)
            toHash.map { p -> async(Dispatchers.IO) { sem.withPermit {
                current.set(p.packageName)
                sha256File(p.applicationInfo!!.sourceDir) { hashed.addAndGet(it) }?.let { hashes[p.packageName] = it; cache!!.put(p, it) } } } }.awaitAll()
        }
        val tHashEnd = System.nanoTime()
        val chunk = (list.size / (Runtime.getRuntime().availableProcessors() * 2)).coerceAtLeast(8)
        val reports = list.chunked(chunk).map { part -> async(Dispatchers.Default) { part.map { p ->
            val label = runCatching { p.applicationInfo!!.loadLabel(pm).toString() }.getOrDefault(p.packageName)
            current.set(label)
            analyze(ctx, p, label, ioc, mdb, acc, nl, adm, launchable, hashes[p.packageName]).also { done.incrementAndGet() } } } }.awaitAll().flatten()
        val t2 = System.nanoTime()
        val dh = DeviceChecks.run(ctx)
        cache?.save()
        ticker.cancel(); progress(Progress(list.size, list.size, ""))
        if (BuildConfig.DEBUG) android.util.Log.i("SafeSpotScan", "deep=$deep apps=${list.size} list=${(t1 - t0) / 1000000}ms hash=${(tHashEnd - t1) / 1000000}ms (${toHash.size} new, ${totalBytes / 1048576}MB) analyze=${(t2 - tHashEnd) / 1000000}ms device=${(System.nanoTime() - t2) / 1000000}ms")
        ScanResult(System.currentTimeMillis(), deep, reports.sortedByDescending { it.score }, dh, score(reports, dh), ioc.version, mdb.version)
    }

    /** Preinstalled and never updated: lives on the verified read-only system image. */
    fun isPristineSystem(ai: ApplicationInfo) = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

    fun score(apps: List<AppReport>, dh: DeviceHealth): Int {
        var s = 100
        s -= apps.count { it.risk == Risk.CRITICAL } * 40
        s -= (apps.count { it.risk == Risk.HIGH } * 12).coerceAtMost(36)
        s -= (apps.count { it.risk == Risk.MEDIUM } * 4).coerceAtMost(16)
        s -= dh.penalty
        return s.coerceIn(0, 100)
    }

    fun analyze(ctx: Context, p: PackageInfo, label: String, ioc: IocDb, mdb: MalwareDb, acc: Set<String>, nl: Set<String>, adm: Set<String>,
                launchable: Set<String>, apkHash: String?): AppReport {
        val pm = ctx.packageManager
        val ai = p.applicationInfo!!
        val system = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val inst = installer(pm, p.packageName)
        val source = when { system && inst == null -> Source.SYSTEM; inst in PLAY -> Source.PLAY; inst in STORES -> Source.STORE
            inst == null -> Source.UNKNOWN; inst in INSTALLERS -> Source.SIDELOAD; else -> Source.SIDELOAD }
        val c = certs(p)
        val s256 = c.map { digest("SHA-256", it) }; val s1 = c.map { digest("SHA-1", it) }
        val req = p.requestedPermissions?.toList() ?: emptyList()
        val flags = p.requestedPermissionsFlags
        val granted = req.filterIndexed { i, _ -> flags != null && i < flags.size && (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0 }
        val isAcc = p.packageName in acc; val isNl = p.packageName in nl; val isAdm = p.packageName in adm
        val cats = HashSet<PermCategory>()
        granted.forEach { Permissions.map[it]?.cat?.let(cats::add) }
        // Another app's overlay grant is not readable; count it when requested.
        if ("android.permission.SYSTEM_ALERT_WINDOW" in req) cats += PermCategory.OVERLAY
        if (isAcc) cats += PermCategory.ACCESSIBILITY
        if (isNl) cats += PermCategory.NOTIFICATIONS
        if (isAdm) cats += PermCategory.ADMIN
        val hasLauncher = p.packageName in launchable
        val sideloaded = source == Source.SIDELOAD || source == Source.UNKNOWN

        val f = mutableListOf<Finding>()
        val (iocName, mal) = Matcher.match(p.packageName, s256, s1, apkHash, ioc, mdb)
        if (iocName != null) f += Finding("Known stalkerware: $iocName", "This app matches the public stalkerware-indicators list (package name or signing certificate).", 100)
        if (mal != null) f += Finding("Known malware: $mal", "The APK file or its signing certificate matches the malware database.", 100)
        if (isAcc) f += Finding("Accessibility service ON", "Can read everything on screen and tap for you. Spyware's favourite permission.", 35)
        if (isAdm) f += Finding("Device administrator", "Can lock or wipe the phone and is harder to uninstall.", 25)
        if (isNl) f += Finding("Reads your notifications", "Sees message previews from every app (WhatsApp, SMS, 2FA codes).", 20)
        granted.forEach { perm -> Permissions.map[perm]?.takeIf { it.weight >= 10 }?.let { f += Finding(it.explain, perm.substringAfterLast('.'), it.weight) } }
        if ("android.permission.SYSTEM_ALERT_WINDOW" in req) f += Finding("Can draw over other apps", "Can show fake login screens on top of real apps.", 8)
        val surveil = cats.intersect(setOf(PermCategory.SMS, PermCategory.CALLS, PermCategory.MIC, PermCategory.LOCATION, PermCategory.NOTIFICATIONS, PermCategory.ACCESSIBILITY)).size
        if (!hasLauncher && !system && surveil > 0) f += Finding("Hidden app", "Has no icon in the app drawer but holds sensitive access.", 25)
        if (sideloaded && !system) f += Finding("Not from an app store", "Installed from a file or unknown source (${inst ?: "no installer recorded"}).", if (surveil > 0) 15 else 5)
        // Classic banking-trojan / spyware combo
        val trojan = "android.permission.SEND_SMS" in req && "android.permission.RECEIVE_BOOT_COMPLETED" in req &&
            "android.permission.SYSTEM_ALERT_WINDOW" in req && (isAcc || "android.permission.BIND_ACCESSIBILITY_SERVICE" in req)
        if (trojan && sideloaded) f += Finding("Trojan-like permission combo", "SMS sending + start at boot + overlay + accessibility, from outside an app store.", 30)
        if (!system && label.trim().lowercase() in SYSTEM_NAMES && !p.packageName.startsWith("com.android.") && !p.packageName.startsWith("com.google.android."))
            f += Finding("Pretends to be a system app", "Named \"$label\" but it is a normal installed app.", 30)
        if (sideloaded && !system && System.currentTimeMillis() - p.firstInstallTime < 7L * 86400000 && surveil >= 2)
            f += Finding("Recently sideloaded with sensitive access", "Installed in the last 7 days from outside a store.", 10)
        if (surveil >= 3 && (isAcc || isNl)) f += Finding("Surveillance combo", "Screen/notification access combined with location, SMS, calls or microphone.", 15)

        // Trusted sources weigh less: Play-installed system-ish apps are rarely stalkerware.
        var score = f.sumOf { it.points }
        if (source == Source.PLAY && iocName == null && mal == null) score = (score * 0.6).toInt()
        if (system && iocName == null && mal == null) score = (score * 0.3).toInt()
        score = score.coerceAtMost(100)
        val risk = when { iocName != null || mal != null -> Risk.CRITICAL; score >= 60 -> Risk.HIGH; score >= 35 -> Risk.MEDIUM; score >= 15 -> Risk.LOW; else -> Risk.SAFE }
        return AppReport(p.packageName, label, p.versionName, system, inst, source, hasLauncher, p.firstInstallTime, p.lastUpdateTime, s256, s1, apkHash,
            granted, req, cats, isAcc, isAdm, isNl, iocName, mal, f.sortedByDescending { it.points }, score, risk)
    }

    /** Quick check of apps installed/updated since [since] (used by the daily real-time check). */
    fun scanSince(ctx: Context, since: Long): List<AppReport> {
        val pm = ctx.packageManager
        val ioc = Dbs.ioc(ctx); val mdb = Dbs.mdb(ctx)
        val acc = enabledAccessibility(ctx); val nl = notificationListeners(ctx); val adm = deviceAdmins(ctx)
        val launchable = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).map { it.activityInfo.packageName }.toSet()
        return packages(pm).filter { it.lastUpdateTime > since && it.packageName != ctx.packageName && it.applicationInfo != null }
            .map { analyze(ctx, it, runCatching { it.applicationInfo!!.loadLabel(pm).toString() }.getOrDefault(it.packageName), ioc, mdb, acc, nl, adm, launchable, null) }
    }
}

/** Pure lookup against prebuilt HashMaps: O(1) per key, no list scans or regex. */
object Matcher {
    fun match(pkg: String, s256: List<String>, s1: List<String>, apkHash: String?, ioc: IocDb, mdb: MalwareDb): Pair<String?, String?> {
        var iocName = ioc.byPackage[pkg]
        if (iocName == null) for (c in s1) { iocName = ioc.byCert[c]; if (iocName != null) break }
        if (iocName == null) for (c in s256) { iocName = ioc.byCert[c]; if (iocName != null) break }
        var mal = apkHash?.let { mdb.sha256[it] }
        if (mal == null) for (c in s256) { mal = mdb.certs[c]; if (mal != null) break }
        if (mal == null) mal = mdb.packages[pkg]
        return iocName to mal
    }
}

/** Databases parsed once and kept in memory until their file changes. */
object Dbs {
    @Volatile private var i: Pair<Long, IocDb>? = null
    @Volatile private var m: Pair<Long, MalwareDb>? = null
    private fun stamp(ctx: Context, n: String) = File(ctx.filesDir, n).let { if (it.exists()) it.lastModified() else 0L }
    fun ioc(ctx: Context): IocDb { val st = stamp(ctx, "ioc.json"); i?.let { if (it.first == st) return it.second }; return IocDb.load(ctx).also { i = st to it } }
    fun mdb(ctx: Context): MalwareDb { val st = stamp(ctx, "malware_db.json"); m?.let { if (it.first == st) return it.second }; return MalwareDb.load(ctx).also { m = st to it } }
}

/** APK SHA-256 cache keyed by package + versionCode + lastUpdateTime + size; written once at the end of a scan. */
class HashCache(ctx: Context) {
    private val f = File(ctx.filesDir, "apk_hash_cache.json")
    private val map = java.util.concurrent.ConcurrentHashMap<String, String>().apply {
        runCatching { val o = JSONObject(f.readText()); o.keys().forEach { put(it, o.getString(it)) } } }
    @Volatile private var dirty = false
    @Suppress("DEPRECATION")
    private fun key(p: PackageInfo): String {
        val vc = if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong()
        val size = runCatching { File(p.applicationInfo!!.sourceDir).length() }.getOrDefault(0L)
        return "${p.packageName}|$vc|${p.lastUpdateTime}|$size"
    }
    fun get(p: PackageInfo): String? = map[key(p)]
    fun put(p: PackageInfo, h: String) { map[key(p)] = h; dirty = true }
    fun save() { if (dirty) runCatching { f.writeText(JSONObject(map as Map<*, *>).toString()) } }
}
