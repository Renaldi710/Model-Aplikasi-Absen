# 📱 Absen Perhutani

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Language-Kotlin-orange?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/Version-1.0.0-red?style=for-the-badge" />
</p>

<p align="center">
  Aplikasi Android untuk akses sistem absensi Perhutani (DANI) secara cepat dan mudah langsung dari smartphone.
</p>

---

## ✨ Fitur

- 🚀 **Akses Langsung** — Buka sistem DANI Perhutani dengan satu tap
- 🔒 **Aman** — Menggunakan Chrome asli via Custom Tabs, bukan browser bawaan
- 📶 **Deteksi Koneksi** — Notifikasi otomatis jika tidak ada internet
- 🔄 **Retry Otomatis** — Tombol coba lagi jika koneksi bermasalah
- 📱 **Mobile Optimized** — Tampilan menyesuaikan layar HP

---

## 📸 Screenshot

> *Coming soon*

---

## 📥 Download APK

Kunjungi halaman [**Releases**](../../releases/latest) untuk download APK versi terbaru.

| Versi | Tanggal | Changelog |
|-------|---------|-----------|
| [v1.0.0](../../releases/tag/dani) | 2026-03-01 | Initial release |

---

## 🛠️ Tech Stack

| Komponen | Detail |
|----------|--------|
| Language | Kotlin |
| Min SDK | API 24 (Android 7.0 Nougat) |
| Target SDK | API 34 (Android 14) |
| Browser Engine | Chrome Custom Tabs (`androidx.browser`) |
| Architecture | Single Activity |

---

## 🚀 Build dari Source

### Prerequisites
- Android Studio Hedgehog atau lebih baru
- JDK 17
- Android SDK API 34

### Langkah Build

```bash
# Clone repo
git clone https://github.com/Renaldi710/Model-Aplikasi-Absen.git
cd Model-Aplikasi-Absen

# Build debug APK
./gradlew assembleDebug

# Build release APK (butuh keystore)
./gradlew assembleRelease
```

APK output ada di: `app/build/outputs/apk/`

### Build Release dengan Keystore

```bash
# Buat file keystore.properties di root project
KEY_STORE_FILE=bebas.jks
KEY_STORE_PASSWORD=your_password
KEY_ALIAS=ikuti key store
KEY_PASSWORD=your_key_password
```

> ⚠️ Jangan commit file `keystore.properties` dan `.jks` ke repository!

---

## 📋 Cara Install APK

1. Download APK dari halaman [Releases](../../releases/dani)
2. Aktifkan **Install dari sumber tidak dikenal** di pengaturan HP
3. Buka file APK dan tap **Install**
4. Jika muncul peringatan Play Protect → tap **Tetap Install**
5. Buka aplikasi dan nikmati 🎉

---

## 🔧 Konfigurasi

Target URL ada di `MainActivity.kt`:

```kotlin
private val targetUrl = "https://dani.perhutani.co.id/login"
```

Ganti URL tersebut jika endpoint sistem berubah.

---

## 🤝 Kontribusi

Pull request welcome! Untuk perubahan besar, buka issue dulu untuk diskusi.

1. Fork repo ini
2. Buat branch baru (`git checkout -b feature/NamaFitur`)
3. Commit perubahan (`git commit -m 'Add: NamaFitur'`)
4. Push ke branch (`git push origin feature/NamaFitur`)
5. Buka Pull Request

---

## 📄 Lisensi

Distributed under the MIT License. Lihat [`LICENSE`](LICENSE) untuk informasi lebih lanjut.

---

## 👤 Author

**Renaldi**

- GitHub: [@USERNAME](https://github.com/Renaldi710)

---

<p align="center">Made with ❤️ for Perhutani</p>
