package com.sorin.radiotuner.ui.viewmodel

import com.sorin.radiotuner.data.model.CountryOption
import com.sorin.radiotuner.data.model.RdsInfo
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.player.PlaybackState

/**
 * (RO)
 * O singură clasă cu TOATĂ starea de care are nevoie ecranul principal.
 * UI-ul (Compose) doar "citește" acest obiect și se redesenează automat
 * când se schimbă — nu ține propria stare împrăștiată prin componente.
 * (EN)
 * A single class with ALL the state needed by the main screen.
 * The UI (Compose) just "reads" this object and automatically redraws
 * when it changes — it doesn't hold its own state scattered across components.
 */
data class RadioUiState(
    val query: String = "",
    val foundStations: List<Station> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val currentStation: Station? = null,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val rdsInfo: RdsInfo? = null,
    val isOnline: Boolean = true,
    val isSearching: Boolean = false,
    val isMenuOpen: Boolean = false,
    val errorMessage: String? = null,
    // --- filtrare pe țară + paginare ("încarcă mai multe") ---
    val countries: List<CountryOption> = emptyList(),
    val selectedCountry: CountryOption? = null,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false
)
