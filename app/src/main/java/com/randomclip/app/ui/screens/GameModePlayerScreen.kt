package com.randomclip.app.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import com.randomclip.app.R
import com.randomclip.app.player.GameModePlayer
import com.randomclip.app.ui.GameIntroPhase
import com.randomclip.app.ui.GameModeUiState
import com.randomclip.app.ui.components.ClickEnergyBar
import com.randomclip.app.ui.components.EnergyEffectOverlay
import com.randomclip.app.ui.components.GameIntroOverlay
import com.randomclip.app.ui.components.GameOverOverlay
import com.randomclip.app.ui.components.GameScoreOverlay
import com.randomclip.app.ui.components.TapSparkleOverlay
import com.randomclip.app.ui.game.VideoScaleResolver
import com.randomclip.app.ui.game.rememberGameVideoRotationZ

@Composable
fun GameModePlayerScreen(
    uiState: GameModeUiState,
    player: GameModePlayer,
    onTap: (normalizedX: Float, normalizedY: Float, pointerCount: Int) -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val showIntro = uiState.introPhase == GameIntroPhase.READY ||
        uiState.introPhase == GameIntroPhase.GO
    val showGameOver = uiState.introPhase == GameIntroPhase.GAME_OVER

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

    val sessionResizeMode = remember(uiState.selectedVideo, uiState.videoDisplayMode) {
        VideoScaleResolver.resolveResizeMode(uiState.selectedVideo, uiState.videoDisplayMode)
    }
    val videoRotationZ = rememberGameVideoRotationZ(
        landscapeGameEnabled = uiState.landscapeGameEnabled,
        videoFlipped = uiState.videoFlipped,
    )

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
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (uiState.isPlaying) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerCount = currentEvent.changes.count { it.pressed }.coerceAtLeast(1)
                            val x = (down.position.x / size.width).coerceIn(0f, 1f)
                            val y = (down.position.y / size.height).coerceIn(0f, 1f)
                            onTap(x, y, pointerCount)
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        AndroidView(
            factory = { playerView },
            update = { view ->
                if (view.resizeMode != sessionResizeMode) {
                    view.resizeMode = sessionResizeMode
                }
                if (view.player !== player.exoPlayer) {
                    view.player = player.exoPlayer
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = videoRotationZ },
        )

        EnergyEffectOverlay(
            intensity = uiState.glowIntensity,
            overdrive = uiState.overdriveGlow,
            modifier = Modifier.fillMaxSize(),
        )

        TapSparkleOverlay(
            sparkles = uiState.tapSparkles,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
        )

        if (uiState.introPhase == GameIntroPhase.DONE) {
            ClickEnergyBar(
                energy = uiState.energy,
                clickPulse = uiState.clickPulse,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 16.dp),
            )

            GameScoreOverlay(
                score = uiState.score,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding(),
            )
        }

        if (showIntro) {
            GameIntroOverlay(
                phase = uiState.introPhase,
                readyLabel = stringResource(R.string.game_intro_ready),
                goLabel = stringResource(R.string.game_intro_go),
            )
        }

        if (showGameOver) {
            GameOverOverlay(
                clickCount = uiState.clickCount,
                score = uiState.score,
                title = stringResource(R.string.game_over_title),
                clicksLabel = stringResource(R.string.game_over_clicks),
                scoreLabel = stringResource(R.string.game_score_format, uiState.score),
                backLabel = stringResource(R.string.game_back_to_menu),
                onBackToMenu = onBackToMenu,
                modifier = Modifier.zIndex(4f),
            )
        }
    }
}
