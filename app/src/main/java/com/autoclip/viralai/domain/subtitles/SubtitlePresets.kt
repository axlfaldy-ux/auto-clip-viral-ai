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
    val accentArgb: Long,
    val animation: SubtitleAnimation,
)

/** The single catalogue used by the editor and, later, the subtitle renderer. */
object SubtitlePresets {
    val all = listOf(
        SubtitlePreset("clean", "Clean", "Teks putih bersih", "Bicara dengan jelas", 0xFFDAE6D5L, SubtitleAnimation.FADE),
        SubtitlePreset("karaoke", "Karaoke", "Sorot kata aktif", "Ini momen terbaik", 0xFFB8F35AL, SubtitleAnimation.WORD_HIGHLIGHT),
        SubtitlePreset("bold_viral", "Bold Viral", "Tebal dan kontras", "JANGAN LEWATKAN!", 0xFFFFD166L, SubtitleAnimation.SCALE_POP),
        SubtitlePreset("lower_third", "Lower Third", "Rapi di area bawah", "Cerita dimulai di sini", 0xFF8BD3DDL, SubtitleAnimation.SLIDE_UP),
        SubtitlePreset("box_highlight", "Box Highlight", "Kotak warna aksen", "Poin penting hari ini", 0xFFFF8C69L, SubtitleAnimation.BOX_FADE),
        SubtitlePreset("word_pop", "Word Pop", "Kata muncul bertahap", "Dengar baik-baik", 0xFFC9A7FFL, SubtitleAnimation.WORD_POP),
        SubtitlePreset("podcast", "Podcast", "Gaya percakapan", "Apa yang terjadi kemudian?", 0xFF8ED1B2L, SubtitleAnimation.PODCAST_FADE),
        SubtitlePreset("classic_caption", "Classic Caption", "Dua baris mudah dibaca", "Satu langkah kecil\nbisa berarti besar", 0xFFE4E7EBL, SubtitleAnimation.CAPTION_FADE),
    )
}
