package com.autoclip.viralai.domain.video

/** Shared product constraints for selection and export modules. */
object ClipSpecification {
    const val MIN_DURATION_MS = 30_000L
    const val MAX_DURATION_MS = 60_000L
    const val OUTPUT_WIDTH = 1080
    const val OUTPUT_HEIGHT = 1920
    const val ASPECT_WIDTH = 9
    const val ASPECT_HEIGHT = 16

    fun acceptsDuration(durationMs: Long): Boolean =
        durationMs in MIN_DURATION_MS..MAX_DURATION_MS
}

data class ClipCandidate(
    val clipId: String,
    val startMs: Long,
    val endMs: Long,
    val score: Float,
) {
    val durationMs: Long get() = endMs - startMs
    val isWithinSpecification: Boolean get() = startMs >= 0 && ClipSpecification.acceptsDuration(durationMs)
}

data class ExportProfile(
    val width: Int = ClipSpecification.OUTPUT_WIDTH,
    val height: Int = ClipSpecification.OUTPUT_HEIGHT,
) {
    val isPortraitNineBySixteen: Boolean
        get() = width > 0 && height > 0 && width * ClipSpecification.ASPECT_HEIGHT == height * ClipSpecification.ASPECT_WIDTH
}
