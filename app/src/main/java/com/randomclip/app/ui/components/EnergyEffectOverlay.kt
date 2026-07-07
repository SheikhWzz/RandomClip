package com.randomclip.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.randomclip.app.ui.theme.AccentColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DarkOrange = Color(0xFFCC6600)
private val GlowOrange = AccentColor
private const val SIZE_SCALE = 0.8f

@Composable
fun EnergyEffectOverlay(
    intensity: Float,
    overdrive: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val combined = (intensity + overdrive * 0.45f).coerceIn(0f, 1f)
    if (combined <= 0.01f) return

    val smoothIntensity by animateFloatAsState(
        targetValue = combined,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "energy_smooth",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "edge_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawBehind {
                val edge = 10.dp.toPx() * SIZE_SCALE
                val corner = 28.dp.toPx() * SIZE_SCALE
                val overdriveBoost = 1f + overdrive.coerceIn(0f, 1f) * 0.55f
                val alpha = (smoothIntensity * 0.38f * pulse * overdriveBoost).coerceIn(0f, 0.42f)
                if (alpha <= 0.002f) return@drawBehind

                val core = GlowOrange.copy(alpha = alpha)
                val outer = DarkOrange.copy(alpha = alpha * 0.55f)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(core, Color.Transparent),
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, edge),
                    cornerRadius = CornerRadius(corner, corner),
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, core),
                    ),
                    topLeft = Offset(0f, size.height - edge),
                    size = Size(size.width, edge),
                    cornerRadius = CornerRadius(corner, corner),
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(core, Color.Transparent),
                    ),
                    topLeft = Offset.Zero,
                    size = Size(edge, size.height),
                    cornerRadius = CornerRadius(corner, corner),
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, core),
                    ),
                    topLeft = Offset(size.width - edge, 0f),
                    size = Size(edge, size.height),
                    cornerRadius = CornerRadius(corner, corner),
                )

                val cornerGlow = corner * 1.1f
                listOf(
                    Offset(0f, 0f),
                    Offset(size.width, 0f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                ).forEach { center ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(outer, GlowOrange.copy(alpha = alpha * 0.35f), Color.Transparent),
                            center = center,
                            radius = cornerGlow,
                        ),
                        radius = cornerGlow,
                        center = center,
                    )
                }
            },
    )
}

@Composable
fun ClickEnergyBar(
    energy: Float,
    clickPulse: Float,
    modifier: Modifier = Modifier,
) {
    val displayLevel by animateFloatAsState(
        targetValue = (energy + clickPulse * 0.45f).coerceIn(0f, 1f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "bar_level",
    )
    if (displayLevel <= 0.02f) return

    val infiniteTransition = rememberInfiniteTransition(label = "bar_pulse")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar_shimmer",
    )

    Box(
        modifier = modifier
            .width(120.dp)
            .height(6.dp)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            val trackHeight = size.height
            val alpha = (0.35f + displayLevel * 0.55f) * shimmer
            val progressWidth = size.width * displayLevel

            drawRoundRect(
                color = DarkOrange.copy(alpha = alpha * 0.3f),
                size = size,
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
            )
            if (progressWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            DarkOrange.copy(alpha = alpha * 0.7f),
                            GlowOrange.copy(alpha = alpha),
                            GlowOrange.copy(alpha = alpha * 0.85f),
                        ),
                    ),
                    size = Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
                )
            }
        }
    }
}

data class TapSparkle(
    val id: Long,
    val x: Float,
    val y: Float,
    val life: Float,
    val intensity: Float = 1f,
)

private val SparkleSize = 40.dp
private val SparkleHalf = 20.dp

@Composable
fun TapSparkleOverlay(
    sparkles: List<TapSparkle>,
    modifier: Modifier = Modifier,
) {
    if (sparkles.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val parentWidth = maxWidth
        val parentHeight = maxHeight
        sparkles.forEach { sparkle ->
            key(sparkle.id) {
                SparkleBurst(
                    life = sparkle.life,
                    intensity = sparkle.intensity,
                    modifier = Modifier.offset(
                        x = parentWidth * sparkle.x - SparkleHalf,
                        y = parentHeight * sparkle.y - SparkleHalf,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SparkleBurst(
    life: Float,
    intensity: Float,
    modifier: Modifier = Modifier,
) {
    val alpha = life.coerceIn(0f, 1f)
    val pop = (0.55f + life * 0.45f) * (0.75f + intensity * 0.25f)
    val expand = 1f + (1f - life) * 0.18f * intensity

    Canvas(
        modifier = modifier
            .size(SparkleSize)
            .graphicsLayer {
                scaleX = pop * expand
                scaleY = pop * expand
                this.alpha = alpha
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = (11f + intensity * 3f).dp.toPx()
        val innerR = outerR * 0.28f
        val glowR = outerR * 1.35f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f * alpha),
                    GlowOrange.copy(alpha = 0.28f * alpha),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        val starPath = fourPointStarPath(cx, cy, outerR, innerR)
        drawPath(
            path = starPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFFFF4D6),
                    GlowOrange.copy(alpha = 0.85f),
                ),
                center = Offset(cx, cy),
                radius = outerR,
            ),
        )
        drawPath(
            path = starPath,
            color = Color.White.copy(alpha = 0.55f),
            style = Stroke(width = 1.1f),
        )

        repeat(4) { index ->
            val angle = (index * PI / 2).toFloat() - (PI / 4).toFloat()
            val dist = outerR * (1.05f + (1f - life) * 0.35f)
            val px = cx + cos(angle) * dist
            val py = cy + sin(angle) * dist
            val dotR = (1.6f + intensity).dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.9f * alpha),
                radius = dotR,
                center = Offset(px, py),
            )
        }

        drawCircle(
            color = Color.White.copy(alpha = 0.95f * alpha),
            radius = (2.4f + intensity * 0.8f).dp.toPx(),
            center = Offset(cx, cy),
        )
    }
}

private fun fourPointStarPath(
    cx: Float,
    cy: Float,
    outerRadius: Float,
    innerRadius: Float,
): Path {
    val path = Path()
    repeat(8) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = (index * PI / 4).toFloat() - (PI / 2).toFloat()
        val x = cx + cos(angle) * radius
        val y = cy + sin(angle) * radius
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
