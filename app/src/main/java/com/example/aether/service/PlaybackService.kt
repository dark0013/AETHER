package com.example.aether.service

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.aether.util.AetherLog
import com.google.common.util.concurrent.Futures
import kotlin.math.exp
import kotlin.math.ln
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var playerA: ExoPlayer
    private lateinit var playerB: ExoPlayer
    private var activePlayer: ExoPlayer? = null

    private var isRitualMode: Boolean = false
    private var nextEndPointMs: Long = -1L
    private var nextStartPointMs: Long = 0L
    private var nextFadeDurationMs: Long = 1000L

    private val handler = Handler(Looper.getMainLooper())
    private val fadeRunnables = mutableListOf<Runnable>()
    private var crossfadeCheckScheduled = false
    private val crossfadeCheckRunnable = object : Runnable {
        override fun run() {
            checkCrossfade()
            val playing = activePlayer?.isPlaying == true
            if (playing) {
                handler.postDelayed(this, 100)
            } else {
                crossfadeCheckScheduled = false
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startCrossfadeCheck() else stopCrossfadeCheck()
        }

        override fun onPlayerError(error: PlaybackException) {
            AetherLog.e(TAG, "Playback error: ${error.errorCodeName}", error)
            skipUnreadableTrack()
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        playerA = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        playerB = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        playerA.addListener(playerListener)
        playerB.addListener(playerListener)

        activePlayer = playerA
        mediaSession = MediaSession.Builder(this, playerA)
            .setCallback(AetherSessionCallback())
            .build()
    }

    private fun startCrossfadeCheck() {
        if (crossfadeCheckScheduled) return
        crossfadeCheckScheduled = true
        handler.post(crossfadeCheckRunnable)
    }

    private fun stopCrossfadeCheck() {
        handler.removeCallbacks(crossfadeCheckRunnable)
        crossfadeCheckScheduled = false
    }

    private fun skipUnreadableTrack() {
        val player = activePlayer ?: return
        cancelFades()
        player.volume = 1f
        nextEndPointMs = -1L
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.prepare()
            player.play()
        } else {
            player.pause()
        }
    }

    private fun checkCrossfade() {
        val player = activePlayer ?: return
        if (!player.isPlaying) return
        if (nextEndPointMs > 0 && player.currentPosition >= nextEndPointMs) {
            startCrossfade()
        } else if (isRitualMode && player.duration > 0 && player.currentPosition >= player.duration - 1600) {
            startResidueAndStop()
        }
    }

    private fun startResidueAndStop() {
        val player = activePlayer ?: return
        fadeVolume(player, player.volume, 0f, 1600L) {
            player.pause()
            player.seekTo(0)
            player.volume = 1f
        }
    }

    private fun startCrossfade() {
        val oldPlayer = activePlayer ?: return
        val nextIndex = oldPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) {
            fallbackHardCut()
            return
        }

        val nextItem = try {
            oldPlayer.getMediaItemAt(nextIndex)
        } catch (_: Exception) {
            fallbackHardCut()
            return
        }

        val newPlayer = if (oldPlayer == playerA) playerB else playerA
        cancelFades()

        newPlayer.stop()
        newPlayer.volume = 0f
        newPlayer.setMediaItem(nextItem)
        newPlayer.seekTo(nextStartPointMs)
        newPlayer.prepare()
        newPlayer.play()

        fadeVolume(oldPlayer, oldPlayer.volume.coerceAtLeast(0.01f), 0f, nextFadeDurationMs) {
            oldPlayer.pause()
            oldPlayer.stop()
            oldPlayer.volume = 1f
        }
        fadeVolume(newPlayer, 0f, 1f, nextFadeDurationMs)

        activePlayer = newPlayer
        mediaSession?.player = newPlayer
        nextEndPointMs = -1L
    }

    private fun fallbackHardCut() {
        val player = activePlayer ?: return
        nextEndPointMs = -1L
        if (!player.hasNextMediaItem()) return
        fadeVolume(player, player.volume.coerceAtLeast(0.01f), 0f, 80L) {
            player.seekToNextMediaItem()
            player.volume = 1f
            player.prepare()
            player.play()
        }
    }

    private fun fadeVolume(player: Player, from: Float, to: Float, duration: Long, onEnd: (() -> Unit)? = null) {
        val steps = 12
        val interval = (duration / steps).coerceAtLeast(16L)
        val start = from.coerceIn(0.0001f, 1f)
        val end = to.coerceIn(0.0001f, 1f)
        val logStart = ln(start.toDouble())
        val logEnd = ln(end.toDouble())

        var currentStep = 0
        val runnable = object : Runnable {
            override fun run() {
                if (currentStep <= steps) {
                    val t = currentStep / steps.toFloat()
                    try {
                        val gain = exp(logStart + (logEnd - logStart) * t).toFloat()
                        player.volume = if (to == 0f && currentStep == steps) 0f else gain.coerceIn(0f, 1f)
                    } catch (_: Exception) {
                        fadeRunnables.remove(this)
                        onEnd?.invoke()
                        return
                    }
                    currentStep++
                    handler.postDelayed(this, interval)
                } else {
                    fadeRunnables.remove(this)
                    onEnd?.invoke()
                }
            }
        }
        fadeRunnables.add(runnable)
        handler.post(runnable)
    }

    private fun cancelFades() {
        fadeRunnables.forEach { handler.removeCallbacks(it) }
        fadeRunnables.clear()
    }

    private inner class AetherSessionCallback : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == "SET_TRANSITION_PLAN") {
                nextEndPointMs = args.getLong("end_point_ms", -1L)
                nextStartPointMs = args.getLong("start_point_ms", 0L)
                nextFadeDurationMs = args.getLong("fade_duration_ms", 1000L)
                AetherLog.d(TAG, "Plan End=$nextEndPointMs Start=$nextStartPointMs")
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            if (customCommand.customAction == "SET_RITUAL_MODE") {
                isRitualMode = args.getBoolean("is_ritual", false)
                if (isRitualMode) {
                    activePlayer?.repeatMode = Player.REPEAT_MODE_OFF
                }
                AetherLog.d(TAG, "Ritual Mode: $isRitualMode")
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = activePlayer ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopCrossfadeCheck()
        cancelFades()
        playerA.removeListener(playerListener)
        playerB.removeListener(playerListener)
        mediaSession?.run {
            playerA.release()
            playerB.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PlaybackService"
    }
}
