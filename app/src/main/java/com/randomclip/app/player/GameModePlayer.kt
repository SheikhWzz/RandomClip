package com.randomclip.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters

/**
 * Dedicated ExoPlayer instance for Game Mode only — not shared with the normal clip player.
 */
class GameModePlayer(context: Context) {

    private val appContext = context.applicationContext
    private var released = false

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        setSeekParameters(SeekParameters.CLOSEST_SYNC)
        repeatMode = Player.REPEAT_MODE_ALL
        volume = 1f
        playbackParameters = PlaybackParameters(DEFAULT_SPEED, 1f)
    }

    val speedController = PlaybackSpeedController(exoPlayer)

    fun load(uri: Uri, startMs: Long = 0L) {
        if (released) return
        speedController.reset()
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        if (startMs > 0) {
            exoPlayer.seekTo(startMs)
        }
        exoPlayer.playbackParameters = PlaybackParameters(DEFAULT_SPEED, 1f)
        exoPlayer.pause()
    }

    fun applySpeed(speed: Float, muted: Boolean) {
        if (released) return
        speedController.applySpeed(speed, muted)
    }

    fun setMuted(muted: Boolean) {
        if (released) return
        exoPlayer.volume = if (muted) 0f else 1f
    }

    fun playSegmentLoop(startMs: Long, endMs: Long) {
        if (released) return
        exoPlayer.seekTo(startMs)
        exoPlayer.play()
    }

    fun pause() {
        if (released) return
        exoPlayer.pause()
    }

    fun release() {
        if (released) return
        released = true
        speedController.reset()
        exoPlayer.release()
    }

    companion object {
        const val MAX_PLAYBACK_SPEED = 2.5f
        const val MIN_MOVING_SPEED = 0.01f
        private const val DEFAULT_SPEED = 1f
    }
}
