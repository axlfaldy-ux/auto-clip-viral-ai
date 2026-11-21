package com.autoclip.viralai.domain.processing

import android.net.Uri
import com.autoclip.viralai.domain.metadata.ClipMetadata
import com.autoclip.viralai.domain.metadata.ClipMetadataPipeline
import com.autoclip.viralai.domain.metadata.ClipTranscript
import com.autoclip.viralai.domain.metadata.MetadataPlatform
import com.autoclip.viralai.domain.metadata.MetadataRepository
import com.autoclip.viralai.domain.video.ClipCandidate
import com.autoclip.viralai.domain.video.ExportProfile

/** One ASR pass per source. Returned word timestamps are shared across the downstream modules. */
interface OnDeviceTranscriber {
    suspend fun transcribe(source: Uri): ClipTranscript
}

interface ClipSelectionEngine {
    suspend fun selectCandidates(source: Uri, transcript: ClipTranscript): List<ClipCandidate>
}

data class TimedFaceCrop(
    val startMs: Long,
    val endMs: Long,
    val centerX: Float,
    val centerY: Float,
    val scale: Float,
    val confidence: Float,
)

interface ActiveSpeakerTracker {
    /** Matches active speech with face/lip motion and returns smoothed portrait crop intervals. */
    suspend fun track(source: Uri, candidate: ClipCandidate, transcript: ClipTranscript): List<TimedFaceCrop>
}

interface VideoExportEngine {
    /** Applies crop and word-timed subtitles using a device codec; returns the exported file URI. */
    suspend fun export(
        source: Uri,
        candidate: ClipCandidate,
        faceCrops: List<TimedFaceCrop>,
        transcript: ClipTranscript,
        profile: ExportProfile,
    ): Uri
}

data class ProcessedClip(
    val candidate: ClipCandidate,
    val videoUri: Uri,
    val metadata: ClipMetadata,
)

/**
 * Integration seam for the future processors. It transcribes once, shares that transcript,
 * filters candidates to 30-60 seconds, exports 9:16, then persists metadata for each completed clip.
 * No concrete media or ML engines are shipped in the UI foundation.
 */
class LocalClipWorkflow(
    private val transcriber: OnDeviceTranscriber,
    private val selector: ClipSelectionEngine,
    private val speakerTracker: ActiveSpeakerTracker,
    private val exporter: VideoExportEngine,
    private val metadataRepository: MetadataRepository,
) {
    suspend fun process(
        source: Uri,
        platform: MetadataPlatform,
        profile: ExportProfile = ExportProfile(),
    ): List<ProcessedClip> {
        require(profile.isPortraitNineBySixteen) { "Export profile must use a 9:16 portrait ratio." }
        val sourceTranscript = transcriber.transcribe(source)
        require(sourceTranscript.words.isNotEmpty()) { "Transcription must include word timestamps." }

        val candidates = selector.selectCandidates(source, sourceTranscript)
            .filter(ClipCandidate::isWithinSpecification)

        return candidates.map { candidate ->
            val clipWords = sourceTranscript.words.filter {
                it.endMs > candidate.startMs && it.startMs < candidate.endMs
            }
            require(clipWords.isNotEmpty()) { "Candidate has no timestamped transcript words." }
            val clipTranscript = sourceTranscript.copy(
                clipId = candidate.clipId,
                text = clipWords.joinToString(" ") { it.text },
                words = clipWords,
            )
            val faceCrops = speakerTracker.track(source, candidate, sourceTranscript)
            val outputUri = exporter.export(source, candidate, faceCrops, clipTranscript, profile)
            val metadata = requireNotNull(
                ClipMetadataPipeline.onClipReady(clipTranscript, platform, metadataRepository),
            ) { "Metadata could not be generated for completed clip ${candidate.clipId}." }
            ProcessedClip(candidate, outputUri, metadata)
        }
    }
}

