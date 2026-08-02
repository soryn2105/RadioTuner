package com.sorin.radiotuner.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.icy.IcyInfo
import com.sorin.radiotuner.data.model.RdsInfo
import com.sorin.radiotuner.data.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * (RO)
 * Implementarea reală a RadioPlayer, folosind ExoPlayer (Media3).
 * NOTĂ DE VERIFICAT LA PRIMA RULARE: citirea metadatelor ICY
 * (echivalentul "RDS" pentru radio online) se face prin Player.Listener.onMetadata(),
 * căutând un bloc IcyInfo. API-ul exact pentru metadate a variat puțin între versiunile
 * Media3 — dacă la compilare apare o eroare aici, e cel mai probabil loc
 * unde trebuie ajustat un import sau o semnătură.
 *
 *  (EN)
 * Real implementation of RadioPlayer, using ExoPlayer (Media3).
 * NOTE TO VERIFY ON FIRST RUN: reading ICY metadata (equivalent to
 * "RDS" for online radio) is done through Player.Listener.onMetadata(),
 * which searches for an IcyInfo block. The exact API for metadata has changed
 * a bit between versions of Media3 — if you see an error at first run, it's
 * probably where you need to adjust an import or a declaration.
 */
class ExoRadioPlayer(context: Context) : RadioPlayer {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _rdsInfo = MutableStateFlow<RdsInfo?>(null)
    override val rdsInfo: StateFlow<RdsInfo?> = _rdsInfo.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = when (state) {
                    Player.STATE_BUFFERING -> PlaybackState.Buffering
                    Player.STATE_READY -> if (exoPlayer.playWhenReady) PlaybackState.Playing else PlaybackState.Idle
                    Player.STATE_ENDED, Player.STATE_IDLE -> PlaybackState.Idle
                    else -> PlaybackState.Idle
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.value = PlaybackState.Error(error.message ?: "Eroare de redare necunoscută")
                _rdsInfo.value = RdsInfo(text = "", isAvailable = false)
            }

            override fun onMetadata(metadata: Metadata) {
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
        })
    }

    override fun play(station: Station) {
        _rdsInfo.value = null
        exoPlayer.setMediaItem(MediaItem.fromUri(station.streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun stop() {
        exoPlayer.playWhenReady = false
        exoPlayer.stop()
        _playbackState.value = PlaybackState.Idle
        _rdsInfo.value = null
    }

    override fun release() {
        exoPlayer.release()
    }
}