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
                val duration = readDurationMs(file.uri)
                if (duration > 0L) {
                    out += VideoItem(
                        uri = file.uri,
                        displayName = file.name ?: file.uri.lastPathSegment.orEmpty(),
                        durationMs = duration,
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

    private fun readDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
}
