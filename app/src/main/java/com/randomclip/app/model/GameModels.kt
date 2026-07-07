package com.randomclip.app.model

enum class GameVideoDisplayMode {
    /** Pick zoom vs fit from video aspect ratio. */
    AUTO,

    /** TikTok-style vertical fill (zoom). */
    VERTICAL_FULLSCREEN,

    /** Show full frame with letterboxing. */
    FIT,
}

data class GameSettings(
    val soundEnabled: Boolean = true,
)

data class GameVideoPreferences(
    val displayMode: GameVideoDisplayMode = GameVideoDisplayMode.AUTO,
    val soundEnabled: Boolean? = null,
    val segmentStartMs: Long? = null,
    val segmentEndMs: Long? = null,
    val videoFlipped: Boolean = false,
    val landscapeGameEnabled: Boolean = false,
    val segmentLoopEnabled: Boolean = true,
)
