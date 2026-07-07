package com.randomclip.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.randomclip.app.model.GameSettings
import com.randomclip.app.model.GameVideoDisplayMode
import com.randomclip.app.model.GameVideoPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.gameSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "game_mode_settings",
)

class GameSettingsRepository(private val context: Context) {

    val settings: Flow<GameSettings> = context.gameSettingsDataStore.data.map { prefs ->
        GameSettings(soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true)
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.gameSettingsDataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun loadVideoPreferences(videoUri: String): GameVideoPreferences {
        val raw = context.gameSettingsDataStore.data.first()[KEY_VIDEO_PREFS] ?: "{}"
        return parseFromJson(raw, videoUri)
    }

    suspend fun setVideoDisplayMode(videoUri: String, mode: GameVideoDisplayMode) {
        updateVideoEntry(videoUri) { it.put("displayMode", mode.name) }
    }

    suspend fun setVideoSoundEnabled(videoUri: String, enabled: Boolean?) {
        updateVideoEntry(videoUri) { entry ->
            if (enabled == null) {
                entry.remove("soundEnabled")
            } else {
                entry.put("soundEnabled", enabled)
            }
        }
    }

    suspend fun setVideoSegment(videoUri: String, startMs: Long, endMs: Long) {
        updateVideoEntry(videoUri) { entry ->
            entry.put("segmentStartMs", startMs)
            entry.put("segmentEndMs", endMs)
        }
    }

    suspend fun setVideoFlipped(videoUri: String, flipped: Boolean) {
        updateVideoEntry(videoUri) { it.put("videoFlipped", flipped) }
    }

    suspend fun setLandscapeGameEnabled(videoUri: String, enabled: Boolean) {
        updateVideoEntry(videoUri) { it.put("landscapeGameEnabled", enabled) }
    }

    suspend fun setSegmentLoopEnabled(videoUri: String, enabled: Boolean) {
        updateVideoEntry(videoUri) { it.put("segmentLoopEnabled", enabled) }
    }

    private suspend fun updateVideoEntry(
        videoUri: String,
        block: (JSONObject) -> Unit,
    ) {
        context.gameSettingsDataStore.edit { prefs ->
            val root = JSONObject(prefs[KEY_VIDEO_PREFS] ?: "{}")
            val entry = root.optJSONObject(videoUri) ?: JSONObject()
            block(entry)
            root.put(videoUri, entry)
            prefs[KEY_VIDEO_PREFS] = root.toString()
        }
    }

    private fun parseFromJson(raw: String, videoUri: String): GameVideoPreferences {
        return try {
            val root = JSONObject(raw)
            val entry = root.optJSONObject(videoUri) ?: return GameVideoPreferences()
            val displayMode = entry.optString("displayMode", GameVideoDisplayMode.AUTO.name)
                .let { name ->
                    runCatching { GameVideoDisplayMode.valueOf(name) }
                        .getOrDefault(GameVideoDisplayMode.AUTO)
                }
            val sound = if (entry.has("soundEnabled")) entry.getBoolean("soundEnabled") else null
            val segmentStart = entry.optLong("segmentStartMs", -1L).takeIf { it >= 0L }
            val segmentEnd = entry.optLong("segmentEndMs", -1L).takeIf { it >= 0L }
            val flipped = entry.optBoolean("videoFlipped", false)
            val landscapeGame = entry.optBoolean("landscapeGameEnabled", false)
            val segmentLoop = entry.optBoolean("segmentLoopEnabled", true)
            GameVideoPreferences(
                displayMode = displayMode,
                soundEnabled = sound,
                segmentStartMs = segmentStart,
                segmentEndMs = segmentEnd,
                videoFlipped = flipped,
                landscapeGameEnabled = landscapeGame,
                segmentLoopEnabled = segmentLoop,
            )
        } catch (_: Exception) {
            GameVideoPreferences()
        }
    }

    companion object {
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("game_sound_enabled")
        private val KEY_VIDEO_PREFS = stringPreferencesKey("game_video_prefs_json")
    }
}
