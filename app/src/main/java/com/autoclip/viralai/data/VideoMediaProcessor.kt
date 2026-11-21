package com.autoclip.viralai.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.OverlayEffect
import com.autoclip.viralai.domain.metadata.TimedWord
import com.autoclip.viralai.domain.processing.TimedFaceCrop
import com.autoclip.viralai.domain.subtitles.SubtitleAnimation
import com.autoclip.viralai.domain.subtitles.SubtitlePresets
import com.autoclip.viralai.domain.subtitles.SubtitleStyle
import com.autoclip.viralai.domain.subtitles.TalkingHeadAnimation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Real on-device media operations. Import uses the URI grant from the system picker. */
class VideoMediaProcessor(private val context: Context) {
    suspend fun durationMs(uri: Uri): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it > 0L }
                ?: throw IOException("Android tidak dapat membaca durasi video ini.")
        } finally {
            retriever.release()
        }
    }

    /** Trims and center-crops a user-selected interval to 1080x1920, then inserts it in Movies. */
    @OptIn(UnstableApi::class)
    suspend fun exportPortraitClip(
        source: Uri,
        startMs: Long,
        lengthMs: Long,
        subtitleWords: List<TimedWord> = emptyList(),
        subtitlePresetId: String = "classic",
        subtitleStyle: SubtitleStyle = SubtitleStyle(),
        cameraAnimation: TalkingHeadAnimation = TalkingHeadAnimation.SUBTLE_CAMERA,
        faceCrops: List<TimedFaceCrop> = emptyList(),
    ): Uri {
        require(startMs >= 0) { "Waktu mulai tidak valid." }
        require(lengthMs in 1_000L..600_000L) { "Durasi potongan harus antara 1 detik dan 10 menit." }
        require(cameraAnimation !in setOf(TalkingHeadAnimation.ACTIVE_SPEAKER, TalkingHeadAnimation.AUTO_REFRAME) || faceCrops.isNotEmpty()) {
            "Pelacakan wajah memerlukan data face_crops dari backend analisis."
        }
        val sourceDuration = durationMs(source)
        require(startMs + lengthMs <= sourceDuration) { "Potongan melampaui durasi video sumber." }

        val output = withContext(Dispatchers.IO) {
            File(context.cacheDir, "clip-${System.currentTimeMillis()}.mp4").also { it.parentFile?.mkdirs() }
        }
        try {
            transform(source, startMs, lengthMs, output, subtitleWords, subtitlePresetId, subtitleStyle, cameraAnimation, faceCrops)
            return saveToGallery(output)
        } finally {
            withContext(Dispatchers.IO) { output.delete() }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun transform(
        source: Uri,
        startMs: Long,
        durationMs: Long,
        output: File,
        subtitleWords: List<TimedWord>,
        subtitlePresetId: String,
        subtitleStyle: SubtitleStyle,
        cameraAnimation: TalkingHeadAnimation,
        faceCrops: List<TimedFaceCrop>,
    ) =
        suspendCancellableCoroutine<Unit> { continuation ->
            val ended = AtomicBoolean(false)
            val mediaItem = MediaItem.Builder()
                .setUri(source)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(startMs + durationMs)
                        .build(),
                )
                .build()
            val effects = mutableListOf<androidx.media3.common.Effect>(
                Presentation.createForWidthAndHeight(
                    1080,
                    1920,
                    Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
                ),
            )
            effects += cameraMovementEffect(cameraAnimation, startMs, subtitleWords, faceCrops)
            if (subtitleWords.isNotEmpty()) {
                effects += OverlayEffect(listOf(WordSubtitleOverlay(subtitleWords, startMs, subtitlePresetId, subtitleStyle, context)))
            }
            val edited = EditedMediaItem.Builder(mediaItem)
                .setEffects(
                    Effects(
                        emptyList(),
                        effects,
                    ),
                )
                .build()
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                        if (ended.compareAndSet(false, true) && continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: androidx.media3.transformer.Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (ended.compareAndSet(false, true) && continuation.isActive) {
                            continuation.resumeWithException(IOException("Ekspor gagal: ${exportException.message ?: "format video tidak didukung"}", exportException))
                        }
                    }
                })
                .build()
            continuation.invokeOnCancellation {
                if (ended.compareAndSet(false, true)) transformer.cancel()
            }
            try {
                transformer.start(edited, output.absolutePath)
            } catch (error: Throwable) {
                if (ended.compareAndSet(false, true) && continuation.isActive) continuation.resumeWithException(error)
            }
        }

    @OptIn(UnstableApi::class)
    private fun cameraMovementEffect(
        animation: TalkingHeadAnimation,
        sourceStartMs: Long,
        words: List<TimedWord>,
        crops: List<TimedFaceCrop>,
    ): androidx.media3.effect.MatrixTransformation = androidx.media3.effect.MatrixTransformation { presentationTimeUs ->
        val clipTimeMs = presentationTimeUs / 1_000L
        val phase = clipTimeMs / 1_000.0
        var scale = 1f
        var translateX = 0f
        var translateY = 0f
        when (animation) {
            TalkingHeadAnimation.DYNAMIC_PUNCH -> {
                val pulse = maxOf(0.0, sin(2.0 * PI * phase / 4.2))
                scale = 1f + (0.13 * pulse * pulse).toFloat()
            }
            TalkingHeadAnimation.SMOOTH_ZOOM -> scale = (1.015 + 0.035 * (0.5 - 0.5 * cos(2.0 * PI * phase / 11.0))).toFloat()
            TalkingHeadAnimation.JUMP_CUT -> scale = if ((phase / 3.2).toInt() % 2 == 0) 1.015f else 1.065f
            TalkingHeadAnimation.SUBTLE_CAMERA -> {
                scale = (1.02 + 0.012 * sin(2.0 * PI * phase / 9.0)).toFloat()
                translateX = (0.024 * sin(2.0 * PI * phase / 12.0)).toFloat()
                translateY = (0.012 * cos(2.0 * PI * phase / 10.0)).toFloat()
            }
            TalkingHeadAnimation.EMPHASIS_ZOOM -> {
                val emphasisWord = words.firstOrNull { it.isEmphasis && sourceStartMs + clipTimeMs in it.startMs..it.endMs }
                val emphasisAmount = if (emphasisWord == null) 0f else {
                    val length = (emphasisWord.endMs - emphasisWord.startMs).coerceAtLeast(1L).toFloat()
                    val progress = ((sourceStartMs + clipTimeMs - emphasisWord.startMs) / length).coerceIn(0f, 1f)
                    minOf(1f, progress / 0.2f, (1f - progress) / 0.2f).coerceAtLeast(0f)
                }
                scale = 1.015f + 0.075f * emphasisAmount
            }
            TalkingHeadAnimation.ACTIVE_SPEAKER,
            TalkingHeadAnimation.AUTO_REFRAME -> {
                val absoluteTime = sourceStartMs + clipTimeMs
                val crop = interpolateFaceCrop(crops, absoluteTime)
                if (crop != null) {
                    scale = crop.scale.coerceIn(1f, 1.18f)
                    translateX = ((0.5f - crop.centerX) * 2f * scale).coerceIn(-0.45f, 0.45f)
                    translateY = ((0.5f - crop.centerY) * 2f * scale).coerceIn(-0.45f, 0.45f)
                } else scale = 1.02f
            }
        }
        Matrix().apply {
            // MatrixTransformation operates on normalized device coordinates (-1..1).
            // Scaling around (0, 0) zooms around the frame center.
            setScale(scale, scale)
            postTranslate(translateX, translateY)
        }
    }

    private fun interpolateFaceCrop(crops: List<TimedFaceCrop>, timeMs: Long): TimedFaceCrop? {
        val usable = crops.filter { it.confidence >= 0.35f }
        val before = usable.lastOrNull { it.startMs <= timeMs }
        val after = usable.firstOrNull { it.endMs > timeMs }
        if (before == null) return after
        if (before == after || timeMs in before.startMs until before.endMs) return before
        if (after == null) return null
        val span = (after.startMs - before.startMs).coerceAtLeast(1L).toFloat()
        val fraction = ((timeMs - before.startMs) / span).coerceIn(0f, 1f)
        return TimedFaceCrop(
            timeMs,
            timeMs + 1,
            before.centerX + (after.centerX - before.centerX) * fraction,
            before.centerY + (after.centerY - before.centerY) * fraction,
            before.scale + (after.scale - before.scale) * fraction,
            before.confidence + (after.confidence - before.confidence) * fraction,
        )
    }

    private suspend fun saveToGallery(file: File): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = "AutoClip-${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AutoClipViralAI")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "AutoClipViralAI")
            if (!directory.exists() && !directory.mkdirs()) throw IOException("Tidak dapat membuat folder video di galeri.")
            val publicFile = File(directory, displayName)
            file.copyTo(publicFile, overwrite = false)
            values.put(MediaStore.Video.Media.DATA, publicFile.absolutePath)
            return@withContext try {
                resolver.insert(collection, values) ?: throw IOException("Tidak dapat mendaftarkan video di galeri.")
            } catch (error: Throwable) {
                publicFile.delete()
                throw error
            }
        }
        val destination = resolver.insert(collection, values) ?: throw IOException("Tidak dapat membuat file di galeri.")
        try {
            resolver.openOutputStream(destination, "w")?.use { target -> file.inputStream().use { it.copyTo(target) } }
                ?: throw IOException("Tidak dapat menulis file hasil ke galeri.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(destination, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            }
            destination
        } catch (error: Throwable) {
            resolver.delete(destination, null, null)
            throw error
        }
    }
}

