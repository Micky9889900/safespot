package com.safespot.scanner

import android.content.Context
import de.blinkt.openvpn.core.ICSOpenVPNApplication

/** Extends ics-openvpn's Application so its status bridge + notification channels are set up in every process. */
class SafeSpotApp : ICSOpenVPNApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        runCatching { CrashReporter.install(this) }
    }

    override fun onCreate() {
        CrashReporter.install(this)
        try { super.onCreate() } catch (t: Throwable) { CrashReporter.nonFatal("ICSOpenVPNApplication.onCreate", t) }
        try { com.safespot.scanner.data.Reminder.ensureChannel(this) } catch (t: Throwable) { CrashReporter.nonFatal("ensureChannel", t) }
    }
}
