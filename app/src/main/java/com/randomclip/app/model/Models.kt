package com.randomclip.app.model

import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
)

data class ClipSelection(
    val video: VideoItem,
    val startPositionMs: Long,
)

data class AppSettings(
    val clipDurationSeconds: Int = 5,
    val soundEnabled: Boolean = false,
    val autoAdvance: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val folderUri: String? = null,
)
