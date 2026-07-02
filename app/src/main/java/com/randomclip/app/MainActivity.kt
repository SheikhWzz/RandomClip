package com.randomclip.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randomclip.app.ui.RandomClipViewModel
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

            RandomClipTheme {
                VideoPlayerScreen(
                    uiState = uiState,
                    playerManager = viewModel.playerManager,
                    onOpenSettings = { viewModel.toggleSettings(true) },
                    onRefresh = { viewModel.refresh() },
                    onManualAdvance = { viewModel.onManualAdvance() },
                )

                SettingsSheet(
                    visible = uiState.showSettings,
                    settings = uiState.settings,
                    onDismiss = { viewModel.toggleSettings(false) },
                    onPickFolder = { folderPicker.launch(null) },
                    onClipDurationChange = viewModel::updateClipDuration,
                    onSoundChange = viewModel::updateSoundEnabled,
                    onAutoAdvanceChange = viewModel::updateAutoAdvance,
                    onPlaybackSpeedChange = viewModel::updatePlaybackSpeed,
                )
            }
        }
    }
}
