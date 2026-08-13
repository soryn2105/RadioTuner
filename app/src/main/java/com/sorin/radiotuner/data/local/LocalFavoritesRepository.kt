package com.sorin.radiotuner.data.local

import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * (RO)
 * Implementarea REALĂ a FavoritesRepository, susținută de DataStore.
 */
class LocalFavoritesRepository(private val store: FavoritesDataStore) : FavoritesRepository {
    override fun observeFavorites(): Flow<List<Station>> = store.observeFavorites()

    override suspend fun toggleFavorite(station: Station) {
        val current = store.observeFavorites().first()
        val exists = current.any { it.id == station.id }
        val updated = if (exists) {
            current.filterNot { it.id == station.id }
        } else {
            current + station.copy(isFavorite = true)
        }
        store.saveFavorites(updated)
    }

    override suspend fun updateFavorite(station: Station) {
        val current = store.observeFavorites().first()
        val exists = current.any { it.id == station.id }
        val updated = if (exists) {
            current.map { if (it.id == station.id) station else it }
        } else {
            current + station.copy(isFavorite = true)
        }
        store.saveFavorites(updated)
    }

    override suspend fun isFavorite(stationId: String): Boolean {
        return store.observeFavorites().first().any { it.id == stationId }
    }

    override fun observeLastTab(): Flow<String?> = store.observeLastTab()
    override suspend fun saveLastTab(tabName: String) = store.saveLastTab(tabName)
    override fun observeLastStation(): Flow<Station?> = store.observeLastStation()
    override suspend fun saveLastStation(station: Station?) = store.saveLastStation(station)
}
