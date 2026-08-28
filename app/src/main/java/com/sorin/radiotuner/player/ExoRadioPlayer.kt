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
 */
class ExoRadioPlayer(private val context: Context) : RadioPlayer {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _rdsInfo = MutableStateFlow<RdsInfo?>(null)
    override val rdsInfo: StateFlow<RdsInfo?> = _rdsInfo.asStateFlow()

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
            _playbackState.value = PlaybackState.Error(error.message ?: "Eroare de redare necunoscută")
            _rdsInfo.value = RdsInfo(text = "", isAvailable = false)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            // (RO) Media3 mapează metadatele ICY (numele melodiei) în câmpul 'title'.
            val title = mediaMetadata.title ?: mediaMetadata.displayTitle
            if (!title.isNullOrBlank()) {
                _rdsInfo.value = RdsInfo(text = title.toString(), isAvailable = true)
            }
        }

        override fun onMetadata(metadata: Metadata) {
            // (RO) Suport suplimentar pentru stream-uri care trimit IcyInfo direct.
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    val title = entry.title
                    if (!title.isNullOrBlank()) {
                        _rdsInfo.value = RdsInfo(text = title, isAvailable = true)
                    }
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
        _rdsInfo.value = null
        val player = controller
        if (player != null) {
            val mediaItem = MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setArtist(station.genre)
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
        // (RO) Oprim playerul definitiv și curățăm lista de redare.
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        _playbackState.value = PlaybackState.Idle
        _rdsInfo.value = null
    }

    override fun release() {
        controller?.removeListener(playerListener)
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
