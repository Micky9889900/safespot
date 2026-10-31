package com.safespot.scanner

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes any uncaught exception to filesDir/crash.txt; MainActivity shows it on next launch. */
object CrashReporter {
    private const val FILE = "crash.txt"
    @Volatile private var dir: File? = null

    fun install(ctx: Context) {
        dir = ctx.filesDir
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        if (prev is Handler) return
        Thread.setDefaultUncaughtExceptionHandler(Handler(prev))
    }

    /** Records a non-fatal caught exception during startup (shown with the next crash report). */
    fun nonFatal(where: String, t: Throwable) {
        Log.w("SafeSpot", "non-fatal in $where", t)
        runCatching { File(dir ?: return, "nonfatal.txt").appendText("[$where] " + stack(t) + "\n") }
    }

    fun pending(ctx: Context): String? = runCatching { File(ctx.filesDir, FILE).takeIf { it.exists() }?.readText() }.getOrNull()
    fun clear(ctx: Context) { runCatching { File(ctx.filesDir, FILE).delete(); File(ctx.filesDir, "nonfatal.txt").delete() } }

    private fun stack(t: Throwable) = StringWriter().also { t.printStackTrace(PrintWriter(it)) }.toString()

    private class Handler(val prev: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(th: Thread, e: Throwable) {
            runCatching {
                val d = dir ?: return@runCatching
                val nf = File(d, "nonfatal.txt").takeIf { it.exists() }?.readText().orEmpty()
                File(d, FILE).writeText(buildString {
                    append("SafeSpot ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
                    append("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())}\n")
                    append("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ABIs ${Build.SUPPORTED_ABIS.joinToString()}\n")
                    append("Thread: ${th.name}\n\n").append(stack(e))
                    if (nf.isNotEmpty()) append("\n--- earlier non-fatal errors ---\n").append(nf)
                })
            }
            prev?.uncaughtException(th, e)
        }
    }
}
