package com.randomclip.app.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.randomclip.app.model.ClipSelection

/**
 * Dual ExoPlayer pool: one plays the current clip, the other preloads the next.
 * seekTo() is always applied after STATE_READY (fixes ignored seeks before prepare).
 */
class VideoPlayerManager(context: Context) {

    private val appContext = context.applicationContext

    private val playerA: ExoPlayer = createPlayer()
    private val playerB: ExoPlayer = createPlayer()

    private var activePlayer: ExoPlayer = playerA
    private var preloadPlayer: ExoPlayer = playerB

    private var activePendingSeekMs: Long? = null
    private var preloadPendingSeekMs: Long? = null
    private var preloadedClip: ClipSelection? = null

    var onActiveClipEnded: (() -> Unit)? = null
    var onIsPlayingChanged: ((Boolean) -> Unit)? = null
    var onPlayerError: ((android.net.Uri, Throwable) -> Unit)? = null

    /** Active player for binding to PlayerView — swaps when preloaded clip is used. */
    val player: ExoPlayer
        get() = activePlayer

    init {
        playerA.addListener(createListener(playerA))
        playerB.addListener(createListener(playerB))
    }

    private fun createPlayer(): ExoPlayer =
        ExoPlayer.Builder(appContext).build().apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            setSeekParameters(SeekParameters.CLOSEST_SYNC)
            repeatMode = Player.REPEAT_MODE_OFF
        }

    private fun createListener(target: ExoPlayer): Player.Listener =
        object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                target.currentMediaItem?.localConfiguration?.uri?.let { uri ->
                    onPlayerError?.invoke(uri, error)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> handlePlayerReady(target)
                    Player.STATE_ENDED -> {
                        if (target === activePlayer) {
                            onActiveClipEnded?.invoke()
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (target === activePlayer) {
                    onIsPlayingChanged?.invoke(isPlaying)
                }
            }
        }

    private fun handlePlayerReady(target: ExoPlayer) {
        when (target) {
            activePlayer -> {
                activePendingSeekMs?.let { seekMs ->
                    target.seekTo(seekMs)
                    activePendingSeekMs = null
                }
                target.playWhenReady = true
            }

            preloadPlayer -> {
                preloadPendingSeekMs?.let { seekMs ->
                    target.seekTo(seekMs)
                    preloadPendingSeekMs = null
                }
                target.playWhenReady = false
            }
        }
    }

    fun applySettings(muted: Boolean, speed: Float) {
        val volume = if (muted) 0f else 1f
        val playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        playerA.volume = volume
        playerB.volume = volume
        playerA.setPlaybackSpeed(playbackSpeed)
        playerB.setPlaybackSpeed(playbackSpeed)
    }

    fun playClip(selection: ClipSelection) {
        if (preloadedClip == selection && preloadPlayer.playbackState != Player.STATE_IDLE) {
            swapPlayers()
            activePlayer.playWhenReady = true
            preloadedClip = null
            onIsPlayingChanged?.invoke(activePlayer.isPlaying)
            return
        }

        clearPreload()
        activePlayer.playWhenReady = false
        activePendingSeekMs = selection.startPositionMs
        activePlayer.setMediaItem(MediaItem.fromUri(selection.video.uri))
        activePlayer.prepare()
    }

    fun preloadClip(selection: ClipSelection) {
        if (preloadedClip == selection) return

        preloadPlayer.playWhenReady = false
        preloadPendingSeekMs = selection.startPositionMs
        preloadPlayer.setMediaItem(MediaItem.fromUri(selection.video.uri))
        preloadPlayer.prepare()
        preloadedClip = selection
    }

    fun togglePlayPause() {
        if (activePlayer.isPlaying) {
            activePlayer.pause()
        } else {
            activePlayer.play()
        }
    }

    fun pause() {
        activePlayer.pause()
    }

    fun play() {
        activePlayer.play()
    }

    fun stopClip() {
        activePlayer.pause()
    }

    private fun swapPlayers() {
        activePlayer.playWhenReady = false
        val newActive = preloadPlayer
        val newPreload = activePlayer
        activePlayer = newActive
        preloadPlayer = newPreload
        activePendingSeekMs = null
        preloadPendingSeekMs = null
        preloadedClip = null
        preloadPlayer.stop()
        preloadPlayer.clearMediaItems()
    }

    private fun clearPreload() {
        preloadPlayer.stop()
        preloadPlayer.clearMediaItems()
        preloadedClip = null
        preloadPendingSeekMs = null
    }

    fun release() {
        playerA.release()
        playerB.release()
        preloadedClip = null
        activePendingSeekMs = null
        preloadPendingSeekMs = null
    }
}
