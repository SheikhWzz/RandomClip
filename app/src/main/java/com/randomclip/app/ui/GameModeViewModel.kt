package com.randomclip.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.randomclip.app.R
import com.randomclip.app.data.SettingsRepository
import com.randomclip.app.data.VideoRepository
import com.randomclip.app.data.VideoScanner
import com.randomclip.app.model.VideoItem
import com.randomclip.app.player.GameModePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameModeUiState(
    val isActive: Boolean = false,
    val energy: Float = 0f,
    val clickCount: Int = 0,
    val comboBoostActive: Boolean = false,
    val milestonePulse: Float = 0f,
    val glowIntensity: Float = 0f,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class GameModeViewModel(application: Application) : AndroidViewModel(application) {

    val player = GameModePlayer(application)

    private val videoRepository = VideoRepository(application, VideoScanner(application))
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(GameModeUiState())
    val uiState: StateFlow<GameModeUiState> = _uiState.asStateFlow()

    private var energyLoopJob: Job? = null
    private var comboBoostJob: Job? = null
    private val recentClickTimes = ArrayDeque<Long>()

    fun start() {
        if (_uiState.value.isActive || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val folderUris = settingsRepository.settings.first().folderUris
            if (folderUris.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = getApplication<Application>().getString(R.string.no_folders_selected),
                    )
                }
                return@launch
            }

            folderUris.forEach { uriString ->
                restorePersistedPermission(Uri.parse(uriString))
            }

            val videos = withContext(Dispatchers.IO) {
                videoRepository.getVideos(folderUris.map(Uri::parse).toSet())
            }
            if (videos.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = getApplication<Application>().getString(R.string.no_videos_found),
                    )
                }
                return@launch
            }

            val video = videos.random()
            player.load(video.uri)

            _uiState.value = GameModeUiState(
                isActive = true,
                isLoading = false,
            )
            startEnergyLoop()
        }
    }

    fun exit() {
        energyLoopJob?.cancel()
        comboBoostJob?.cancel()
        recentClickTimes.clear()
        player.pause()
        _uiState.value = GameModeUiState()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onTap() {
        if (!_uiState.value.isActive) return

        val now = System.currentTimeMillis()
        recentClickTimes.addLast(now)
        while (recentClickTimes.isNotEmpty() && now - recentClickTimes.first() > COMBO_WINDOW_MS) {
            recentClickTimes.removeFirst()
        }

        val comboTriggered = recentClickTimes.size >= COMBO_MIN_CLICKS
        if (comboTriggered) {
            activateComboBoost()
        }

        val newClickCount = _uiState.value.clickCount + 1
        val milestoneHit = MILESTONE_CLICKS.any { it == newClickCount }
        val newEnergy = (_uiState.value.energy + CLICK_ENERGY_BOOST).coerceAtMost(1f)

        performHaptic(milestoneHit)

        _uiState.update { state ->
            state.copy(
                energy = newEnergy,
                clickCount = newClickCount,
                milestonePulse = if (milestoneHit) 1f else state.milestonePulse,
                glowIntensity = newEnergy.coerceIn(0f, 1f),
            )
        }
        applyPlaybackSpeed()
    }

    private fun startEnergyLoop() {
        energyLoopJob?.cancel()
        energyLoopJob = viewModelScope.launch {
            while (isActive && _uiState.value.isActive) {
                delay(ENERGY_TICK_MS)
                decayEnergy()
                decayMilestonePulse()
                updateGlow()
                applyPlaybackSpeed()
            }
        }
    }

    private fun decayEnergy() {
        _uiState.update { state ->
            if (state.energy <= 0f) return@update state
            state.copy(energy = (state.energy - ENERGY_DECAY_PER_TICK).coerceAtLeast(0f))
        }
    }

    private fun decayMilestonePulse() {
        _uiState.update { state ->
            if (state.milestonePulse <= 0f) return@update state
            state.copy(milestonePulse = (state.milestonePulse - MILESTONE_PULSE_DECAY).coerceAtLeast(0f))
        }
    }

    private fun updateGlow() {
        _uiState.update { state ->
            val base = state.energy
            val pulse = state.milestonePulse * 0.5f
            val combo = if (state.comboBoostActive) 0.25f else 0f
            state.copy(glowIntensity = (base + pulse + combo).coerceIn(0f, 1f))
        }
    }

    private fun activateComboBoost() {
        comboBoostJob?.cancel()
        _uiState.update { it.copy(comboBoostActive = true) }
        performHaptic(strong = true)
        comboBoostJob = viewModelScope.launch {
            delay(COMBO_BOOST_DURATION_MS)
            _uiState.update { it.copy(comboBoostActive = false) }
            applyPlaybackSpeed()
        }
    }

    private fun applyPlaybackSpeed() {
        val state = _uiState.value
        val baseSpeed = state.energy * MAX_ENERGY_SPEED
        val speed = when {
            state.energy <= 0f -> 0f
            state.comboBoostActive -> maxOf(baseSpeed, COMBO_BOOST_SPEED)
            else -> baseSpeed
        }
        player.applySpeed(speed)
    }

    private fun performHaptic(milestone: Boolean = false, strong: Boolean = false) {
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = ContextCompat.getSystemService(context, VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(context, Vibrator::class.java)
        } ?: return

        val duration = when {
            strong || milestone -> 40L
            else -> 15L
        }
        val amplitude = when {
            strong -> 200
            milestone -> 160
            else -> 80
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    override fun onCleared() {
        energyLoopJob?.cancel()
        comboBoostJob?.cancel()
        player.release()
        super.onCleared()
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

    companion object {
        private const val CLICK_ENERGY_BOOST = 0.15f
        private const val ENERGY_DECAY_PER_TICK = 0.02f
        private const val ENERGY_TICK_MS = 100L
        private const val MAX_ENERGY_SPEED = 1.6f
        private const val COMBO_BOOST_SPEED = 2.0f
        private const val COMBO_WINDOW_MS = 300L
        private const val COMBO_MIN_CLICKS = 3
        private const val COMBO_BOOST_DURATION_MS = 500L
        private const val MILESTONE_PULSE_DECAY = 0.06f
        private val MILESTONE_CLICKS = setOf(10, 25, 50, 100, 200)
    }
}
