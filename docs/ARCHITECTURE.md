# Arsitektur Auto Clip Viral AI

## Implementasi saat ini

Impor menggunakan Storage Access Framework. Pemotongan, crop 9:16, subtitle berbasis timestamp kata, gerak kamera, encoding, dan penyimpanan galeri dijalankan pada perangkat melalui Media3 Transformer. Pemilihan otomatis, transkripsi, penekanan kata, dan keyframe wajah memerlukan backend HTTPS; aplikasi tidak mengklaim mengerjakan AI secara lokal. Pelacakan wajah mengikuti keyframe backend, sedangkan identifikasi active speaker dan sinkronisasi gerak bibir harus disediakan oleh backend.

## Susunan modul

```text
app/src/main/java/com/autoclip/viralai/
  data/                       # Media3 exporter dan klien backend analisis
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
| Automatic viral clip selection | `VideoAnalysisClient` mengirim video ke `/v1/analyze`, validasi kandidat 30–60 detik, ekspor kandidat | Backend dan model analisis konten harus disediakan dan dikonfigurasi |
| Active speaker face tracking | Analisis meminta `face_tracking` saat mode aktif; exporter mengikuti keyframe `face_crops` backend dan menginterpolasi pusat wajah/crop | Backend/model perlu mendeteksi wajah dan mencocokkan pembicara aktif dengan gerak bibir/audio; tanpa keyframe UI menonaktifkan ekspor tracking |
| 8 subtitle templates | Delapan font asset lokal dan `WordSubtitleOverlay` memakai font, ukuran, warna, outline, shadow, posisi, dan animasi yang sama dengan preview | Perlu backend mengembalikan timestamp per kata; ekspor manual tanpa timestamp memang tidak memiliki subtitle |
| Talking head movement | Dynamic punch, smooth zoom, jump-cut framing, subtle movement, emphasis zoom, face tracking/reframe memakai transformasi frame bertimestamp | Penanda `emphasis` dan `face_crops` berasal dari backend; jump-cut saat ini perubahan framing, bukan penghilangan jeda audio |
| Title, hook, caption, hashtag | `LocalMetadataGenerator`, editor, preset platform, simpan/copy | Model generatif opsional bila kelak sesuai perangkat; saat ini aturan lokal, bukan LLM |
| Video export 9:16 | `VideoMediaProcessor` memakai Media3 Transformer, trim, crop, overlay, dan MediaStore | Batas encoder/format input perangkat dapat menyebabkan ekspor gagal dengan pesan error |
| Durasi 30–60 detik | Slider manual dan validasi kandidat respons backend; klip sumber di bawah 30 detik tetap dapat dipotong manual | Durasi klip otomatis bergantung pada respons backend |

### Kontrak backend analisis

Setelan menyimpan URL dasar HTTPS. Tombol analisis mengirim `POST {baseUrl}/v1/analyze` sebagai `multipart/form-data` dengan field `video` (file sumber) dan `options` (JSON: `word_timestamps`, `word_emphasis`, `face_tracking`). Backend tidak boleh mengandalkan API key yang ditanam dalam aplikasi; autentikasi dan kredensial model harus dikelola server-side.

Contoh respons JSON:

```json
{
  "clips": [
    {
      "start_ms": 12000,
      "end_ms": 47000,
      "score": 0.87,
      "transcript": "contoh transkrip",
      "words": [
        { "text": "contoh", "start_ms": 12000, "end_ms": 12400, "emphasis": false },
        { "text": "transkrip", "start_ms": 12420, "end_ms": 13100, "emphasis": true }
      ],
      "face_crops": [
        { "start_ms": 12000, "end_ms": 15000, "center_x": 0.52, "center_y": 0.40, "scale": 1.08, "confidence": 0.92 }
      ]
    }
  ]
}
```

Timestamp kata dan crop wajib menggunakan waktu absolut terhadap video sumber. `words` opsional; tanpa field itu hasil diekspor tanpa subtitle dan UI menyatakannya. `face_crops` opsional untuk mode wajah; mode active-speaker/auto-reframe dinonaktifkan untuk ekspor kandidat yang tidak memiliki keyframe wajah valid. `center_x` dan `center_y` dinormalisasi pada 0–1, `scale` 1–2, dan confidence 0–1. Error HTTP, respons invalid, dan URL non-HTTPS ditampilkan ke pengguna. Batas unggah dan autentikasi perlu ditambahkan sesuai deployment backend.

## Dependensi saat ini dan rencana

Dependensi mencakup AndroidX Compose dan Media3 Transformer/Effect. Tidak ada FFmpeg atau API key/backend bawaan. File font disimpan di `app/src/main/assets/fonts`; preview dan video memakai font asset yang sama. Media3 melakukan transformasi video dan overlay font secara lokal, tanpa menambah dependency berbayar.

Analisis otomatis memerlukan deployment backend yang menerapkan kontrak di atas. Backend dapat menggunakan model open-source yang di-host sendiri untuk menghindari biaya per panggilan; biaya GPU/hosting tetap bergantung beban. Tidak ada backend gratis yang dapat dijamin dari repository ini. Face framing membutuhkan `face_crops`; active-speaker matching dan sinkronisasi bibir tetap harus disediakan backend.
