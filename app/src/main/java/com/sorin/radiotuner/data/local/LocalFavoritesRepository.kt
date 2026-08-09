package com.sorin.radiotuner.data.local

import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.collections.filterNot

/**
 * (RO)
 * Implementarea REALĂ a FavoritesRepository, susținută de DataStore.
 * Aceasta e singura clasă din aplicație care știe că favoritele sunt salvate caJSON local -
 * restul aplicației vede doar interfața.
 * (EN)
 * The REAL implementation of FavoritesRepository, backed by DataStore.
 * This is the only class in the application that knows that favorite stations are saved as JSON locally -
 * the rest of the application only sees the interface.
 */

class LocalFavoritesRepository(private val store: FavoritesDataStore) : FavoritesRepository {
    override fun observeFavorites(): Flow<List<Station>> = store.observeFavorites()

    override suspend fun toggleFavorite(station: Station) {
        val current = store.observeFavorites().first()
        val exists = current.any {it.id == station.id }
        val updated = if (exists) {
            current.filterNot { it.id == station.id }
        } else {
            current + station
        }
        store.saveFavorites(updated)
    }

    override suspend fun updateFavorite(station: Station) {
        val current = store.observeFavorites().first()
        val updated = current.map {
            if (it.id == station.id) station else it
        }
        store.saveFavorites(updated)
    }

    override suspend fun isFavorite(stationId: String): Boolean {
        return store.observeFavorites().first().any {it.id == stationId}
    }
}