package com.safespot.scanner

import de.blinkt.openvpn.core.ICSOpenVPNApplication

/** Extends ics-openvpn's Application so its status bridge + notification channels are set up in every process. */
class SafeSpotApp : ICSOpenVPNApplication() {
    override fun onCreate() {
        super.onCreate()
        com.safespot.scanner.data.Reminder.ensureChannel(this)
    }
}
