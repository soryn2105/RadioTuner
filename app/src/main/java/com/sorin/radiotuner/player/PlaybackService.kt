package com.sorin.radiotuner.player

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * (RO)
 * Serviciu de fundal care găzduiește sesiunea media și playerul.
 * Acesta permite aplicației să continue redarea chiar și când ecranul este stins
 * sau când utilizatorul iese din aplicație, prin intermediul unei notificări de tip "Foreground Service".
 *
 * (EN)
 * Background service that hosts the media session and the player.
 * This allows the app to continue playback even when the screen is off
 * or when the user leaves the app, through a "Foreground Service" notification.
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Gestionează automat focusul audio
            .setWakeMode(C.WAKE_MODE_NETWORK) // Permite playerului să țină CPU-ul activ
            .build()
            
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
