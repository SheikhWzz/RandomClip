package com.randomclip.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomclip.app.ui.theme.AccentColor
import kotlinx.coroutines.delay

@Composable
fun GameOverOverlay(
    clickCount: Int,
    score: Int,
    title: String,
    clicksLabel: String,
    scoreLabel: String,
    backLabel: String,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayedClicks by remember { mutableIntStateOf(0) }

    LaunchedEffect(clickCount) {
        displayedClicks = 0
        if (clickCount <= 0) return@LaunchedEffect
        val steps = clickCount.coerceAtMost(40)
        val stepDelay = (700L / steps).coerceAtLeast(16L)
        repeat(steps) { step ->
            displayedClicks = ((clickCount * (step + 1)) / steps.toFloat()).toInt()
            delay(stepDelay)
        }
        displayedClicks = clickCount
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(280)) + scaleIn(tween(320), initialScale = 0.88f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .background(Color(0xFF141414), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "$displayedClicks",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentColor,
                )

                Text(
                    text = clicksLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = scoreLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                )

                Button(
                    onClick = onBackToMenu,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentColor,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = backLabel,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
