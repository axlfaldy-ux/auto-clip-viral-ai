package com.autoclip.viralai.domain.subtitles

enum class SubtitleAnimation(val label: String) {
    FADE("Fade per frasa"),
    WORD_HIGHLIGHT("Highlight kata aktif"),
    SCALE_POP("Pop singkat"),
    SLIDE_UP("Slide dari bawah"),
    BOX_FADE("Fade dengan panel"),
    WORD_POP("Pop per kata"),
    PODCAST_FADE("Fade per dialog"),
    CAPTION_FADE("Fade subtitle"),
}

data class SubtitlePreset(
    val id: String,
    val displayName: String,
    val description: String,
    val previewText: String,
    val fontAsset: String,
    val accentArgb: Long,
    val animation: SubtitleAnimation,
)

data class SubtitleStyle(
    val sizeFraction: Float = 0.046f,
    val textArgb: Long = 0xFFFFFFFF,
    val outlineArgb: Long = 0xFF000000,
    val outlineFraction: Float = 0.0025f,
    val shadowFraction: Float = 0.004f,
    val positionFraction: Float = 0.84f,
)

enum class TalkingHeadAnimation(val label: String) {
    DYNAMIC_PUNCH("Dynamic Punch In / Punch Out"),
    SMOOTH_ZOOM("Smooth Zoom"),
    JUMP_CUT("Jump Cut"),
    ACTIVE_SPEAKER("Active Speaker Face Tracking"),
    AUTO_REFRAME("Auto Reframe wajah"),
    SUBTLE_CAMERA("Subtle Camera Movement"),
    EMPHASIS_ZOOM("Emphasis Zoom kata penting"),
}

/** One catalogue drives the preview and the video renderer. */
object SubtitlePresets {
    val all = listOf(
        SubtitlePreset("classic", "Classic", "Roboto klasik, jelas dan seimbang", "Bicara dengan jelas", "fonts/classic-roboto.ttf", 0xFFFFFFFFL, SubtitleAnimation.FADE),
        SubtitlePreset("bold_viral", "Bold Viral", "Tebal dan kontras", "JANGAN LEWATKAN!", "fonts/bold-source-sans.ttf", 0xFFFFD166L, SubtitleAnimation.SCALE_POP),
        SubtitlePreset("elegant", "Elegant", "Serif elegan dengan karakter editorial", "Cerita dimulai di sini", "fonts/elegant-noto-serif.ttf", 0xFFF1DEBCL, SubtitleAnimation.FADE),
        SubtitlePreset("typewriter", "Typewriter", "Monospace seperti mesin tik", "Satu kata demi kata", "fonts/typewriter-cutive-mono.ttf", 0xFFE6E6E6L, SubtitleAnimation.WORD_HIGHLIGHT),
        SubtitlePreset("modern", "Modern", "Sans modern dengan bentuk fleksibel", "Fokus pada ceritanya", "fonts/modern-roboto-flex.ttf", 0xFFB8F35AL, SubtitleAnimation.WORD_POP),
        SubtitlePreset("rounded", "Rounded", "Huruf bulat bergaya kasual", "Dengar baik-baik", "fonts/rounded-coming-soon.ttf", 0xFF8ED1B2L, SubtitleAnimation.SCALE_POP),
        SubtitlePreset("cinematic", "Cinematic", "Script sinematik untuk dialog pendek", "Momen yang tak terlupa", "fonts/cinematic-dancing-script.ttf", 0xFFFFE4A6L, SubtitleAnimation.FADE),
        SubtitlePreset("impact", "Impact", "Gothic condensed untuk penekanan", "INI POIN PENTING", "fonts/impact-carrois-gothic.ttf", 0xFFFF7058L, SubtitleAnimation.SCALE_POP),
    )
}
