package com.systemtools.v4

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.app.admin.DevicePolicyManager

/**
 * Profile Owner controller. Setelah provisioning selesai, salinan app di dalam
 * profil kerja menerima callback ini dan menyiapkan folder tersembunyi.
 */
class VaultAdminReceiver : DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, VaultAdminReceiver::class.java)

        if (!dpm.isProfileOwnerApp(context.packageName)) return

        try {
            dpm.setProfileName(admin, "System 4.0")
        } catch (_: Exception) {
        }

        // Sembunyikan launcher icon milik salinan di dalam profil kerja
        try {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {
        }

        // Izinkan profil utama membuka VaultActivity di dalam profil kerja
        try {
            val filter = IntentFilter(VaultOpener.ACTION_OPEN)
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            dpm.addCrossProfileIntentFilter(
                admin, filter,
                DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_PROFILE
            )
        } catch (_: Exception) {
        }

        Prefs.putBool(context, "profile_ready", true)
    }
}
