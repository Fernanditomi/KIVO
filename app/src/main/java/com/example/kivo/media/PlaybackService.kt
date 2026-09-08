package com.example.kivo.media

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    override fun onCreate() {
        super.onCreate()
        
        // Configure Audio Attributes for proper Audio Focus handling
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Handles focus automatically
            .build()
        
        val intent = Intent(this, com.example.kivo.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()

        // Setup Audio Effects when audio session is available
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    setupAudioEffects(player.audioSessionId)
                }
            }
        })
    }

    private fun setupAudioEffects(sessionId: Int) {
        if (equalizer == null) {
            try {
                equalizer = Equalizer(0, sessionId).apply { enabled = true }
                bassBoost = BassBoost(0, sessionId).apply { enabled = true }
                virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                "SET_EQUALIZER_GAIN" -> {
                    val band = args.getInt("band")
                    val gain = args.getInt("gain")
                    equalizer?.setBandLevel(band.toShort(), gain.toShort())
                }
                "SET_BASS_BOOST" -> {
                    val strength = args.getInt("strength")
                    bassBoost?.setStrength(strength.toShort())
                }
                "SET_VIRTUALIZER" -> {
                    val strength = args.getInt("strength")
                    virtualizer?.setStrength(strength.toShort())
                }
                "GET_EQUALIZER_DATA" -> {
                    val eq = equalizer
                    if (eq != null) {
                        val resultBundle = Bundle().apply {
                            putInt("numBands", eq.numberOfBands.toInt())
                            putShortArray("bandLevels", ShortArray(eq.numberOfBands.toInt()) { i -> eq.getBandLevel(i.toShort()) })
                            putInt("minLevel", eq.bandLevelRange[0].toInt())
                            putInt("maxLevel", eq.bandLevelRange[1].toInt())
                            putInt("bassStrength", bassBoost?.roundedStrength?.toInt() ?: 0)
                            putInt("virtStrength", virtualizer?.roundedStrength?.toInt() ?: 0)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
