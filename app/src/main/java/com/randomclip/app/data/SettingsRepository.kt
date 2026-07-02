package com.randomclip.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.randomclip.app.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "random_clip_settings",
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            clipDurationSeconds = prefs[KEY_CLIP_DURATION] ?: 5,
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: false,
            autoAdvance = prefs[KEY_AUTO_ADVANCE] ?: true,
            playbackSpeed = prefs[KEY_PLAYBACK_SPEED] ?: 1.0f,
            folderUri = prefs[KEY_FOLDER_URI],
        )
    }

    suspend fun setClipDuration(seconds: Int) {
        context.settingsDataStore.edit { it[KEY_CLIP_DURATION] = seconds.coerceIn(2, 15) }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setAutoAdvance(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_AUTO_ADVANCE] = enabled }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.settingsDataStore.edit { it[KEY_PLAYBACK_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setFolderUri(uri: String?) {
        context.settingsDataStore.edit {
            if (uri == null) {
                it.remove(KEY_FOLDER_URI)
            } else {
                it[KEY_FOLDER_URI] = uri
            }
        }
    }

    companion object {
        private val KEY_CLIP_DURATION = intPreferencesKey("clip_duration_seconds")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_AUTO_ADVANCE = booleanPreferencesKey("auto_advance")
        private val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val KEY_FOLDER_URI = stringPreferencesKey("folder_uri")
    }
}
