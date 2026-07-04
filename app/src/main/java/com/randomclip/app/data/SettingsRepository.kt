package com.randomclip.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.randomclip.app.model.AppSettings
import com.randomclip.app.model.VideoDisplayMode
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
            folderUris = prefs[KEY_FOLDER_URIS] ?: emptySet<String>(),
            displayMode = prefs[KEY_DISPLAY_MODE]?.let { stored ->
                runCatching { VideoDisplayMode.valueOf(stored) }
                    .getOrDefault(VideoDisplayMode.VERTICAL_FULLSCREEN)
            } ?: VideoDisplayMode.VERTICAL_FULLSCREEN,
            lockPortrait = prefs[KEY_LOCK_PORTRAIT] ?: false,
            avoidRepeats = prefs[KEY_AVOID_REPEATS] ?: true,
            pauseOnLock = prefs[KEY_PAUSE_ON_LOCK] ?: true,
            randomMode = prefs[KEY_RANDOM_MODE] ?: false,
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

    suspend fun addFolderUri(uri: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[KEY_FOLDER_URIS] ?: emptySet<String>()
            prefs[KEY_FOLDER_URIS] = current + uri
        }
    }

    suspend fun removeFolderUri(uri: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[KEY_FOLDER_URIS] ?: emptySet<String>()
            prefs[KEY_FOLDER_URIS] = current - uri
        }
    }

    suspend fun setDisplayMode(mode: VideoDisplayMode) {
        context.settingsDataStore.edit { it[KEY_DISPLAY_MODE] = mode.name }
    }

    suspend fun setLockPortrait(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_LOCK_PORTRAIT] = enabled }
    }

    suspend fun setAvoidRepeats(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_AVOID_REPEATS] = enabled }
    }

    suspend fun setPauseOnLock(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_PAUSE_ON_LOCK] = enabled }
    }

    suspend fun setRandomMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_RANDOM_MODE] = enabled }
    }

    companion object {
        private val KEY_CLIP_DURATION = intPreferencesKey("clip_duration_seconds")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_AUTO_ADVANCE = booleanPreferencesKey("auto_advance")
        private val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val KEY_FOLDER_URIS = stringSetPreferencesKey("folder_uris")
        private val KEY_DISPLAY_MODE = stringPreferencesKey("display_mode")
        private val KEY_LOCK_PORTRAIT = booleanPreferencesKey("lock_portrait")
        private val KEY_AVOID_REPEATS = booleanPreferencesKey("avoid_repeats")
        private val KEY_PAUSE_ON_LOCK = booleanPreferencesKey("pause_on_lock")
        private val KEY_RANDOM_MODE = booleanPreferencesKey("random_mode")
    }
}
