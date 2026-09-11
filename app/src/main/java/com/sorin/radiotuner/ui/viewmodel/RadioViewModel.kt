package com.sorin.radiotuner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorin.radiotuner.data.model.CountryOption
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

    /** Câte posturi cerem pe pagină — trebuie să corespundă cu "limit" din repository. */
    private val pageSize = 100

    init {
        observeFavorites()
        observeConnectivity()
        observePlayback()
        loadCountries()
        restoreState()
        search("") // încarcă o listă inițială
    }

    private fun restoreState() {
        viewModelScope.launch {
            favoritesRepository.observeLastTab().collectLatest { tab ->
                if (tab != null) {
                    _uiState.value = _uiState.value.copy(selectedTab = tab)
                }
            }
        }
        viewModelScope.launch {
            favoritesRepository.observeLastStation().collectLatest { station ->
                if (station != null && _uiState.value.currentStation == null) {
                    _uiState.value = _uiState.value.copy(currentStation = station)
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Tab & Stare UI
    // ---------------------------------------------------------------

    fun onTabSelected(tabName: String) {
        _uiState.value = _uiState.value.copy(selectedTab = tabName)
        viewModelScope.launch {
            favoritesRepository.saveLastTab(tabName)
        }
    }

    // ---------------------------------------------------------------
    // Căutare
    // ---------------------------------------------------------------

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        search(newQuery)
    }

    /**
     * (RO) O căutare nouă (text schimbat SAU țară schimbată) pornește
     * mereu de la offset 0 și înlocuiește lista — nu o adaugă la cea
     * veche (aia e treaba lui loadMore()).
     *
     * (EN) A new search (changed text OR changed country) always starts
     * at offset 0 and replaces the list — it doesn't append to the old
     * one (that's loadMore()'s job).
     */
    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, errorMessage = null)

            val selectedCountry = _uiState.value.selectedCountry
            val result = stationRepository.search(
                query,
                country = selectedCountry?.name,
                countryCode = selectedCountry?.code,
                language = null, 
                offset = 0
            )
            result.onSuccess { stations ->
                _uiState.value = _uiState.value.copy(
                    foundStations = stations,
                    isSearching = false,
                    errorMessage = null,
                    canLoadMore = stations.size >= pageSize
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = error.message ?: "Eroare la căutare",
                    canLoadMore = false
                )
            }
        }
    }

    /**
     * (RO) "Încarcă mai multe" — cere următoarea pagină (offset = câte
     * posturi avem deja) și o ADAUGĂ la lista curentă. Așa înlocuim
     * limita fixă de rezultate cu ceva controlat de utilizator, nu de
     * un număr fix în cod.
     *
     * (EN) "Load more" — requests the next page (offset = however many
     * stations we already have) and APPENDS it to the current list. This
     * is how we replace the fixed result cap with something the user
     * controls, instead of a fixed number in code.
     */
    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val selectedCountry = _uiState.value.selectedCountry
            val currentOffset = _uiState.value.foundStations.size
            val result = stationRepository.search(
                _uiState.value.query,
                country = selectedCountry?.name,
                countryCode = selectedCountry?.code,
                language = null,
                offset = currentOffset
            )
            result.onSuccess { newStations ->
                _uiState.value = _uiState.value.copy(
                    foundStations = _uiState.value.foundStations + newStations,
                    isLoadingMore = false,
                    canLoadMore = newStations.size >= pageSize
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    errorMessage = error.message ?: "Eroare la încărcarea mai multor posturi"
                )
            }
        }
    }

    // ---------------------------------------------------------------
    // Filtru de țară
    // ---------------------------------------------------------------

    private fun loadCountries() {
        viewModelScope.launch {
            stationRepository.listCountries().onSuccess { countries ->
                _uiState.value = _uiState.value.copy(countries = countries)
            }
            // (RO) Eșec la încărcarea țărilor nu e critic — selectorul
            // rămâne gol, dar căutarea de bază tot funcționează.
        }
    }

    fun onCountrySelected(country: CountryOption?) {
        _uiState.value = _uiState.value.copy(selectedCountry = country)
        search(_uiState.value.query)
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

    /** (RO) Adaugă un post manual direct în lista de favorite. */
    fun addManualStation(name: String, url: String, imageUrl: String, genre: String, country: String, homepage: String) {
        viewModelScope.launch {
            val manualStation = Station(
                id = "manual-${System.currentTimeMillis()}",
                name = name.trim(),
                genre = genre.trim().ifEmpty { "Manual / Direct" },
                country = country.trim().ifEmpty { "Local" },
                streamUrl = url.trim(),
                isFavorite = true,
                imageUrl = imageUrl.trim().ifEmpty { null },
                homepage = homepage.trim().ifEmpty { null }
            )
            favoritesRepository.toggleFavorite(manualStation)
        }
    }

    /** (RO) Actualizează datele unui post existent în favorite. */
    fun updateManualStation(station: Station, name: String, url: String, imageUrl: String, genre: String, country: String, homepage: String) {
        viewModelScope.launch {
            val updated = station.copy(
                name = name.trim(),
                streamUrl = url.trim(),
                imageUrl = imageUrl.trim().ifEmpty { null },
                genre = genre.trim().ifEmpty { "Manual / Direct" },
                country = country.trim().ifEmpty { "Local" },
                homepage = homepage.trim().ifEmpty { null }
            )
            favoritesRepository.updateFavorite(updated)
            
            // Dacă postul actualizat este cel care cântă acum, îl reîncărcăm în player
            if (_uiState.value.currentStation?.id == updated.id) {
                _uiState.value = _uiState.value.copy(currentStation = updated)
            }
        }
    }

    // ---------------------------------------------------------------
    // Redare
    // ---------------------------------------------------------------

    fun selectStation(station: Station) {
        _uiState.value = _uiState.value.copy(currentStation = station, isMenuOpen = false)
        radioPlayer.play(station)
        viewModelScope.launch {
            favoritesRepository.saveLastStation(station)
        }
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