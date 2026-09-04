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
 * Optimizată pentru extragerea RDS de la posturile românești (Kiss, ZU, Digi, Prahova).
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
            _rdsInfo.value = RdsInfo(text = "Eroare de conexiune", isAvailable = true)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            // (RO) Media3 mapează automat metadatele dinamice (ICY) aici.
            // Verificăm câmpurile titlu, artist și subtitlu (folosit des de servere românești)
            val title = mediaMetadata.title ?: mediaMetadata.displayTitle
            val artist = mediaMetadata.artist
            val subtitle = mediaMetadata.subtitle

            val stationName = currentStation?.name?.lowercase() ?: ""
            
            // Verificăm dacă informația primită este diferită de numele postului (RDS real)
            val hasRealTitle = !title.isNullOrBlank() && title.toString().lowercase().trim() != stationName
            val hasRealArtist = !artist.isNullOrBlank() && artist.toString().lowercase().trim() != stationName
            val hasRealSubtitle = !subtitle.isNullOrBlank() && subtitle.toString().lowercase().trim() != stationName

            val rdsText = when {
                hasRealArtist && hasRealTitle -> "$artist - $title"
                hasRealTitle -> title.toString()
                hasRealSubtitle -> subtitle.toString()
                else -> null
            }

            if (rdsText != null) {
                _rdsInfo.value = RdsInfo(text = rdsText.trim(), isAvailable = true)
            } else {
                // Dacă nu avem date noi, păstrăm info despre post
                showStationInfo()
            }
        }

        override fun onMetadata(metadata: Metadata) {
            // (RO) Suport direct pentru ICY (Shoutcast) - backup critic
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    val title = entry.title
                    if (!title.isNullOrBlank() && title.lowercase().trim() != currentStation?.name?.lowercase()) {
                        _rdsInfo.value = RdsInfo(text = title.trim(), isAvailable = true)
                    }
                }
            }
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
        showStationInfo() // Afișăm imediat info post până la încărcarea RDS
        
        val player = controller
        if (player != null) {
            // (RO) MediaItem-ul setat prin controller va fi trimis serviciului.
            // IMPORTANT: Setăm metadatele minime pentru a nu "confunda" extractorul RDS din serviciu.
            val mediaItem = MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        // Folosim station name ca display title, dar lăsăm 'title' gol 
                        // ca să fie populat de fluxul ICY/RDS.
                        .setDisplayTitle(station.name)
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
