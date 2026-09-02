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
 * Gestionează extragerea avansată a RDS (Artist - Melodie).
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
            _rdsInfo.value = RdsInfo(text = "Reconectare...", isAvailable = true)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateRdsFromMetadata(mediaMetadata)
        }

        override fun onMetadata(metadata: Metadata) {
            // Suport fallback pentru ICY brut
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    val title = entry.title
                    if (!title.isNullOrBlank() && title.lowercase() != currentStation?.name?.lowercase()) {
                        _rdsInfo.value = RdsInfo(text = title, isAvailable = true)
                    }
                }
            }
        }
    }

    private fun updateRdsFromMetadata(metadata: MediaMetadata) {
        val title = metadata.title ?: metadata.displayTitle
        val artist = metadata.artist
        val station = metadata.station
        val subtitle = metadata.subtitle

        // Colectăm toți candidații pentru RDS
        val candidates = listOfNotNull(title, artist, displayTitleFrom(metadata), subtitle, station)
            .map { it.toString().trim() }
            .filter { it.isNotEmpty() }

        val stationName = currentStation?.name?.lowercase() ?: ""
        
        // Căutăm prima informație care NU este doar numele postului
        val dynamicInfo = candidates.firstOrNull { it.lowercase() != stationName }
        
        if (dynamicInfo != null) {
            // Dacă avem și artist și titlu separat, le combinăm frumos
            val finalRds = if (!artist.isNullOrBlank() && !title.isNullOrBlank() && title.toString().lowercase() != stationName) {
                "$artist - $title"
            } else {
                dynamicInfo
            }
            _rdsInfo.value = RdsInfo(text = finalRds, isAvailable = true)
        } else {
            // Fallback la info post
            currentStation?.let {
                _rdsInfo.value = RdsInfo(text = "${it.name} (${it.genre})", isAvailable = true)
            }
        }
    }

    private fun displayTitleFrom(metadata: MediaMetadata): CharSequence? {
        return metadata.displayTitle
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
        _rdsInfo.value = RdsInfo(text = "${station.name} (${station.genre})", isAvailable = true)
        
        val player = controller
        if (player != null) {
            // IMPORTANT: Nu setăm Titlul aici pentru a nu polua fluxul RDS
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
