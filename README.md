# Auto Clip Viral AI

Aplikasi Android native Kotlin + Jetpack Compose, dibangun lewat GitHub Actions.

## Tahap 1 tersedia

- Home dan impor video lokal melalui Android Storage Access Framework.
- Clip Results yang menunjukkan status nyata setiap komponen; belum menampilkan klip palsu.
- Subtitle Studio dengan delapan preset yang dapat dipilih manual dan disimpan lokal. Pratinjau masih statis; renderer/animasi ekspor belum dibuat.
- Metadata Studio dengan 5 judul, 3 hook, caption berbasis kutipan transkrip, hashtag per platform, 4 CTA, edit, regenerate, salin, dan penyimpanan lokal per ID.
- Batas domain target klip 30–60 detik serta ekspor 9:16 1080×1920.
- `LocalClipWorkflow` mengatur urutan transkripsi satu kali, seleksi, speaker tracking, ekspor, lalu metadata per klip; engine konkrit belum tersedia.

Generator metadata saat ini adalah ekstraksi kata kunci + aturan lokal dari transkrip yang diberikan pengguna. Ia bukan model generatif dan tidak memiliki data tren real-time. Pemroses video, ASR, seleksi otomatis, speaker tracking, subtitle rendering, serta ekspor belum berjalan.

## Build APK

Workflow `.github/workflows/android-apk.yml` membangun APK debug pada push ke `main`/`master`, pull request, atau pemicu manual. Ambil artifact `auto-clip-viral-ai-debug-apk` dari halaman Actions.

Toolchain: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Android API 35, minimum API 26.

Lihat [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) untuk pemetaan seluruh spesifikasi dan status implementasi.
