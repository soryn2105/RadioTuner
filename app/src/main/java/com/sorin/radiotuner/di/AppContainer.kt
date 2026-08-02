package com.sorin.radiotuner.di

import android.content.Context
import com.sorin.radiotuner.data.local.FavoritesDataStore
import com.sorin.radiotuner.data.local.LocalFavoritesRepository
import com.sorin.radiotuner.data.network.NetworkStationRepository
import com.sorin.radiotuner.data.repository.FavoritesRepository
import com.sorin.radiotuner.data.repository.StationRepository
import com.sorin.radiotuner.player.ExoRadioPlayer
import com.sorin.radiotuner.player.RadioPlayer
import com.sorin.radiotuner.util.ConnectivityObserver

/**
 * (RO)
 * "Injecție de dependințe" manuală — un singur loc unde deciziile de genul:
 * "ce implementare concretă folosim pentru fiecare interfață" se iau O DATĂ,
 * la pornirea aplicației. Restul codului (ViewModel, UI) nu ia niciodată aceste decizii,
 * primește doar interfețele deja gata.
 *  Pentru un proiect mai mare s-ar putea folosi Hilt/Koin,
 *  dar pentru scop educațional, DI manual e mai transparent — vezi exact ce se întâmplă,
 *  fără "magie" de bibliotecă.
 *
 *  (EN)
 *  Manual dependency injection — one place where decisions like:
 *  "which concrete implementation to use for each interface" are made at application startup.
 *  The rest of the code (ViewModel, UI) does not have to make these decisions,
 *  it just sees the interfaces already ready. For a bigger project,
 *  Hilt/Koin could be used, but for the sake of education,
 *  manual DI is more transparent — see what happens without the library.
 */

class AppContainer (context: Context) {
    val stationRepository: StationRepository by lazy {
        NetworkStationRepository()
    }

    val favoritesRepository: FavoritesRepository by lazy {
        LocalFavoritesRepository(FavoritesDataStore(context))
    }

    val radioPlayer: RadioPlayer by lazy {
        ExoRadioPlayer(context)
    }

    val connectivityObserver: ConnectivityObserver by lazy {
        ConnectivityObserver(context)
    }
}