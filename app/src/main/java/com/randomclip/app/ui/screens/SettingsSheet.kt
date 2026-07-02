package com.randomclip.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.randomclip.app.model.AppSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    visible: Boolean,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onPickFolder: () -> Unit,
    onClipDurationChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onAutoAdvanceChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SettingRow(label = "Clip-Länge: ${settings.clipDurationSeconds}s") {
                Slider(
                    value = settings.clipDurationSeconds.toFloat(),
                    onValueChange = { onClipDurationChange(it.roundToInt()) },
                    valueRange = 2f..15f,
                    steps = 12,
                )
            }

            SettingRow(label = "Ton") {
                Switch(
                    checked = settings.soundEnabled,
                    onCheckedChange = onSoundChange,
                )
            }

            SettingRow(label = "Auto-Advance") {
                Switch(
                    checked = settings.autoAdvance,
                    onCheckedChange = onAutoAdvanceChange,
                )
            }

            SettingRow(label = "Geschwindigkeit: ${"%.1f".format(settings.playbackSpeed)}x") {
                Slider(
                    value = settings.playbackSpeed,
                    onValueChange = onPlaybackSpeedChange,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                )
            }

            Button(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (settings.folderUri == null) {
                        "Video-Ordner wählen"
                    } else {
                        "Anderen Ordner wählen"
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        content()
    }
}
