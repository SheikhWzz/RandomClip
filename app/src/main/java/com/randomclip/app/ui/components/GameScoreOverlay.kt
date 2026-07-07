package com.randomclip.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randomclip.app.R
import com.randomclip.app.ui.theme.AccentColor
import com.randomclip.app.ui.theme.SurfaceColor

@Composable
fun GameScoreOverlay(
    score: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .padding(16.dp)
            .background(
                color = SurfaceColor.copy(alpha = 0.72f),
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 1.dp,
                color = AccentColor.copy(alpha = 0.28f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.game_score_format, score),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFE8E8E8),
            fontWeight = FontWeight.Medium,
        )
    }
}
