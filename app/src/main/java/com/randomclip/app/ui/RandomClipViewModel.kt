package com.randomclip.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.randomclip.app.data.SettingsRepository
import com.randomclip.app.data.VideoRepository
import com.randomclip.app.data.VideoScanner
import com.randomclip.app.model.AppSettings
import com.randomclip.app.model.ClipSelection
import com.randomclip.app.model.VideoItem
import com.randomclip.app.player.VideoPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val settings: AppSettings = AppSettings(),
    val videos: List<VideoItem> = emptyList(),
    val currentClip: ClipSelection? = null,
    val isLoading: Boolean = false,
    val showSettings: Boolean = false,
    val statusMessage: String? = null,
    val awaitingManualAdvance: Boolean = false,
)

class RandomClipViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val videoRepository = VideoRepository(application, VideoScanner(application))
    val playerManager = VideoPlayerManager(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var clipTimerJob: Job? = null
    private var settingsCollectJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                onClipFinished()
            }
        }
    }

    init {
        playerManager.player.addListener(playerListener)
        settingsCollectJob = viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                playerManager.applySettings(
                    muted = !settings.soundEnabled,
                    speed = settings.playbackSpeed,
                )
                val folder = settings.folderUri?.let(Uri::parse)
                if (folder != null && _uiState.value.videos.isEmpty()) {
                    loadVideos(folder)
                }
            }
        }
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.setFolderUri(uri.toString())
            loadVideos(uri, forceRefresh = true)
        }
    }

    fun refresh() {
        val folderUri = _uiState.value.settings.folderUri ?: return
        viewModelScope.launch {
            loadVideos(Uri.parse(folderUri), forceRefresh = true)
            playRandomClip()
        }
    }

    fun toggleSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun updateClipDuration(seconds: Int) {
        viewModelScope.launch { settingsRepository.setClipDuration(seconds) }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun updateAutoAdvance(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoAdvance(enabled) }
    }

    fun updatePlaybackSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setPlaybackSpeed(speed) }
    }

    fun onManualAdvance() {
        if (_uiState.value.awaitingManualAdvance || !_uiState.value.settings.autoAdvance) {
            playRandomClip()
        }
    }

    private suspend fun loadVideos(folderUri: Uri, forceRefresh: Boolean = false) {
        restorePersistedPermission(folderUri)
        _uiState.update { it.copy(isLoading = true, statusMessage = "Scanne Videos…") }
        val videos = videoRepository.getVideos(folderUri, forceRefresh)
        _uiState.update {
            it.copy(
                videos = videos,
                isLoading = false,
                statusMessage = if (videos.isEmpty()) {
                    "Keine Videos im Ordner gefunden"
                } else {
                    null
                },
            )
        }
        if (videos.isNotEmpty() && _uiState.value.currentClip == null) {
            playRandomClip()
        }
    }

    private fun playRandomClip() {
        val state = _uiState.value
        val selection = videoRepository.pickRandomClip(
            videos = state.videos,
            clipDurationSeconds = state.settings.clipDurationSeconds,
            excludeUri = state.currentClip?.video?.uri,
        ) ?: return

        _uiState.update {
            it.copy(
                currentClip = selection,
                awaitingManualAdvance = false,
                statusMessage = selection.video.displayName,
            )
        }

        playerManager.playStagedOr(selection)
        scheduleNextClip(selection)
        stageFollowingClip(selection)
    }

    private fun stageFollowingClip(current: ClipSelection) {
        val next = videoRepository.pickRandomClip(
            videos = _uiState.value.videos,
            clipDurationSeconds = _uiState.value.settings.clipDurationSeconds,
            excludeUri = current.video.uri,
        ) ?: return
        playerManager.stageNextClip(next)
    }

    private fun scheduleNextClip(selection: ClipSelection) {
        clipTimerJob?.cancel()
        val durationMs = _uiState.value.settings.clipDurationSeconds * 1000L
        val endMs = (selection.startPositionMs + durationMs)
            .coerceAtMost(selection.video.durationMs)

        clipTimerJob = viewModelScope.launch {
            while (true) {
                val pos = playerManager.player.currentPosition
                if (pos >= endMs - 50) break
                delay(100)
            }
            onClipFinished()
        }
    }

    private fun onClipFinished() {
        clipTimerJob?.cancel()
        playerManager.player.pause()

        if (_uiState.value.settings.autoAdvance) {
            playRandomClip()
        } else {
            _uiState.update { it.copy(awaitingManualAdvance = true) }
        }
    }

    private fun restorePersistedPermission(folderUri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                folderUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Permission may already be granted or revoked by the user.
        }
    }

    override fun onCleared() {
        clipTimerJob?.cancel()
        settingsCollectJob?.cancel()
        playerManager.player.removeListener(playerListener)
        playerManager.release()
        super.onCleared()
    }
}
