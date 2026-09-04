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
 * Optimizată pentru extragerea RDS agresivă de la posturile românești (Kiss, ZU, ProFM, Capital).
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
            // (RO) Media3 mapează automat metadatele ICY (Shoutcast) aici.
            processAdvancedMetadata(mediaMetadata)
        }

        override fun onMetadata(metadata: Metadata) {
            // (RO) Suport direct pentru metadate ICY brute (critic pentru Kiss FM / ZU)
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    processRawTitle(entry.title)
                }
            }
        }
    }

    private fun processAdvancedMetadata(metadata: MediaMetadata) {
        // (RO) Verificăm toate câmpurile posibile unde serverele românești pot trimite melodia
        val candidates = listOfNotNull(
            metadata.title,
            metadata.artist,
            metadata.displayTitle,
            metadata.subtitle
        ).map { it.toString().trim() }.filter { it.isNotEmpty() }

        val stationName = currentStation?.name?.lowercase() ?: ""
        
        // (RO) Căutăm prima informație care NU este numele postului
        val dynamicInfo = candidates.firstOrNull { 
            val low = it.lowercase()
            low != stationName && !low.contains("stream title")
        }

        if (dynamicInfo != null) {
            // Dacă avem artist și titlu separat, le formatăm frumos
            val artist = metadata.artist?.toString()?.trim()
            val title = metadata.title?.toString()?.trim()
            
            val finalRds = if (!artist.isNullOrBlank() && !title.isNullOrBlank() && title.lowercase() != stationName) {
                "$artist - $title"
            } else {
                dynamicInfo
            }
            _rdsInfo.value = RdsInfo(text = finalRds, isAvailable = true)
        } else {
            showStationInfo()
        }
    }

    private fun processRawTitle(rawTitle: String?) {
        if (rawTitle.isNullOrBlank()) return
        
        val stationName = currentStation?.name?.lowercase() ?: ""
        val lowTitle = rawTitle.lowercase().trim()
        
        if (lowTitle != stationName && !lowTitle.contains("stream title")) {
            _rdsInfo.value = RdsInfo(text = rawTitle.trim(), isAvailable = true)
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
            // (RO) IMPORTANT: Nu setăm Titlul sau Artistul aici.
            // Le lăsăm goale pentru a permite extractorului RDS să le populeze cu date live.
            val mediaItem = MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        // Setăm doar info de bază în câmpuri care nu sunt folosite de RDS-ul live
                        .setStation(station.name)
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
