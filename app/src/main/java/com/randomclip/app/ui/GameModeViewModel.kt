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
import com.randomclip.app.data.GameSettingsRepository
import com.randomclip.app.data.SettingsRepository
import com.randomclip.app.data.VideoRepository
import com.randomclip.app.data.VideoScanner
import com.randomclip.app.model.GameVideoDisplayMode
import com.randomclip.app.model.VideoItem
import com.randomclip.app.player.GameModePlayer
import com.randomclip.app.ui.components.SegmentDragHandle
import com.randomclip.app.ui.components.TapSparkle
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

private data class GameTapEvent(val time: Long, val pointerCount: Int)

enum class GameModeScreen {
    NONE,
    HUB,
    SETTINGS,
    PREVIEW,
    PLAYING,
}

enum class GameIntroPhase {
    NONE,
    READY,
    GO,
    DONE,
    GAME_OVER,
}

data class GameModeUiState(
    val screen: GameModeScreen = GameModeScreen.NONE,
    val videos: List<VideoItem> = emptyList(),
    val selectedVideo: VideoItem? = null,
    val videoDisplayMode: GameVideoDisplayMode = GameVideoDisplayMode.AUTO,
    val soundEnabled: Boolean = true,
    val videoFlipped: Boolean = false,
    val segmentStartMs: Long = 0L,
    val segmentEndMs: Long = 0L,
    val landscapeGameEnabled: Boolean = false,
    val segmentLoopEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val introPhase: GameIntroPhase = GameIntroPhase.NONE,
    val energy: Float = 0f,
    val clickCount: Int = 0,
    val comboBoostActive: Boolean = false,
    val milestonePulse: Float = 0f,
    val glowIntensity: Float = 0f,
    val clickPulse: Float = 0f,
    val overdriveGlow: Float = 0f,
    val tapSparkles: List<TapSparkle> = emptyList(),
    val score: Int = 0,
) {
    val isInGameFlow: Boolean
        get() = screen != GameModeScreen.NONE

    val isPlaying: Boolean
        get() = screen == GameModeScreen.PLAYING && introPhase == GameIntroPhase.DONE
}

class GameModeViewModel(application: Application) : AndroidViewModel(application) {

    val player = GameModePlayer(application)

    private val videoRepository = VideoRepository(application, VideoScanner(application))
    private val settingsRepository = SettingsRepository(application)
    private val gameSettingsRepository = GameSettingsRepository(application)

    private val _uiState = MutableStateFlow(GameModeUiState())
    val uiState: StateFlow<GameModeUiState> = _uiState.asStateFlow()

    private var energyLoopJob: Job? = null
    private var comboBoostJob: Job? = null
    private var introJob: Job? = null
    private var previewLoopJob: Job? = null
    private var gameplayLoopJob: Job? = null
    private val recentTaps = ArrayDeque<GameTapEvent>()
    private var scoreAccumulator = 0f
    private var isScrubbingSegment = false
    private var smoothedPlaybackSpeed = 0f
    private var nextSparkleId = 0L

    init {
        viewModelScope.launch {
            gameSettingsRepository.settings.collect { settings ->
                _uiState.update { state ->
                    if (state.screen == GameModeScreen.PREVIEW || state.screen == GameModeScreen.PLAYING) {
                        state
                    } else {
                        state.copy(soundEnabled = settings.soundEnabled)
                    }
                }
            }
        }
    }

