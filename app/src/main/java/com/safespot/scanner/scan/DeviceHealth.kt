package com.safespot.scanner.scan

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class Level { GOOD, WARN, BAD, INFO }

data class Check(val title: String, val value: String, val level: Level, val advice: String = "", val settingsAction: String? = null)

data class DeviceHealth(val checks: List<Check>, val penalty: Int, val patchAgeDays: Long?)

object DeviceChecks {
    /** Minimal result used when the checks fail or time out, so a scan always finishes. */
    fun fallback() = DeviceHealth(listOf(Check("Device checks", "Unavailable right now", Level.INFO)), 0, null)
    /** Never throws: each failure degrades to [fallback]. */
    fun run(ctx: Context): DeviceHealth = try { runUnsafe(ctx) } catch (t: Throwable) {
        runCatching { com.safespot.scanner.CrashReporter.nonFatal("DeviceChecks.run", t) }; fallback() }
    private fun runUnsafe(ctx: Context): DeviceHealth {
        val c = mutableListOf<Check>()
        var penalty = 0
        // Security patch
        val patch = Build.VERSION.SECURITY_PATCH
        val age = runCatching {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(patch)!!
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - d.time)
        }.getOrNull()
        val patchLevel = when { age == null -> Level.INFO; age <= 90 -> Level.GOOD; age <= 365 -> Level.WARN; else -> Level.BAD }
        if (patchLevel == Level.WARN) penalty += 5
        if (patchLevel == Level.BAD) penalty += 12
        c += Check("Security patch", "$patch" + (age?.let { " ($it days old)" } ?: ""), patchLevel,
            "Install system updates in Settings → System → Software update.", Settings.ACTION_DEVICE_INFO_SETTINGS)
        c += Check("Android version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            if (Build.VERSION.SDK_INT >= 31) Level.GOOD else Level.WARN)
        // Screen lock
        val km = ctx.getSystemService(KeyguardManager::class.java)
        val secure = km?.isDeviceSecure == true
        if (!secure) penalty += 15
        c += Check("Screen lock", if (secure) "PIN / pattern / password set" else "No secure screen lock", if (secure) Level.GOOD else Level.BAD,
            "Anyone holding your phone can install spyware in a minute. Set a PIN.", Settings.ACTION_SECURITY_SETTINGS)
        // Dev options / ADB
        val dev = Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        val adb = Settings.Global.getInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        if (dev) penalty += 4
        if (adb) penalty += 10
        c += Check("Developer options", if (dev) "On" else "Off", if (dev) Level.WARN else Level.GOOD,
            "Turn off unless you are a developer.", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        c += Check("USB debugging (ADB)", if (adb) "On" else "Off", if (adb) Level.BAD else Level.GOOD,
            "USB debugging lets a computer install and control apps. Turn it off.", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        // Root hints
        val su = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/app/Superuser.apk", "/data/adb/magisk").any { File(it).exists() }
        if (su) penalty += 10
        c += Check("Root indicators", if (su) "su / Magisk files found" else "None found", if (su) Level.WARN else Level.GOOD,
            "A rooted phone lets apps bypass Android's protections.")
        // Storage
        runCatching {
            val st = StatFs(Environment.getDataDirectory().path)
            val free = st.availableBytes.toDouble(); val total = st.totalBytes.toDouble()
            val pct = (free / total * 100).toInt()
            c += Check("Storage free", "%.1f GB of %.1f GB ($pct%%)".format(free / 1e9, total / 1e9), if (pct > 15) Level.GOOD else if (pct > 5) Level.WARN else Level.BAD)
        }
        // Battery
        ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { b ->
            val t = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0
            val lvl = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val h = b.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
            val hs = when (h) { BatteryManager.BATTERY_HEALTH_GOOD -> "Good"; BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"; BatteryManager.BATTERY_HEALTH_COLD -> "Cold"; else -> "Unknown" }
            c += Check("Battery", "%d%% · %.1f °C · %s".format(lvl, t, hs), if (t in 0.0..40.0 && h != BatteryManager.BATTERY_HEALTH_OVERHEAT) Level.GOOD else Level.WARN,
                "A phone that is always warm while idle can mean something runs in the background.")
        }
        // Network
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val net = when { caps == null -> "Offline"; wifi -> "Wi-Fi"; caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"; else -> "Other" }
        c += Check("Network", net, Level.INFO)
        if (wifi && Build.VERSION.SDK_INT >= 31) {
            val wi = caps?.transportInfo as? WifiInfo
            val sec = when (wi?.currentSecurityType) {
                WifiInfo.SECURITY_TYPE_OPEN -> "Open (no password)"; WifiInfo.SECURITY_TYPE_WEP -> "WEP (broken)"
                WifiInfo.SECURITY_TYPE_PSK -> "WPA2"; WifiInfo.SECURITY_TYPE_SAE -> "WPA3"; WifiInfo.SECURITY_TYPE_OWE -> "Enhanced open"
                null, WifiInfo.SECURITY_TYPE_UNKNOWN -> "Unknown (Android hides it)"; else -> "Secured"
            }
            val l = when { sec.startsWith("Open") || sec.startsWith("WEP") -> Level.BAD; sec.startsWith("Unknown") -> Level.INFO; else -> Level.GOOD }
            c += Check("Wi-Fi security", sec, l, "On open Wi-Fi, turn on the VPN.")
        }
        c += Check("VPN", if (vpn) "Active" else "Off", if (vpn) Level.GOOD else Level.INFO)
        return DeviceHealth(c, penalty, age)
    }
}
