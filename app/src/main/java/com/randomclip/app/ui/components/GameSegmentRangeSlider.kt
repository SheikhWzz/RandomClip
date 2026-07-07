package com.randomclip.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.randomclip.app.ui.theme.AccentColor
import com.randomclip.app.ui.theme.DividerColor
import com.randomclip.app.ui.theme.SurfaceColor
import com.randomclip.app.ui.theme.TextSecondaryColor
import kotlin.math.roundToInt

enum class SegmentDragHandle {
    START,
    END,
}

@Composable
fun GameSegmentRangeSlider(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    onStartChange: (Long, Boolean) -> Unit,
    onEndChange: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minSegmentMs: Long = 1_500L,
) {
    if (durationMs <= 0L) return

    val density = LocalDensity.current
    val thumbSize = 36.dp
    val thumbPx = with(density) { thumbSize.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        val trackWidth = constraints.maxWidth.toFloat()
        val usableWidth = (trackWidth - thumbPx).coerceAtLeast(1f)

        fun msToX(ms: Long): Float = (ms.toFloat() / durationMs.toFloat()) * usableWidth
        fun xToMs(x: Float): Long = ((x / usableWidth).coerceIn(0f, 1f) * durationMs).roundToInt().toLong()

        var startX by remember { mutableFloatStateOf(0f) }
        var endX by remember { mutableFloatStateOf(usableWidth) }

        LaunchedEffect(startMs, endMs, durationMs, usableWidth) {
            startX = msToX(startMs)
            endX = msToX(endMs)
        }

        val minGapX = msToX(minSegmentMs.coerceAtMost(durationMs))

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = thumbSize / 2)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DividerColor),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = with(density) { (thumbPx / 2f + startX).toDp() })
                .width(with(density) { (endX - startX).coerceAtLeast(0f).toDp() })
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AccentColor.copy(alpha = 0.55f)),
        )

        SegmentThumb(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = with(density) { startX.toDp() })
                .pointerInput(durationMs, endX, usableWidth) {
                    detectDragGestures(
                        onDragStart = { onStartChange(xToMs(startX), true) },
                        onDragEnd = { onStartChange(xToMs(startX), false) },
                        onDragCancel = { onStartChange(xToMs(startX), false) },
                    ) { change, dragAmount ->
                        change.consume()
                        val maxX = (endX - minGapX).coerceAtLeast(0f)
                        startX = (startX + dragAmount.x).coerceIn(0f, maxX)
                        onStartChange(xToMs(startX), true)
                    }
                },
        )

        SegmentThumb(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = with(density) { endX.toDp() })
                .pointerInput(durationMs, startX, usableWidth) {
                    detectDragGestures(
                        onDragStart = { onEndChange(xToMs(endX), true) },
                        onDragEnd = { onEndChange(xToMs(endX), false) },
                        onDragCancel = { onEndChange(xToMs(endX), false) },
                    ) { change, dragAmount ->
                        change.consume()
                        val minX = (startX + minGapX).coerceAtMost(usableWidth)
                        endX = (endX + dragAmount.x).coerceIn(minX, usableWidth)
                        onEndChange(xToMs(endX), true)
                    }
                },
        )
    }
}

@Composable
private fun SegmentThumb(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(SurfaceColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun SegmentTimeLabels(
    startMs: Long,
    endMs: Long,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(startMs),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondaryColor,
        )
        Text(
            text = formatTime(endMs),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondaryColor,
        )
    }
}
