package com.sorin.radiotuner.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sorin.radiotuner.data.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * (RO) Extensie la nivel de Context care creează un singur DataStore pentru
 * toată aplicația, sub numele "radio_tuner_prefs".
 *
 * (EN) Context-level extension that creates a single DataStore for the entire application,
 *  under the name "radio_tuner_prefs".
 */

private val Context.dataStore by preferencesDataStore(name = "radio_tuner_prefs")

/**
 * (RO)
 *  Wrapper subțire peste DataStore, responsabil DOAR de citirea/scrierea
 *  brută a listei de favorite (ca JSON). Nu conține logică de UI.
 *  LocalFavoritesRepository (fișierul de lângă acesta) îl folosește prin
 *  interfața FavoritesRepository.
 *  (EN)
 *  Thin wrapper over DataStore, responsible ONLY for reading/writing
 *  raw data of the favorite list (as JSON). Does not contain UI logic.
 *  LocalFavoritesRepository (file next to this one)
 *  uses it through the FavoritesRepository interface.
 */

class FavoritesDataStore(private val context: Context) {
    private val key = stringPreferencesKey("favorites_json")
    private val lastTabKey = stringPreferencesKey("last_tab")
    private val lastStationKey = stringPreferencesKey("last_station_json")
    private val gson = Gson()

    fun observeFavorites(): Flow<List<Station>> {
        return context.dataStore.data.map {prefs ->
            val json = prefs[key] ?: return@map emptyList()
            val type = object : TypeToken<List<Station>>() {}.type
            try {
                gson.fromJson<List<Station>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveFavorites(stations: List<Station>) {
        context.dataStore.edit {prefs ->
            prefs[key] = gson.toJson(stations)
        }
    }

    fun observeLastTab(): Flow<String?> = context.dataStore.data.map { it[lastTabKey] }

    suspend fun saveLastTab(tabName: String) {
        context.dataStore.edit { it[lastTabKey] = tabName }
    }

    fun observeLastStation(): Flow<Station?> = context.dataStore.data.map { prefs ->
        val json = prefs[lastStationKey] ?: return@map null
        try {
            gson.fromJson(json, Station::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveLastStation(station: Station?) {
        context.dataStore.edit { prefs ->
            if (station == null) prefs.remove(lastStationKey)
            else prefs[lastStationKey] = gson.toJson(station)
        }
    }
}