package com.randomclip.app.ui.game

import androidx.media3.ui.AspectRatioFrameLayout
import com.randomclip.app.model.GameVideoDisplayMode
import com.randomclip.app.model.VideoItem

object VideoScaleResolver {

    fun resolveResizeMode(video: VideoItem?, displayMode: GameVideoDisplayMode): Int {
        if (video == null) return AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        val isPortrait = video.height > 0 && video.width > 0 && video.height >= video.width

        return when (displayMode) {
            GameVideoDisplayMode.AUTO -> {
                if (isPortrait) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
            GameVideoDisplayMode.VERTICAL_FULLSCREEN -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            GameVideoDisplayMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }
}
