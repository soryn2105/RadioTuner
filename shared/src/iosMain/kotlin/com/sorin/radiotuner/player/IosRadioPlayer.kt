package com.sorin.radiotuner.player

import com.sorin.radiotuner.data.model.RdsInfo
import com.sorin.radiotuner.data.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.*
import platform.Foundation.NSURL
import platform.darwin.NSObject

/**
 * (RO)
 * Implementarea RadioPlayer pentru iPhone (iOS) folosind AVPlayer din SDK-ul Apple.
 * Activează automat AVAudioSession pentru redare în fundal (când ecranul este oprit).
 */
class IosRadioPlayer : RadioPlayer {

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _rdsInfo = MutableStateFlow<RdsInfo?>(null)
    override val rdsInfo: StateFlow<RdsInfo?> = _rdsInfo.asStateFlow()

    private var player: AVPlayer? = null
    private var currentStation: Station? = null

    init {
        configureAudioSession()
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun configureAudioSession() {
        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
            audioSession.setActive(true, error = null)
        } catch (_: Exception) {
            // Sesiunea audio e fallback
        }
    }

    override fun play(station: Station) {
        currentStation = station
        _playbackState.value = PlaybackState.Buffering
        _rdsInfo.value = RdsInfo(text = station.name, isAvailable = true)

        try {
            val url = NSURL.URLWithString(station.streamUrl) ?: run {
                _playbackState.value = PlaybackState.Error("URL invalid: ${station.streamUrl}")
                return
            }

            val playerItem = AVPlayerItem(uRL = url)
            if (player == null) {
                player = AVPlayer(playerItem = playerItem)
            } else {
                player?.replaceCurrentItemWithPlayerItem(playerItem)
            }

            player?.play()
            _playbackState.value = PlaybackState.Playing
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.Error(e.message ?: "Eroare la pornirea redării pe iOS")
        }
    }

    override fun stop() {
        player?.pause()
        _playbackState.value = PlaybackState.Idle
        _rdsInfo.value = null
    }

    override fun release() {
        stop()
        player = null
        currentStation = null
    }
}
