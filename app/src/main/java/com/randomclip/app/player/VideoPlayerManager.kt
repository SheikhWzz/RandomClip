package com.randomclip.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.randomclip.app.model.ClipSelection

/**
 * Single ExoPlayer instance reused across clips.
 * Hardware decoding via MediaCodec is used by default (no software fallback forced).
 */
class VideoPlayerManager(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .build()
        .apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            setSeekParameters(SeekParameters.CLOSEST_SYNC)
            repeatMode = Player.REPEAT_MODE_OFF
        }

    private var stagedNext: ClipSelection? = null

    fun applySettings(muted: Boolean, speed: Float) {
        player.volume = if (muted) 0f else 1f
        player.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    fun stageNextClip(selection: ClipSelection) {
        stagedNext = selection
    }

    fun playClip(selection: ClipSelection) {
        val uri = selection.video.uri
        val currentUri = player.currentMediaItem?.localConfiguration?.uri

        if (currentUri == uri && player.playbackState != Player.STATE_IDLE) {
            player.seekTo(selection.startPositionMs)
        } else {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.seekTo(selection.startPositionMs)
        }

        player.playWhenReady = true
        stagedNext = null
    }

    fun playStagedOr(selection: ClipSelection) {
        val staged = stagedNext
        if (staged != null && staged.video.uri == selection.video.uri &&
            staged.startPositionMs == selection.startPositionMs
        ) {
            playClip(staged)
        } else {
            playClip(selection)
        }
    }

    fun release() {
        player.release()
        stagedNext = null
    }
}
