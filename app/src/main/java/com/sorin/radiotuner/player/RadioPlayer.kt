package com.sorin.radiotuner.player

import com.sorin.radiotuner.data.model.RdsInfo
import com.sorin.radiotuner.data.model.Station
import kotlinx.coroutines.flow.StateFlow

/**
 * (RO)
 * Stările posibile ale redării — UI-ul reacționează la ele, nu la ExoPlayer direct.
 * (EN)
 * Possible radio playback states — UI reacts to them, not directly from ExoPlayer.
 */
sealed class PlaybackState {
    data object Idle : PlaybackState()
    data object Buffering : PlaybackState()
    data object Playing : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

/**
 * (RO)
 * Contract pentru redarea audio. RadioViewModel depinde DOAR de această
 * interfață — dacă mâine înlocuim ExoPlayer cu altă bibliotecă, se
 * schimbă doar implementarea (ExoRadioPlayer.kt), nu și ViewModel-ul
 * sau UI-ul.
 * (EN)
 * Audio playback contract. RadioViewModel depends ONLY on this
 * interface — if tomorrow we replace ExoPlayer with another library,
 * only the implementation (ExoRadioPlayer.kt) changes, not the ViewModel or UI.
 */
interface RadioPlayer {
    val playbackState: StateFlow<PlaybackState>
    val rdsInfo: StateFlow<RdsInfo?>

    fun play(station: Station)
    fun stop()
    fun release()
}