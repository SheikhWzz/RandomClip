package com.randomclip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.randomclip.app.R
import com.randomclip.app.model.VideoItem
import com.randomclip.app.ui.theme.AccentColor

private val FrameOuter = Color(0xFF2A1F14)
private val FrameInner = Color(0xFF5C4033)
private val GalleryWall = Color(0xFF121212)

@Composable
fun GameHubScreen(
    videos: List<VideoItem>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onVideoSelected: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GalleryWall),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White,
                )
            }
            Text(
                text = stringResource(R.string.game_hub_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.game_settings_title),
                    tint = Color.White,
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A1A),
                                Color(0xFF0D0D0D),
                                Color(0xFF1A1A1A),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(videos, key = { it.uri }) { video ->
                        GameGalleryFrame(
                            video = video,
                            onClick = { onVideoSelected(video) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameGalleryFrame(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val frameMs = (video.durationMs / 2).coerceAtLeast(0L)
    val cardWidth = 148.dp
    val cardHeight = 248.dp

    Column(
        modifier = modifier
            .width(cardWidth + 24.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .border(5.dp, FrameOuter, RoundedCornerShape(6.dp))
                .padding(5.dp)
                .border(2.dp, FrameInner, RoundedCornerShape(4.dp))
                .padding(4.dp)
                .border(1.dp, AccentColor.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                .background(Color.Black, RoundedCornerShape(2.dp))
                .padding(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1A1A1A)),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(video.uri)
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .videoFrameMillis(frameMs)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f),
                                ),
                            ),
                        ),
                )
            }
        }

        Text(
            text = video.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFE8E8E8),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(cardWidth + 8.dp),
        )
    }
}
