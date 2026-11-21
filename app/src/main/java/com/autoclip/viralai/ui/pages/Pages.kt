package com.autoclip.viralai.ui.pages

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.autoclip.viralai.domain.subtitles.SubtitlePresets
import com.autoclip.viralai.domain.video.ClipSpecification
import com.autoclip.viralai.data.VideoMediaProcessor
import com.autoclip.viralai.data.VideoAnalysisClient
import com.autoclip.viralai.data.RecommendedSegment
import com.autoclip.viralai.domain.metadata.TimedWord
import com.autoclip.viralai.domain.subtitles.SubtitleStyle
import com.autoclip.viralai.domain.subtitles.TalkingHeadAnimation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

@Composable
fun HomePage(importedName: String?, onImport: () -> Unit, onResults: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("AUTO CLIP  /  AI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Text("Ubah video panjang\njadi momen pilihan.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
            Spacer(Modifier.height(8.dp))
            Text("Pilih video untuk memulai alur klip vertikal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("MULAI PROYEK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Impor video dari perangkat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Pilih video lokal. File tetap berada di perangkat Anda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onImport, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Pilih video") }
                }
            }
        }
        item {
            SectionHeading("Format klip")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoPill("Rasio", "9:16", Modifier.weight(1f))
                InfoPill("Durasi target", "${ClipSpecification.MIN_DURATION_MS / 1000}–${ClipSpecification.MAX_DURATION_MS / 1000} dtk", Modifier.weight(1f))
            }
        }
        item {
            SectionHeading("Proyek saat ini")
            Spacer(Modifier.height(10.dp))
            if (importedName == null) {
                EmptyCard("Belum ada video", "Video yang Anda pilih akan muncul di sini.")
            } else {
                Card(
                    onClick = onResults,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AccentDot()
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(importedName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text("Video diimpor · pilih klip di Clip Results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ImportVideoPage(
    importedName: String?, importedUri: String?, onBack: () -> Unit,
    onPickVideo: () -> Unit, onOpenResults: () -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        TextButton(onClick = onBack) { Text("‹  Kembali") }
        Spacer(Modifier.height(12.dp))
        Text("Impor video", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Pilih video dari penyimpanan perangkat. Aplikasi hanya meminta akses ke file yang Anda pilih.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Text("▶", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(if (importedName == null) "Pilih video untuk proyek" else "Video siap ditinjau", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(importedName ?: "Format sumber mengikuti dukungan Android pada perangkat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onPickVideo, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text(if (importedName == null) "Telusuri video" else "Pilih video lain")
                }
            }
        }
        if (importedUri != null) {
            Spacer(Modifier.height(16.dp))
            InfoPill("Akses lokal", "Berkas dipilih melalui Android", Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onOpenResults, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Lihat status klip") }
        }
    }
}

@Composable
fun ClipResultsPage(
    importedName: String?,
    importedUri: String?,
    apiEndpoint: String,
    subtitlePresetId: String,
    subtitleStyle: SubtitleStyle,
    cameraAnimation: TalkingHeadAnimation,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val processor = remember(context) { VideoMediaProcessor(context) }
    val scope = rememberCoroutineScope()
    var sourceDuration by rememberSaveable(importedUri) { mutableStateOf(0L) }
    var startMs by rememberSaveable(importedUri) { mutableStateOf(0f) }
    var selectedDuration by rememberSaveable(importedUri) { mutableStateOf(30_000f) }
    var processing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var recommendations by remember(importedUri) { mutableStateOf<List<RecommendedSegment>>(emptyList()) }
    var pendingExport by remember { mutableStateOf<Triple<Long, Long, List<TimedWord>>?>(null) }
    val exportClip: (Long, Long, List<TimedWord>) -> Unit = { clipStart, clipLength, words ->
        processing = true
        resultMessage = "Mengekspor video…"
        scope.launch {
            runCatching {
                processor.exportPortraitClip(
                    Uri.parse(requireNotNull(importedUri)), clipStart, clipLength, words, subtitlePresetId,
                    subtitleStyle, cameraAnimation,
                    recommendations.firstOrNull { it.startMs == clipStart && it.endMs - it.startMs == clipLength }?.faceCrops.orEmpty(),
                )
            }
                .onSuccess { resultMessage = "Video disimpan ke galeri: $it" }
                .onFailure { if (it is CancellationException) throw it else resultMessage = "Ekspor gagal: ${it.message ?: "kesalahan tidak diketahui"}" }
            processing = false
        }
    }
    val writePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val request = pendingExport
        pendingExport = null
        if (granted && request != null) exportClip(request.first, request.second, request.third)
        else if (!granted) resultMessage = "Izin penyimpanan diperlukan pada Android 9 dan lebih lama untuk menulis ke galeri."
    }
    val requestExport: (Long, Long, List<TimedWord>) -> Unit = { clipStart, clipLength, words ->
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingExport = Triple(clipStart, clipLength, words)
            writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else exportClip(clipStart, clipLength, words)
    }
    LaunchedEffect(importedUri) {
        sourceDuration = 0L
        if (importedUri != null) {
            runCatching { processor.durationMs(Uri.parse(importedUri)) }
                .onSuccess { duration ->
                    sourceDuration = duration
                    selectedDuration = if (duration >= 1_000L) selectedDuration.coerceIn(1_000f, duration.toFloat()) else duration.toFloat()
                    startMs = startMs.coerceIn(0f, (duration - selectedDuration.toLong()).coerceAtLeast(0L).toFloat())
                }
                .onFailure { if (it is CancellationException) throw it else resultMessage = it.message ?: "Video tidak dapat dibaca." }
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Clip Results", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Rangkaian pemrosesan video", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (importedUri != null && sourceDuration >= 1_000L) {
            item {
                SectionHeading("Analisis konten otomatis")
                Text(
                    if (apiEndpoint.isBlank()) "Isi URL backend HTTPS di Setelan untuk menjalankan seleksi klip. Video akan diunggah ke backend tersebut."
                    else "Video akan dianalisis oleh backend yang dikonfigurasi. Hanya klip 30–60 detik dari respons valid yang ditawarkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        analyzing = true
                        resultMessage = "Mengunggah video dan meminta analisis…"
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    VideoAnalysisClient(context.contentResolver).analyze(
                                        apiEndpoint,
                                        Uri.parse(importedUri),
                                        requestFaceTracking = cameraAnimation in setOf(
                                            TalkingHeadAnimation.ACTIVE_SPEAKER,
                                            TalkingHeadAnimation.AUTO_REFRAME,
                                        ),
                                        requestWordEmphasis = cameraAnimation == TalkingHeadAnimation.EMPHASIS_ZOOM,
                                    )
                                }
                            }.onSuccess { clips ->
                                val validClips = clips.filter { it.endMs <= sourceDuration && it.startMs >= 0L }
                                recommendations = validClips
                                resultMessage = if (validClips.isEmpty()) "Backend tidak mengembalikan kandidat valid berdurasi 30–60 detik dalam rentang video." else "Backend mengembalikan ${validClips.size} kandidat valid."
                            }.onFailure { if (it is CancellationException) throw it else resultMessage = "Analisis gagal: ${it.message ?: "kesalahan tidak diketahui"}" }
                            analyzing = false
                        }
                    },
                    enabled = !analyzing && apiEndpoint.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(if (analyzing) "Menganalisis…" else "Analisis dan pilih klip") }
                recommendations.forEachIndexed { index, candidate ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Kandidat ${index + 1} · ${candidate.startMs / 1000}–${candidate.endMs / 1000} dtk · skor ${"%.2f".format(candidate.score)}", fontWeight = FontWeight.SemiBold)
                            if (candidate.transcript.isNotBlank()) Text(candidate.transcript, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (candidate.words.isEmpty()) Text("Backend tidak mengirim timestamp kata; klip akan diekspor tanpa subtitle.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            if (cameraAnimation in setOf(TalkingHeadAnimation.ACTIVE_SPEAKER, TalkingHeadAnimation.AUTO_REFRAME) && candidate.faceCrops.isEmpty()) {
                                Text("Backend tidak mengirim face_crops; pilih animasi lain atau ekspor tidak dapat menjalankan tracking.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            OutlinedButton(
                                onClick = {
                                    requestExport(candidate.startMs, candidate.endMs - candidate.startMs, candidate.words)
                                },
                                enabled = !processing && (cameraAnimation !in setOf(TalkingHeadAnimation.ACTIVE_SPEAKER, TalkingHeadAnimation.AUTO_REFRAME) || candidate.faceCrops.isNotEmpty()),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (candidate.words.isEmpty()) "Ekspor tanpa subtitle" else "Ekspor dengan subtitle ke galeri") }
                        }
                    }
                }
                resultMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
            item {
                SectionHeading("Potong dan ekspor")
                Text("Durasi sumber: ${sourceDuration / 1000} detik", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val minClip = if (sourceDuration >= ClipSpecification.MIN_DURATION_MS) ClipSpecification.MIN_DURATION_MS else 1_000L
                val maxClip = minOf(ClipSpecification.MAX_DURATION_MS, sourceDuration)
                val safeDuration = selectedDuration.coerceIn(minClip.toFloat(), maxClip.toFloat())
                Text("Durasi klip: ${safeDuration.toLong() / 1000} detik", fontWeight = FontWeight.SemiBold)
                if (maxClip > minClip) {
                    Slider(
                        value = safeDuration,
                        onValueChange = { selectedDuration = it; startMs = startMs.coerceAtMost((sourceDuration - it.toLong()).coerceAtLeast(0).toFloat()) },
                        valueRange = minClip.toFloat()..maxClip.toFloat(),
                        enabled = !processing,
                    )
                }
                Text("Mulai: ${startMs.toLong() / 1000} detik", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = startMs.coerceIn(0f, (sourceDuration - safeDuration.toLong()).coerceAtLeast(0).toFloat()),
                    onValueChange = { startMs = it },
                    valueRange = 0f..(sourceDuration - safeDuration.toLong()).coerceAtLeast(0).toFloat().coerceAtLeast(1f),
                    enabled = !processing && sourceDuration > safeDuration,
                )
                Text(
                    if (sourceDuration < ClipSpecification.MIN_DURATION_MS) "Sumber lebih pendek dari 30 detik; pemotongan manual tetap diizinkan sampai durasi sumber. Video di-crop tengah ke 1080×1920. Potongan manual tanpa subtitle."
                    else "Video dipotong pada perangkat dan di-crop tengah ke 1080×1920. Potongan manual tanpa subtitle; hasil analisis dengan timestamp kata dapat merender preset subtitle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (cameraAnimation in setOf(TalkingHeadAnimation.ACTIVE_SPEAKER, TalkingHeadAnimation.AUTO_REFRAME)) {
                    Text("Analisis kandidat terlebih dahulu agar backend menyediakan keyframe wajah untuk mode ini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        requestExport(startMs.toLong(), safeDuration.toLong(), emptyList())
                    },
                    enabled = !processing && safeDuration >= 1_000f && cameraAnimation !in setOf(TalkingHeadAnimation.ACTIVE_SPEAKER, TalkingHeadAnimation.AUTO_REFRAME),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(if (processing) "Memproses…" else "Potong dan simpan ke galeri") }
                resultMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
        item {
            if (importedName == null) {
                EmptyCard("Belum ada sumber video", "Impor video lokal untuk menyiapkan proyek.")
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Impor video")
                }
            } else {
                EmptyCard("Video dipilih", "$importedName · gunakan analisis backend atau potongan manual di atas.")
            }
        }
        item { SectionHeading("Spesifikasi yang disiapkan") }
        item { PipelineStatus("Automatic viral clip selection", "Memerlukan backend analisis konten; ekspor potongan manual tersedia di atas.", false) }
        item { PipelineStatus("Active speaker face tracking", "Berjalan dengan keyframe face_crops yang dikirim backend saat analisis.", apiEndpoint.isNotBlank()) }
        item { PipelineStatus("Subtitle engine", "Delapan preset dirender ke video jika backend mengirim timestamp per kata.", true) }
        item { PipelineStatus("Viral metadata", "Generator aturan lokal tersedia dengan transkrip yang dimasukkan pengguna.", true) }
        item { PipelineStatus("Video export", "Potong manual ke ${ClipSpecification.OUTPUT_WIDTH}×${ClipSpecification.OUTPUT_HEIGHT} (9:16) dan simpan ke galeri.", true) }
    }
}

@Composable
private fun PipelineStatus(title: String, detail: String, available: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp).clip(CircleShape).background(
                    if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SubtitleStudioPage(
    selectedTemplate: String,
    selectedAnimation: TalkingHeadAnimation,
    style: SubtitleStyle,
    onStyleChange: (SubtitleStyle) -> Unit,
    onAnimationChange: (TalkingHeadAnimation) -> Unit,
    onSelectTemplate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = SubtitlePresets.all.firstOrNull { it.displayName == selectedTemplate }
        ?: SubtitlePresets.all.first { it.id == "classic" }
    var textColorHex by rememberSaveable(style.textArgb) { mutableStateOf(style.textArgb.toHexColor()) }
    var outlineColorHex by rememberSaveable(style.outlineArgb) { mutableStateOf(style.outlineArgb.toHexColor()) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Subtitle Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Pilih satu dari delapan preset subtitle.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().height(172.dp).padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("PRATINJAU  ·  ${selected.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(10.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        SubtitleFontPreview(selected.previewText, selected.fontAsset, style, selected.id, Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                    }
                    Text("${selected.animation.label} · ${selected.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionHeading("Ukuran, warna, outline, shadow, dan posisi")
            Text("Ukuran ${(style.sizeFraction * 100).toInt()}% dari tinggi video", style = MaterialTheme.typography.bodySmall)
            Slider(value = style.sizeFraction, onValueChange = { onStyleChange(style.copy(sizeFraction = it)) }, valueRange = 0.025f..0.075f)
            OutlinedTextField(
                value = textColorHex,
                onValueChange = { value -> textColorHex = value; value.toArgbColor()?.let { onStyleChange(style.copy(textArgb = it)) } },
                label = { Text("Warna teks (#RRGGBB)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = outlineColorHex,
                onValueChange = { value -> outlineColorHex = value; value.toArgbColor()?.let { onStyleChange(style.copy(outlineArgb = it)) } },
                label = { Text("Warna outline (#RRGGBB)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("Ketebalan outline ${(style.outlineFraction * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = style.outlineFraction, onValueChange = { onStyleChange(style.copy(outlineFraction = it)) }, valueRange = 0f..0.008f)
            Text("Shadow ${(style.shadowFraction * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = style.shadowFraction, onValueChange = { onStyleChange(style.copy(shadowFraction = it)) }, valueRange = 0f..0.015f)
            Text("Posisi vertikal ${(style.positionFraction * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = style.positionFraction, onValueChange = { onStyleChange(style.copy(positionFraction = it)) }, valueRange = 0.62f..0.94f)
        }
        item {
            SectionHeading("Talking head movement")
            Text("Tracking wajah memerlukan face_crops dari respons analisis backend. Gerak zoom lain dirender lokal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Jump Cut saat ini mengubah framing secara berkala; pemotongan jeda suara dan transisi antar klip belum tersedia.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(TalkingHeadAnimation.entries) { animation ->
                    FilterChip(
                        selected = selectedAnimation == animation,
                        onClick = { onAnimationChange(animation) },
                        label = { Text(animation.label) },
                    )
                }
            }
        }
        item { SectionHeading("Delapan template · pilih satu") }
        items(SubtitlePresets.all) { preset ->
            val chosen = preset.id == selected.id
            val borderColor by animateColorAsState(
                targetValue = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                label = "template-border",
            )
            Card(
                onClick = { onSelectTemplate(preset.displayName) },
                colors = CardDefaults.cardColors(containerColor = if (chosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                border = BorderStroke(if (chosen) 1.5.dp else 1.dp, borderColor),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(preset.accentArgb)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(preset.displayName, fontWeight = FontWeight.SemiBold)
                        Text(preset.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Animasi: ${preset.animation.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (chosen) "Dipilih" else "Pilih", color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SubtitleFontPreview(text: String, fontAsset: String, style: SubtitleStyle, presetId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val typeface = remember(fontAsset) { Typeface.createFromAsset(context.assets, fontAsset) }
    val preset = remember(presetId) { SubtitlePresets.all.firstOrNull { it.id == presetId } }
    Canvas(modifier.fillMaxWidth().height(54.dp)) {
        val canvas = drawContext.canvas.nativeCanvas
        val line = text.replace('\n', ' ')
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            this.typeface = Typeface.create(typeface, if (presetId == "bold_viral" || presetId == "impact") Typeface.BOLD else Typeface.NORMAL)
            val presetSize = if (presetId == "bold_viral" || presetId == "impact") 1.12f else 1f
            val animationSize = if (preset?.animation == com.autoclip.viralai.domain.subtitles.SubtitleAnimation.SCALE_POP ||
                preset?.animation == com.autoclip.viralai.domain.subtitles.SubtitleAnimation.WORD_POP
            ) 1.08f else 1f
            textSize = size.height * 0.52f * (style.sizeFraction / 0.046f) * presetSize * animationSize
            color = style.textArgb.toInt()
            setShadowLayer(
                textSize * style.shadowFraction / style.sizeFraction,
                0f,
                textSize * style.shadowFraction / style.sizeFraction * 0.55f,
                style.outlineArgb.toInt(),
            )
        }
        val centerX = size.width / 2f
        val baseline = size.height * style.positionFraction
        val shown = if (presetId == "bold_viral" || presetId == "impact") line.uppercase() else line
        if (style.outlineFraction > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeWidth = textSize * style.outlineFraction / style.sizeFraction
            paint.color = style.outlineArgb.toInt()
            canvas.drawText(shown, centerX, baseline, paint)
            paint.style = Paint.Style.FILL
            paint.color = style.textArgb.toInt()
        }
        canvas.drawText(shown, centerX, baseline, paint)
    }
}

private fun Long.toHexColor(): String = "#%06X".format(this and 0xFFFFFFL)

private fun String.toArgbColor(): Long? {
    val digits = trim().removePrefix("#")
    if (digits.length != 6) return null
    return runCatching { 0xFF000000L or digits.toLong(16) }.getOrNull()
}

@Composable
fun SettingsPage(apiEndpoint: String, onApiEndpointChange: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Setelan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Format target proyek Auto Clip Viral AI.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionHeading("Video")
            Spacer(Modifier.height(8.dp))
            InfoPill("Rasio ekspor target", "9:16 · ${ClipSpecification.OUTPUT_WIDTH}×${ClipSpecification.OUTPUT_HEIGHT}", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            InfoPill("Durasi klip target", "${ClipSpecification.MIN_DURATION_MS / 1000}–${ClipSpecification.MAX_DURATION_MS / 1000} detik", Modifier.fillMaxWidth())
        }
        item {
            SectionHeading("Pemrosesan")
            Spacer(Modifier.height(8.dp))
            EmptyCard("Pemrosesan video lokal", "Potong, crop 9:16, dan simpan hasil ke galeri tanpa mengunggah video.")
            Spacer(Modifier.height(12.dp))
            SectionHeading("Backend analisis AI")
            OutlinedTextField(
                value = apiEndpoint,
                onValueChange = onApiEndpointChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL dasar backend HTTPS") },
                placeholder = { Text("https://api.example.com") },
                supportingText = { Text("Video dikirim ke endpoint /v1/analyze. Jangan masukkan API key di aplikasi; lindungi kredensial di backend.") },
                singleLine = true,
            )
            Text("Backend harus mengembalikan kandidat 30–60 detik dengan start_ms, end_ms, score, dan transcript. Lihat kontrak API di docs/ARCHITECTURE.md.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(Modifier.height(8.dp))
            Text("Auto Clip Viral AI  ·  0.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Build APK melalui GitHub Actions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyCard(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoPill(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun AccentDot() {
    Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}
