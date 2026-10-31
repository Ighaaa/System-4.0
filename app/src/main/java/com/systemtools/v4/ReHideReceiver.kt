package com.systemtools.v4

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Auto re-hide terjadwal: memastikan app yang sempat dibuka
 * tersembunyi kembali walau folder sudah ditutup lama.
 */
class ReHideReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REHIDE) return
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
            as android.app.admin.DevicePolicyManager
        val admin = ComponentName(context, VaultAdminReceiver::class.java)
        if (!dpm.isProfileOwnerApp(context.packageName)) return

        val sp = context.getSharedPreferences("sys40_temp", Context.MODE_PRIVATE)
        val targets = sp.getStringSet("visible", emptySet()) ?: emptySet()
        for (pkg in targets) {
            try {
                dpm.setApplicationHidden(admin, pkg, true)
            } catch (_: Exception) {
            }
        }
        sp.edit().clear().apply()
    }

    companion object {
        const val ACTION_REHIDE = "com.systemtools.v4.REHIDE"
        private const val REQUEST = 3001

        fun schedule(context: Context, delayMs: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pending(context)
            val at = System.currentTimeMillis() + delayMs
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } catch (_: SecurityException) {
            }
        }

        fun cancel(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pending(context))
        }

        private fun pending(context: Context): PendingIntent {
            val i = Intent(context, ReHideReceiver::class.java).setAction(ACTION_REHIDE)
            return PendingIntent.getBroadcast(
                context, REQUEST, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
