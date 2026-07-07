package com.randomclip.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomclip.app.R
import com.randomclip.app.ui.components.PlayWithGearIcon

@Composable
fun DashboardScreen(
    onStartPlayback: () -> Unit,
    onStartGameMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGeneralSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoSettingsDesc = stringResource(R.string.video_play_settings_desc)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
    ) {
        // App Settings – always physical top-left (ignore RTL for this control)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val appSettingsInteractionSource = remember { MutableInteractionSource() }
            val appSettingsIsPressed by appSettingsInteractionSource.collectIsPressedAsState()
            val appSettingsScale by animateFloatAsState(
                targetValue = if (appSettingsIsPressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "app_settings_scale",
            )

            IconButton(
                onClick = onOpenGeneralSettings,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp)
                    .size(48.dp)
                    .scale(appSettingsScale),
                interactionSource = appSettingsInteractionSource,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.app_settings),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            val playInteractionSource = remember { MutableInteractionSource() }
            val playIsPressed by playInteractionSource.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (playIsPressed) 0.85f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "play_scale",
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(playScale)
                    .clickable(
                        onClick = onStartPlayback,
                        interactionSource = playInteractionSource,
                        indication = null,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.play_button_transparent_for_haupt_menu),
                    contentDescription = stringResource(R.string.start_playback),
                    modifier = Modifier.size(64.dp),
                )
            }

            Button(
                onClick = onStartGameMode,
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9500),
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = stringResource(R.string.start_game_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val videoSettingsInteractionSource = remember { MutableInteractionSource() }
                val videoSettingsIsPressed by videoSettingsInteractionSource.collectIsPressedAsState()
                val videoSettingsScale by animateFloatAsState(
                    targetValue = if (videoSettingsIsPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "video_settings_scale",
                )

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(56.dp)
                        .scale(videoSettingsScale)
                        .semantics {
                            contentDescription = videoSettingsDesc
                        },
                    interactionSource = videoSettingsInteractionSource,
                ) {
                    PlayWithGearIcon(
                        tint = Color.White,
                        size = 32.dp,
                    )
                }

                val favoritesInteractionSource = remember { MutableInteractionSource() }
                val favoritesIsPressed by favoritesInteractionSource.collectIsPressedAsState()
                val favoritesScale by animateFloatAsState(
                    targetValue = if (favoritesIsPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "favorites_scale",
                )

                IconButton(
                    onClick = onOpenFavorites,
                    modifier = Modifier
                        .size(56.dp)
                        .scale(favoritesScale),
                    interactionSource = favoritesInteractionSource,
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.favorites),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}
