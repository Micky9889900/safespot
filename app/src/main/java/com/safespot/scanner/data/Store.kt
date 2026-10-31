package com.safespot.scanner.data

import android.content.Context
import com.safespot.scanner.scan.Risk
import com.safespot.scanner.scan.ScanResult
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class HistoryEntry(val time: Long, val score: Int, val apps: Int, val risky: Int, val threats: Int, val deep: Boolean,
                        val levels: Map<String, Int>)

data class Badge(val id: String, val title: String, val desc: String, val icon: String)

/** All engagement stats come from real scans stored on the device. Nothing is invented. */
class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("safespot", Context.MODE_PRIVATE)

    val badges = listOf(
        Badge("first_scan", "First scan", "Ran your first security scan", "🛡"),
        Badge("deep_scan", "Deep diver", "Completed a deep scan", "🔬"),
        Badge("streak3", "3-day streak", "Checked your phone 3 days in a row", "🔥"),
        Badge("streak7", "7-day streak", "Checked your phone 7 days in a row", "🏆"),
        Badge("zero_risk", "Zero risk", "A scan with no medium/high-risk apps", "💎"),
        Badge("score90", "Fortress", "Security score of 90 or more", "🏰"),
        Badge("fixer", "Fixer", "Raised your score compared with the previous scan", "🔧"),
        Badge("vpn_first", "Private tunnel", "Connected the VPN for the first time", "🌐"),
        Badge("ten_scans", "Vigilant", "Ran 10 scans", "👁"),
    )

    fun history(): List<HistoryEntry> = runCatching {
        val a = JSONArray(p.getString("history", "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
            val lv = o.optJSONObject("levels"); val m = HashMap<String, Int>(); lv?.keys()?.forEach { m[it] = lv.getInt(it) }
            HistoryEntry(o.getLong("t"), o.getInt("s"), o.getInt("a"), o.getInt("r"), o.optInt("th"), o.optBoolean("d"), m) } }
    }.getOrDefault(emptyList())

    val unlocked: Set<String> get() = p.getStringSet("badges", emptySet()) ?: emptySet()
    val streak: Int get() = p.getInt("streak", 0)
    val bestStreak: Int get() = p.getInt("bestStreak", 0)
    val firstScan: Long get() = p.getLong("firstScan", 0)
    val daysProtected: Int get() = if (firstScan == 0L) 0 else ((System.currentTimeMillis() - firstScan) / 86400000L).toInt() + 1
    var lastScan: Long
        get() = p.getLong("lastScan", 0)
        set(v) { p.edit().putLong("lastScan", v).apply() }
    var reminderOn: Boolean
        get() = p.getBoolean("reminder", false)
        set(v) { p.edit().putBoolean("reminder", v).apply() }
    var cloudLookupConsent: Boolean
        get() = p.getBoolean("cloud", false)
        set(v) { p.edit().putBoolean("cloud", v).apply() }
    var safeDns: Boolean
        get() = p.getBoolean("safeDns", true)
        set(v) { p.edit().putBoolean("safeDns", v).apply() }

    private fun unlock(ids: Collection<String>): List<Badge> {
        val cur = unlocked; val new = ids.filter { it !in cur }
        if (new.isNotEmpty()) p.edit().putStringSet("badges", cur + new).apply()
        return badges.filter { it.id in new }
    }
    fun unlockVpn() = unlock(listOf("vpn_first"))

    /** Records a scan, updates streak and returns newly unlocked badges. */
    fun record(r: ScanResult): List<Badge> {
        val h = history()
        val prev = h.lastOrNull()
        val levels = Risk.values().associate { lv -> lv.name to r.apps.count { it.risk == lv } }
        val o = JSONObject().put("t", r.time).put("s", r.score).put("a", r.apps.size).put("r", r.risky.size).put("th", r.threats.size).put("d", r.deep)
            .put("levels", JSONObject(levels as Map<*, *>))
        val arr = JSONArray(); (h.takeLast(59)).forEach { e ->
            arr.put(JSONObject().put("t", e.time).put("s", e.score).put("a", e.apps).put("r", e.risky).put("th", e.threats).put("d", e.deep).put("levels", JSONObject(e.levels as Map<*, *>))) }
        arr.put(o)
        // streak by calendar day
        val today = LocalDate.now().toEpochDay()
        val last = p.getLong("streakDay", -10)
        val st = when (today - last) { 0L -> streak.coerceAtLeast(1); 1L -> streak + 1; else -> 1 }
        p.edit().putString("history", arr.toString()).putLong("lastScan", r.time).putLong("streakDay", today).putInt("streak", st)
            .putInt("bestStreak", maxOf(bestStreak, st)).apply()
        if (firstScan == 0L) p.edit().putLong("firstScan", r.time).apply()
        val ids = mutableListOf("first_scan")
        if (r.deep) ids += "deep_scan"
        if (st >= 3) ids += "streak3"; if (st >= 7) ids += "streak7"
        if (r.risky.isEmpty()) ids += "zero_risk"
        if (r.score >= 90) ids += "score90"
        if (prev != null && r.score > prev.score) ids += "fixer"
        if (h.size + 1 >= 10) ids += "ten_scans"
        return unlock(ids)
    }

    companion object {
        val tips = listOf(
            "Spyware usually needs physical access. A strong screen lock is your best defence.",
            "Check Settings → Accessibility now and then. Only apps you recognise should be ON.",
            "Never install APK files sent in messages, even from friends.",
            "Google Play Protect is free. Keep it on in the Play Store menu.",
            "If your battery drains fast and the phone is warm while idle, run a deep scan.",
            "Turn off USB debugging when you're not developing apps.",
            "On public Wi-Fi, connect the VPN before logging into anything.",
            "Review which apps can read your notifications: they can see your 2FA codes.",
            "Uninstall apps you no longer use. Fewer apps means fewer risks.",
            "Install system updates quickly. Old security patches are how many attacks get in.",
            "Stalkerware often hides its icon. SafeSpot lists apps without a launcher icon.",
            "Use a different PIN for your phone than for your bank card.",
        )
        fun tipOfDay() = tips[(LocalDate.now().toEpochDay() % tips.size).toInt()]
    }
}
