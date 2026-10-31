# System 4.0 — Folder Tersembunyi Android

App "folder aman" untuk Android 10–15 **tanpa root**. Menggunakan mekanisme resmi
Android **Work Profile** (sama seperti app Island/Shelter): app yang disembunyikan
benar-benar hilang dari launcher dan Settings, dan hanya bisa dibuka lewat pemicu.

## Fitur
- 📁 Folder tersembunyi berisi app pilihan Anda (grid)
- 🔒 Sembunyikan/tampilkan app kapan saja (benar-benar hilang dari launcher)
- 📞 Dua pemicu buka:
  - Kode dialer: `*#*#9991#*#*`
  - Misscall: telepon nomor pemicu lalu langsung tutup (terdeteksi dari call log)
- 🕶️ System 4.0 menyembunyikan dirinya sendiri dari launcher
- ⏱️ Auto re-hide: layar mati / setelah 10 menit
- 🆘 Pintu darurat: kode `*#*#9990#*#*` atau tap notifikasi → PIN master

## Cara dapat APK (tanpa Android Studio)
1. Download ZIP project ini, ekstrak.
2. Buat akun GitHub (gratis) → buat repository baru (mis. `system40`) → **Add file → Upload files** → upload SEMUA isi folder hasil ekstrak (termasuk folder `.github` — aktifkan "show hidden files" di file manager HP) → Commit.
3. Buka tab **Actions** di repo → tunggu workflow "Build APK" selesai (± 5 menit).
4. Klik run yang selesai → bagian **Artifacts** → download **System40-APK**.
5. Pindahkan `app-debug.apk` ke HP → buka → izinkan instal dari sumber tidak dikenal.
   (Jika Play Protect menolak: Play Protect → ikon roda gigi → matikan "Scan aplikasi dengan Play Protect" / pilih "Tetap instal".)

## Setup pertama kali di HP
1. Buka **System 4.0** (icon gembok).
2. **Langkah 1** — Buat Folder Aman: konfirmasi dialog "profil kerja" dari Android.
3. **Langkah 2** — izinkan Notifikasi + Log Panggilan.
4. **Langkah 3** — matikan optimasi baterai + izinkan tampil di atas app lain (agar pemicu responsif).
5. **Langkah 4** — atur PIN master + nomor pemicu misscall (gunakan nomor asli yang bisa ditelepon, mis. nomor SIM kedua Anda).
6. **Langkah 5** — Sembunyikan System 4.0.

## Menyembunyikan sebuah aplikasi
1. Buka folder tersembunyi (kode dialer atau misscall).
2. Tekan **＋ Instal App via Play** → masuk akun Google → instal app yang ingin disembunyikan (data mulai dari awal — sifat profil kerja tanpa root).
3. Tekan **Kelola App** → nyalakan switch untuk app tersebut → app hilang dari launcher.
4. Uninstall aslinya dari HP utama (opsional, agar tidak ada duplikat).
5. Buka kembali kapan saja lewat folder System 4.0 — app otomatis tersembunyi lagi saat layar mati.

## Catatan penting
- **Samsung (Fold 3)**: kode dialer `*#*#9991#*#*` didukung. Pastikan di One UI:
  Setelan → Baterai → Batas penggunaan latar belakang → jadikan System 4.0 "Tidak pernah tidur",
  dan nonaktifkan "Menidurkan aplikasi yang tidak digunakan" untuk app ini.
- Kode dialer dapat diubah di `VaultOpener.kt` (`TRIGGER_CODE`) dan `AndroidManifest.xml` (`android:host`).
- Nomor pemicu misscall harus nomor asli yang bisa ditelepon — nomor pendek seperti `9991`
  biasanya tidak masuk call log (gunakan kode dialer untuk angka pendek).
- Jika HP di-restart dan pemicu tidak merespons: tap notifikasi "System 4.0 — Layanan
  sistem berjalan normal" → masukkan PIN master.
- **Gunakan hanya di perangkat milik Anda sendiri.** Jangan gunakan untuk memata-matai
  perangkat orang lain tanpa izin.

## Arsitektur singkat
- `MainActivity` — wizard setup (sisi profil utama)
- `VaultAdminReceiver` — Profile Owner controller (pengendali hide/unhide)
- `VaultActivity` — folder tersembunyi (berjalan di dalam profil kerja)
- `ManageActivity` — kelola hide/show app
- `SecretCodeReceiver` — pemicu kode dialer
- `TriggerService` + `CallLogObserver` — pemicu misscall (foreground service)
- `ReHideReceiver` — auto re-hide terjadwal
- `RecoveryActivity` — pintu darurat PIN master
