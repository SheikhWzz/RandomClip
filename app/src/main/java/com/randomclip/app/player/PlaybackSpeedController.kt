package com.randomclip.app.player

import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.roundToInt

/**
 * Applies playback speed only when the rounded value actually changes (reduces ExoPlayer jank).
 */
class PlaybackSpeedController(
    private val player: ExoPlayer,
) {
    private var lastAppliedSpeed: Float? = null
    private var lastMuted: Boolean? = null

    fun applySpeed(speed: Float, muted: Boolean) {
        if (speed <= GameModePlayer.MIN_MOVING_SPEED) {
            if (player.isPlaying) {
                player.pause()
            }
            lastAppliedSpeed = null
            return
        }

        val rounded = (speed * 100f).roundToInt() / 100f
        if (rounded == lastAppliedSpeed && muted == lastMuted) return

        lastAppliedSpeed = rounded
        lastMuted = muted
        player.volume = if (muted) 0f else 1f
        player.playbackParameters = PlaybackParameters(rounded, 1f)
        if (!player.isPlaying) {
            player.play()
        }
    }

    fun reset() {
        lastAppliedSpeed = null
        lastMuted = null
    }
}
