package com.randomclip.app

import android.content.Intent
import android.content.pm.ActivityInfo
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
import com.randomclip.app.ui.GameModeViewModel
import com.randomclip.app.ui.RandomClipViewModel
import com.randomclip.app.ui.Screen
import com.randomclip.app.ui.screens.DashboardScreen
import com.randomclip.app.ui.screens.FavoritesSheet
import com.randomclip.app.ui.screens.GameModePlayerScreen
import com.randomclip.app.ui.screens.GeneralSettingsScreen
import com.randomclip.app.ui.screens.SettingsScreen
import com.randomclip.app.ui.screens.VideoPlayerScreen
import com.randomclip.app.ui.theme.RandomClipTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: RandomClipViewModel by viewModels()
    private val gameModeViewModel: GameModeViewModel by viewModels()

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

            LaunchedEffect(gameModeState.isActive) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (gameModeState.isActive) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            LaunchedEffect(uiState.settings.lockPortrait, gameModeState.isActive) {
                if (gameModeState.isActive) return@LaunchedEffect
                requestedOrientation = if (uiState.settings.lockPortrait) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            DisposableEffect(lifecycleOwner, gameModeState.isActive) {
                val observer = LifecycleEventObserver { _, event ->
                    if (gameModeState.isActive) return@LifecycleEventObserver
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

            BackHandler(enabled = gameModeState.isActive) {
                gameModeViewModel.exit()
            }

            BackHandler(
                enabled = !gameModeState.isActive && uiState.currentScreen != Screen.DASHBOARD,
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
                if (gameModeState.isActive) {
                    GameModePlayerScreen(
                        uiState = gameModeState,
                        player = gameModeViewModel.player,
                        onTap = { gameModeViewModel.onTap() },
                        onExit = { gameModeViewModel.exit() },
                    )
                } else {
                    when (uiState.currentScreen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            onStartPlayback = {
                                viewModel.navigateTo(Screen.PLAYER)
                                viewModel.skipToNext()
                            },
                            onStartGameMode = { gameModeViewModel.start() },
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
                                recreate()
                            },
                            onBack = { viewModel.navigateBack() },
                        )
                    }
                }
            }
        }
    }
}
