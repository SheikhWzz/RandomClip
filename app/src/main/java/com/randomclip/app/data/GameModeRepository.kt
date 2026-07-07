package com.randomclip.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.randomclip.app.model.GameModeData
import com.randomclip.app.model.RepSegment
import com.randomclip.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GameModeRepository(private val context: Context) {

    suspend fun loadForVideo(video: VideoItem, folderUris: Set<String>): GameModeData? =
        withContext(Dispatchers.IO) {
            loadFromParent(video)
                ?: loadFromFolderTrees(video, folderUris)
        }

    private fun loadFromParent(video: VideoItem): GameModeData? {
        val videoFile = DocumentFile.fromSingleUri(context, video.uri) ?: return null
        val parent = videoFile.parentFile ?: return null
        val jsonDoc = findJsonSibling(parent, video) ?: return null
        return readAndParse(jsonDoc, video.displayName)
    }

    private fun loadFromFolderTrees(video: VideoItem, folderUris: Set<String>): GameModeData? {
        for (folderUriString in folderUris) {
            val root = DocumentFile.fromTreeUri(context, Uri.parse(folderUriString)) ?: continue
            val jsonDoc = findJsonForVideo(root, video) ?: continue
            readAndParse(jsonDoc, video.displayName)?.let { return it }
        }
        return null
    }

    private fun findJsonForVideo(dir: DocumentFile, video: VideoItem): DocumentFile? {
        if (!dir.isDirectory) return null

        val files = dir.listFiles()
        val videoInDir = files.any { file ->
            !file.isDirectory && file.uri == video.uri
        } || files.any { file ->
            !file.isDirectory && namesMatch(file.name, video.displayName)
        }

        if (videoInDir) {
            findJsonSibling(dir, video)?.let { return it }
        }

        for (child in files) {
            if (child.isDirectory) {
                findJsonForVideo(child, video)?.let { return it }
            }
        }
        return null
    }

    private fun findJsonSibling(dir: DocumentFile, video: VideoItem): DocumentFile? {
        val stem = video.displayName.substringBeforeLast('.', video.displayName)
        dir.listFiles().firstOrNull { file ->
            !file.isDirectory && file.name?.equals("$stem.json", ignoreCase = true) == true
        }?.let { return it }

        return dir.listFiles().firstOrNull { file ->
            !file.isDirectory && file.name?.endsWith(".json", ignoreCase = true) == true &&
                readAndParse(file, video.displayName)?.let { matchesVideoFile(it, video) } == true
        }
    }

    private fun matchesVideoFile(data: GameModeData, video: VideoItem): Boolean {
        return namesMatch(data.videoFile, video.displayName) ||
            namesMatch(
                data.videoFile.substringBeforeLast('.', data.videoFile),
                video.displayName.substringBeforeLast('.', video.displayName),
            )
    }

    private fun readAndParse(jsonDoc: DocumentFile, fallbackVideoName: String): GameModeData? {
        val jsonText = context.contentResolver.openInputStream(jsonDoc.uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: return null
        return parseGameModeJson(jsonText, fallbackVideoName)
    }

    private fun namesMatch(fileName: String?, displayName: String): Boolean {
        if (fileName.isNullOrBlank()) return false
        return fileName == displayName || fileName.equals(displayName, ignoreCase = true)
    }

    private fun parseGameModeJson(jsonText: String, fallbackVideoName: String): GameModeData? {
        return try {
            val root = JSONObject(jsonText)
            val videoFile = root.optString("videoFile", fallbackVideoName)
            val repsArray = root.getJSONArray("reps")
            val reps = buildList {
                for (index in 0 until repsArray.length()) {
                    val rep = repsArray.getJSONObject(index)
                    val start = rep.getLong("start")
                    val end = rep.getLong("end")
                    if (end > start) {
                        add(RepSegment(startMs = start, endMs = end))
                    }
                }
            }
            if (reps.isEmpty()) null else GameModeData(videoFile = videoFile, reps = reps)
        } catch (_: Exception) {
            null
        }
    }
}
