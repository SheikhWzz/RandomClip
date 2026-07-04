package com.randomclip.app.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import com.randomclip.app.model.VideoDisplayMode
import com.randomclip.app.player.VideoPlayerManager
import com.randomclip.app.ui.UiState
import com.randomclip.app.ui.theme.OverlayColor

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

    // Handle system back button
    BackHandler(enabled = true) {
        onBack()
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
                            // Check if tap is in bottom area to show video name
                            val screenHeight = this.size.height
                            if (offset.y > screenHeight * 0.7f) {
                                showVideoName = true
                            } else {
                                onTogglePlayPause()
                            }
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
                        if (dragTotalY < -100f) { // Swiped UP -> NEXT (random)
                            onSkipNext()
                        } else if (dragTotalY > 100f) { // Swiped DOWN -> PREVIOUS (history)
                            onSkipPrevious()
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragTotalY += dragAmount
                    }
                )
            }
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

        // Calculate display cutout padding
        val cutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LocalLayoutDirection.current)

        // Favorites mode indicator (top right, shown when tapping bottom area)
        uiState.statusMessage?.takeIf { uiState.videos.isNotEmpty() && showVideoName && uiState.isFavoritesPlaylistMode }?.let { _ ->
            Text(
                text = "Favorites",
                color = androidx.compose.ui.graphics.Color(0xFFFF9500),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = cutoutPadding + 16.dp, top = 16.dp)
            )
        }

        // Instagram-style Play/Pause feedback icon (smaller, no background)
        var showPlayPauseFeedback by remember { mutableStateOf(false) }
        var wasPlaying by remember { mutableStateOf(false) }
        
        LaunchedEffect(uiState.isPlaying) {
            // Only show feedback on manual play/pause toggle, not on automatic clip transitions
            if (wasPlaying != uiState.isPlaying) {
                showPlayPauseFeedback = true
                delay(500)
                showPlayPauseFeedback = false
            }
            wasPlaying = uiState.isPlaying
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
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        // Vertical Action Rail (left side)
        ActionRail(
            visible = uiState.currentClip != null,
            onFavorite = onFavorite,
            onOpenSettings = onOpenSettings,
            onBack = onBack,
            onToggleDisplayMode = onToggleDisplayMode,
            onToggleRandomMode = onToggleRandomMode,
            isFavorite = uiState.isFavorite,
            displayMode = uiState.settings.displayMode,
            randomMode = uiState.settings.randomMode,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // Video name (only shows briefly when tapping bottom area)
        uiState.statusMessage?.takeIf { uiState.videos.isNotEmpty() && showVideoName }?.let { title ->
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
                    .background(OverlayColor)
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
private fun ActionRail(
    visible: Boolean,
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
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "rail_alpha"
    )
    
    Column(
        modifier = modifier
            .padding(start = 16.dp, top = 100.dp, bottom = 100.dp)
            .alpha(alpha),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Favorite (top of rail)
        ActionRailButton(
            onClick = onFavorite,
            icon = Icons.Default.Favorite,
            contentDescription = "Favorisieren",
            isFilled = isFavorite,
            isFavoriteButton = true,
        )
        
        // Display Mode Toggle
        ActionRailButton(
            onClick = onToggleDisplayMode,
            icon = if (displayMode == VideoDisplayMode.VERTICAL_FULLSCREEN) Icons.Default.Crop else Icons.Default.AspectRatio,
            contentDescription = "Anzeigemodus",
            isFilled = false,
            isFavoriteButton = false,
        )
        
        // Random Mode Toggle
        ActionRailButton(
            onClick = onToggleRandomMode,
            icon = Icons.Default.Shuffle,
            contentDescription = "Zufälliger Modus",
            isFilled = randomMode,
            isFavoriteButton = false,
        )
        
        // Settings
        ActionRailButton(
            onClick = onOpenSettings,
            icon = Icons.Default.Settings,
            contentDescription = "Einstellungen",
            isFilled = false,
            isFavoriteButton = false,
        )
        
        // Back button (bottom of rail)
        ActionRailButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Zurück",
            isFilled = false,
            isFavoriteButton = false,
        )
    }
}

@Composable
private fun ActionRailButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isFilled: Boolean,
    isFavoriteButton: Boolean,
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Heart bounce animation (1.0 -> 1.3 -> 1.0) when liked
    val heartBounce by animateFloatAsState(
        targetValue = if (isFilled && isFavoriteButton) 1.3f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 300f
        ),
        label = "heart_bounce"
    )
    
    val iconSize by animateFloatAsState(
        targetValue = when {
            isPressed -> 20.4f // 10% smaller when pressed
            isFilled && isFavoriteButton -> 25.2f * heartBounce // Bounce when favorited
            else -> 22.68f // Normal size (10% smaller than 28dp)
        },
        animationSpec = tween(durationMillis = 150),
        label = "icon_size"
    )
    
    val actualColor = when {
        isFilled && isFavoriteButton -> androidx.compose.ui.graphics.Color(0xFFFF3B30) // Red for favorite
        isFilled -> androidx.compose.ui.graphics.Color(0xFFFF9500) // Orange for active
        isPressed -> androidx.compose.ui.graphics.Color(0xFFFF9500).copy(alpha = 0.8f) // Orange tint when pressed
        else -> androidx.compose.ui.graphics.Color.White
    }
    
    Box(
        modifier = Modifier
            .size(38.88.dp) // 10% smaller than 43.2dp
            .clickable(
                onClick = onClick,
                indication = null, // Remove ripple
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
            .scale(if (isPressed) 0.95f else 1f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = actualColor,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}
