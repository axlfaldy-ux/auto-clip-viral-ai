package com.autoclip.viralai.ui.pages

import com.autoclip.viralai.domain.subtitles.SubtitlePresets
import com.autoclip.viralai.domain.video.ClipSpecification

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                            Text("Video diimpor · analisis belum dijalankan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun ClipResultsPage(importedName: String?, onImport: () -> Unit, modifier: Modifier = Modifier) {
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
        item {
            if (importedName == null) {
                EmptyCard("Belum ada sumber video", "Impor video lokal untuk menyiapkan proyek.")
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Impor video")
                }
            } else {
                EmptyCard("Video dipilih", "$importedName · hasil klip belum tersedia karena mesin pemrosesan belum dibuat.")
            }
        }
        item { SectionHeading("Spesifikasi yang disiapkan") }
        item { PipelineStatus("Automatic viral clip selection", "Rentang kandidat ${ClipSpecification.MIN_DURATION_MS / 1000}–${ClipSpecification.MAX_DURATION_MS / 1000} detik; belum berjalan.", false) }
        item { PipelineStatus("Active speaker face tracking", "Sinkronisasi aktivitas audio dan gerak bibir; belum berjalan.", false) }
        item { PipelineStatus("Subtitle engine", "Delapan preset dapat dipilih. Animasi dan render kata belum berjalan.", true) }
        item { PipelineStatus("Viral metadata", "Generator aturan lokal tersedia dengan transkrip yang dimasukkan pengguna.", true) }
        item { PipelineStatus("Video export", "${ClipSpecification.OUTPUT_WIDTH}×${ClipSpecification.OUTPUT_HEIGHT} (9:16); ekspor belum berjalan.", false) }
        item {
            Text(
                "Satu transkrip bertimestamp akan dipakai bersama untuk pemilihan klip, speaker tracking, subtitle, dan metadata saat mesin analisis ditambahkan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
fun SubtitleStudioPage(selectedTemplate: String, onSelectTemplate: (String) -> Unit, modifier: Modifier = Modifier) {
    val selected = SubtitlePresets.all.firstOrNull { it.displayName == selectedTemplate }
        ?: SubtitlePresets.all.first { it.id == "karaoke" }
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
                        Text(selected.previewText, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Color(selected.accentArgb), fontWeight = FontWeight.Bold)
                    }
                    Text("Animasi: ${selected.animation.label}. Pratinjau statis; renderer belum tersedia.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun SettingsPage(modifier: Modifier = Modifier) {
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
            EmptyCard("Lokal dan tanpa API berbayar", "Tahap UI ini belum menjalankan analisis, pelacakan wajah, atau ekspor video.")
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
