# Design Specification: Migrasi Chrome Custom Tabs ke Embedded WebView

- **Date**: 2026-09-22
- **Topic**: WebView Migration for Absen Perhutani
- **Target**: `com.renaldi.absenperhutani`

## 1. Problem Statement & Goals
* **Problem**: Aplikasi saat ini menggunakan Chrome Custom Tabs untuk membuka portal absensi DANI Perhutani (`https://dani.perhutani.co.id/login`). Tampilan Custom Tabs memunculkan address bar, menu browser, dan kontrol navigasi Chrome yang membingungkan bagi pengguna lansia/gaptek.
* **Goal**: Mengganti Custom Tabs dengan embedded `WebView` native yang full-screen tanpa UI browser agar terasa seperti aplikasi native seutuhnya.
* **Constraints**:
  * Portal web dilindungi Cloudflare WAF (JavaScript challenge). WebView harus mampu menyelesaikan challenge (JS aktif, DOM storage aktif, CookieManager persisten, default User-Agent dipertahankan tanpa modifikasi).
  * Desain ramah lansia: interaksi minim, tombol retry besar, navigasi kembali (back button) intuitif.
  * Murni YAGNI: tidak memerlukan GPS/kamera, tidak menambah library/framework baru.

## 2. Architecture & UI Changes

### A. Layout (`app/src/main/res/layout/activity_main.xml`)
Mengubah layout menjadi container 3 elemen bertingkat:
1. **`WebView` (`@+id/webView`)**:
   * Lebar & tinggi: `match_parent`.
   * Default visibility: `View.GONE` (atau ditampilkan langsung begitu URL mulai dimuat).
2. **`ProgressBar` (`@+id/progressBar`)**:
   * Mengubah dari spinner tengah ke `ProgressBar` horizontal di sisi paling atas (`android:layout_alignParentTop="true"`).
   * Menggunakan style `?android:attr/progressBarStyleHorizontal`.
   * Tinggi: `3dp` - `4dp` dengan warna hijau Perhutani (`#2E7D32`).
   * Hilang otomatis saat progress mencapai 100%. Tidak menghalangi interaksi jika muncul challenge interaktif Cloudflare (misal: checkbox Turnstile).
3. **`LinearLayout` (`@+id/errorLayout`)**:
   * Tampil hanya saat terjadi error koneksi fatal.
   * Pesan teks jelas dan tombol "Coba Lagi" (`@+id/retryButton`) berukuran besar (`minHeight="54dp"`, lebar proporsional) agar mudah ditekan lansia.

### B. Controller (`app/src/main/java/com/renaldi/absenperhutani/MainActivity.kt`)

#### 1. Inisialisasi & WebSettings
* Aktifkan:
  * `settings.javaScriptEnabled = true` (Wajib untuk Cloudflare & Single-Page App).
  * `settings.domStorageEnabled = true` (Wajib untuk local storage).
  * `settings.databaseEnabled = true`.
* **User-Agent**: Wajib menggunakan bawaan default WebView (`settings.userAgentString` bawaan sistem). Dilarang keras meng-override atau menambahkan string kustom karena akan memicu blokir Cloudflare WAF.

#### 2. CookieManager (Persistensi Sesi & Cloudflare Clearance)
* `CookieManager.getInstance().setAcceptCookie(true)`
* `CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)`
* Panggil `CookieManager.getInstance().flush()` pada:
  * Event `onPageFinished()` di `WebViewClient`.
  * Lifecycle `onPause()` di `MainActivity`.
  * Memastikan cookie `cf_clearance` dan session login pengguna tersimpan persisten ke disk dan tidak hilang saat aplikasi ditutup.

#### 3. Client & Error Handling
* **`WebChromeClient`**:
  * Override `onProgressChanged(view, newProgress)` untuk mengupdate nilai horizontal `ProgressBar` dan menyembunyikannya saat `newProgress == 100`.
* **`WebViewClient`**:
  * Override `shouldOverrideUrlLoading(view, request)`: return `false` agar navigasi internal tetap di dalam WebView.
  * Override `onReceivedError()`:
    * Tampilkan `errorLayout` hanya untuk error jaringan fatal (`ERROR_HOST_LOOKUP`, `ERROR_CONNECT`, `ERROR_TIMEOUT`, dll.).
    * Abaikan status HTTP non-fatal (seperti 403 sementara saat Cloudflare sedang memproses tantangan).
* **Back Button Navigation**:
  * Daftarkan callback via `onBackPressedDispatcher`:
    * Jika `webView.canGoBack()`, panggil `webView.goBack()`.
    * Jika tidak bisa kembali (sudah di halaman awal/login), tutup Activity (`finish()`).

### C. Dependency Cleanup
* Hapus library `androidx.browser:browser` dari:
  * `app/build.gradle.kts`
  * `gradle/libs.versions.toml`
* Tidak ada library baru yang ditambahkan karena `android.webkit.WebView` adalah komponen bawaan Android SDK.

## 3. Edge Cases & Mitigasi
* **Sinyal Putus di Lapangan**: Pengguna dapat menekan tombol "Coba Lagi" yang memicu `webView.reload()` atau memuat ulang URL target jika belum termuat.
* **Proses Verifikasi Cloudflare**: Karena progress bar diletakkan di atas (bukan spinner fullscreen di tengah), pengguna tetap dapat melihat dan mengklik jika Cloudflare menampilkan challenge interaktif.
* **State Preservation**: `CookieManager.flush()` menjamin pengguna tidak perlu login berulang kali jika sesi web masih valid.

## 4. Verification Plan
* Verifikasi build APK via `./gradlew assembleDebug`.
* Pastikan tidak ada compiler warning atau dependency error setelah menghapus `androidx.browser`.
* Jalankan Android lint via `./gradlew lintDebug` untuk memastikan kepatuhan WebView settings & accessibility.
