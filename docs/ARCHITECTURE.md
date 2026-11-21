# Arsitektur Auto Clip Viral AI

## Batas Tahap 1

Tahap ini membangun UI Android, alur impor video lokal, katalog preset subtitle, generator metadata lokal berbasis transkrip, spesifikasi produk, dan kontrak modul pemrosesan. Belum ada model ASR, AI pemilih momen, deteksi speaker, renderer subtitle, atau exporter yang dijalankan. UI menunjukkan status tersebut dan tidak menawarkan tombol untuk proses yang belum ada.

## Susunan modul

```text
app/src/main/java/com/autoclip/viralai/
  data/                       # implementasi repository dan akses berkas (bertahap)
  domain/
    metadata/                 # transkrip bersama, generator, penyimpanan per clipId
    processing/               # kontrak seleksi klip, speaker tracker, video exporter
    subtitles/                # katalog 8 preset dan tipe animasi
    video/                    # batas 30–60 detik, profil output 9:16, kandidat klip
  ui/
    pages/                    # Home, Import, Clip Results, Subtitle Studio, Metadata, Settings
    theme/
```

UI hanya memanggil fungsi yang tersedia. Lapisan domain mendefinisikan kontrak sehingga implementasi media/AI dapat ditambahkan tanpa menaruh algoritme di composable.

## Pemetaan spesifikasi

| Fitur | Fondasi Tahap 1 | Implementasi yang masih diperlukan |
|---|---|---|
| Automatic viral clip selection | `ClipSelectionEngine`, `ClipCandidate`, batas durasi 30–60 detik | ASR/VAD, scoring audio/teks/visual, pemilihan rentang dari video panjang |
| Active speaker face tracking | `ActiveSpeakerTracker`, `TimedFaceCrop`, kata bertimestamp pada `ClipTranscript` | Face landmarks, sinkronisasi audio-gerak bibir, smoothing crop |
| 8 subtitle templates | Katalog `SubtitlePresets`; pilihan manual disimpan lokal | Renderer animasi, kata bertimestamp, overlay saat ekspor |
| Title, hook, caption, hashtag | `LocalMetadataGenerator`, editor, preset platform, simpan/copy | Model generatif opsional bila kelak sesuai perangkat; saat ini aturan lokal, bukan LLM |
| Video export 9:16 | `ExportProfile` dan `VideoExportEngine`; target 1080×1920 | Integrasi Media3 Transformer/MediaCodec dan validasi file hasil |
| Durasi 30–60 detik | `ClipSpecification.acceptsDuration` dan validasi kandidat | Penegakan pada mesin seleksi dan sebelum ekspor |

`LocalClipWorkflow` mengorkestrasi kontrak yang akan diimplementasikan: transkripsi satu kali per video sumber, seleksi dan validasi durasi kandidat, speaker tracking, ekspor potret, lalu pembuatan/penyimpanan metadata untuk setiap klip yang berhasil diekspor. `ClipTranscript` dan kata bertimestamp yang sama diberikan ke seleksi, tracking, subtitle/export, dan metadata. Belum ada engine konkret atau UI yang memanggil workflow ini, jadi pemrosesan otomatis belum aktif.

## Dependensi saat ini dan rencana

Dependensi build saat ini dibatasi pada AndroidX Activity, Compose UI/Foundation/Material 3, dan Compose tooling untuk debug. Tidak ada layanan berbayar atau library media/AI yang dipasang pada tahap UI.

Untuk implementasi media berikutnya, jalur yang direncanakan adalah Media3 Transformer/Effect dan `MediaCodec` perangkat. Untuk transkrip offline, evaluasi runtime Android seperti sherpa-onnx dengan model kecil yang diunduh sesuai kebutuhan. Deteksi landmark wajah dapat ditambahkan sebagai modul opsional. Model tidak perlu dibundel di APK dasar.
