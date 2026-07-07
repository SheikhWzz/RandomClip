package com.randomclip.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.randomclip.app.data.SettingsRepository
import com.randomclip.app.data.VideoRepository
import com.randomclip.app.data.VideoScanner
import com.randomclip.app.R
import com.randomclip.app.model.AppSettings
import com.randomclip.app.model.ClipSelection
import com.randomclip.app.model.FavoriteItem
import com.randomclip.app.model.VideoDisplayMode
import com.randomclip.app.model.VideoItem
import com.randomclip.app.player.VideoPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val settings: AppSettings = AppSettings(),
    val videos: List<VideoItem> = emptyList(),
    val currentClip: ClipSelection? = null,
    val isLoading: Boolean = false,
    val showSettings: Boolean = false,
    val showFavorites: Boolean = false,
    val favorites: List<FavoriteItem> = emptyList(),
    val statusMessage: String? = null,
    val awaitingManualAdvance: Boolean = false,
    val isPlaying: Boolean = false,
    val showOverlayControls: Boolean = true,
    val currentScreen: Screen = Screen.DASHBOARD,
    val isFavorite: Boolean = false,
    val isFavoritesPlaylistMode: Boolean = false,
    val previousScreen: Screen = Screen.DASHBOARD,
)

enum class Screen {
    DASHBOARD,
    PLAYER,
    SETTINGS,
    GENERAL_SETTINGS,
    FAVORITES,
}

private enum class TransientStatus {
    FAVORITE_REMOVED,
    MOMENT_SAVED,
    PLAYLIST_EXITED,
}

class RandomClipViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val videoRepository = VideoRepository(application, VideoScanner(application))
    val playerManager = VideoPlayerManager(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var clipTimerJob: Job? = null
    private var overlayHideJob: Job? = null
    private var settingsCollectJob: Job? = null
    private var clipTransitionInProgress = false

    private val history = mutableListOf<ClipSelection>()
    private var historyIndex = -1
    private val antiRepeatQueue = mutableListOf<Uri>()
    private var favoritesPlaylistIndex = 0
    private var activeTransientStatus: TransientStatus? = null
    private val screenBackStack = mutableListOf(Screen.DASHBOARD)

    private fun syncScreenFromStack() {
        val current = screenBackStack.last()
        val previous = screenBackStack.getOrNull(screenBackStack.lastIndex - 1) ?: Screen.DASHBOARD
        _uiState.update { it.copy(currentScreen = current, previousScreen = previous) }
    }

    private fun pushScreen(screen: Screen) {
        if (screenBackStack.last() == screen) return
        screenBackStack.add(screen)
        syncScreenFromStack()
    }

    init {
        playerManager.onActiveClipEnded = { onClipFinished() }
        playerManager.onIsPlayingChanged = { playing ->
            _uiState.update { it.copy(isPlaying = playing) }
        }
        playerManager.onPlayerError = { uri, _ ->
            handlePlaybackError(uri)
        }

        settingsCollectJob = viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val oldSettings = _uiState.value.settings
                _uiState.update { it.copy(settings = settings) }
                playerManager.applySettings(
                    muted = !settings.soundEnabled,
                    speed = settings.playbackSpeed,
                )
                
                if (settings.folderUris != oldSettings.folderUris) {
                    loadVideos(settings.folderUris.map { Uri.parse(it) }.toSet())
                }
            }
        }
        
        loadFavorites()
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.addFolderUri(uri.toString())
            // loadVideos will be triggered by settings collection
        }
    }

    fun removeFolder(uri: String) {
        viewModelScope.launch {
            settingsRepository.removeFolderUri(uri)
        }
    }

    fun refresh() {
        val uris = _uiState.value.settings.folderUris
        if (uris.isEmpty()) return
        viewModelScope.launch {
            loadVideos(uris.map { Uri.parse(it) }.toSet(), forceRefresh = true)
            playRandomClip()
        }
    }

    fun navigateTo(screen: Screen) {
        pushScreen(screen)
    }

    fun navigateBack(): Boolean {
        if (screenBackStack.size <= 1) return false
        screenBackStack.removeAt(screenBackStack.lastIndex)
        syncScreenFromStack()
        return true
    }

    fun navigateToDashboard() {
        screenBackStack.clear()
        screenBackStack.add(Screen.DASHBOARD)
        syncScreenFromStack()
    }

    fun toggleSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun toggleFavorites(show: Boolean) {
        _uiState.update { it.copy(showFavorites = show) }
        if (show) loadFavorites()
    }

    fun updateRandomMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRandomMode(enabled) }
    }

    fun startFavoritesPlaylist() {
        val favorites = _uiState.value.favorites
        if (favorites.isEmpty()) return
        
        favoritesPlaylistIndex = 0
        _uiState.update { it.copy(isFavoritesPlaylistMode = true) }
        if (_uiState.value.currentScreen != Screen.PLAYER) {
            pushScreen(Screen.PLAYER)
        }
        playFavoriteClip(favorites[0])
    }

    fun exitPlaylistMode() {
        if (!_uiState.value.isFavoritesPlaylistMode) return

        _uiState.update {
            it.copy(
                isFavoritesPlaylistMode = false,
                statusMessage = getApplication<Application>().getString(R.string.playlist_mode_exited),
            )
        }
        activeTransientStatus = TransientStatus.PLAYLIST_EXITED
        viewModelScope.launch {
            delay(1000)
            if (activeTransientStatus == TransientStatus.PLAYLIST_EXITED) {
                _uiState.update { it.copy(statusMessage = null) }
                activeTransientStatus = null
            }
        }
    }

    private fun playNextInFavoritesPlaylist() {
        if (!_uiState.value.isFavoritesPlaylistMode) return
        
        val favorites = _uiState.value.favorites
        if (favorites.isEmpty()) return // Don't exit playlist mode, just do nothing
        
        favoritesPlaylistIndex = (favoritesPlaylistIndex + 1) % favorites.size
        playFavoriteClip(favorites[favoritesPlaylistIndex])
    }

    private fun playPreviousInFavoritesPlaylist() {
        if (!_uiState.value.isFavoritesPlaylistMode) return
        
        val favorites = _uiState.value.favorites
        if (favorites.isEmpty()) return // Don't exit playlist mode, just do nothing
        
        favoritesPlaylistIndex = (favoritesPlaylistIndex - 1 + favorites.size) % favorites.size
        playFavoriteClip(favorites[favoritesPlaylistIndex])
    }

    fun loadFavorites() {
        viewModelScope.launch {
            val favs = videoRepository.getFavorites()
            _uiState.update { it.copy(favorites = favs) }
        }
    }

    fun favoriteCurrentMoment() {
        val clip = _uiState.value.currentClip ?: return
        val pos = playerManager.player.currentPosition
        val currentVideoUri = clip.video.uri
        
        // Check if this video is already a favorite
        val existingFavorite = _uiState.value.favorites.find { it.videoUri == currentVideoUri }
        
        if (existingFavorite != null) {
            _uiState.update { it.copy(isFavorite = false) }
            viewModelScope.launch {
                videoRepository.deleteFavorite(existingFavorite.id)
                loadFavorites()
                _uiState.update {
                    it.copy(
                        statusMessage = getApplication<Application>().getString(R.string.favorite_removed),
                    )
                }
                activeTransientStatus = TransientStatus.FAVORITE_REMOVED
                delay(1500)
                if (activeTransientStatus == TransientStatus.FAVORITE_REMOVED) {
                    _uiState.update { it.copy(statusMessage = clip.video.displayName) }
                    activeTransientStatus = null
                }
            }
        } else {
            _uiState.update { it.copy(isFavorite = true) }
            viewModelScope.launch {
                videoRepository.saveFavorite(clip.video, pos)
                loadFavorites()
                _uiState.update {
                    it.copy(
                        statusMessage = getApplication<Application>().getString(R.string.moment_saved),
                    )
                }
                activeTransientStatus = TransientStatus.MOMENT_SAVED
                delay(1500)
                if (activeTransientStatus == TransientStatus.MOMENT_SAVED) {
                    _uiState.update { it.copy(statusMessage = clip.video.displayName) }
                    activeTransientStatus = null
                }
            }
        }
    }

    fun playFavorite(favorite: FavoriteItem) {
        if (_uiState.value.currentScreen != Screen.PLAYER) {
            pushScreen(Screen.PLAYER)
        }
        playFavoriteClip(favorite)
    }

    private fun playFavoriteClip(favorite: FavoriteItem) {
        val video = VideoItem(
            uri = favorite.videoUri,
            displayName = favorite.displayName,
            durationMs = favorite.durationMs
        )
        val selection = ClipSelection(video, favorite.timestampMs)
        
        history.add(selection)
        historyIndex = history.size - 1
        
        _uiState.update {
            it.copy(
                currentClip = selection,
                showFavorites = false,
                isPlaying = true,
                isFavorite = true,
                statusMessage = video.displayName,
            )
        }
        playerManager.playClip(selection)
        scheduleNextClip(selection, _uiState.value.settings.clipDurationSeconds)
        revealOverlayControls()
    }

    fun deleteFavorite(id: Long) {
        viewModelScope.launch {
            videoRepository.deleteFavorite(id)
            loadFavorites()
        }
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

    fun updateDisplayMode(mode: VideoDisplayMode) {
        viewModelScope.launch { settingsRepository.setDisplayMode(mode) }
    }

    fun updateLockPortrait(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLockPortrait(enabled) }
    }

    fun updateAvoidRepeats(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAvoidRepeats(enabled) }
    }

    fun updatePauseOnLock(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPauseOnLock(enabled) }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch { settingsRepository.setLanguage(languageCode) }
    }

    fun toggleDisplayMode() {
        val next = when (_uiState.value.settings.displayMode) {
            VideoDisplayMode.VERTICAL_FULLSCREEN -> VideoDisplayMode.FIT
            VideoDisplayMode.FIT -> VideoDisplayMode.VERTICAL_FULLSCREEN
        }
        updateDisplayMode(next)
        revealOverlayControls()
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
        revealOverlayControls()
    }

    fun stopClip() {
        playerManager.stopClip()
        _uiState.update { it.copy(isPlaying = false) }
        revealOverlayControls()
    }

    fun skipToNext() {
        revealOverlayControls()
        if (_uiState.value.isFavoritesPlaylistMode) {
            playNextInFavoritesPlaylist()
        } else {
            playRandomClip()
        }
    }

    fun playPreviousClip() {
        if (_uiState.value.isFavoritesPlaylistMode) {
            playPreviousInFavoritesPlaylist()
        } else {
            if (historyIndex > 0) {
                historyIndex--
                val selection = history[historyIndex]
                
                _uiState.update {
                    it.copy(
                        currentClip = selection,
                        awaitingManualAdvance = false,
                        statusMessage = selection.video.displayName,
                        isPlaying = true,
                    )
                }
                playerManager.playClip(selection)
                scheduleNextClip(selection, _uiState.value.settings.clipDurationSeconds)
                revealOverlayControls()
            }
        }
    }

    fun onManualAdvance() {
        if (_uiState.value.awaitingManualAdvance || !_uiState.value.settings.autoAdvance) {
            skipToNext()
        }
    }

    fun revealOverlayControls() {
        overlayHideJob?.cancel()
        _uiState.update { it.copy(showOverlayControls = true) }
        overlayHideJob = viewModelScope.launch {
            delay(OVERLAY_HIDE_MS)
            _uiState.update { it.copy(showOverlayControls = false) }
        }
    }

    private fun handlePlaybackError(uri: Uri) {
        viewModelScope.launch {
            videoRepository.markAsUnplayable(uri)
            // Remove from current list to avoid picking it again in this session
            _uiState.update { it.copy(videos = it.videos.filter { v -> v.uri != uri }) }
            playRandomClip()
        }
    }

    private suspend fun loadVideos(folderUris: Set<Uri>, forceRefresh: Boolean = false) {
        if (folderUris.isEmpty()) {
            _uiState.update {
                it.copy(
                    videos = emptyList(),
                    statusMessage = getApplication<Application>().getString(R.string.no_folders_selected),
                )
            }
            return
        }

        folderUris.forEach { restorePersistedPermission(it) }

        _uiState.update {
            it.copy(
                isLoading = true,
                statusMessage = getApplication<Application>().getString(R.string.scanning_videos),
            )
        }
        val videos = videoRepository.getVideos(folderUris, forceRefresh)
        _uiState.update {
            it.copy(
                videos = videos,
                isLoading = false,
                statusMessage = if (videos.isEmpty()) {
                    getApplication<Application>().getString(R.string.no_videos_found)
                } else {
                    null
                },
            )
        }
        // Don't auto-start player when loading videos - only start when user enters player screen
    }

    private fun playRandomClip() {
        if (clipTransitionInProgress) return

        val state = _uiState.value
        if (state.videos.isEmpty()) return
        
        val excludeUris = if (state.settings.avoidRepeats) antiRepeatQueue else emptyList<Uri>()

        // Random Mode: Generate random clip duration and speed
        val (clipDuration, playbackSpeed) = if (state.settings.randomMode) {
            val randomDuration = Random.nextInt(2, 16)
            val randomSpeed = Random.nextFloat() * 2.5f + 0.5f
            randomDuration to randomSpeed
        } else {
            state.settings.clipDurationSeconds to state.settings.playbackSpeed
        }

        val selection = videoRepository.pickRandomClip(
            videos = state.videos,
            clipDurationSeconds = clipDuration,
            excludeUris = excludeUris,
        ) ?: return

        clipTransitionInProgress = true
        
        // Update History
        history.add(selection)
        if (history.size > 20) history.removeAt(0)
        historyIndex = history.size - 1
        
        // Update Anti-Repeat
        if (state.settings.avoidRepeats) {
            antiRepeatQueue.add(selection.video.uri)
            if (antiRepeatQueue.size > 15) antiRepeatQueue.removeAt(0)
        }

        // Check if current video is a favorite
        val isVideoFavorite = state.favorites.any { it.videoUri == selection.video.uri }

        _uiState.update {
            it.copy(
                currentClip = selection,
                isFavorite = isVideoFavorite,
                awaitingManualAdvance = false,
                statusMessage = selection.video.displayName,
                isPlaying = true,
            )
        }

        // Apply playback speed
        playerManager.applySettings(
            muted = !state.settings.soundEnabled,
            speed = playbackSpeed,
        )

        playerManager.playClip(selection)
        scheduleNextClip(selection, clipDuration)
        preloadFollowingClip(selection)
        revealOverlayControls()
        clipTransitionInProgress = false
    }

    private fun preloadFollowingClip(current: ClipSelection) {
        val state = _uiState.value
        val excludeUris = if (state.settings.avoidRepeats) antiRepeatQueue + current.video.uri else listOf(current.video.uri)
        
        val next = videoRepository.pickRandomClip(
            videos = state.videos,
            clipDurationSeconds = state.settings.clipDurationSeconds,
            excludeUris = excludeUris,
        ) ?: return
        playerManager.preloadClip(next)
    }

    private fun scheduleNextClip(selection: ClipSelection, clipDurationSeconds: Int) {
        clipTimerJob?.cancel()
        val durationMs = clipDurationSeconds * 1000L
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
        // Don't auto-advance if we're not in the player screen anymore
        if (_uiState.value.currentScreen != Screen.PLAYER) return
        
        if (clipTransitionInProgress) return
        clipTransitionInProgress = true
        clipTimerJob?.cancel()
        playerManager.pause()

        if (_uiState.value.settings.autoAdvance) {
            clipTransitionInProgress = false
            if (_uiState.value.isFavoritesPlaylistMode) {
                playNextInFavoritesPlaylist()
            } else {
                playRandomClip()
            }
        } else {
            _uiState.update {
                it.copy(
                    awaitingManualAdvance = true,
                    isPlaying = false,
                )
            }
            clipTransitionInProgress = false
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
        overlayHideJob?.cancel()
        settingsCollectJob?.cancel()
        playerManager.onActiveClipEnded = null
        playerManager.onIsPlayingChanged = null
        playerManager.release()
        super.onCleared()
    }

    companion object {
        private const val OVERLAY_HIDE_MS = 2500L
    }
}
