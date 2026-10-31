package com.safespot.scanner.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safespot.scanner.MainActivity
import com.safespot.scanner.R
import com.safespot.scanner.scan.Risk
import com.safespot.scanner.scan.Scanner

/**
 * Daily reminder + "real-time" protection: once a day (and whenever the app opens) SafeSpot checks
 * apps installed or updated since the last check and notifies if any is risky. Android does not
 * allow background apps to receive PACKAGE_ADDED from the manifest, so this is the honest equivalent.
 */
object Reminder {
    const val CH = "safespot_alerts"
    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) ctx.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CH, "Security alerts & reminders", NotificationManager.IMPORTANCE_DEFAULT))
    }
    private fun pi(ctx: Context) = PendingIntent.getBroadcast(ctx, 7, Intent(ctx, DailyReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun schedule(ctx: Context, on: Boolean) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pi(ctx))
        if (on) am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, pi(ctx))
    }
    fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(ctx).notify(id, NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).build())
    }
    /** Checks apps changed since the last check. Returns risky ones. */
    fun checkNewApps(ctx: Context, notify: Boolean): List<com.safespot.scanner.scan.AppReport> {
        val p = ctx.getSharedPreferences("safespot", 0)
        val since = p.getLong("rtCheck", System.currentTimeMillis())
        p.edit().putLong("rtCheck", System.currentTimeMillis()).apply()
        val bad = runCatching { Scanner.scanSince(ctx, since) }.getOrDefault(emptyList()).filter { it.risk >= Risk.MEDIUM }
        if (notify && bad.isNotEmpty()) notify(ctx, 2, "New app needs a look", bad.joinToString { "${it.label} (${it.risk.label})" })
        return bad
    }
}

class DailyReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pr = goAsync()
        Thread {
            try {
                val bad = Reminder.checkNewApps(ctx, true)
                if (bad.isEmpty()) Reminder.notify(ctx, 1, "Daily security check", "Keep your streak going: open SafeSpot and run today's scan. " + Store.tipOfDay())
            } finally { pr.finish() }
        }.start()
    }
}