    fun openHub() {
        if (_uiState.value.screen != GameModeScreen.NONE) return
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
            folderUris.forEach { restorePersistedPermission(Uri.parse(it)) }
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
            val globalSound = gameSettingsRepository.settings.first().soundEnabled
            _uiState.update {
                it.copy(
                    screen = GameModeScreen.HUB,
                    videos = videos,
                    isLoading = false,
                    soundEnabled = globalSound,
                )
            }
        }
    }

    fun closeGameFlow() {
        stopPreviewLoop()
        stopGameplayLoop()
        energyLoopJob?.cancel()
        comboBoostJob?.cancel()
        introJob?.cancel()
        recentTaps.clear()
        scoreAccumulator = 0f
        isScrubbingSegment = false
        smoothedPlaybackSpeed = 0f
        player.pause()
        _uiState.value = GameModeUiState()
    }

    fun openSettings() {
        _uiState.update { it.copy(screen = GameModeScreen.SETTINGS) }
    }

    fun finishGameAndReturnToHub() {
        stopGameplayLoop()
        energyLoopJob?.cancel()
        comboBoostJob?.cancel()
        introJob?.cancel()
        recentTaps.clear()
        player.pause()
        _uiState.update {
            it.copy(
                screen = GameModeScreen.HUB,
                selectedVideo = null,
                landscapeGameEnabled = false,
                introPhase = GameIntroPhase.NONE,
                energy = 0f,
                clickCount = 0,
                score = 0,
                tapSparkles = emptyList(),
            )
        }
    }

    fun backToHub() {
        stopPreviewLoop()
        player.pause()
        _uiState.update {
            it.copy(
                screen = GameModeScreen.HUB,
                selectedVideo = null,
                landscapeGameEnabled = false,
                introPhase = GameIntroPhase.NONE,
            )
        }
    }

    fun setGlobalSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            gameSettingsRepository.setSoundEnabled(enabled)
            _uiState.update { it.copy(soundEnabled = enabled) }
        }
    }

    fun selectVideo(video: VideoItem) {
        viewModelScope.launch {
            val prefs = gameSettingsRepository.loadVideoPreferences(video.uri.toString())
            val sound = prefs.soundEnabled
                ?: gameSettingsRepository.settings.first().soundEnabled
            val (startMs, endMs) = resolveSegment(prefs.segmentStartMs, prefs.segmentEndMs, video.durationMs)
            _uiState.update {
                it.copy(
                    screen = GameModeScreen.PREVIEW,
                    selectedVideo = video,
                    videoDisplayMode = normalizeDisplayMode(prefs.displayMode, video),
                    soundEnabled = sound,
                    videoFlipped = prefs.videoFlipped,
                    segmentStartMs = startMs,
                    segmentEndMs = endMs,
                    landscapeGameEnabled = prefs.landscapeGameEnabled,
                    segmentLoopEnabled = prefs.segmentLoopEnabled,
                )
            }
            startPreviewLoop()
        }
    }

    fun togglePreviewOrientationLayout() {
        val video = _uiState.value.selectedVideo ?: return
        val next = when (_uiState.value.videoDisplayMode) {
            GameVideoDisplayMode.VERTICAL_FULLSCREEN -> GameVideoDisplayMode.FIT
            else -> GameVideoDisplayMode.VERTICAL_FULLSCREEN
        }
        viewModelScope.launch {
            gameSettingsRepository.setVideoDisplayMode(video.uri.toString(), next)
            _uiState.update { it.copy(videoDisplayMode = next) }
        }
    }

    fun togglePreviewSound() {
        val video = _uiState.value.selectedVideo ?: return
        val next = !_uiState.value.soundEnabled
        viewModelScope.launch {
            gameSettingsRepository.setVideoSoundEnabled(video.uri.toString(), next)
            _uiState.update { it.copy(soundEnabled = next) }
            player.setMuted(!next)
        }
    }

    fun toggleLandscapeGame() {
        val video = _uiState.value.selectedVideo ?: return
        val next = !_uiState.value.landscapeGameEnabled
        viewModelScope.launch {
            gameSettingsRepository.setLandscapeGameEnabled(video.uri.toString(), next)
            _uiState.update { it.copy(landscapeGameEnabled = next) }
        }
    }

    fun toggleSegmentLoop() {
        val video = _uiState.value.selectedVideo ?: return
        val next = !_uiState.value.segmentLoopEnabled
        viewModelScope.launch {
            gameSettingsRepository.setSegmentLoopEnabled(video.uri.toString(), next)
            _uiState.update { it.copy(segmentLoopEnabled = next) }
        }
    }

    fun toggleVideoFlipped() {
        val video = _uiState.value.selectedVideo ?: return
        val next = !_uiState.value.videoFlipped
        viewModelScope.launch {
            gameSettingsRepository.setVideoFlipped(video.uri.toString(), next)
            _uiState.update { it.copy(videoFlipped = next) }
        }
    }

    fun onSegmentHandleChange(handle: SegmentDragHandle, positionMs: Long, isDragging: Boolean) {
        val video = _uiState.value.selectedVideo ?: return
        val duration = video.durationMs
        if (duration <= 0L) return

        val state = _uiState.value
        val (newStart, newEnd) = when (handle) {
            SegmentDragHandle.START -> {
                val start = positionMs.coerceIn(0L, state.segmentEndMs - MIN_SEGMENT_MS)
                start to state.segmentEndMs
            }
            SegmentDragHandle.END -> {
                val end = positionMs.coerceIn(state.segmentStartMs + MIN_SEGMENT_MS, duration)
                state.segmentStartMs to end
            }
        }

        isScrubbingSegment = isDragging
        _uiState.update { it.copy(segmentStartMs = newStart, segmentEndMs = newEnd) }
        player.exoPlayer.seekTo(if (handle == SegmentDragHandle.START) newStart else newEnd)

        if (isDragging) {
            stopPreviewLoop()
            if (player.exoPlayer.isPlaying) player.pause()
        } else {
            viewModelScope.launch {
                val uri = video.uri.toString()
                gameSettingsRepository.setVideoSegment(uri, newStart, newEnd)
                startPreviewLoop()
            }
        }
    }

    fun startGameFromPreview() {
        val video = _uiState.value.selectedVideo ?: return
        val state = _uiState.value
        stopPreviewLoop()
        scoreAccumulator = 0f
        smoothedPlaybackSpeed = 0f
        viewModelScope.launch {
            player.load(video.uri, startMs = state.segmentStartMs)
            player.setMuted(!state.soundEnabled)
            _uiState.update {
                it.copy(
                    screen = GameModeScreen.PLAYING,
                    introPhase = GameIntroPhase.READY,
                    energy = 0f,
                    clickCount = 0,
                    comboBoostActive = false,
                    milestonePulse = 0f,
                    glowIntensity = 0f,
                    clickPulse = 0f,
                    overdriveGlow = 0f,
                    tapSparkles = emptyList(),
                    score = 0,
                )
            }
            runIntroSequence()
        }
    }

    private fun runIntroSequence() {
        introJob?.cancel()
        introJob = viewModelScope.launch {
            delay(INTRO_READY_MS)
            _uiState.update { it.copy(introPhase = GameIntroPhase.GO) }
            delay(INTRO_GO_MS)
            _uiState.update { it.copy(introPhase = GameIntroPhase.DONE) }
            startGameplayLoop()
            startEnergyLoop()
        }
    }

    private fun startPreviewLoop() {
        if (isScrubbingSegment) return
        val state = _uiState.value
        val video = state.selectedVideo ?: return
        stopPreviewLoop()

        val startMs = state.segmentStartMs
        val endMs = state.segmentEndMs
        if (endMs <= startMs) return

        player.load(video.uri, startMs = startMs)
        player.setMuted(!state.soundEnabled)
        player.playSegmentLoop(startMs, endMs)

        previewLoopJob = viewModelScope.launch {
            while (isActive) {
                delay(50)
                if (isScrubbingSegment) continue
                if (player.exoPlayer.currentPosition >= endMs - 80) {
                    player.exoPlayer.seekTo(startMs)
                }
            }
        }
    }

    private fun startGameplayLoop() {
        stopGameplayLoop()
        val state = _uiState.value
        val startMs = state.segmentStartMs
        val endMs = state.segmentEndMs
        if (endMs <= startMs) return

        gameplayLoopJob = viewModelScope.launch {
            while (isActive && _uiState.value.screen == GameModeScreen.PLAYING) {
                delay(50)
                if (_uiState.value.introPhase != GameIntroPhase.DONE) continue
                if (player.exoPlayer.currentPosition >= endMs - 80) {
                    if (_uiState.value.segmentLoopEnabled) {
                        player.exoPlayer.seekTo(startMs)
                    } else {
                        endGameSession()
                        break
                    }
                }
            }
        }
    }

    private fun endGameSession() {
        stopGameplayLoop()
        energyLoopJob?.cancel()
        comboBoostJob?.cancel()
        player.pause()
        performHaptic(strong = true)
        _uiState.update { it.copy(introPhase = GameIntroPhase.GAME_OVER) }
    }

    private fun stopPreviewLoop() {
        previewLoopJob?.cancel()
        previewLoopJob = null
    }

    private fun stopGameplayLoop() {
        gameplayLoopJob?.cancel()
        gameplayLoopJob = null
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onTap(normalizedX: Float, normalizedY: Float, pointerCount: Int = 1) {
        if (!_uiState.value.isPlaying) return

        val now = System.currentTimeMillis()
        val fingers = pointerCount.coerceAtLeast(1)
        recentTaps.addLast(GameTapEvent(now, fingers))
        while (recentTaps.isNotEmpty() && now - recentTaps.first().time > COMBO_WINDOW_MS) {
            recentTaps.removeFirst()
        }

        if (recentTaps.size >= COMBO_MIN_CLICKS) {
            activateComboBoost()
        }

        val rapidTaps = recentTaps.filter { now - it.time <= RAPID_CLICK_WINDOW_MS }
        val rapidClicks = rapidTaps.size
        val interval = averageTapInterval(now)
        val newClickCount = _uiState.value.clickCount + 1
        val milestoneHit = MILESTONE_CLICKS.any { it == newClickCount }
        val boost = clickEnergyBoost(now, fingers, interval)
        val rawEnergy = (_uiState.value.energy + boost).coerceAtMost(1f)
        val newEnergy = if (_uiState.value.energy <= 0f) {
            NORMAL_ENERGY_LEVEL
        } else {
            rawEnergy
        }

        val twoFingerTap = fingers >= 2 || rapidTaps.any { it.pointerCount >= 2 }
        val hyperTap = interval != null && interval < FAST_TAP_INTERVAL_MS
        val isMaxEnergy = newEnergy >= MAX_ENERGY_SPARKLE_THRESHOLD
        val sparkleIntensity = when {
            twoFingerTap -> 1f
            isMaxEnergy -> 0.95f
            hyperTap -> 0.82f
            interval != null && interval <= NORMAL_TAP_INTERVAL_MS -> 0.48f
            else -> 0.34f
        }

        val overdrive = when {
            twoFingerTap -> 1f
            hyperTap && rapidClicks >= 4 -> 0.85f
            hyperTap -> 0.6f
            else -> _uiState.value.overdriveGlow
        }

        performHaptic(milestoneHit)

        _uiState.update { state ->
            val newSparkle = TapSparkle(
                id = nextSparkleId++,
                x = normalizedX.coerceIn(0f, 1f),
                y = normalizedY.coerceIn(0f, 1f),
                life = 1f,
                intensity = sparkleIntensity,
            )
            val sparkles = (state.tapSparkles + newSparkle).takeLast(MAX_SPARKLES)
            state.copy(
                energy = newEnergy,
                clickCount = newClickCount,
                milestonePulse = if (milestoneHit) 1f else state.milestonePulse,
                clickPulse = 1f,
                overdriveGlow = overdrive,
                tapSparkles = sparkles,
                glowIntensity = (newEnergy + 0.35f + overdrive * 0.25f).coerceIn(0f, 1f),
            )
        }
        applyPlaybackSpeed()
    }

    private fun averageTapInterval(now: Long): Float? {
        val window = recentTaps.filter { now - it.time <= TAP_RATE_WINDOW_MS }
        if (window.size < 2) return null
        return (window.last().time - window.first().time) / (window.size - 1).toFloat()
    }

    private fun clickEnergyBoost(now: Long, pointerCount: Int, intervalMs: Float?): Float {
        val rapidTaps = recentTaps.filter { now - it.time <= RAPID_CLICK_WINDOW_MS }
        val maxPointers = maxOf(pointerCount, rapidTaps.maxOfOrNull { it.pointerCount } ?: 1)
        return when {
            maxPointers >= 2 -> TWO_FINGER_ENERGY_BOOST
            intervalMs != null && intervalMs < FAST_TAP_INTERVAL_MS -> FAST_TAP_ENERGY_BOOST
            intervalMs != null && intervalMs <= NORMAL_TAP_INTERVAL_MS -> NORMAL_TAP_ENERGY_BOOST
            else -> GENTLE_TAP_ENERGY_BOOST
        }
    }

    private fun startEnergyLoop() {
        energyLoopJob?.cancel()
        energyLoopJob = viewModelScope.launch {
            while (isActive && _uiState.value.screen == GameModeScreen.PLAYING) {
                delay(ENERGY_TICK_MS)
                decayEnergy()
                decayMilestonePulse()
                decayClickPulse()
                decayOverdriveGlow()
                decayTapSparkles()
                updateGlow()
                tickScore()
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

    private fun decayClickPulse() {
        _uiState.update { state ->
            if (state.clickPulse <= 0f) return@update state
            state.copy(clickPulse = (state.clickPulse - CLICK_PULSE_DECAY).coerceAtLeast(0f))
        }
    }

    private fun decayOverdriveGlow() {
        _uiState.update { state ->
            if (state.overdriveGlow <= 0f) return@update state
            state.copy(overdriveGlow = (state.overdriveGlow - OVERDRIVE_DECAY).coerceAtLeast(0f))
        }
    }

    private fun decayTapSparkles() {
        _uiState.update { state ->
            if (state.tapSparkles.isEmpty()) return@update state
            val updated = state.tapSparkles
                .map { it.copy(life = it.life - SPARKLE_DECAY) }
                .filter { it.life > 0.05f }
            state.copy(tapSparkles = updated)
        }
    }

    private fun updateGlow() {
        _uiState.update { state ->
            val base = state.energy * 0.55f
            val pulse = state.milestonePulse * 0.32f
            val combo = if (state.comboBoostActive) 0.18f else 0f
            val click = state.clickPulse * 0.4f
            val overdrive = state.overdriveGlow * 0.5f
            state.copy(glowIntensity = (base + pulse + combo + click + overdrive).coerceIn(0f, 1f))
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
        advanceSmoothedSpeed()
        val state = _uiState.value
        player.applySpeed(smoothedPlaybackSpeed, muted = !state.soundEnabled)
    }

    private fun advanceSmoothedSpeed() {
        val target = targetPlaybackSpeed(_uiState.value)
        val factor = if (target < smoothedPlaybackSpeed) SPEED_SMOOTH_DOWN else SPEED_SMOOTH_UP
        smoothedPlaybackSpeed += (target - smoothedPlaybackSpeed) * factor
        if (target <= 0f && smoothedPlaybackSpeed < MIN_MOVING_SPEED) {
            smoothedPlaybackSpeed = 0f
        }
    }

    private fun targetPlaybackSpeed(state: GameModeUiState): Float {
        if (state.energy < MIN_ENERGY_TO_PLAY) return 0f
        val delta = state.energy - NORMAL_ENERGY_LEVEL
        val speed = if (delta >= 0f) {
            val linear = (delta / (1f - NORMAL_ENERGY_LEVEL)).coerceIn(0f, 1f)
            val curved = linear * linear * (3f - 2f * linear)
            val eased = curved * curved
            BASE_PLAYBACK_SPEED + eased * (MAX_ENERGY_SPEED - BASE_PLAYBACK_SPEED)
        } else {
            val spanBelow = NORMAL_ENERGY_LEVEL - MIN_ENERGY_TO_PLAY
            BASE_PLAYBACK_SPEED + (delta / spanBelow) * (BASE_PLAYBACK_SPEED - MIN_IDLE_SPEED)
        }
        return if (state.comboBoostActive) {
            maxOf(speed, COMBO_BOOST_SPEED)
        } else {
            speed
        }
    }

    private fun currentPlaybackSpeed(): Float = smoothedPlaybackSpeed

    private fun tickScore() {
        if (!_uiState.value.isPlaying) return
        val speed = currentPlaybackSpeed()
        if (speed <= GameModePlayer.MIN_MOVING_SPEED) return

        scoreAccumulator += speed * (ENERGY_TICK_MS / 1000f) * SCORE_POINTS_PER_SPEED_SECOND
        val wholePoints = scoreAccumulator.toInt()
        if (wholePoints > 0) {
            scoreAccumulator -= wholePoints
            _uiState.update { it.copy(score = it.score + wholePoints) }
        }
    }

    private fun resolveSegment(
        savedStart: Long?,
        savedEnd: Long?,
        durationMs: Long,
    ): Pair<Long, Long> {
        if (durationMs <= 0L) return 0L to 0L
        val end = savedEnd?.coerceIn(MIN_SEGMENT_MS, durationMs) ?: durationMs
        val start = savedStart?.coerceIn(0L, end - MIN_SEGMENT_MS) ?: 0L
        return if (end - start < MIN_SEGMENT_MS) {
            0L to durationMs.coerceAtLeast(MIN_SEGMENT_MS)
        } else {
            start to end
        }
    }

    private fun normalizeDisplayMode(
        mode: GameVideoDisplayMode,
        video: VideoItem,
    ): GameVideoDisplayMode {
        return when (mode) {
            GameVideoDisplayMode.AUTO -> {
                if (video.height > 0 && video.width > 0 && video.height >= video.width) {
                    GameVideoDisplayMode.VERTICAL_FULLSCREEN
                } else {
                    GameVideoDisplayMode.FIT
                }
            }
            GameVideoDisplayMode.VERTICAL_FULLSCREEN,
            GameVideoDisplayMode.FIT,
            -> mode
        }
    }

    private fun performHaptic(milestone: Boolean = false, strong: Boolean = false) {
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getSystemService(context, VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(context, Vibrator::class.java)
        } ?: return

        val duration = if (strong || milestone) 40L else 15L
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
        stopPreviewLoop()
        stopGameplayLoop()
        energyLoopJob?.cancel()
        comboBoostJob?.cancel()
        introJob?.cancel()
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
        }
    }

    companion object {
        private const val MIN_SEGMENT_MS = 1_500L
        private const val MIN_ENERGY_TO_PLAY = 0.05f
        private const val NORMAL_ENERGY_LEVEL = 0.44f
        private const val BASE_PLAYBACK_SPEED = 1.0f
        private const val MIN_IDLE_SPEED = 0.3f
        private const val MIN_MOVING_SPEED = 0.08f
        private const val SPEED_SMOOTH_DOWN = 0.1f
        private const val SPEED_SMOOTH_UP = 0.2f
        private const val GENTLE_TAP_ENERGY_BOOST = 0.045f
        private const val NORMAL_TAP_ENERGY_BOOST = 0.065f
        private const val FAST_TAP_ENERGY_BOOST = 0.11f
        private const val TWO_FINGER_ENERGY_BOOST = 0.2f
        private const val TAP_RATE_WINDOW_MS = 700L
        private const val NORMAL_TAP_INTERVAL_MS = 520f
        private const val FAST_TAP_INTERVAL_MS = 170f
        private const val CLICK_PULSE_DECAY = 0.14f
        private const val RAPID_CLICK_WINDOW_MS = 500L
        private const val ENERGY_DECAY_PER_TICK = 0.042f
        private const val ENERGY_TICK_MS = 100L
        private const val MAX_ENERGY_SPEED = 2.0f
        private const val COMBO_BOOST_SPEED = 2.0f
        private const val COMBO_WINDOW_MS = 280L
        private const val COMBO_MIN_CLICKS = 4
        private const val COMBO_BOOST_DURATION_MS = 400L
        private const val MILESTONE_PULSE_DECAY = 0.05f
        private const val INTRO_READY_MS = 600L
        private const val INTRO_GO_MS = 500L
        private const val SCORE_POINTS_PER_SPEED_SECOND = 10f
        private const val MAX_ENERGY_SPARKLE_THRESHOLD = 0.94f
        private const val OVERDRIVE_DECAY = 0.09f
        private const val SPARKLE_DECAY = 0.07f
        private const val MAX_SPARKLES = 10
        private val MILESTONE_CLICKS = setOf(10, 25, 50, 100, 200)
    }
}
