package com.randomclip.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.media3.ui.AspectRatioFrameLayout
import com.randomclip.app.R
import com.randomclip.app.model.VideoDisplayMode
import com.randomclip.app.model.VideoItem

fun resolveResizeMode(displayMode: VideoDisplayMode, @Suppress("UNUSED_PARAMETER") video: VideoItem?): Int =
    when (displayMode) {
        VideoDisplayMode.VERTICAL_FULLSCREEN -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        VideoDisplayMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

@Composable
fun displayModeLabel(mode: VideoDisplayMode): String = when (mode) {
    VideoDisplayMode.VERTICAL_FULLSCREEN -> stringResource(R.string.display_fullscreen)
    VideoDisplayMode.FIT -> stringResource(R.string.display_fit)
}
