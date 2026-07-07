package com.randomclip.app.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.randomclip.app.player.GameModePlayer
import com.randomclip.app.ui.GameModeUiState

private val AccentOrange = Color(0xFFFF9500)

@Composable
fun GameModePlayerScreen(
    uiState: GameModeUiState,
    player: GameModePlayer,
    onTap: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(enabled = uiState.isActive, onBack = onExit)

    val playerView = remember(context) {
        PlayerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerView.player = null
            player.pause()
        }
    }

    val glowAlpha = (uiState.glowIntensity * 0.85f).coerceIn(0f, 1f)
    val borderWidth = (4 + uiState.glowIntensity * 10).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(uiState.isActive) {
                if (!uiState.isActive) return@pointerInput
                detectTapGestures(onTap = { onTap() })
            },
    ) {
        if (uiState.isActive) {
            AndroidView(
                factory = { playerView },
                update = { view ->
                    if (view.player !== player.exoPlayer) {
                        view.player = player.exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (glowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(0.dp))
                    .border(
                        width = borderWidth,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AccentOrange.copy(alpha = glowAlpha),
                                AccentOrange.copy(alpha = glowAlpha * 0.35f),
                                AccentOrange.copy(alpha = glowAlpha),
                            ),
                        ),
                        shape = RoundedCornerShape(0.dp),
                    ),
            )
        }
    }
}
