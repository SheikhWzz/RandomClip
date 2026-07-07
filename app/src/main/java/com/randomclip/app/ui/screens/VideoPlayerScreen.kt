package com.randomclip.app.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import com.randomclip.app.R
import com.randomclip.app.model.VideoDisplayMode
import com.randomclip.app.player.VideoPlayerManager
import com.randomclip.app.ui.UiState
import com.randomclip.app.ui.components.PlayWithGearIcon
import com.randomclip.app.ui.theme.AccentColor
import com.randomclip.app.ui.theme.OverlayColor
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    uiState: UiState,
    playerManager: VideoPlayerManager,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onFavorite: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleDisplayMode: () -> Unit,
    onToggleRandomMode: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val displayMode = uiState.settings.displayMode
    val currentVideo = uiState.currentClip?.video

    var showVideoName by remember { mutableStateOf(false) }
    LaunchedEffect(showVideoName) {
        if (showVideoName) {
            delay(2000)
            showVideoName = false
        }
    }

    var showPlayPauseFeedback by remember { mutableStateOf(false) }
    fun triggerPlayPauseFeedback() {
        showPlayPauseFeedback = true
    }

    BackHandler(enabled = true, onBack = onBack)

    LaunchedEffect(uiState.currentClip) {
        if (uiState.currentClip != null) {
            playerManager.play()
        }
    }

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
            playerManager.stopClip()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(uiState.currentClip) {
                detectTapGestures(
                    onTap = { offset ->
                        if (uiState.currentClip != null) {
                            val screenHeight = this.size.height
                            if (offset.y > screenHeight * 0.7f) {
                                showVideoName = true
                            } else {
                                triggerPlayPauseFeedback()
                                onTogglePlayPause()
                            }
                        }
                    },
                    onLongPress = {
                        if (uiState.currentClip != null) {
                            onFavorite()
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                var dragTotalY = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragTotalY = 0f },
                    onDragEnd = {
                        if (dragTotalY < -100f) {
                            onSkipNext()
                        } else if (dragTotalY > 100f) {
                            onSkipPrevious()
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragTotalY += dragAmount
                    },
                )
            },
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
                text = uiState.statusMessage ?: stringResource(R.string.select_folder_hint),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
            .calculateLeftPadding(LocalLayoutDirection.current)

        uiState.statusMessage?.takeIf {
            uiState.videos.isNotEmpty() && showVideoName && uiState.isFavoritesPlaylistMode
        }?.let {
            Text(
                text = stringResource(R.string.favorites),
                color = AccentColor,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = cutoutPadding + 16.dp, top = 16.dp),
            )
        }

        LaunchedEffect(showPlayPauseFeedback) {
            if (showPlayPauseFeedback) {
                delay(500)
                showPlayPauseFeedback = false
            }
        }

        AnimatedVisibility(
            visible = showPlayPauseFeedback && uiState.currentClip != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }

        if (uiState.currentClip != null) {
            ActionRail(
                onFavorite = onFavorite,
                onOpenSettings = onOpenSettings,
                onBack = onBack,
                onToggleDisplayMode = onToggleDisplayMode,
                onToggleRandomMode = onToggleRandomMode,
                isFavorite = uiState.isFavorite,
                displayMode = uiState.settings.displayMode,
                randomMode = uiState.settings.randomMode,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        uiState.statusMessage?.takeIf { uiState.videos.isNotEmpty() && showVideoName }?.let { title ->
            Text(
                text = buildString {
                    append(title)
                    currentVideo?.aspectRatio?.let { ratio ->
                        append(stringResource(R.string.aspect_ratio_separator))
                        append(
                            if (currentVideo.isLandscape) {
                                stringResource(R.string.orientation_landscape)
                            } else {
                                stringResource(R.string.orientation_portrait)
                            },
                        )
                        append(" ")
                        append("%.2f".format(ratio))
                    }
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(OverlayColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (uiState.awaitingManualAdvance) {
            Text(
                text = stringResource(R.string.swipe_for_next),
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
private fun ActionRail(
    onFavorite: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleDisplayMode: () -> Unit,
    onToggleRandomMode: () -> Unit,
    onBack: () -> Unit,
    isFavorite: Boolean,
    displayMode: VideoDisplayMode,
    randomMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(start = 16.dp, top = 100.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        FavoriteRailButton(
            onClick = onFavorite,
            isFavorite = isFavorite,
            contentDescription = stringResource(R.string.favorite),
        )

        ActionRailButton(
            onClick = onToggleDisplayMode,
            icon = if (displayMode == VideoDisplayMode.VERTICAL_FULLSCREEN) {
                Icons.Default.Crop
            } else {
                Icons.Default.AspectRatio
            },
            contentDescription = stringResource(R.string.display_mode),
            isFilled = false,
        )

        ActionRailButton(
            onClick = onToggleRandomMode,
            icon = Icons.Default.Shuffle,
            contentDescription = stringResource(R.string.random_mode),
            isFilled = randomMode,
        )

        PlaySettingsRailButton(
            onClick = onOpenSettings,
            contentDescription = stringResource(R.string.video_play_settings_desc),
        )

        ActionRailButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            isFilled = false,
        )
    }
}

@Composable
private fun FavoriteRailButton(
    onClick: () -> Unit,
    isFavorite: Boolean,
    contentDescription: String,
) {
    var bounceTarget by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(isFavorite) {
        bounceTarget = 1.3f
    }

    val scale by animateFloatAsState(
        targetValue = bounceTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (bounceTarget > 1f) Spring.StiffnessMedium else Spring.StiffnessLow,
        ),
        finishedListener = { finalValue ->
            if (finalValue > 1.1f) {
                bounceTarget = 1f
            }
        },
        label = "heart_bounce",
    )

    val iconColor by animateColorAsState(
        targetValue = if (isFavorite) AccentColor else Color.White,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "heart_color",
    )

    Box(
        modifier = Modifier
            .size(38.88.dp)
            .scale(scale)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.68.dp),
        )
    }
}

@Composable
private fun PlaySettingsRailButton(
    onClick: () -> Unit,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(38.88.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        PlayWithGearIcon(
            tint = Color.White,
            size = 22.68.dp,
        )
    }
}

@Composable
private fun ActionRailButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isFilled: Boolean,
) {
    val iconColor = when {
        isFilled -> AccentColor
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(38.88.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.68.dp),
        )
    }
}
