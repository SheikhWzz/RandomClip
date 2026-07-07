package com.randomclip.app.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

object GameVideoRotation {
    fun resolveRotationZ(videoFlipped: Boolean): Float = if (videoFlipped) 180f else 0f
}

@Composable
fun rememberGameVideoRotationZ(
    landscapeGameEnabled: Boolean,
    videoFlipped: Boolean,
): Float {
    val configuration = LocalConfiguration.current
    return remember(landscapeGameEnabled, videoFlipped, configuration.orientation) {
        GameVideoRotation.resolveRotationZ(videoFlipped)
    }
}
