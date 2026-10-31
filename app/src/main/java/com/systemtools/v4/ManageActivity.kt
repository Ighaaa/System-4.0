package com.systemtools.v4

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Kelola app di dalam profil kerja: sembunyikan / tampilkan permanen.
 * Activity ini juga berjalan di dalam profil kerja.
 */
class ManageActivity : AppCompatActivity() {

    private val admin by lazy { ComponentName(this, VaultAdminReceiver::class.java) }
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adapter: ManageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (!dpm.isProfileOwnerApp(packageName)) {
            Toast.makeText(this, "Buka lewat folder tersembunyi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_manage)

        val rv = findViewById<RecyclerView>(R.id.rvManage)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ManageAdapter(AppListHelper.loadAll(this, dpm, admin)) { entry, hide ->
            toggle(entry, hide)
        }
        rv.adapter = adapter

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            val play = packageManager.getLaunchIntentForPackage("com.android.vending")
            play?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (play != null) startActivity(play)
            else Toast.makeText(this, "Play Store tidak ada di profil ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggle(entry: VaultActivity.AppEntry, hide: Boolean) {
        if (hide) {
            AlertDialog.Builder(this)
                .setTitle("Sembunyikan ${entry.label}?")
                .setMessage(
                    "App akan hilang dari launcher & Setelan. " +
                        "Buka lagi lewat folder System 4.0 (kode dialer)."
                )
                .setPositiveButton("Sembunyikan") { _, _ ->
                    try {
                        dpm.setApplicationHidden(admin, entry.pkg, true)
                        reload()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        } else {
            try {
                dpm.setApplicationHidden(admin, entry.pkg, false)
                reload()
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun reload() {
        adapter.replace(AppListHelper.loadAll(this, dpm, admin))
    }
}

class ManageAdapter(
    private var items: List<VaultActivity.AppEntry>,
    private val onToggle: (VaultActivity.AppEntry, Boolean) -> Unit
) : RecyclerView.Adapter<ManageAdapter.VH>() {

    fun replace(newItems: List<VaultActivity.AppEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.ivIcon)
        val label: TextView = v.findViewById(R.id.tvLabel)
        val pkg: TextView = v.findViewById(R.id.tvPkg)
        val sw: MaterialSwitch = v.findViewById(R.id.swHide)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_manage, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.icon.setImageDrawable(item.icon)
        h.label.text = item.label
        h.pkg.text = if (item.hidden) "🔒 tersembunyi" else item.pkg
        h.sw.setOnCheckedChangeListener(null)
        h.sw.isChecked = item.hidden
        h.sw.setOnCheckedChangeListener { _, checked ->
            onToggle(item, checked)
        }
    }
}
