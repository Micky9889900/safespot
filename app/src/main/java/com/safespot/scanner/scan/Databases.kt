package com.safespot.scanner.scan

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Stalkerware IOC list (AssoEchap stalkerware-indicators, CC-BY 4.0), bundled in assets/ioc.json. */
class IocDb(val version: String, val byPackage: Map<String, String>, val byCert: Map<String, String>) {
    companion object {
        fun load(ctx: Context): IocDb {
            val f = File(ctx.filesDir, "ioc.json")
            val txt = if (f.exists()) f.readText() else ctx.assets.open("ioc.json").bufferedReader().readText()
            val o = JSONObject(txt)
            val pk = HashMap<String, String>(); val ct = HashMap<String, String>()
            val apps = o.getJSONArray("apps")
            for (i in 0 until apps.length()) {
                val a = apps.getJSONObject(i); val n = a.optString("name")
                a.optJSONArray("packages")?.let { for (j in 0 until it.length()) pk[it.getString(j)] = n }
                a.optJSONArray("certificates")?.let { for (j in 0 until it.length()) ct[it.getString(j).uppercase()] = n }
            }
            return IocDb(o.optString("updated", "bundled"), pk, ct)
        }
    }
}

/**
 * Malware hash database. Bundled assets/malware_db.json, replaced by a downloaded copy
 * (filesDir/malware_db.json) when the user taps "Update databases".
 * Format: {"version":"..","sha256":{"<apk sha256>":"Family"},"certSha256":{..},"packages":{..}}
 */
class MalwareDb(val version: String, val sha256: Map<String, String>, val certs: Map<String, String>, val packages: Map<String, String>) {
    val size get() = sha256.size + certs.size + packages.size
    companion object {
        const val URL_DB = "https://micky9889900.github.io/vpn/malware_db.json"
        const val URL_IOC = "https://micky9889900.github.io/vpn/ioc.json"
        private fun map(o: JSONObject?): Map<String, String> {
            if (o == null) return emptyMap()
            val m = HashMap<String, String>(); o.keys().forEach { m[it.uppercase()] = o.getString(it) }; return m
        }
        fun load(ctx: Context): MalwareDb {
            val f = File(ctx.filesDir, "malware_db.json")
            val txt = if (f.exists()) f.readText() else ctx.assets.open("malware_db.json").bufferedReader().readText()
            val o = JSONObject(txt)
            val pk = HashMap<String, String>(); o.optJSONObject("packages")?.let { p -> p.keys().forEach { pk[it] = p.getString(it) } }
            return MalwareDb(o.optString("version", "bundled"), map(o.optJSONObject("sha256")), map(o.optJSONObject("certSha256")), pk)
        }

        /** Downloads both DBs. Returns a human message. Never throws. */
        fun update(ctx: Context): String {
            val out = mutableListOf<String>()
            for ((url, name) in listOf(URL_DB to "malware_db.json", URL_IOC to "ioc.json")) {
                try {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 10000; c.readTimeout = 20000
                    if (c.responseCode == 200) {
                        val t = c.inputStream.bufferedReader().readText(); JSONObject(t) // validate
                        File(ctx.filesDir, name).writeText(t); out += "$name updated"
                    } else out += "$name: server returned ${c.responseCode} (kept current copy)"
                } catch (e: Exception) { out += "$name: ${e.javaClass.simpleName} (kept current copy)" }
            }
            ctx.getSharedPreferences("db", 0).edit().putLong("checked", System.currentTimeMillis()).apply()
            return out.joinToString("\n")
        }
    }
}
