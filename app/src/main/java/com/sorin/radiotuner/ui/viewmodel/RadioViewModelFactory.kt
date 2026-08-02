package com.sorin.radiotuner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sorin.radiotuner.di.AppContainer

/**
 * (RO)
 * RadioViewModel primește dependințele prin constructor (nu le creează
 * singur) — asta se numește "injecție prin constructor" și e ce face
 * clasa testabilă/izolată. Dar Android are nevoie de un Factory ca să
 * știe CUM să construiască un ViewModel cu constructor personalizat.
 * (EN)
 *  RadioViewModel receives dependencies through its constructor (it doesn't
 * create them itself) — this is called "constructor injection" and it makes
 * the class testable/isolated. But Android needs a Factory to know HOW to
 * construct a ViewModel with a custom constructor.
 */
class RadioViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RadioViewModel(
            stationRepository = container.stationRepository,
            favoritesRepository = container.favoritesRepository,
            radioPlayer = container.radioPlayer,
            connectivityObserver = container.connectivityObserver
        ) as T
    }
}