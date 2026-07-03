package com.randomclip.app.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import com.randomclip.app.model.VideoDisplayMode
import com.randomclip.app.player.VideoPlayerManager
import com.randomclip.app.ui.UiState
import com.randomclip.app.ui.theme.MochaOverlay

@Composable
fun VideoPlayerScreen(
    uiState: UiState,
    playerManager: VideoPlayerManager,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    onRefresh: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onFavorite: () -> Unit,
    onToggleDisplayMode: () -> Unit,
    onManualAdvance: () -> Unit,
    onRevealControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val displayMode = uiState.settings.displayMode
    val currentVideo = uiState.currentClip?.video

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

    DisposableEffect(lifecycleOwner, playerManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> playerManager.pause()
                Lifecycle.Event.ON_STOP -> playerManager.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerView.player = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(uiState.currentClip) {
                detectTapGestures(
                    onTap = {
                        onRevealControls()
                        if (uiState.currentClip != null) {
                            onTogglePlayPause()
                        }
                    },
                    onLongPress = {
                        if (uiState.currentClip != null) {
                            onFavorite()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var dragTotalY = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragTotalY = 0f },
                    onDragEnd = {
                        if (dragTotalY < -100f) { // Swiped UP
                            onSkipNext()
                        } else if (dragTotalY > 100f) { // Swiped DOWN
                            onSkipPrevious()
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragTotalY += dragAmount
                    }
                )
            }
            .then(
                if (!uiState.settings.autoAdvance || uiState.awaitingManualAdvance) {
                    Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -40f) onManualAdvance()
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        if (uiState.currentClip != null) {
            AndroidView(
                factory = { playerView },
                update = { view ->
                    val targetMode = resolveResizeMode(displayMode, currentVideo)
                    if (view.resizeMode != targetMode) {
                        view.resizeMode = targetMode
                    }
                    if (view.player !== playerManager.player) {
                        view.player = playerManager.player
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (uiState.videos.isEmpty()) {
            Text(
                text = uiState.statusMessage ?: "Ordner in den Einstellungen wählen",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            IconButton(onClick = {
                onRevealControls()
                onOpenFavorites()
            }) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Favoriten",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = {
                onRevealControls()
                onRefresh()
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Aktualisieren",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = {
                onRevealControls()
                onOpenSettings()
            }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Einstellungen",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.showOverlayControls && uiState.currentClip != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            PlaybackControlsOverlay(
                isPlaying = uiState.isPlaying,
                displayMode = displayMode,
                onTogglePlayPause = onTogglePlayPause,
                onStop = onStop,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onFavorite = onFavorite,
                onToggleDisplayMode = onToggleDisplayMode,
            )
        }

        uiState.statusMessage?.takeIf { uiState.videos.isNotEmpty() }?.let { title ->
            Text(
                text = buildString {
                    append(title)
                    currentVideo?.aspectRatio?.let { ratio ->
                        append(" · ")
                        append(if (currentVideo.isLandscape) "Quer" else "Hoch")
                        append(" ")
                        append("%.2f".format(ratio))
                    }
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(MochaOverlay)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (uiState.awaitingManualAdvance) {
            Text(
                text = "Wischen oder Skip für nächsten Clip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun PlaybackControlsOverlay(
    isPlaying: Boolean,
    displayMode: VideoDisplayMode,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onFavorite: () -> Unit,
    onToggleDisplayMode: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .background(MochaOverlay)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSkipPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Vorheriger Clip",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Nächster Clip",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorisieren",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            IconButton(onClick = onToggleDisplayMode) {
                Icon(
                    imageVector = if (displayMode == VideoDisplayMode.VERTICAL_FULLSCREEN) {
                        Icons.Default.Crop
                    } else {
                        Icons.Default.AspectRatio
                    },
                    contentDescription = "Anzeigemodus: ${displayModeLabel(displayMode)}",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
