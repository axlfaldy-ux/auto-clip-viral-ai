package com.autoclip.viralai.domain.metadata

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Canonical transcript input intended to be shared by clip scoring and metadata generation. */
data class TimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val isEmphasis: Boolean = false,
)

data class ClipTranscript(
    val clipId: String,
    val text: String,
    val languageTag: String = "id",
    val words: List<TimedWord> = emptyList(),
)

enum class MetadataPlatform(val label: String) {
    TIKTOK("TikTok"),
    YOUTUBE_SHORTS("YouTube Shorts"),
    INSTAGRAM_REELS("Instagram Reels"),
    FACEBOOK_REELS("Facebook Reels"),
}

data class ClipMetadata(
    val clipId: String,
    val sourceTranscript: String,
    val platform: MetadataPlatform,
    val titles: List<String>,
    val hooks: List<String>,
    val caption: String,
    val hashtags: List<String>,
    val ctaComment: String,
    val ctaLike: String,
    val ctaShare: String,
    val ctaFollow: String,
    val generatedAtMillis: Long = System.currentTimeMillis(),
)

/** Local, extractive and rule-based suggestions. It does not call a cloud model or claim trend data. */
object LocalMetadataGenerator {
    private val stopWords = setOf(
        "yang", "dan", "di", "ke", "dari", "untuk", "pada", "dengan", "ini", "itu", "saya", "kamu",
        "dia", "mereka", "kita", "kami", "ada", "jadi", "akan", "sudah", "belum", "bisa", "dapat",
        "atau", "juga", "karena", "kalau", "dalam", "oleh", "sebagai", "lebih", "sangat", "satu", "dua",
        "apa", "kenapa", "bagaimana", "siapa", "mana", "saat", "ketika", "tentang", "soal", "tersebut",
        "adalah", "bukan", "tidak", "enggak", "nggak", "gak", "ya", "kan", "nih", "sih", "dong",
        "the", "and", "for", "with", "this", "that", "you", "your", "are", "was", "from", "have",
    )

    fun generate(transcript: ClipTranscript, platform: MetadataPlatform): ClipMetadata? {
        val source = transcript.text.replace(Regex("\\s+"), " ").trim()
        if (source.isBlank()) return null

        val keywords = extractKeywords(source)
        val topic = keywords.firstOrNull()?.let(::titleCase) ?: firstUsefulPhrase(source)
        val secondTopic = keywords.getOrNull(1)?.let(::titleCase)
        val topicSlug = keywords.firstOrNull()?.let(::slug) ?: "video"
        val contextSlug = secondTopic?.let(::slug)

        val titles = listOf(
            "Penasaran dengan $topic? Ini konteksnya",
            "Kenapa $topic dibahas di sini?",
            "Mengenal $topic dari konteks video",
            "Pembahasan singkat: $topic",
            "Satu pertanyaan untuk direnungkan: $topic",
        ).map { trimWords(it, if (platform == MetadataPlatform.TIKTOK) 9 else 13) }
            .distinct()
            .let { candidates -> candidates + List((5 - candidates.size).coerceAtLeast(0)) { "Satu pembahasan tentang $topic" } }
            .take(5)

        val hooks = listOf(
            "Pernah bertanya soal $topic?",
            "Mari bahas: ${topic.lowercase()}",
            "Apa konteks di balik ${topic.lowercase()}?",
        )

        // The quoted passage is verbatim transcript text; only a neutral question is added.
        val excerpt = source.take(260).let { if (source.length > 260) it.trimEnd() + "…" else it }
        val captionPrompt = when (platform) {
            MetadataPlatform.TIKTOK -> "Apa pendapatmu?"
            MetadataPlatform.YOUTUBE_SHORTS -> "Tulis pendapatmu di komentar."
            MetadataPlatform.INSTAGRAM_REELS -> "Bagaimana menurutmu?"
            MetadataPlatform.FACEBOOK_REELS -> "Bagaimana pendapatmu tentang ini?"
        }
        val caption = "“$excerpt”\n\n$captionPrompt"
        val nicheTag = if (contextSlug != null && contextSlug != topicSlug) "#$topicSlug$contextSlug" else "#$topicSlug"
        val topicTags = keywords.take(3).map { "#${slug(it)}" }.distinct()
        val platformTags = when (platform) {
            MetadataPlatform.TIKTOK -> listOf("#tiktokindonesia", "#kontenindonesia")
            MetadataPlatform.YOUTUBE_SHORTS -> listOf("#shorts", "#youtubeindonesia")
            MetadataPlatform.INSTAGRAM_REELS -> listOf("#reels", "#reelsindonesia")
            MetadataPlatform.FACEBOOK_REELS -> listOf("#facebookreels", "#kontenindonesia")
        }
        val hashtags = (listOf(nicheTag) + topicTags + platformTags).distinct().take(6)

        return ClipMetadata(
            clipId = transcript.clipId,
            sourceTranscript = source,
            platform = platform,
            titles = titles,
            hooks = hooks,
            caption = caption,
            hashtags = hashtags,
            ctaComment = "Menurut kamu, apa pendapatmu tentang ${topic.lowercase()}?",
            ctaLike = "Kalau pembahasan ini bermanfaat, beri like.",
            ctaShare = "Bagikan video ini kepada teman yang tertarik dengan ${topic.lowercase()}.",
            ctaFollow = "Ikuti akun ini untuk melihat pembahasan berikutnya.",
        )
    }

