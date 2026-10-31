package com.sorin.radiotuner.player

import com.sorin.radiotuner.data.model.RdsInfo
import com.sorin.radiotuner.data.model.Station
import kotlinx.coroutines.flow.StateFlow

/**
 * (RO) Stările posibile ale redării — comune pentru Android și iOS.
 */
sealed class PlaybackState {
    data object Idle : PlaybackState()
    data object Buffering : PlaybackState()
    data object Playing : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

/**
 * (RO) Contract pentru redarea audio.
 * Pe Android e implementat cu ExoPlayer / Media3.
 * Pe iOS e implementat cu AVPlayer.
 */
interface RadioPlayer {
    val playbackState: StateFlow<PlaybackState>
    val rdsInfo: StateFlow<RdsInfo?>

    fun play(station: Station)
    fun stop()
    fun release()
}
