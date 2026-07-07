package com.randomclip.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.randomclip.app.ui.GameIntroPhase

@Composable
fun GameIntroOverlay(
    phase: GameIntroPhase,
    readyLabel: String,
    goLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = phase == GameIntroPhase.READY,
            enter = fadeIn(tween(120)) + scaleIn(tween(180), initialScale = 0.9f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120)),
        ) {
            IntroText(text = readyLabel)
        }
        AnimatedVisibility(
            visible = phase == GameIntroPhase.GO,
            enter = fadeIn(tween(100)) + scaleIn(tween(150), initialScale = 0.85f),
            exit = fadeOut(tween(100)) + scaleOut(tween(100)),
        ) {
            IntroText(text = goLabel)
        }
    }
}

@Composable
private fun IntroText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Black,
        fontSize = 72.sp,
        color = Color.White,
    )
}
