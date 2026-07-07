package com.randomclip.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randomclip.app.ui.GameIntroPhase
import com.randomclip.app.ui.GameModeScreen
import com.randomclip.app.ui.GameModeViewModel
import com.randomclip.app.ui.components.SegmentDragHandle
import com.randomclip.app.ui.game.GameOrientationResolver
import com.randomclip.app.ui.RandomClipViewModel
import com.randomclip.app.ui.Screen
import com.randomclip.app.ui.screens.DashboardScreen
import com.randomclip.app.ui.screens.FavoritesSheet
import com.randomclip.app.ui.screens.GameHubScreen
import com.randomclip.app.ui.screens.GameModePlayerScreen
import com.randomclip.app.ui.screens.GameSettingsScreen
import com.randomclip.app.ui.screens.GameVideoPreviewScreen
import com.randomclip.app.ui.screens.GeneralSettingsScreen
import com.randomclip.app.ui.screens.SettingsScreen
import com.randomclip.app.ui.screens.VideoPlayerScreen
import com.randomclip.app.ui.theme.RandomClipTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: RandomClipViewModel by viewModels()
    private val gameModeViewModel: GameModeViewModel by viewModels()
    private var frozenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    private fun refreshFrozenOrientation() {
        frozenOrientation = GameOrientationResolver.orientationFromConfiguration(
            configuration = resources.configuration,
            displayRotation = display?.rotation,
        )
    }

    private fun applyGameFlowOrientation() {
        val state = gameModeViewModel.uiState.value
        if (!state.isInGameFlow) return
        requestedOrientation = GameOrientationResolver.resolveForGameFlow(
            context = this,
            screen = state.screen,
            landscapeGameEnabled = state.landscapeGameEnabled,
            frozenOrientation = frozenOrientation,
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshFrozenOrientation()
        applyGameFlowOrientation()
    }

    override fun onResume() {
        super.onResume()
        refreshFrozenOrientation()
        applyGameFlowOrientation()
    }

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.onFolderSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshFrozenOrientation()
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val gameModeState by gameModeViewModel.uiState.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(gameModeState.errorMessage) {
                gameModeState.errorMessage?.let { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    gameModeViewModel.clearError()
                }
            }

            LaunchedEffect(
                gameModeState.isInGameFlow,
                gameModeState.screen,
                gameModeState.landscapeGameEnabled,
                uiState.settings.lockPortrait,
            ) {
                refreshFrozenOrientation()
                requestedOrientation = if (gameModeState.isInGameFlow) {
                    GameOrientationResolver.resolveForGameFlow(
                        context = this@MainActivity,
                        screen = gameModeState.screen,
                        landscapeGameEnabled = gameModeState.landscapeGameEnabled,
                        frozenOrientation = frozenOrientation,
                    )
                } else {
                    GameOrientationResolver.resolveForNormalApp(
                        lockPortrait = uiState.settings.lockPortrait,
                    )
                }
            }

            LaunchedEffect(gameModeState.screen) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (gameModeState.screen == GameModeScreen.PLAYING) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            DisposableEffect(lifecycleOwner, gameModeState.isInGameFlow) {
                val observer = LifecycleEventObserver { _, event ->
                    if (gameModeState.isInGameFlow) return@LifecycleEventObserver
                    if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                        if (uiState.settings.pauseOnLock) {
                            viewModel.playerManager.pause()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            BackHandler(enabled = gameModeState.screen == GameModeScreen.PLAYING) {
                if (gameModeState.introPhase == GameIntroPhase.GAME_OVER) {
                    gameModeViewModel.finishGameAndReturnToHub()
                } else {
                    gameModeViewModel.closeGameFlow()
                }
            }

            BackHandler(enabled = gameModeState.screen == GameModeScreen.PREVIEW) {
                gameModeViewModel.backToHub()
            }

            BackHandler(enabled = gameModeState.screen == GameModeScreen.SETTINGS) {
                gameModeViewModel.backToHub()
            }

            BackHandler(enabled = gameModeState.screen == GameModeScreen.HUB) {
                gameModeViewModel.closeGameFlow()
            }

            BackHandler(
                enabled = !gameModeState.isInGameFlow && uiState.currentScreen != Screen.DASHBOARD,
            ) {
                when (uiState.currentScreen) {
                    Screen.PLAYER -> {
                        if (uiState.isFavoritesPlaylistMode) {
                            viewModel.exitPlaylistMode()
                        }
                        viewModel.navigateToDashboard()
                    }
                    else -> viewModel.navigateBack()
                }
            }

            RandomClipTheme {
                when (gameModeState.screen) {
                    GameModeScreen.PLAYING -> GameModePlayerScreen(
                        uiState = gameModeState,
                        player = gameModeViewModel.player,
                        onTap = { x, y, fingers -> gameModeViewModel.onTap(x, y, fingers) },
                        onBackToMenu = { gameModeViewModel.finishGameAndReturnToHub() },
                    )
                    GameModeScreen.PREVIEW -> {
                        val video = gameModeState.selectedVideo
                        if (video != null) {
                            GameVideoPreviewScreen(
                                video = video,
                                displayMode = gameModeState.videoDisplayMode,
                                soundEnabled = gameModeState.soundEnabled,
                                videoFlipped = gameModeState.videoFlipped,
                                landscapeGameEnabled = gameModeState.landscapeGameEnabled,
                                segmentLoopEnabled = gameModeState.segmentLoopEnabled,
                                segmentStartMs = gameModeState.segmentStartMs,
                                segmentEndMs = gameModeState.segmentEndMs,
                                player = gameModeViewModel.player,
                                onBack = { gameModeViewModel.backToHub() },
                                onToggleLandscapeGame = { gameModeViewModel.toggleLandscapeGame() },
                                onToggleSegmentLoop = { gameModeViewModel.toggleSegmentLoop() },
                                onToggleSound = { gameModeViewModel.togglePreviewSound() },
                                onToggleVideoFlipped = { gameModeViewModel.toggleVideoFlipped() },
                                onSegmentStartChange = { ms, dragging ->
                                    gameModeViewModel.onSegmentHandleChange(
                                        SegmentDragHandle.START,
                                        ms,
                                        dragging,
                                    )
                                },
                                onSegmentEndChange = { ms, dragging ->
                                    gameModeViewModel.onSegmentHandleChange(
                                        SegmentDragHandle.END,
                                        ms,
                                        dragging,
                                    )
                                },
                                onStartGame = { gameModeViewModel.startGameFromPreview() },
                            )
                        }
                    }
                    GameModeScreen.SETTINGS -> GameSettingsScreen(
                        soundEnabled = gameModeState.soundEnabled,
                        onSoundChange = { gameModeViewModel.setGlobalSoundEnabled(it) },
                        onBack = { gameModeViewModel.backToHub() },
                    )
                    GameModeScreen.HUB -> GameHubScreen(
                        videos = gameModeState.videos,
                        isLoading = gameModeState.isLoading,
                        onBack = { gameModeViewModel.closeGameFlow() },
                        onOpenSettings = { gameModeViewModel.openSettings() },
                        onVideoSelected = { gameModeViewModel.selectVideo(it) },
                    )
                    GameModeScreen.NONE -> when (uiState.currentScreen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            onStartPlayback = {
                                viewModel.navigateTo(Screen.PLAYER)
                                viewModel.skipToNext()
                            },
                            onStartGameMode = { gameModeViewModel.openHub() },
                            onOpenSettings = { viewModel.navigateTo(Screen.SETTINGS) },
                            onOpenGeneralSettings = { viewModel.navigateTo(Screen.GENERAL_SETTINGS) },
                            onOpenFavorites = { viewModel.navigateTo(Screen.FAVORITES) },
                        )
                        Screen.PLAYER -> VideoPlayerScreen(
                            uiState = uiState,
                            playerManager = viewModel.playerManager,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipToNext() },
                            onSkipPrevious = { viewModel.playPreviousClip() },
                            onFavorite = { viewModel.favoriteCurrentMoment() },
                            onOpenSettings = { viewModel.navigateTo(Screen.SETTINGS) },
                            onToggleDisplayMode = { viewModel.toggleDisplayMode() },
                            onToggleRandomMode = {
                                viewModel.updateRandomMode(!uiState.settings.randomMode)
                            },
                            onToggleLoopClip = { viewModel.toggleLoopClip() },
                            onBack = {
                                if (uiState.isFavoritesPlaylistMode) {
                                    viewModel.exitPlaylistMode()
                                }
                                viewModel.navigateToDashboard()
                            },
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            settings = uiState.settings,
                            onBack = { viewModel.navigateBack() },
                            onPickFolder = { folderPicker.launch(null) },
                            onRemoveFolder = { viewModel.removeFolder(it) },
                            onClipDurationChange = viewModel::updateClipDuration,
                            onSoundChange = viewModel::updateSoundEnabled,
                            onAutoAdvanceChange = viewModel::updateAutoAdvance,
                            onPlaybackSpeedChange = viewModel::updatePlaybackSpeed,
                            onAdvancedSpeedChange = viewModel::updatePlaybackSpeed,
                            onDisplayModeChange = viewModel::updateDisplayMode,
                            onLockPortraitChange = viewModel::updateLockPortrait,
                            onAvoidRepeatsChange = viewModel::updateAvoidRepeats,
                            onPauseOnLockChange = viewModel::updatePauseOnLock,
                            onRandomModeChange = viewModel::updateRandomMode,
                            onLoopClipChange = viewModel::updateLoopClip,
                            onOpenFavorites = { viewModel.navigateTo(Screen.FAVORITES) },
                        )
                        Screen.FAVORITES -> FavoritesSheet(
                            visible = true,
                            favorites = uiState.favorites,
                            onDismiss = { viewModel.navigateBack() },
                            onFavoriteSelected = { viewModel.playFavorite(it) },
                            onDeleteFavorite = { viewModel.deleteFavorite(it) },
                            onStartPlaylist = { viewModel.startFavoritesPlaylist() },
                        )
                        Screen.GENERAL_SETTINGS -> GeneralSettingsScreen(
                            selectedLanguage = uiState.settings.language,
                            onLanguageChange = { languageCode ->
                                if (languageCode == uiState.settings.language) return@GeneralSettingsScreen
                                viewModel.updateLanguage(languageCode)
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(languageCode),
                                )
                            },
                            onBack = { viewModel.navigateBack() },
                        )
                    }
                }
            }
        }
    }
}
