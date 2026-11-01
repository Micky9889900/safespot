package com.safespot.scanner.vpn

import android.content.Context
import kotlinx.coroutines.launch
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
    val score: Long = 0,
) {
    val flag: String get() = countryCode.uppercase().takeIf { it.length == 2 }
        ?.map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }?.joinToString("") ?: "🌐"

    fun toJson() = JSONObject().put("id", id).put("type", type).put("host", host).put("port", port).put("proto", proto).put("country", country)
        .put("countryCode", countryCode).put("config", config).put("provider", provider).put("speedMbps", speedMbps).put("pingMs", listedPingMs).put("score", score)
}

object Servers {
    data class Loaded(val servers: List<VpnServer>, val source: String)

    private fun get(url: String): String? = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 5000; c.readTimeout = 12000; c.setRequestProperty("User-Agent", "SafeSpot/1.0")
        if (c.responseCode != 200) null else c.inputStream.bufferedReader().readText()
    }.getOrNull()

    fun parseJson(txt: String): List<VpnServer> {
        val root = JSONObject(txt); val a: JSONArray = root.getJSONArray("servers")
        return (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
            VpnServer(o.optString("id", "s$i"), o.optString("type", "openvpn"), o.getString("host"), o.optInt("port", 443), o.optString("proto", "tcp"),
                o.optString("country"), o.optString("countryCode"), o.getString("config"), o.optString("provider", "custom"),
                o.optDouble("speedMbps", 0.0), o.optInt("pingMs", 0), score = o.optLong("score", 0)) } }.filter { it.type == "openvpn" }
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
                (f[4].toLongOrNull() ?: 0) / 1e6, f[3].toIntOrNull() ?: 0, score = f[2].toLongOrNull() ?: 0)
        }.toList()

    private fun cacheFile(ctx: Context) = File(ctx.filesDir, "servers_cache.json")

    /** Instant: last good list from disk (no network). */
    fun loadCached(ctx: Context): Loaded? = runCatching { cacheFile(ctx).takeIf { it.exists() }?.let { Loaded(parseJson(it.readText()), "Cached list") } }.getOrNull()?.takeIf { it.servers.isNotEmpty() }

    /** Network: SafeSpot list (small, top ~40) first, then the VPN Gate API. Writes the cache. */
    fun loadRemote(ctx: Context): Loaded? {
        val cache = cacheFile(ctx)
        get(BuildConfig.SERVERS_URL)?.let { t -> runCatching { parseJson(t) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
            cache.writeText(t); return Loaded(it, "SafeSpot server list") } }
        get(BuildConfig.VPNGATE_URL)?.let { t -> parseVpnGate(t).takeIf { it.isNotEmpty() }?.let { list ->
            val best = rank(list).take(60)
            cache.writeText(JSONObject().put("servers", JSONArray(best.map { it.toJson() })).toString())
            return Loaded(best, "VPN Gate (live)") } }
        return null
    }

    fun load(ctx: Context): Loaded = loadRemote(ctx) ?: loadCached(ctx) ?: Loaded(emptyList(), "Offline: no server list")

    /** Static rank before probing: measured ping > UDP > listed speed/score > listed ping. */
    fun rank(list: List<VpnServer>): List<VpnServer> = list.sortedByDescending { s ->
        var r = s.speedMbps.coerceAtMost(500.0) + s.score / 50_000.0
        if (s.proto.startsWith("udp")) r += 60
        if (s.listedPingMs in 1..60) r += 40 else if (s.listedPingMs > 200) r -= 40
        if (s.pingMs > 0) r += 400 - s.pingMs.coerceAtMost(400) else if (s.pingMs == -2) r -= 1000 // -2 = probed dead
        r }

    /** Real TCP connect time. UDP servers can't be TCP-probed on their port; VPN Gate relays also listen on TCP 443, so probe that. */
    fun ping(s: VpnServer, timeoutMs: Int = 1500): Int = runCatching {
        val port = if (s.proto.startsWith("udp") && s.provider == "vpngate") 443 else s.port
        val t = System.nanoTime()
        Socket().use { it.connect(InetSocketAddress(s.host, port), timeoutMs) }
        ((System.nanoTime() - t) / 1_000_000).toInt().coerceAtLeast(1)
    }.getOrDefault(-2)

    /** Probe [n] candidates in parallel (1.5 s cap); responsive ones in the order they answered. */
    suspend fun fastest(cands: List<VpnServer>, n: Int = 8): List<VpnServer> = kotlinx.coroutines.coroutineScope {
        val ch = kotlinx.coroutines.channels.Channel<VpnServer?>(n)
        val top = cands.take(n)
        top.forEach { s -> launch(kotlinx.coroutines.Dispatchers.IO) { val ms = ping(s); ch.send(if (ms > 0) s.copy(pingMs = ms) else null) } }
        val out = ArrayList<VpnServer>(); repeat(top.size) { ch.receive()?.let(out::add) }; out
    }
}
