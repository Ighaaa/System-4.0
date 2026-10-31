package com.systemtools.v4

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Layar setup (sisi profil utama). Tampilan menyamar sebagai "System 4.0".
 *
 * Langkah setup:
 * 1. Buat folder aman (provisioning profil kerja)
 * 2. Izinkan notifikasi + izin log panggilan
 * 3. Matikan optimasi baterai + tampil di atas aplikasi lain
 * 4. Atur PIN master + nomor trigger misscall
 * 5. Sembunyikan System 4.0 dari launcher
 */
class MainActivity : AppCompatActivity() {

    private val admin by lazy { ComponentName(this, VaultAdminReceiver::class.java) }
    private lateinit var dpm: DevicePolicyManager
    private lateinit var tvStatus: TextView

    private val provisionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnProvision).setOnClickListener { startProvisioning() }
        findViewById<Button>(R.id.btnPerms).setOnClickListener { requestPerms() }
        findViewById<Button>(R.id.btnBattery).setOnClickListener { ignoreBattery() }
        findViewById<Button>(R.id.btnOverlay).setOnClickListener { askOverlay() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveConfig() }
        findViewById<Button>(R.id.btnOpen).setOnClickListener { VaultOpener.openVault(this) }
        findViewById<Button>(R.id.btnHideSelf).setOnClickListener { hideSelf() }

        // Prefill
        findViewById<EditText>(R.id.etTrigger).setText(
            Prefs.get(this, Prefs.KEY_TRIGGER_NUMBER, Prefs.DEFAULT_TRIGGER)
        )

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val sb = StringBuilder()
        sb.append(if (isProfileOwner()) "✔ Folder aman (profil kerja): AKTIF\n"
                  else "✖ Folder aman (profil kerja): belum dibuat — lakukan Langkah 1\n")
        sb.append(
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED
            ) "✔ Izin log panggilan: AKTIF\n" else "✖ Izin log panggilan: belum — Langkah 2\n"
        )
        sb.append(
            if (Settings.canDrawOverlays(this)) "✔ Tampil di atas app lain: AKTIF\n"
            else "○ Tampil di atas app lain: disarankan aktif (Langkah 3)\n"
        )
        val ignored = (getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
            .isIgnoringBatteryOptimizations(packageName)
        sb.append(
            if (ignored) "✔ Optimasi baterai: NONAKTIF untuk app ini\n"
            else "○ Optimasi baterai: disarankan dimatikan (Langkah 3)\n"
        )
        sb.append(
            if (Prefs.getBool(this, Prefs.KEY_SELF_HIDDEN))
                "✔ System 4.0: TERSEMBUNYI dari launcher (buka via kode dialer)\n"
            else "○ System 4.0: masih terlihat di launcher\n"
        )
        tvStatus.text = sb.toString()

        if (isProfileOwner() && !isServiceRunning()) TriggerService.start(this)
    }

    private fun isProfileOwner(): Boolean = dpm.isProfileOwnerApp(packageName)

    private fun isServiceRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return am.getRunningServices(200)?.any { it.service.className == TriggerService::class.java.name } ?: false
    }

    private fun startProvisioning() {
        val i = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
        if (i.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "Perangkat tidak mendukung profil kerja", Toast.LENGTH_LONG).show()
            return
        }
        i.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
            admin
        )
        i.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
        i.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true)
        try {
            provisionLauncher.launch(i)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memulai: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPerms() {
        permLauncher.launch(
            arrayOf(
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    private fun ignoreBattery() {
        try {
            val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName"))
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "Buka manual: Setelan > Baterai > Tanpa optimasi", Toast.LENGTH_LONG).show()
        }
    }

    private fun askOverlay() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Buka manual: Setelan > Aplikasi > System 4.0", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveConfig() {
        val pin = findViewById<EditText>(R.id.etPin).text.toString().trim()
        val trigger = findViewById<EditText>(R.id.etTrigger).text.toString().trim()
        if (pin.length < 4) {
            Toast.makeText(this, "PIN minimal 4 angka (ini kunci pemulihan Anda!)", Toast.LENGTH_LONG).show()
            return
        }
        Prefs.put(this, Prefs.KEY_PIN, pin)
        Prefs.put(this, Prefs.KEY_TRIGGER_NUMBER, trigger.ifEmpty { Prefs.DEFAULT_TRIGGER })
        Toast.makeText(this, "Tersimpan ✔", Toast.LENGTH_SHORT).show()
    }

    private fun hideSelf() {
        if (Prefs.get(this, Prefs.KEY_PIN, "").length < 4) {
            Toast.makeText(this, "Atur PIN master dulu (Langkah 4)", Toast.LENGTH_LONG).show()
            return
        }
        TriggerService.start(this)
        packageManager.setComponentEnabledSetting(
            ComponentName(this, MainActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Prefs.putBool(this, Prefs.KEY_SELF_HIDDEN, true)
        Toast.makeText(
            this,
            "System 4.0 tersembunyi. Buka dengan *#*#${VaultOpener.TRIGGER_CODE}#*#* di dialer",
            Toast.LENGTH_LONG
        ).show()
        refresh()
    }
}
