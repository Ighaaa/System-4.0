package com.systemtools.v4

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * Pemicu 1: kode rahasia di dialer.
 * - *#*#9991#*#*  -> buka folder tersembunyi
 * - *#*#9990#*#*  -> layar PIN pemulihan
 *
 * Receiver ini terpasang di salinan profil utama DAN profil kerja;
 * keduanya berakhir memanggil openVault yang menyeberang profil dengan aman.
 */
class SecretCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != TelephonyManager.ACTION_SECRET_CODE) return
        val code = intent.data?.host
        when (code) {
            VaultOpener.TRIGGER_CODE -> VaultOpener.openVault(context)
            VaultOpener.RECOVERY_CODE -> {
                try {
                    context.startActivity(
                        Intent(context, RecoveryActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}
