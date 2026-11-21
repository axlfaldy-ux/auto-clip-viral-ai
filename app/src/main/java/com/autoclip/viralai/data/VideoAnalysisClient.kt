package com.autoclip.viralai.data

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONObject
import com.autoclip.viralai.domain.metadata.TimedWord
import com.autoclip.viralai.domain.processing.TimedFaceCrop
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class RecommendedSegment(
    val startMs: Long,
    val endMs: Long,
    val score: Double,
    val transcript: String,
    val words: List<TimedWord>,
    val faceCrops: List<TimedFaceCrop>,
)

/** Multipart client for the documented backend contract in docs/ARCHITECTURE.md. */
class VideoAnalysisClient(private val resolver: ContentResolver) {
    fun analyze(
        endpoint: String,
        source: Uri,
        requestFaceTracking: Boolean = false,
        requestWordEmphasis: Boolean = false,
    ): List<RecommendedSegment> {
        val base = endpoint.trim().trimEnd('/')
        require(base.startsWith("https://")) {
            "Endpoint backend harus menggunakan HTTPS."
        }
        val boundary = "AutoClip-${UUID.randomUUID()}"
        val connection = (URL("$base/v1/analyze").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
            setChunkedStreamingMode(256 * 1024)
        }
        try {
            val name = source.lastPathSegment?.substringAfterLast('/')
                ?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: "source-video"
            val mimeType = resolver.getType(source)?.takeIf(String::isNotBlank) ?: "application/octet-stream"
            connection.outputStream.buffered().use { output ->
                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"video\"; filename=\"$name\"\r\nContent-Type: $mimeType\r\n\r\n".toByteArray())
                resolver.openInputStream(source)?.use { input -> input.copyTo(output) }
                    ?: throw IOException("Tidak dapat membuka video terpilih.")
                output.write("\r\n--$boundary\r\nContent-Disposition: form-data; name=\"options\"\r\nContent-Type: application/json; charset=utf-8\r\n\r\n".toByteArray())
                output.write(JSONObject().apply {
                    put("word_timestamps", true)
                    put("word_emphasis", requestWordEmphasis)
                    put("face_tracking", requestFaceTracking)
                }.toString().toByteArray())
                output.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
                throw IOException("Backend menolak analisis (HTTP $status)${message.takeIf(String::isNotBlank)?.let { ": $it" } ?: ""}.")
            }
            val clips = JSONObject(body).optJSONArray("clips") ?: throw IOException("Respons backend tidak memiliki array 'clips'.")
            return (0 until clips.length()).mapNotNull clipLoop@ { index ->
                val item = clips.optJSONObject(index) ?: return@clipLoop null
                val start = item.optLong("start_ms", -1L)
                val end = item.optLong("end_ms", -1L)
                if (start < 0 || end <= start) return@clipLoop null
                val wordArray = item.optJSONArray("words")
                val words = if (wordArray == null) emptyList() else (0 until wordArray.length()).mapNotNull wordLoop@ { wordIndex ->
                    val word = wordArray.optJSONObject(wordIndex) ?: return@wordLoop null
                    val text = word.optString("text").trim()
                    val wordStart = word.optLong("start_ms", -1L)
                    val wordEnd = word.optLong("end_ms", -1L)
                    if (text.isBlank() || wordStart < 0 || wordEnd <= wordStart) null else TimedWord(
                        text, wordStart, wordEnd, word.optBoolean("emphasis", false),
                    )
                }.filter { it.startMs >= start && it.endMs <= end }.sortedBy { it.startMs }
                val cropArray = item.optJSONArray("face_crops")
                val faceCrops = if (cropArray == null) emptyList() else (0 until cropArray.length()).mapNotNull cropLoop@ { cropIndex ->
                    val crop = cropArray.optJSONObject(cropIndex) ?: return@cropLoop null
                    val cropStart = crop.optLong("start_ms", -1L)
                    val cropEnd = crop.optLong("end_ms", -1L)
                    val centerX = crop.optDouble("center_x", Double.NaN).toFloat()
                    val centerY = crop.optDouble("center_y", Double.NaN).toFloat()
                    val scale = crop.optDouble("scale", 1.0).toFloat()
                    val confidence = crop.optDouble("confidence", 0.0).toFloat()
                    if (cropStart < start || cropEnd <= cropStart || cropEnd > end ||
                        !centerX.isFinite() || !centerY.isFinite() || centerX !in 0f..1f || centerY !in 0f..1f ||
                        !scale.isFinite() || scale !in 1f..2f || confidence !in 0f..1f
                    ) null else TimedFaceCrop(cropStart, cropEnd, centerX, centerY, scale, confidence)
                }.sortedBy { it.startMs }
                RecommendedSegment(start, end, item.optDouble("score", 0.0), item.optString("transcript"), words, faceCrops)
            }.filter { it.endMs - it.startMs in 30_000L..60_000L }
        } finally {
            connection.disconnect()
        }
    }
}
