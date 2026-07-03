package com.randomclip.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.randomclip.app.model.AppSettings
import com.randomclip.app.model.VideoDisplayMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    visible: Boolean,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onPickFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onClipDurationChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onAutoAdvanceChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onDisplayModeChange: (VideoDisplayMode) -> Unit,
    onLockPortraitChange: (Boolean) -> Unit,
    onAvoidRepeatsChange: (Boolean) -> Unit,
    onPauseOnLockChange: (Boolean) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Einstellungen",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingRow(label = "Clip-Länge: ${settings.clipDurationSeconds}s") {
                    Slider(
                        value = settings.clipDurationSeconds.toFloat(),
                        onValueChange = { onClipDurationChange(it.roundToInt()) },
                        valueRange = 2f..15f,
                        steps = 12,
                    )
                }
            }

            item {
                SettingRow(label = "Ton") {
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = onSoundChange,
                    )
                }
            }

            item {
                SettingRow(label = "Auto-Advance") {
                    Switch(
                        checked = settings.autoAdvance,
                        onCheckedChange = onAutoAdvanceChange,
                    )
                }
            }

            item {
                SettingRow(label = "Geschwindigkeit: ${"%.1f".format(settings.playbackSpeed)}x") {
                    Slider(
                        value = settings.playbackSpeed,
                        onValueChange = onPlaybackSpeedChange,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                    )
                }
            }

            item {
                SettingRow(label = "Anzeige: ${displayModeLabel(settings.displayMode)}") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onDisplayModeChange(VideoDisplayMode.VERTICAL_FULLSCREEN) },
                            enabled = settings.displayMode != VideoDisplayMode.VERTICAL_FULLSCREEN,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Vollbild")
                        }
                        Button(
                            onClick = { onDisplayModeChange(VideoDisplayMode.FIT) },
                            enabled = settings.displayMode != VideoDisplayMode.FIT,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Fit")
                        }
                    }
                }
            }

            item {
                SettingRow(label = "Orientierung sperren (Hochformat)") {
                    Switch(
                        checked = settings.lockPortrait,
                        onCheckedChange = onLockPortraitChange,
                    )
                }
            }

            item {
                SettingRow(label = "Wiederholungen vermeiden") {
                    Switch(
                        checked = settings.avoidRepeats,
                        onCheckedChange = onAvoidRepeatsChange,
                    )
                }
            }

            item {
                SettingRow(label = "Bei Bildschirmsperre pausieren") {
                    Switch(
                        checked = settings.pauseOnLock,
                        onCheckedChange = onPauseOnLockChange,
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video-Ordner",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onPickFolder) {
                        Text("Hinzufügen")
                    }
                }
            }

            items(settings.folderUris.toList()) { uri ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uri.substringAfterLast("%2F").substringAfterLast("/"),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveFolder(uri) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Entfernen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}
