package com.systemtools.v4

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Folder tersembunyi. Activity ini berjalan di dalam PROFIL KERJA
 * (dibuka lewat cross-profile intent dari pemicu di profil utama).
 *
 * - Grid berisi app yang ada di dalam profil (termasuk yang disembunyikan)
 * - Tap app -> di-unhide sesaat -> dibuka -> auto re-hide (layar mati / 10 menit)
 * - Tombol kelola untuk hide/show permanen
 */
class VaultActivity : AppCompatActivity() {

    private val admin by lazy { ComponentName(this, VaultAdminReceiver::class.java) }
    private lateinit var dpm: DevicePolicyManager
    private val tempVisible = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reHideAll()
        }
    }

    private val reHideRunnable = Runnable { reHideAll() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (!dpm.isProfileOwnerApp(packageName)) {
            Toast.makeText(this, "Buka folder lewat kode dialer dari HP utama", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_vault)

        val rv = findViewById<RecyclerView>(R.id.rvVault)
        rv.layoutManager = GridLayoutManager(this, 4)
        rv.adapter = VaultGridAdapter(loadApps()) { info -> openApp(info.pkg) }

        findViewById<Button>(R.id.btnManage).setOnClickListener {
            startActivity(Intent(this, ManageActivity::class.java))
        }
        findViewById<Button>(R.id.btnHideAll).setOnClickListener {
            reHideAll()
            Toast.makeText(this, "Semua app tersembunyi lagi ✔", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPlay).setOnClickListener { openPlayInProfile() }

        ContextCompat.registerReceiver(
            this, screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Batalkan alarm re-hide yang lalu (kita sedang membukanya sekarang)
        ReHideReceiver.cancel(this)
        refreshAdapter()
    }

    private fun loadApps(): List<AppEntry> {
        val pm = packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = ArrayList<AppEntry>()
        val resolved = try {
            pm.queryIntentActivities(query, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            emptyList()
        }
        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == packageName) continue
            val hidden = try {
                dpm.isApplicationHidden(admin, pkg)
            } catch (e: Exception) {
                false
            }
            list.add(
                AppEntry(
                    label = ri.loadLabel(pm)?.toString() ?: pkg,
                    pkg = pkg,
                    icon = ri.activityInfo.loadIcon(pm),
                    hidden = hidden
                )
            )
        }
        return list.distinctBy { it.pkg }.sortedWith(
            compareByDescending<AppEntry> { it.hidden }.thenBy { it.label.lowercase() }
        )
    }

    private fun refreshAdapter() {
        val rv = findViewById<RecyclerView>(R.id.rvVault)
        (rv.adapter as? VaultGridAdapter)?.replace(loadApps())
    }

    private fun openApp(pkg: String) {
        try {
            dpm.setApplicationHidden(admin, pkg, false)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }
        tempVisible.add(pkg)
        handler.postDelayed({
            try {
                val launch = packageManager.getLaunchIntentForPackage(pkg)
                launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (launch != null) startActivity(launch)
            } catch (_: Exception) {
            }
        }, 300)
        // Auto re-hide 10 menit ke depan
        handler.removeCallbacks(reHideRunnable)
        handler.postDelayed(reHideRunnable, 10 * 60 * 1000L)
        refreshAdapter()
    }

    private fun reHideAll() {
        val targets = tempVisible.toList()
        tempVisible.clear()
        for (pkg in targets) {
            try {
                dpm.setApplicationHidden(admin, pkg, true)
            } catch (_: Exception) {
            }
        }
        if (targets.isNotEmpty()) refreshAdapter()
    }

    private fun openPlayInProfile() {
        try {
            val play = packageManager.getLaunchIntentForPackage("com.android.vending")
            if (play != null) {
                play.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(play)
            } else {
                Toast.makeText(this, "Play Store tidak ada di profil ini", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka Play: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {
        }
        handler.removeCallbacks(reHideRunnable)
        // Jadwalkan re-hide otomatis 10 menit ke depan walau activity sudah ditutup
        ReHideReceiver.schedule(this, 10 * 60 * 1000L)
    }

    data class AppEntry(
        val label: String,
        val pkg: String,
        val icon: android.graphics.drawable.Drawable,
        val hidden: Boolean
    )
}

/** Adapter grid folder. */
class VaultGridAdapter(
    private var items: List<VaultActivity.AppEntry>,
    private val onClick: (VaultActivity.AppEntry) -> Unit
) : RecyclerView.Adapter<VaultGridAdapter.VH>() {

    fun replace(newItems: List<VaultActivity.AppEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.ivIcon)
        val label: TextView = v.findViewById(R.id.tvLabel)
        val badge: TextView = v.findViewById(R.id.tvBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.icon.setImageDrawable(item.icon)
        h.label.text = item.label
        h.badge.visibility = if (item.hidden) View.VISIBLE else View.GONE
        h.itemView.setOnClickListener { onClick(item) }
    }
}

/** Utility kecil untuk dipakai ManageActivity. */
object AppListHelper {
    fun loadAll(context: Context, dpm: DevicePolicyManager, admin: ComponentName): List<VaultActivity.AppEntry> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val out = ArrayList<VaultActivity.AppEntry>()
        val resolved = try {
            pm.queryIntentActivities(query, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            emptyList()
        }
        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue
            val hidden = try {
                dpm.isApplicationHidden(admin, pkg)
            } catch (e: Exception) {
                false
            }
            out.add(
                VaultActivity.AppEntry(
                    label = ri.loadLabel(pm)?.toString() ?: pkg,
                    pkg = pkg,
                    icon = ri.activityInfo.loadIcon(pm),
                    hidden = hidden
                )
            )
        }
        return out.distinctBy { it.pkg }.sortedWith(
            compareByDescending<VaultActivity.AppEntry> { it.hidden }.thenBy { it.label.lowercase() }
        )
    }
}
