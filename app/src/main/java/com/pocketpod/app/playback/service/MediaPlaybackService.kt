package com.pocketpod.app.playback.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pocketpod.app.performance.j2mode.J2PerformanceMode

class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        // Create an audio attributes instance to handle audio focus automatically
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // True enables automatic audio focus handling
            .setHandleAudioBecomingNoisy(true) // Pause when headphones disconnect
            .build()

        val j2Mode = J2PerformanceMode(this)
        if (!j2Mode.isEnabled) {
            // Apply enhancements for non-low-end devices, e.g., crossfade, offload
        }

        mediaSession = MediaSession.Builder(this, player)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // If the user swipes the app away, we might want to stop the player if not playing
        if (player.playWhenReady.not() || player.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
}
