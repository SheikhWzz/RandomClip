package com.randomclip.app.model

import android.net.Uri

import java.util.Date

enum class VideoDisplayMode {
    /** TikTok-style: fills screen vertically, crops edges if needed. */
    VERTICAL_FULLSCREEN,

    /** Shows full frame with letterboxing if needed. */
    FIT,
}

data class VideoItem(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val width: Int = 0,
    val height: Int = 0,
) {
    val aspectRatio: Float?
        get() = if (width > 0 && height > 0) width.toFloat() / height else null

    val isLandscape: Boolean
        get() = width > height
}

data class FavoriteItem(
    val id: Long,
    val videoUri: Uri,
    val timestampMs: Long,
    val savedAt: Date,
    val displayName: String,
    val durationMs: Long,
)

data class ClipSelection(
    val video: VideoItem,
    val startPositionMs: Long,
    val endPositionMs: Long? = null,
)

data class RepSegment(
    val startMs: Long,
    val endMs: Long,
)

data class GameModeData(
    val videoFile: String,
    val reps: List<RepSegment>,
)

data class AppSettings(
    val clipDurationSeconds: Int = 5,
    val soundEnabled: Boolean = false,
    val autoAdvance: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val folderUris: Set<String> = emptySet(),
    val displayMode: VideoDisplayMode = VideoDisplayMode.VERTICAL_FULLSCREEN,
    val lockPortrait: Boolean = false,
    val avoidRepeats: Boolean = true,
    val pauseOnLock: Boolean = true,
    val randomMode: Boolean = false,
    val loopClip: Boolean = false,
    val language: String = "en",
)