    private fun extractKeywords(text: String): List<String> {
        val tokens = Regex("[\\p{L}\\p{N}]{4,}").findAll(text.lowercase())
            .map { it.value }
            .filterNot { it in stopWords || it.all(Char::isDigit) }
            .toList()
        val counts = tokens.groupingBy { it }.eachCount()
        return counts.keys.sortedWith(compareByDescending<String> { counts[it] ?: 0 }.thenBy { tokens.indexOf(it) }).take(5)
    }

    private fun firstSentence(text: String): String =
        text.split(Regex("(?<=[.!?])\\s+"), limit = 2).first().trim().ifBlank { text.take(100) }

    private fun firstUsefulPhrase(text: String): String = trimWords(firstSentence(text), 4).trimEnd('.', '?', '!').ifBlank { "Video" }

    private fun trimWords(text: String, limit: Int): String {
        val words = text.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        return if (words.size <= limit) text.trim() else words.take(limit).joinToString(" ").trimEnd(',', ':', ';') + "…"
    }

    private fun titleCase(value: String): String = value.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun slug(value: String): String = java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]"), "")
        .take(28)
        .ifBlank { "video" }
}

/** Persists editor results by clip ID. Export can later write this same payload beside its MP4. */
class MetadataRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("clip_metadata", Context.MODE_PRIVATE)

    fun get(clipId: String): ClipMetadata? = runCatching {
        preferences.getString(key(clipId), null)?.let(::decode)
    }.getOrNull()

    fun save(metadata: ClipMetadata) {
        preferences.edit().putString(key(metadata.clipId), encode(metadata).toString()).apply()
    }

    fun saveDraft(clipId: String, transcript: String, platformName: String) {
        preferences.edit()
            .putString("${key(clipId)}_draft_text", transcript)
            .putString("${key(clipId)}_draft_platform", platformName)
            .apply()
    }

    fun getDraftTranscript(clipId: String): String =
        preferences.getString("${key(clipId)}_draft_text", "").orEmpty()

    fun getDraftPlatform(clipId: String): MetadataPlatform? = runCatching {
        preferences.getString("${key(clipId)}_draft_platform", null)?.let { MetadataPlatform.valueOf(it) }
    }.getOrNull()

    private fun key(clipId: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(clipId.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "metadata_$digest"
    }

    private fun encode(metadata: ClipMetadata) = JSONObject().apply {
        put("clipId", metadata.clipId)
        put("sourceTranscript", metadata.sourceTranscript)
        put("platform", metadata.platform.name)
        put("titles", JSONArray(metadata.titles))
        put("hooks", JSONArray(metadata.hooks))
        put("caption", metadata.caption)
        put("hashtags", JSONArray(metadata.hashtags))
        put("ctaComment", metadata.ctaComment)
        put("ctaLike", metadata.ctaLike)
        put("ctaShare", metadata.ctaShare)
        put("ctaFollow", metadata.ctaFollow)
        put("generatedAtMillis", metadata.generatedAtMillis)
    }

    private fun decode(json: String): ClipMetadata {
        val value = JSONObject(json)
        return ClipMetadata(
            clipId = value.getString("clipId"),
            sourceTranscript = value.optString("sourceTranscript"),
            platform = MetadataPlatform.valueOf(value.getString("platform")),
            titles = value.stringList("titles"),
            hooks = value.stringList("hooks"),
            caption = value.optString("caption"),
            hashtags = value.stringList("hashtags"),
            ctaComment = value.optString("ctaComment"),
            ctaLike = value.optString("ctaLike"),
            ctaShare = value.optString("ctaShare"),
            ctaFollow = value.optString("ctaFollow"),
            generatedAtMillis = value.optLong("generatedAtMillis"),
        )
    }

    private fun JSONObject.stringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { index -> array.optString(index) }
    }
}

/** Called by the future clip processor after the shared transcript is ready for a completed clip. */
object ClipMetadataPipeline {
    fun onClipReady(
        transcript: ClipTranscript,
        platform: MetadataPlatform,
        repository: MetadataRepository,
    ): ClipMetadata? {
        val metadata = LocalMetadataGenerator.generate(transcript, platform) ?: return null
        repository.save(metadata)
        return metadata
    }
}
