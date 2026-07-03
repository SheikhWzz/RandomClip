package com.randomclip.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.randomclip.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "ts", "flv", "wmv",
)

private data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
)

class VideoScanner(private val context: Context) {

    suspend fun scanFolder(folderUri: Uri): List<VideoItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val results = mutableListOf<VideoItem>()
        collectVideos(root, results)
        results
    }

    private fun collectVideos(dir: DocumentFile, out: MutableList<VideoItem>) {
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                collectVideos(file, out)
            } else if (isVideoFile(file)) {
                val metadata = readMetadata(file.uri)
                if (metadata.durationMs > 0L) {
                    out += VideoItem(
                        uri = file.uri,
                        displayName = file.name ?: file.uri.lastPathSegment.orEmpty(),
                        durationMs = metadata.durationMs,
                        width = metadata.width,
                        height = metadata.height,
                    )
                }
            }
        }
    }

    private fun isVideoFile(file: DocumentFile): Boolean {
        val mime = file.type
        if (mime?.startsWith("video/") == true) return true
        val ext = file.name
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?: return false
        return ext in VIDEO_EXTENSIONS ||
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.startsWith("video/") == true
    }

    private fun readMetadata(uri: Uri): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?: 0
            val (finalWidth, finalHeight) = if (rotation == 90 || rotation == 270) {
                height to width
            } else {
                width to height
            }
            VideoMetadata(durationMs, finalWidth, finalHeight)
        } catch (_: Exception) {
            VideoMetadata(0L, 0, 0)
        } finally {
            retriever.release()
        }
    }
}
