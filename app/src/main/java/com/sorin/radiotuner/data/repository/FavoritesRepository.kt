package com.sorin.radiotuner.data.repository

import com.sorin.radiotuner.data.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * (RO)
 * Contract pentru favorite. Flow<> înseamnă că UI-ul se actualizează
 *  * automat de fiecare dată când lista de favorite se schimbă (adăugare/
 *  * eliminare), fără să cerem noi date manual.
 *
 *  (EN)
 *  Contract for favorites. Flow<> means that the UI automatically updates
 *  * every time the list of favorites changes (addition/removal), without
 *  * having to request new data manually.
 */

interface FavoritesRepository {
    fun observeFavorites(): Flow<List<Station>>
    suspend fun toggleFavorite(station: Station)
    suspend fun updateFavorite(station: Station)
    suspend fun isFavorite(stationId: String): Boolean

    // (RO) Persistență stare UI
    fun observeLastTab(): Flow<String?>
    suspend fun saveLastTab(tabName: String)
    fun observeLastStation(): Flow<Station?>
    suspend fun saveLastStation(station: Station?)
}