package com.randomclip.app.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import com.randomclip.app.R
import com.randomclip.app.model.GameVideoDisplayMode
import com.randomclip.app.model.VideoItem
import com.randomclip.app.player.GameModePlayer
import com.randomclip.app.ui.components.GameSegmentRangeSlider
import com.randomclip.app.ui.components.GameSettingToggle
import com.randomclip.app.ui.components.GameSettingsSection
import com.randomclip.app.ui.components.SegmentTimeLabels
import com.randomclip.app.ui.game.VideoScaleResolver
import com.randomclip.app.ui.theme.BackgroundColor
import com.randomclip.app.ui.theme.SurfaceColor
import com.randomclip.app.ui.theme.TextSecondaryColor
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameVideoPreviewScreen(
    video: VideoItem,
    displayMode: GameVideoDisplayMode,
    soundEnabled: Boolean,
    videoFlipped: Boolean,
    landscapeGameEnabled: Boolean,
    segmentLoopEnabled: Boolean,
    segmentStartMs: Long,
    segmentEndMs: Long,
    player: GameModePlayer,
    onBack: () -> Unit,
    onToggleLandscapeGame: () -> Unit,
    onToggleSegmentLoop: () -> Unit,
    onToggleVideoFlipped: () -> Unit,
    onToggleSound: () -> Unit,
    onSegmentStartChange: (Long, Boolean) -> Unit,
    onSegmentEndChange: (Long, Boolean) -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val playerView = remember(context) {
        PlayerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerView.player = null
        }
    }

    val resizeMode = VideoScaleResolver.resolveResizeMode(video, displayMode)
    val flipRotation = if (videoFlipped) 180f else 0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.game_preview_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .background(SurfaceColor),
                ) {
                    AndroidView(
                        factory = { playerView },
                        update = { view ->
                            view.resizeMode = resizeMode
                            if (view.player !== player.exoPlayer) {
                                view.player = player.exoPlayer
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .graphicsLayer { rotationZ = flipRotation },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onStartGame,
                            ),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = video.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    VideoMetadataRow(
                        label = stringResource(R.string.game_meta_duration),
                        value = formatDuration(video.durationMs),
                    )
                    if (video.width > 0 && video.height > 0) {
                        VideoMetadataRow(
                            label = stringResource(R.string.game_meta_resolution),
                            value = stringResource(
                                R.string.game_resolution_format,
                                video.width,
                                video.height,
                            ),
                        )
                        VideoMetadataRow(
                            label = stringResource(R.string.game_meta_orientation),
                            value = stringResource(
                                if (video.height >= video.width) {
                                    R.string.orientation_portrait
                                } else {
                                    R.string.orientation_landscape
                                },
                            ),
                        )
                    }
                }

                GameSettingsSection(title = stringResource(R.string.game_segment_range_title)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameSegmentRangeSlider(
                            durationMs = video.durationMs,
                            startMs = segmentStartMs,
                            endMs = segmentEndMs,
                            onStartChange = onSegmentStartChange,
                            onEndChange = onSegmentEndChange,
                        )
                        SegmentTimeLabels(
                            startMs = segmentStartMs,
                            endMs = segmentEndMs,
                            formatTime = ::formatDuration,
                        )
                        Text(
                            text = stringResource(
                                R.string.game_segment_duration,
                                formatDuration((segmentEndMs - segmentStartMs).coerceAtLeast(0L)),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryColor,
                        )
                    }
                }

                GameSettingsSection(title = stringResource(R.string.game_preview_settings)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GameSettingToggle(
                            label = stringResource(R.string.game_landscape_mode_toggle),
                            description = stringResource(
                                if (landscapeGameEnabled) {
                                    R.string.game_landscape_mode_on_desc
                                } else {
                                    R.string.game_one_hand_mode_desc
                                },
                            ),
                            checked = landscapeGameEnabled,
                            onCheckedChange = { onToggleLandscapeGame() },
                        )
                        GameSettingToggle(
                            label = stringResource(R.string.game_segment_loop_toggle),
                            description = stringResource(
                                if (segmentLoopEnabled) {
                                    R.string.game_segment_loop_on_desc
                                } else {
                                    R.string.game_segment_loop_off_desc
                                },
                            ),
                            checked = segmentLoopEnabled,
                            onCheckedChange = { onToggleSegmentLoop() },
                        )
                        GameSettingToggle(
                            label = stringResource(R.string.game_flip_video_toggle),
                            description = stringResource(
                                if (videoFlipped) {
                                    R.string.game_flip_video_on_desc
                                } else {
                                    R.string.game_flip_video_off_desc
                                },
                            ),
                            checked = videoFlipped,
                            onCheckedChange = { onToggleVideoFlipped() },
                        )
                        GameSettingToggle(
                            label = stringResource(R.string.game_sound_toggle),
                            description = stringResource(
                                if (soundEnabled) R.string.game_sound_on else R.string.game_sound_off,
                            ),
                            checked = soundEnabled,
                            onCheckedChange = { onToggleSound() },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.game_tap_video_to_start),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryColor,
                )
            }
        }
    }
}

@Composable
private fun VideoMetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "—"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
