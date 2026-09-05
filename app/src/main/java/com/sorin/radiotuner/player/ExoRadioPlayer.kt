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
 * Optimizată pentru extragerea RDS agresivă (Artist - Melodie) de la posturile românești.
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
            // (RO) Media3 mapează automat metadatele ICY (Shoutcast/Icecast) aici.
            // Pentru posturile românești, titlul melodiei vine de obicei în câmpul 'title'.
            processMetadata(mediaMetadata)
        }

        override fun onMetadata(metadata: Metadata) {
            // (RO) Suport direct pentru metadate ICY brute - foarte important pentru Kiss/ZU/Digi
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    val streamTitle = entry.title
                    if (!streamTitle.isNullOrBlank()) {
                        updateRdsText(streamTitle)
                    }
                }
            }
        }
    }

    private fun processMetadata(metadata: MediaMetadata) {
        val title = metadata.title?.toString()
        val artist = metadata.artist?.toString()
        val displayTitle = metadata.displayTitle?.toString()
        
        // (RO) Prioritizăm Title/Artist dacă sunt prezente și diferite de numele postului
        val stationName = currentStation?.name ?: ""
        
        val filteredTitle = if (title?.trim()?.lowercase() != stationName.lowercase().trim()) title else null
        val filteredArtist = if (artist?.trim()?.lowercase() != stationName.lowercase().trim()) artist else null
        
        when {
            !filteredArtist.isNullOrBlank() && !filteredTitle.isNullOrBlank() -> {
                updateRdsText("$filteredArtist - $filteredTitle")
            }
            !filteredTitle.isNullOrBlank() -> {
                updateRdsText(filteredTitle)
            }
            !displayTitle.isNullOrBlank() && displayTitle.trim().lowercase() != stationName.lowercase().trim() -> {
                updateRdsText(displayTitle)
            }
            else -> {
                // Dacă nu avem metadate noi, afișăm informația de bază a postului
                showStationInfo()
            }
        }
    }

    private fun updateRdsText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            _rdsInfo.value = RdsInfo(text = trimmed, isAvailable = true)
        }
    }

    private fun showStationInfo() {
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
        showStationInfo()
        
        val player = controller
        if (player != null) {
            // (RO) NU setăm Titlul sau Artistul aici.
            // Le lăsăm goale în MediaItem pentru a forța playerul să accepte metadatele dinamice.
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
