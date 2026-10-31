package com.systemtools.v4

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Pintu darurat: buka dengan kode *#*#9990#*#* atau tap notifikasi layanan.
 * PIN master yang benar akan memunculkan kembali icon System 4.0.
 */
class RecoveryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery)

        findViewById<Button>(R.id.btnRecover).setOnClickListener {
            val pin = findViewById<EditText>(R.id.etRecoveryPin).text.toString().trim()
            val saved = Prefs.get(this, Prefs.KEY_PIN, "")
            if (saved.isNotEmpty() && pin == saved) {
                packageManager.setComponentEnabledSetting(
                    ComponentName(this, MainActivity::class.java),
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                Prefs.putBool(this, Prefs.KEY_SELF_HIDDEN, false)
                TriggerService.start(this)
                Toast.makeText(this, "Pulih ✔ System 4.0 muncul kembali di launcher", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "PIN salah", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
