package com.autoclip.viralai.ui.pages.metadata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoclip.viralai.domain.metadata.ClipMetadata
import com.autoclip.viralai.domain.metadata.ClipTranscript
import com.autoclip.viralai.domain.metadata.LocalMetadataGenerator
import com.autoclip.viralai.domain.metadata.MetadataPlatform
import com.autoclip.viralai.domain.metadata.MetadataRepository

@Composable
fun MetadataStudioPage(
    clipId: String,
    sourceName: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val repository = remember { MetadataRepository(context) }
    var transcript by rememberSaveable(clipId) { mutableStateOf(repository.get(clipId)?.sourceTranscript ?: repository.getDraftTranscript(clipId)) }
    var platformName by rememberSaveable(clipId) {
        mutableStateOf((repository.get(clipId)?.platform ?: repository.getDraftPlatform(clipId) ?: MetadataPlatform.TIKTOK).name)
    }
    var metadata by remember(clipId) { mutableStateOf(repository.get(clipId)) }
    var selectedTitle by rememberSaveable(clipId) { mutableStateOf(0) }
    val platform = MetadataPlatform.valueOf(platformName)
    val hasTranscriptChanged = metadata != null && transcript.trim().replace(Regex("\\s+"), " ") != metadata?.sourceTranscript

    fun save(updated: ClipMetadata) {
        metadata = updated
        repository.save(updated)
    }

    fun regenerate() {
        val fresh = LocalMetadataGenerator.generate(ClipTranscript(clipId, transcript), platform)
        if (fresh != null) {
            selectedTitle = 0
            save(fresh)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Viral Metadata", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Buat dan edit metadata berdasarkan transkrip yang Anda berikan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("SUMBER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(sourceName ?: "Draft transkrip lokal", fontWeight = FontWeight.SemiBold)
                    Text("Metadata disimpan lokal dengan ID proyek/klip: ${clipId.take(18)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text("Platform tujuan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(MetadataPlatform.entries) { item ->
                    FilterChip(
                        selected = platform == item,
                        onClick = {
                            platformName = item.name
                            repository.saveDraft(clipId, transcript, item.name)
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = transcript,
                onValueChange = {
                    transcript = it
                    repository.saveDraft(clipId, it, platformName)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Transkrip video/klip (bahasa Indonesia)") },
                supportingText = { Text("Tempel transkrip yang benar. Generator tidak membuat atau menebak isi video.") },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(16.dp),
            )
        }
        item {
            Button(onClick = { regenerate() }, enabled = transcript.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(if (metadata == null) "Buat metadata lokal" else "Regenerate metadata")
            }
            Spacer(Modifier.height(5.dp))
            Text("Ekstraksi kata kunci + aturan lokal; tanpa API berbayar, model generatif, atau klaim tren.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hasTranscriptChanged) {
            item {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                    Text("Transkrip berubah. Regenerate untuk menyelaraskan saran dengan teks terbaru.", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        val current = metadata
        if (current != null && current.platform != platform) {
            item {
                Text("Preset platform berubah. Regenerate untuk menyesuaikan judul, caption, dan hashtag.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (current != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Judul", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        current.titles.getOrNull(selectedTitle)?.let { clipboard.setText(AnnotatedString(it)) }
                    }, enabled = current.titles.isNotEmpty()) { Text("Salin judul") }
                }
                Text("Pilih judul utama; semua alternatif tetap bisa diedit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(current.titles.indices.toList()) { index ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedTitle == index, onClick = { selectedTitle = index })
                    OutlinedTextField(
                        value = current.titles[index],
                        onValueChange = { value -> save(current.copy(titles = current.titles.toMutableList().also { it[index] = value })) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Alternatif ${index + 1}") },
                        minLines = 1,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            item { SectionTitle("Hook pembuka · 3 alternatif") }
            items(current.hooks.indices.toList()) { index ->
                EditableField("Hook ${index + 1}", current.hooks[index]) { value ->
                    save(current.copy(hooks = current.hooks.toMutableList().also { it[index] = value }))
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Caption")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { clipboard.setText(AnnotatedString(current.caption)) }) { Text("Salin caption") }
                }
                Text("Disusun dari kutipan transkrip; cek kembali akurasi transkripsi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                EditableField("Caption", current.caption, minLines = 3) { save(current.copy(caption = it)) }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Hashtag")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { clipboard.setText(AnnotatedString(current.hashtags.joinToString(" "))) }) { Text("Salin hashtag") }
                }
                Text("Gabungan niche, kata topik, dan platform. Bukan data trending.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                EditableField("Hashtag (pisahkan dengan spasi)", current.hashtags.joinToString(" "), minLines = 2) { text ->
                    save(current.copy(hashtags = text.split(Regex("\\s+")).filter(String::isNotBlank)))
                }
            }
            item { SectionTitle("Ajakan bertindak") }
            item { EditableField("Komentar", current.ctaComment) { save(current.copy(ctaComment = it)) } }
            item { EditableField("Like", current.ctaLike) { save(current.copy(ctaLike = it)) } }
            item { EditableField("Bagikan", current.ctaShare) { save(current.copy(ctaShare = it)) } }
            item { EditableField("Follow", current.ctaFollow) { save(current.copy(ctaFollow = it)) } }
            item {
                OutlinedButton(onClick = { regenerate() }, enabled = transcript.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Regenerate semua saran")
                }
            }
        } else {
            item {
                Text("Hasil metadata akan tampil di sini setelah transkrip diisi dan tombol Buat ditekan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        maxLines = if (minLines > 1) 6 else 3,
        shape = RoundedCornerShape(14.dp),
    )
}
