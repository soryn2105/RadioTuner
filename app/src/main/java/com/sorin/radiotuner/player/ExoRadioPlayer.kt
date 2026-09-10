package com.sorin.radiotuner.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sorin.radiotuner.data.model.RdsInfo
import com.sorin.radiotuner.data.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * (RO)
 * Implementarea RadioPlayer care se conectează la [PlaybackService].
 * Logică unificată pentru RDS (România + Global).
 */
class ExoRadioPlayer(private val context: Context) : RadioPlayer {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _rdsInfo = MutableStateFlow<RdsInfo?>(null)
    override val rdsInfo: StateFlow<RdsInfo?> = _rdsInfo.asStateFlow()
    
    private var currentStation: Station? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val player = controller ?: return
            _playbackState.value = when (state) {
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> if (player.playWhenReady) PlaybackState.Playing else PlaybackState.Idle
                Player.STATE_ENDED, Player.STATE_IDLE -> PlaybackState.Idle
                else -> PlaybackState.Idle
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.value = PlaybackState.Error(error.message ?: "Eroare de redare")
            _rdsInfo.value = RdsInfo(text = "Se reconectează...", isAvailable = true)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            // (RO) Unificăm datele: tratăm orice info dinamică drept RDS prioritat
            processDynamicMetadata(
                title = mediaMetadata.title?.toString(),
                artist = mediaMetadata.artist?.toString(),
                display = mediaMetadata.displayTitle?.toString()
            )
        }

        override fun onMetadata(metadata: Metadata) {
            // (RO) Captăm StreamTitle direct (metoda preferată de Kiss/ZU/ProFM)
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo && !entry.title.isNullOrBlank()) {
                    processDynamicMetadata(title = entry.title, artist = null, display = null)
                }
            }
        }
    }

    private fun processDynamicMetadata(title: String?, artist: String?, display: String?) {
        val stationName = currentStation?.name?.lowercase() ?: ""
        
        // (RO) Verificăm dacă informația primită este utilă (nu este doar numele postului)
        val validTitle = if (title?.lowercase()?.trim() != stationName) title?.trim() else null
        val validArtist = if (artist?.lowercase()?.trim() != stationName) artist?.trim() else null
        val validDisplay = if (display?.lowercase()?.trim() != stationName) display?.trim() else null

        val rdsText = when {
            !validArtist.isNullOrBlank() && !validTitle.isNullOrBlank() -> "$validArtist - $validTitle"
            !validTitle.isNullOrBlank() -> validTitle
            !validDisplay.isNullOrBlank() -> validDisplay
            else -> null
        }

        if (rdsText != null) {
            _rdsInfo.value = RdsInfo(text = rdsText, isAvailable = true)
        } else {
            showBaselineInfo()
        }
    }

    private fun showBaselineInfo() {
        currentStation?.let {
            _rdsInfo.value = RdsInfo(text = "${it.name} (${it.genre})", isAvailable = true)
        }
    }

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    override fun play(station: Station) {
        currentStation = station
        showBaselineInfo()
        
        val player = controller
        if (player != null) {
            // (RO) NU setăm Title/Artist în metadata-ul fix pentru a forța preluarea RDS-ului de pe stream
            val mediaItem = MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setAlbumTitle(station.country)
                        .setArtworkUri(station.imageUrl?.let { Uri.parse(it) })
                        .build()
                )
                .build()
            
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } else {
            controllerFuture?.addListener({
                play(station)
            }, MoreExecutors.directExecutor())
        }
    }

    override fun stop() {
        val player = controller ?: return
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        _playbackState.value = PlaybackState.Idle
        _rdsInfo.value = null
        currentStation = null
    }

    override fun release() {
        controller?.removeListener(playerListener)
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
