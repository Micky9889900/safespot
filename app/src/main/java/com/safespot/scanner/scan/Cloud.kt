package com.safespot.scanner.scan

import com.safespot.scanner.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Optional MalwareBazaar (abuse.ch) hash lookup. Only SHA-256 hashes are sent, only after the user
 * opts in, and only if the build has an abuse.ch Auth-Key (gradle property mbAuthKey).
 */
object Cloud {
    val available get() = BuildConfig.MB_AUTH_KEY.isNotBlank()
    sealed class Hit { data class Malware(val signature: String) : Hit(); object Clean : Hit(); data class Error(val msg: String) : Hit() }
    fun lookup(sha256: String): Hit = try {
        val c = URL("https://mb-api.abuse.ch/api/v1/").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 10000; c.readTimeout = 15000
        c.setRequestProperty("Auth-Key", BuildConfig.MB_AUTH_KEY)
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        c.outputStream.use { it.write("query=get_info&hash=${URLEncoder.encode(sha256.lowercase(), "UTF-8")}".toByteArray()) }
        val o = JSONObject(c.inputStream.bufferedReader().readText())
        when (o.optString("query_status")) {
            "ok" -> Hit.Malware(o.optJSONArray("data")?.optJSONObject(0)?.optString("signature")?.takeIf { it.isNotBlank() && it != "null" } ?: "Known malware")
            "hash_not_found" -> Hit.Clean
            else -> Hit.Error(o.optString("query_status"))
        }
    } catch (e: Exception) { Hit.Error(e.javaClass.simpleName) }
}