@UnstableApi
private class WordSubtitleOverlay(
    private val words: List<TimedWord>,
    private val sourceStartMs: Long,
    presetId: String,
    private val style: SubtitleStyle,
    context: Context,
) : CanvasOverlay(true) {
    private val preset = SubtitlePresets.all.firstOrNull { it.id == presetId }
        ?: SubtitlePresets.all.first { it.id == "classic" }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.textArgb.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(
            if (preset.id == "classic" || preset.id == "elegant" || preset.id == "cinematic") "sans-serif" else "sans-serif-black",
            if (preset.id == "classic" || preset.id == "elegant" || preset.id == "cinematic") Typeface.NORMAL else Typeface.BOLD,
        )
        setShadowLayer(style.shadowFraction * 1920f, 0f, style.shadowFraction * 1920f * 0.55f, style.outlineArgb.toInt())
    }
    private val typeface = Typeface.createFromAsset(context.assets, preset.fontAsset)
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(205, 0, 0, 0) }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = OverlaySettings.Builder()
        .setAlphaScale(1f)
        .build()

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        val clipTimeMs = presentationTimeUs / 1_000L
        val absoluteTimeMs = sourceStartMs + clipTimeMs
        val activeIndex = words.indexOfFirst { absoluteTimeMs >= it.startMs && absoluteTimeMs < it.endMs }
        if (activeIndex < 0) return
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val from = (activeIndex - 2).coerceAtLeast(0)
        val to = (activeIndex + 2).coerceAtMost(words.lastIndex)
        val visible = words.subList(from, to + 1)
        val scale = if (preset.animation == SubtitleAnimation.SCALE_POP || preset.animation == SubtitleAnimation.WORD_POP) 1.08f else 1f
        val baseSize = if (preset.id == "bold_viral" || preset.id == "impact") style.sizeFraction * 1.12f else style.sizeFraction
        textPaint.textSize = height * baseSize * scale
        textPaint.typeface = Typeface.create(typeface, if (preset.id == "bold_viral" || preset.id == "impact") Typeface.BOLD else Typeface.NORMAL)
        var gap = textPaint.textSize * 0.32f
        val displayWords = visible.map { if (preset.id == "bold_viral") it.text.uppercase() else it.text }
        var widths = displayWords.map { textPaint.measureText(it) }
        val widestWord = widths.maxOrNull() ?: 0f
        if (widestWord > width * 0.9f) {
            textPaint.textSize *= (width * 0.9f / widestWord)
            gap = textPaint.textSize * 0.32f
            widths = displayWords.map { textPaint.measureText(it) }
        }
        val maxLineWidth = width * 0.9f
        val lineRanges = mutableListOf<IntRange>()
        var lineStart = 0
        var lineWidth = 0f
        widths.forEachIndexed { index, wordWidth ->
            val nextWidth = if (index == lineStart) wordWidth else lineWidth + gap + wordWidth
            if (index > lineStart && nextWidth > maxLineWidth) {
                lineRanges += lineStart until index
                lineStart = index
                lineWidth = wordWidth
            } else {
                lineWidth = nextWidth
            }
        }
        if (displayWords.isNotEmpty()) lineRanges += lineStart..displayWords.lastIndex
        val lineWidths = lineRanges.map { range ->
            range.sumOf { widths[it].toDouble() }.toFloat() + gap * (range.last - range.first)
        }
        val lineHeight = textPaint.textSize * 1.18f
        val lastBaseline = height * style.positionFraction
        val firstBaseline = lastBaseline - (lineRanges.size - 1).coerceAtLeast(0) * lineHeight
        if (preset.animation == SubtitleAnimation.BOX_FADE || preset.animation == SubtitleAnimation.PODCAST_FADE) {
            val panelWidth = (lineWidths.maxOrNull() ?: 0f) + 40f
            canvas.drawRoundRect(
                RectF((width - panelWidth) / 2f, firstBaseline - textPaint.textSize * 1.25f, (width + panelWidth) / 2f, lastBaseline + 18f),
                18f,
                18f,
                boxPaint,
            )
        }
        val activeWord = words[activeIndex]
        val wordProgress = ((absoluteTimeMs - activeWord.startMs).toFloat() / (activeWord.endMs - activeWord.startMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
        val fadeAlpha = if (preset.animation == SubtitleAnimation.FADE || preset.animation == SubtitleAnimation.PODCAST_FADE || preset.animation == SubtitleAnimation.CAPTION_FADE) {
            (110 + (145 * minOf(wordProgress * 5f, (1f - wordProgress) * 5f, 1f)).toInt()).coerceIn(0, 255)
        } else 255
        canvas.save()
        if (preset.animation == SubtitleAnimation.SLIDE_UP) canvas.translate(0f, (1f - wordProgress) * 18f)
        lineRanges.forEachIndexed { lineIndex, range ->
            var x = (width - lineWidths[lineIndex]) / 2f
            val wordBaseline = firstBaseline + lineIndex * lineHeight
            range.forEach { localIndex ->
                val actualIndex = from + localIndex
                val wordWidth = widths[localIndex]
                val highlightActiveWord = actualIndex == activeIndex && (
                    words[actualIndex].isEmphasis ||
                        preset.animation == SubtitleAnimation.WORD_HIGHLIGHT ||
                        preset.animation == SubtitleAnimation.WORD_POP ||
                        preset.id == "bold_viral"
                    )
                val activeTextColor = if (highlightActiveWord) {
                    (preset.accentArgb and 0x00FFFFFF).toInt() or (0xFF shl 24)
                } else style.textArgb.toInt()
                textPaint.color = activeTextColor
                textPaint.alpha = if (actualIndex == activeIndex) 255 else fadeAlpha
                if (style.outlineFraction > 0f) {
                    textPaint.style = Paint.Style.STROKE
                    textPaint.strokeWidth = style.outlineFraction * height
                    textPaint.strokeJoin = Paint.Join.ROUND
                    textPaint.color = style.outlineArgb.toInt()
                    canvas.drawText(displayWords[localIndex], x + wordWidth / 2f, wordBaseline, textPaint)
                    textPaint.style = Paint.Style.FILL
                    textPaint.color = activeTextColor
                }
                canvas.drawText(displayWords[localIndex], x + wordWidth / 2f, wordBaseline, textPaint)
                x += wordWidth + gap
            }
        }
        canvas.restore()
        textPaint.color = style.textArgb.toInt()
        textPaint.alpha = 255
    }
}
