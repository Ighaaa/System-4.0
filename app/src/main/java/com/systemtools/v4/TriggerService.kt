package com.systemtools.v4

import android.app.NotificationCompat
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ServiceInfo
import android.provider.CallLog
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Pemicu 2: misscall.
 * Foreground service ringan yang memantau call log. Ketika Anda menelepon
 * nomor trigger lalu langsung menutup panggilan, folder tersembunyi terbuka.
 */
class TriggerService : Service() {

    private var observer: CallLogObserver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = buildNotification()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            ServiceCompat.startForeground(this, NOTIF_ID, notif, 0)
        }

        val granted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_CALL_LOG
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (granted && observer == null) {
            observer = CallLogObserver(this).also {
                contentResolver.registerContentObserver(
                    CallLog.Calls.CONTENT_URI, true, it
                )
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        super.onDestroy()
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, App.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("System 4.0")
            .setContentText("Layanan sistem berjalan normal")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(recoveryIntent())
            .build()

    private fun recoveryIntent(): PendingIntent {
        val i = Intent(this, RecoveryActivity::class.java)
        return PendingIntent.getActivity(
            this, 4001, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val NOTIF_ID = 42

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, TriggerService::class.java)
                )
            } catch (_: Exception) {
            }
        }
    }
}

/** Observer call log: memicu openVault kalau ada panggilan keluar ke nomor trigger. */
class CallLogObserver(private val service: Service) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var lastFire = 0L

    override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
        check()
    }

    override fun onChange(selfChange: Boolean) {
        check()
    }

    private fun check() {
        val ctx = service
        val granted = ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.READ_CALL_LOG
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val now = System.currentTimeMillis()
        if (now - lastFire < 4000) return

        val trigger = Prefs.get(ctx, Prefs.KEY_TRIGGER_NUMBER, Prefs.DEFAULT_TRIGGER)
        if (normalize(trigger).isEmpty()) return

        try {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                null, null,
                CallLog.Calls.DATE + " DESC"
            )?.use { c ->
                if (c.moveToFirst()) {
                    val number = c.getString(0)
                    val date = c.getLong(1)
                    val type = c.getInt(2)
                    val matched =
                        type == CallLog.Calls.OUTGOING_TYPE &&
                            date > now - 60_000 &&
                            normalize(number ?: "") == normalize(trigger)
                    if (matched) {
                        lastFire = now
                        VaultOpener.openVault(ctx)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun normalize(s: String): String = s.filter { it.isDigit() }
}
