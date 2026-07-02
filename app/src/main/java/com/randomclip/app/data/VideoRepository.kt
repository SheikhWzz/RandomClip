package com.randomclip.app.data

import android.content.Context
import android.net.Uri
import com.randomclip.app.data.local.VideoCacheDatabase
import com.randomclip.app.data.local.VideoEntity
import com.randomclip.app.model.ClipSelection
import com.randomclip.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class VideoRepository(
    context: Context,
    private val scanner: VideoScanner,
) {
    private val videoDao = VideoCacheDatabase.getInstance(context).videoDao()

    suspend fun getVideos(folderUri: Uri, forceRefresh: Boolean = false): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val folderKey = folderUri.toString()
            if (!forceRefresh) {
                val cached = videoDao.getVideosForFolder(folderKey)
                if (cached.isNotEmpty()) {
                    return@withContext cached.map { it.toVideoItem() }
                }
            }

            val scanned = scanner.scanFolder(folderUri)
            videoDao.deleteForFolder(folderKey)
            if (scanned.isNotEmpty()) {
                val now = System.currentTimeMillis()
                videoDao.insertAll(
                    scanned.map {
                        VideoEntity(
                            uri = it.uri.toString(),
                            displayName = it.displayName,
                            durationMs = it.durationMs,
                            folderUri = folderKey,
                            scannedAt = now,
                        )
                    },
                )
            }
            scanned
        }

    fun pickRandomClip(
        videos: List<VideoItem>,
        clipDurationSeconds: Int,
        excludeUri: Uri? = null,
    ): ClipSelection? {
        if (videos.isEmpty()) return null

        val pool = if (excludeUri != null && videos.size > 1) {
            videos.filter { it.uri != excludeUri }.ifEmpty { videos }
        } else {
            videos
        }

        val video = pool.random()
        val clipMs = clipDurationSeconds * 1000L
        val maxStart = (video.durationMs - clipMs).coerceAtLeast(0L)
        val startMs = if (maxStart > 0L) Random.nextLong(0L, maxStart + 1) else 0L

        return ClipSelection(video = video, startPositionMs = startMs)
    }

    private fun VideoEntity.toVideoItem() = VideoItem(
        uri = Uri.parse(uri),
        displayName = displayName,
        durationMs = durationMs,
    )
}
