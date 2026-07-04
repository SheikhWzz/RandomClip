package com.randomclip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.randomclip.app.R
import com.randomclip.app.ui.theme.AccentColor

@Composable
fun DashboardScreen(
    onStartPlayback: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            // App Title
            Text(
                text = "Clip-It",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Large Play Button with custom shape (no background, only form is orange)
            val playInteractionSource = remember { MutableInteractionSource() }
            val playIsPressed by playInteractionSource.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (playIsPressed) 0.85f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "play_scale"
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(playScale)
                    .clickable(
                        onClick = onStartPlayback,
                        interactionSource = playInteractionSource,
                        indication = null
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.play_button_transparent_for_haupt_menu),
                    contentDescription = "Wiedergabe starten",
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Action Icons Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Settings Icon
                val settingsInteractionSource = remember { MutableInteractionSource() }
                val settingsIsPressed by settingsInteractionSource.collectIsPressedAsState()
                val settingsScale by animateFloatAsState(
                    targetValue = if (settingsIsPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "settings_scale"
                )
                
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(56.dp)
                        .scale(settingsScale),
                    interactionSource = settingsInteractionSource
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Einstellungen",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Favorites Icon
                val favoritesInteractionSource = remember { MutableInteractionSource() }
                val favoritesIsPressed by favoritesInteractionSource.collectIsPressedAsState()
                val favoritesScale by animateFloatAsState(
                    targetValue = if (favoritesIsPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "favorites_scale"
                )
                
                IconButton(
                    onClick = onOpenFavorites,
                    modifier = Modifier
                        .size(56.dp)
                        .scale(favoritesScale),
                    interactionSource = favoritesInteractionSource
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favoriten",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomPlayIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(
        modifier = modifier
    ) {
        val path = Path().apply {
            // Sharp, angular play shape with asymmetric cut
            // Left edge: straight vertical line
            moveTo(0f, 0f)
            // Top edge: diagonal to right
            lineTo(size.width * 0.7f, size.height * 0.3f)
            // Right edge: sharp asymmetric cut (lightning-style)
            lineTo(size.width * 0.5f, size.height * 0.5f)
            lineTo(size.width * 0.7f, size.height * 0.7f)
            // Bottom edge: diagonal back to left
            lineTo(0f, size.height)
            // Close path back to start
            close()
        }
        
        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}
