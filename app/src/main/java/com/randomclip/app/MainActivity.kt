package com.randomclip.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randomclip.app.ui.RandomClipViewModel
import com.randomclip.app.ui.screens.FavoritesSheet
import com.randomclip.app.ui.screens.SettingsSheet
import com.randomclip.app.ui.screens.VideoPlayerScreen
import com.randomclip.app.ui.theme.RandomClipTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RandomClipViewModel by viewModels()

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
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(uiState.settings.lockPortrait) {
                requestedOrientation = if (uiState.settings.lockPortrait) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
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

            LaunchedEffect(Unit) {
                viewModel.revealOverlayControls()
            }

            RandomClipTheme {
                VideoPlayerScreen(
                    uiState = uiState,
                    playerManager = viewModel.playerManager,
                    onOpenSettings = { viewModel.toggleSettings(true) },
                    onOpenFavorites = { viewModel.toggleFavorites(true) },
                    onRefresh = { viewModel.refresh() },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onStop = { viewModel.stopClip() },
                    onSkipNext = { viewModel.skipToNext() },
                    onSkipPrevious = { viewModel.playPreviousClip() },
                    onFavorite = { viewModel.favoriteCurrentMoment() },
                    onToggleDisplayMode = { viewModel.toggleDisplayMode() },
                    onManualAdvance = { viewModel.onManualAdvance() },
                    onRevealControls = { viewModel.revealOverlayControls() },
                )

                SettingsSheet(
                    visible = uiState.showSettings,
                    settings = uiState.settings,
                    onDismiss = { viewModel.toggleSettings(false) },
                    onPickFolder = { folderPicker.launch(null) },
                    onRemoveFolder = { viewModel.removeFolder(it) },
                    onClipDurationChange = viewModel::updateClipDuration,
                    onSoundChange = viewModel::updateSoundEnabled,
                    onAutoAdvanceChange = viewModel::updateAutoAdvance,
                    onPlaybackSpeedChange = viewModel::updatePlaybackSpeed,
                    onDisplayModeChange = viewModel::updateDisplayMode,
                    onLockPortraitChange = viewModel::updateLockPortrait,
                    onAvoidRepeatsChange = viewModel::updateAvoidRepeats,
                    onPauseOnLockChange = viewModel::updatePauseOnLock,
                )
                
                FavoritesSheet(
                    visible = uiState.showFavorites,
                    favorites = uiState.favorites,
                    onDismiss = { viewModel.toggleFavorites(false) },
                    onFavoriteSelected = { viewModel.playFavorite(it) },
                    onDeleteFavorite = { viewModel.deleteFavorite(it) }
                )
            }
        }
    }
}
