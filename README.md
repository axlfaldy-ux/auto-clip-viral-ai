# Auto Clip Viral AI

Aplikasi Android native Kotlin + Jetpack Compose, dibangun lewat GitHub Actions.

## Fitur yang tersedia

- Home dan impor video lokal melalui Android Storage Access Framework.
- Clip Results dengan pemotongan video lokal, crop tengah 9:16, dan simpan ke galeri melalui Media3 Transformer.
- Pemilihan klip otomatis melalui backend HTTPS yang dikonfigurasi pengguna; tidak ada analisis tiruan di aplikasi.
- Subtitle Studio dengan delapan font lokal, kontrol ukuran/warna/outline/shadow/posisi, serta tujuh mode gerak kamera. Preview dan ekspor menggunakan font asset yang sama.
- Metadata Studio dengan 5 judul, 3 hook, caption berbasis kutipan transkrip, hashtag per platform, 4 CTA, edit, regenerate, salin, dan penyimpanan lokal per ID.
- Pengaturan potongan 30–60 detik jika durasi sumber mencukupi, target 1080×1920, serta penyimpanan hasil ke galeri.
- Metadata lokal berdasarkan transkrip tetap tersedia.

Pemotongan/crop/ekspor/font overlay dan gerak kamera generik berjalan lokal melalui Media3. Subtitle kata, emphasis zoom, dan auto-reframe wajah memakai timestamp/keyframe dari backend HTTPS. Seleksi viral dan transkripsi otomatis memerlukan backend; aplikasi mengunggah video hanya ketika pengguna meminta analisis. Backend harus mengikuti kontrak pada `docs/ARCHITECTURE.md`; kredensial dan model AI dikelola di server, bukan ditanam dalam APK. Repository tidak menyertakan FFmpeg, maka renderer memakai Media3 yang sudah ada. Generator metadata tetap lokal, extractive, dan bukan data tren real-time.

## Build APK

Workflow `.github/workflows/android-apk.yml` membangun APK debug pada push ke `main`/`master`, pull request, atau pemicu manual. Ambil artifact `auto-clip-viral-ai-debug-apk` dari halaman Actions.

Toolchain: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Android API 35, minimum API 26.

Lihat [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) untuk pemetaan seluruh spesifikasi dan status implementasi.
