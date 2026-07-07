package com.randomclip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.randomclip.app.R
import com.randomclip.app.model.AppSettings
import com.randomclip.app.model.VideoDisplayMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onPickFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onClipDurationChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onAutoAdvanceChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onAdvancedSpeedChange: (Float) -> Unit,
    onDisplayModeChange: (VideoDisplayMode) -> Unit,
    onLockPortraitChange: (Boolean) -> Unit,
    onAvoidRepeatsChange: (Boolean) -> Unit,
    onPauseOnLockChange: (Boolean) -> Unit,
    onRandomModeChange: (Boolean) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D0D),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color(0xFF0D0D0D),
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection(title = stringResource(R.string.clip_length)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.clip_length_seconds, settings.clipDurationSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                    Slider(
                        value = settings.clipDurationSeconds.toFloat(),
                        onValueChange = { onClipDurationChange(it.roundToInt()) },
                        valueRange = 2f..15f,
                        steps = 12,
                        enabled = !settings.randomMode,
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.playback_section)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingToggle(
                        label = stringResource(R.string.enable_sound),
                        checked = settings.soundEnabled,
                        onCheckedChange = onSoundChange,
                    )
                    SettingToggle(
                        label = stringResource(R.string.auto_advance),
                        checked = settings.autoAdvance,
                        onCheckedChange = onAutoAdvanceChange,
                    )
                    SettingToggle(
                        label = stringResource(R.string.anti_repeat),
                        checked = settings.avoidRepeats,
                        onCheckedChange = onAvoidRepeatsChange,
                    )
                    SettingToggle(
                        label = stringResource(R.string.lock_portrait),
                        checked = settings.lockPortrait,
                        onCheckedChange = onLockPortraitChange,
                    )
                    SettingToggle(
                        label = stringResource(R.string.pause_on_lock),
                        checked = settings.pauseOnLock,
                        onCheckedChange = onPauseOnLockChange,
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.speed_section)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SpeedChip(
                            label = "0.5x",
                            selected = settings.playbackSpeed == 0.5f,
                            onClick = { onPlaybackSpeedChange(0.5f) },
                            enabled = !settings.randomMode,
                            modifier = Modifier.weight(1f),
                        )
                        SpeedChip(
                            label = "1x",
                            selected = settings.playbackSpeed == 1.0f,
                            onClick = { onPlaybackSpeedChange(1.0f) },
                            enabled = !settings.randomMode,
                            modifier = Modifier.weight(1f),
                        )
                        SpeedChip(
                            label = "1.5x",
                            selected = settings.playbackSpeed == 1.5f,
                            onClick = { onPlaybackSpeedChange(1.5f) },
                            enabled = !settings.randomMode,
                            modifier = Modifier.weight(1f),
                        )
                        SpeedChip(
                            label = "2x",
                            selected = settings.playbackSpeed == 2.0f,
                            onClick = { onPlaybackSpeedChange(2.0f) },
                            enabled = !settings.randomMode,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.advanced),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF8A8A8A),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.speed_format, settings.playbackSpeed),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                            )
                            Slider(
                                value = settings.playbackSpeed,
                                onValueChange = onAdvancedSpeedChange,
                                valueRange = 1f..10f,
                                enabled = !settings.randomMode,
                            )
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.random_mode_section)) {
                SettingToggle(
                    label = stringResource(R.string.random_mode_desc),
                    checked = settings.randomMode,
                    onCheckedChange = onRandomModeChange,
                )
            }

            SettingsSection(title = stringResource(R.string.display_section)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = { onDisplayModeChange(VideoDisplayMode.VERTICAL_FULLSCREEN) },
                        enabled = settings.displayMode != VideoDisplayMode.VERTICAL_FULLSCREEN,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9500),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(stringResource(R.string.display_fullscreen))
                    }
                    Button(
                        onClick = { onDisplayModeChange(VideoDisplayMode.FIT) },
                        enabled = settings.displayMode != VideoDisplayMode.FIT,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9500),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(stringResource(R.string.display_fit))
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.video_folders)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onPickFolder,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9500),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(stringResource(R.string.add_folder))
                    }

                    settings.folderUris.toList().forEach { uri ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1A))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = uri.substringAfterLast("%2F").substringAfterLast("/"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onRemoveFolder(uri) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = Color(0xFFFF3B30),
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onOpenFavorites,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9500),
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.show_favorites))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFF9500),
        )
        content()
        HorizontalDivider(color = Color(0xFF2A2A2A))
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFF9500),
                checkedTrackColor = Color(0xFFFF9500).copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun SpeedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        enabled = enabled,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFF9500),
            selectedLabelColor = Color.White,
            disabledContainerColor = Color(0xFF2A2A2A),
            disabledLabelColor = Color(0xFF8A8A8A),
        ),
    )
}
