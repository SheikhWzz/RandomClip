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

    fun load(uri: Uri) {
        if (released) return
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playbackParameters = PlaybackParameters(DEFAULT_SPEED, 1f)
        exoPlayer.pause()
    }

    fun applySpeed(speed: Float) {
        if (released) return

        // Media3 requires speed > 0 — use pause() for "standby", never PlaybackParameters(0f).
        if (speed <= MIN_MOVING_SPEED) {
            exoPlayer.pause()
            return
        }

        val clamped = speed.coerceIn(MIN_MOVING_SPEED, MAX_PLAYBACK_SPEED)
        exoPlayer.playbackParameters = PlaybackParameters(clamped, 1f)
        if (!exoPlayer.isPlaying) {
            exoPlayer.play()
        }
    }

    fun pause() {
        if (released) return
        exoPlayer.pause()
    }

    fun release() {
        if (released) return
        released = true
        exoPlayer.release()
    }

    companion object {
        const val MAX_PLAYBACK_SPEED = 2.5f
        const val MIN_MOVING_SPEED = 0.01f
        private const val DEFAULT_SPEED = 1f
    }
}
