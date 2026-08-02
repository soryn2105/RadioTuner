package com.sorin.radiotuner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.FavoritesRepository
import com.sorin.radiotuner.data.repository.StationRepository
import com.sorin.radiotuner.player.PlaybackState
import com.sorin.radiotuner.player.RadioPlayer
import com.sorin.radiotuner.util.ConnectivityObserver
import com.sorin.radiotuner.util.ReconnectStrategy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * (RO)
 * Singurul loc din aplicație unde se ia decizii de tip "ce se întâmplă
 * când utilizatorul apasă X". UI-ul (Compose) apelează doar funcțiile
 * publice de aici (ex. selectStation, togglePlayStop) și citește
 * starea din `uiState` — nu conține el însuși logică de business.
 * Depinde DOAR de interfețe (StationRepository, FavoritesRepository,
 * RadioPlayer, ConnectivityObserver) — vezi ARHITECTURA.md.
 * (EN)
 * This is the only place in the app where decisions are made about "what happens
 * when the user presses X". The UI (Compose) only calls the public functions
 * here (e.g., selectStation, togglePlayStop) and reads the state from `uiState` — it does not contain business logic itself.
 * It depends ONLY on interfaces (StationRepository, FavoritesRepository, RadioPlayer, ConnectivityObserver) — see ARHITECTURA.md
 */

class RadioViewModel(
    private val stationRepository: StationRepository,
    private val favoritesRepository: FavoritesRepository,
    private val radioPlayer: RadioPlayer,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val reconnectStrategy = ReconnectStrategy()
    private var reconnectJob: Job? = null
    private var wasPlayingBeforeOffline = false

    init {
        observeFavorites()
        observeConnectivity()
        observePlayback()
        search("") // încarcă o listă inițială (populară / implicită)
    }

    // ---------------------------------------------------------------
    // Căutare
    // ---------------------------------------------------------------

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        search(newQuery)
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val result = stationRepository.search(query)
            result.onSuccess { stations ->
                _uiState.value = _uiState.value.copy(
                    foundStations = stations,
                    isSearching = false,
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = error.message ?: "Eroare la căutare"
                )
            }
        }
    }

    // ---------------------------------------------------------------
    // Favorite
    // ---------------------------------------------------------------

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.observeFavorites().collectLatest { favorites ->
                _uiState.value = _uiState.value.copy(favoriteStations = favorites)
            }
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(station)
        }
    }

    // ---------------------------------------------------------------
    // Redare
    // ---------------------------------------------------------------

    fun selectStation(station: Station) {
        _uiState.value = _uiState.value.copy(currentStation = station, isMenuOpen = false)
        radioPlayer.play(station)
    }

    fun togglePlayStop() {
        val current = _uiState.value.currentStation ?: return
        if (_uiState.value.playbackState is PlaybackState.Playing) {
            radioPlayer.stop()
        } else {
            radioPlayer.play(current)
        }
    }

    fun skipToNext() = skip(delta = 1)
    fun skipToPrevious() = skip(delta = -1)

    private fun skip(delta: Int) {
        val list = _uiState.value.foundStations.ifEmpty { _uiState.value.favoriteStations }
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _uiState.value.currentStation?.id }
        val nextIndex = (currentIndex + delta).let { i -> ((i % list.size) + list.size) % list.size }
        selectStation(list[nextIndex])
    }

    private fun observePlayback() {
        viewModelScope.launch {
            radioPlayer.playbackState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(playbackState = state)
            }
        }
        viewModelScope.launch {
            radioPlayer.rdsInfo.collectLatest { rds ->
                _uiState.value = _uiState.value.copy(rdsInfo = rds)
            }
        }
    }

    // ---------------------------------------------------------------
    // Conexiune la internet + reconectare anti-buclă
    // ---------------------------------------------------------------

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { isOnline ->
                val wasOnline = _uiState.value.isOnline
                _uiState.value = _uiState.value.copy(isOnline = isOnline)

                if (!isOnline) {
                    // Am pierdut internetul: reținem dacă reda ceva, oprim playerul
                    // (evită să rămână "agățat" în buffering la nesfârșit) și anulăm
                    // orice încercare de reconectare în curs.
                    wasPlayingBeforeOffline = _uiState.value.playbackState is PlaybackState.Playing
                    reconnectJob?.cancel()
                    if (wasPlayingBeforeOffline) radioPlayer.stop()
                } else if (!wasOnline && wasPlayingBeforeOffline) {
                    // Internetul a revenit și rulam ceva înainte: reconectăm,
                    // dar prin ReconnectStrategy (backoff), nu instant, ca să nu
                    // intrăm într-o buclă de reîncercări dacă rețeaua e instabilă.
                    scheduleReconnect()
                }
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            val delayMillis = reconnectStrategy.nextDelayMillis()
            delay(delayMillis)
            val station = _uiState.value.currentStation
            if (_uiState.value.isOnline && station != null) {
                radioPlayer.play(station)
                reconnectStrategy.reset()
            }
        }
    }

    // ---------------------------------------------------------------
    // Meniu (mod portret)
    // ---------------------------------------------------------------

    fun toggleMenu(open: Boolean) {
        _uiState.value = _uiState.value.copy(isMenuOpen = open)
    }

    override fun onCleared() {
        super.onCleared()
        radioPlayer.release()
    }
}