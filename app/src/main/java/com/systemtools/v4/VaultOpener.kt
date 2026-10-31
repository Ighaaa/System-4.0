package com.systemtools.v4

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import androidx.core.app.NotificationCompat

/** Helper untuk membuka folder tersembunyi (berjalan di sisi profil utama). */
object VaultOpener {

    const val ACTION_OPEN = "com.systemtools.v4.OPEN_VAULT"

    /** Kode dialer untuk membuka folder. */
    const val TRIGGER_CODE = "9991"

    /** Kode dialer untuk layar PIN pemulihan. */
    const val RECOVERY_CODE = "9990"

    fun openVault(context: Context) {
        val i = Intent(ACTION_OPEN).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        try {
            context.startActivity(i)
        } catch (e: Exception) {
            // Gagal start langsung (BATU/background start dibatasi) -> notifikasi heads-up
            fallbackNotify(context)
        }
    }

    private fun fallbackNotify(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(ACTION_OPEN).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        val pi = PendingIntent.getActivity(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, App.CH_TRIGGER)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("System 4.0")
            .setContentText("Folder tersembunyi siap — ketuk untuk membuka")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            nm.notify(2001, notif)
        } catch (_: SecurityException) {
        }
    }
}
