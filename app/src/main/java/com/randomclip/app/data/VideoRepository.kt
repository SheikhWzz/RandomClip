package com.randomclip.app.data

import android.content.Context
import android.net.Uri
import com.randomclip.app.data.local.VideoCacheDatabase
import com.randomclip.app.data.local.VideoEntity
import com.randomclip.app.data.local.FavoriteEntity
import com.randomclip.app.model.ClipSelection
import com.randomclip.app.model.FavoriteItem
import com.randomclip.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.random.Random

class VideoRepository(
    context: Context,
    private val scanner: VideoScanner,
) {
    private val database = VideoCacheDatabase.getInstance(context)
    private val videoDao = database.videoDao()
    private val favoriteDao = database.favoriteDao()

    suspend fun getVideos(folderUris: Set<Uri>, forceRefresh: Boolean = false): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val allVideos = mutableListOf<VideoItem>()

            val jobs = folderUris.map { folderUri ->
                async {
                    val folderKey = folderUri.toString()
                    if (!forceRefresh) {
                        val cached = videoDao.getVideosForFolder(folderKey)
                        if (cached.isNotEmpty()) {
                            return@async cached.map { it.toVideoItem() }
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
                                    width = it.width,
                                    height = it.height,
                                    folderUri = folderKey,
                                    scannedAt = now,
                                    isPlayable = true
                                )
                            },
                        )
                    }
                    scanned
                }
            }

            jobs.awaitAll().forEach { allVideos.addAll(it) }
            allVideos
        }

    suspend fun markAsUnplayable(uri: Uri) {
        withContext(Dispatchers.IO) {
            videoDao.markAsUnplayable(uri.toString())
        }
    }

    suspend fun saveFavorite(video: VideoItem, timestampMs: Long) {
        withContext(Dispatchers.IO) {
            favoriteDao.insert(
                FavoriteEntity(
                    videoUri = video.uri.toString(),
                    timestampMs = timestampMs,
                    savedAt = System.currentTimeMillis(),
                    displayName = video.displayName,
                    durationMs = video.durationMs
                )
            )
        }
    }

    suspend fun getFavorites(): List<FavoriteItem> = withContext(Dispatchers.IO) {
        favoriteDao.getAllFavorites().map { it.toFavoriteItem() }
    }

    suspend fun deleteFavorite(id: Long) {
        withContext(Dispatchers.IO) {
            favoriteDao.delete(id)
        }
    }

    fun pickRandomClip(
        videos: List<VideoItem>,
        clipDurationSeconds: Int,
        excludeUris: List<Uri> = emptyList(),
    ): ClipSelection? {
        if (videos.isEmpty()) return null

        val excludeSet = excludeUris.toSet()
        val pool = if (excludeSet.isNotEmpty() && videos.size > excludeSet.size) {
            videos.filter { it.uri !in excludeSet }
        } else {
            videos
        }

        val video = pool.random()
        val clipMs = clipDurationSeconds * 1000L
        val maxStart = (video.durationMs - clipMs).coerceAtLeast(0L)
        val startMs = if (maxStart > 0L) Random.nextLong(0L, maxStart + 1) else 0L

        return ClipSelection(video = video, startPositionMs = startMs)
    }

    fun pickRandomVideo(
        videos: List<VideoItem>,
        excludeUris: List<Uri> = emptyList(),
    ): VideoItem? {
        if (videos.isEmpty()) return null

        val excludeSet = excludeUris.toSet()
        val pool = if (excludeSet.isNotEmpty() && videos.size > excludeSet.size) {
            videos.filter { it.uri !in excludeSet }
        } else {
            videos
        }

        return pool.random()
    }

    private fun VideoEntity.toVideoItem() = VideoItem(
        uri = Uri.parse(uri),
        displayName = displayName,
        durationMs = durationMs,
        width = width,
        height = height,
    )

    private fun FavoriteEntity.toFavoriteItem() = FavoriteItem(
        id = id,
        videoUri = Uri.parse(videoUri),
        timestampMs = timestampMs,
        savedAt = Date(savedAt),
        displayName = displayName,
        durationMs = durationMs
    )
}
