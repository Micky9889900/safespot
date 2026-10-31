package com.safespot.scanner.vpn

import android.content.Context
import android.util.Base64
import com.safespot.scanner.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * One VPN endpoint. The same schema is used for free VPN Gate relays today and for
 * paid / own servers later: just publish a new servers.json, no app update needed.
 */
data class VpnServer(
    val id: String,
    val type: String,        // "openvpn" (wireguard reserved for later)
    val host: String,
    val port: Int,
    val proto: String,       // tcp / udp
    val country: String,
    val countryCode: String,
    val config: String,      // full .ovpn text
    val provider: String,    // "vpngate", "safespot", ...
    val speedMbps: Double = 0.0,
    val listedPingMs: Int = 0,
    var pingMs: Int = -1,
) {
    val flag: String get() = countryCode.uppercase().takeIf { it.length == 2 }
        ?.map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }?.joinToString("") ?: "🌐"

    fun toJson() = JSONObject().put("id", id).put("type", type).put("host", host).put("port", port).put("proto", proto).put("country", country)
        .put("countryCode", countryCode).put("config", config).put("provider", provider).put("speedMbps", speedMbps).put("pingMs", listedPingMs)
}

object Servers {
    data class Loaded(val servers: List<VpnServer>, val source: String)

    private fun get(url: String): String? = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 25000; c.setRequestProperty("User-Agent", "SafeSpot/1.0")
        if (c.responseCode != 200) null else c.inputStream.bufferedReader().readText()
    }.getOrNull()

    fun parseJson(txt: String): List<VpnServer> {
        val root = JSONObject(txt); val a: JSONArray = root.getJSONArray("servers")
        return (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
            VpnServer(o.optString("id", "s$i"), o.optString("type", "openvpn"), o.getString("host"), o.optInt("port", 443), o.optString("proto", "tcp"),
                o.optString("country"), o.optString("countryCode"), o.getString("config"), o.optString("provider", "custom"),
                o.optDouble("speedMbps", 0.0), o.optInt("pingMs", 0)) } }.filter { it.type == "openvpn" }
    }

    /** VPN Gate CSV: #HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,...,OpenVPN_ConfigData_Base64 */
    fun parseVpnGate(csv: String): List<VpnServer> = csv.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("*") && !it.startsWith("#") }
        .mapNotNull { line ->
            val f = line.split(','); if (f.size < 15) return@mapNotNull null
            val cfg = runCatching { String(Base64.decode(f[14], Base64.DEFAULT)) }.getOrNull() ?: return@mapNotNull null
            val remote = Regex("(?m)^remote\\s+(\\S+)\\s+(\\d+)").find(cfg) ?: return@mapNotNull null
            val proto = Regex("(?m)^proto\\s+(\\S+)").find(cfg)?.groupValues?.get(1) ?: "tcp"
            VpnServer("vg-${f[1]}-${remote.groupValues[2]}", "openvpn", f[1], remote.groupValues[2].toInt(), proto, f[5], f[6], cfg, "vpngate",
                (f[4].toLongOrNull() ?: 0) / 1e6, f[3].toIntOrNull() ?: 0)
        }.toList()

    /** Remote JSON first, then VPN Gate API, then the cached list. */
    fun load(ctx: Context): Loaded {
        val cache = File(ctx.filesDir, "servers_cache.json")
        get(BuildConfig.SERVERS_URL)?.let { t -> runCatching { parseJson(t) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
            cache.writeText(t); return Loaded(it, "SafeSpot server list") } }
        get(BuildConfig.VPNGATE_URL)?.let { t -> parseVpnGate(t).takeIf { it.isNotEmpty() }?.let { list ->
            val best = list.sortedByDescending { it.speedMbps }.take(120)
            cache.writeText(JSONObject().put("servers", JSONArray(best.map { it.toJson() })).toString())
            return Loaded(best, "VPN Gate (live)") } }
        if (cache.exists()) runCatching { return Loaded(parseJson(cache.readText()), "Cached list") }
        return Loaded(emptyList(), "Offline: no server list")
    }

    /** Real TCP connect time to the server's VPN port. */
    fun ping(s: VpnServer): Int = runCatching {
        val t = System.nanoTime()
        Socket().use { it.connect(InetSocketAddress(s.host, s.port), 2500) }
        ((System.nanoTime() - t) / 1_000_000).toInt()
    }.getOrDefault(-1)
}
