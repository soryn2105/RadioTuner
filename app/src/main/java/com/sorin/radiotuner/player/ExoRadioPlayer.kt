package com.sorin.radiotuner.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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
    private var currentDynamicRds: String? = null

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
            updateRdsText(
                title = mediaMetadata.title?.toString(),
                artist = mediaMetadata.artist?.toString(),
                displayTitle = mediaMetadata.displayTitle?.toString()
            )
        }

        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            // (RO) Suport direct pentru ICY (Shoutcast) - foarte important pentru Kiss/ZU/Digi
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    val streamTitle = entry.title
                    if (!streamTitle.isNullOrBlank()) {
                        updateRdsText(title = streamTitle, artist = null, displayTitle = null)
                    }
                }
            }
        }
    }

    private fun updateRdsText(title: String?, artist: String?, displayTitle: String?) {
        val stationName = currentStation?.name?.lowercase() ?: ""
        
        // (RO) Încercăm să găsim cea mai bună informație dinamică
        val candidates = listOfNotNull(title, artist, displayTitle)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Căutăm un text care nu este doar numele postului
        val dynamicInfo = candidates.firstOrNull { 
            it.lowercase() != stationName && !it.lowercase().contains("stream title")
        }

        if (dynamicInfo != null) {
            // Dacă avem artist și titlu separat, le combinăm
            val finalRds = if (!artist.isNullOrBlank() && !title.isNullOrBlank() && title.lowercase() != stationName) {
                "$artist - $title"
            } else {
                dynamicInfo
            }
            currentDynamicRds = finalRds
            _rdsInfo.value = RdsInfo(text = finalRds, isAvailable = true)
        } else {
            // Dacă nu avem RDS nou, păstrăm ultimul RDS dinamic dacă există,
            // prevenind suprscrierea cu numele postului de către pachetele periodice de mentenanță.
            if (currentDynamicRds != null) {
                _rdsInfo.value = RdsInfo(text = currentDynamicRds!!, isAvailable = true)
            } else {
                currentStation?.let {
                    _rdsInfo.value = RdsInfo(text = "${it.name} (${it.genre})", isAvailable = true)
                }
            }
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
        currentDynamicRds = null
        // Info inițial (până vine RDS-ul)
        _rdsInfo.value = RdsInfo(text = "${station.name} (${station.genre})", isAvailable = true)
        
        val player = controller
        if (player != null) {
            // (RO) NU setăm Title/Artist în metadata inițială pentru a lăsa 
            // extractorul de metadate să lucreze fără interferențe.
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
        currentDynamicRds = null
        currentStation = null
    }

    override fun release() {
        controller?.removeListener(playerListener)
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
