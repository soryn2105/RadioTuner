package com.sorin.radiotuner.data.network

import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.StationRepository

/**
 * * (RO)
 * Implementarea REALĂ a StationRepository, bazată pe Radio Browser API.
 * RadioViewModel nu știe (și nu trebuie să știe) că aceasta e sursa —
 * el vede doar interfața StationRepository.
 *
 * *(EN)
 *  The REAL implementation of StationRepository, based on the Radio Browser API.
 *  RadioViewModel does not know (and should not know) that this is the source —
 *  it only sees the StationRepository interface.
 */
class NetworkStationRepository(private val api: RadioBrowserApi) : StationRepository {
    override suspend fun search(query: String) : Result<List<Station>> {
        return try {
            val stations = api.search(name = query)
            Result.success(stations.mapNotNull { it.toStationOrNull() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * (RO)
     * Ignorăm intrările fără id sau fără URL de stream — nu sunt redabile.
     *
     * (EN)
     * We ignore entries without an id or without a stream URL — they are not playable.
     */
    private fun RadioBrowserStationDto.toStationOrNull(): Station? {
        val id = stationUuid ?: return null
        val url = urlResolved ?: return null
        return Station(
            id = id,
            name = name?.trim().takeUnless { it.isNullOrBlank() } ?: "Post necunoscut",
            genre = tags?.split(",")?.firstOrNull()?.trim()?.replaceFirstChar { it.uppercase() } ?: "—",
            country = country?.takeUnless { it.isBlank() } ?: "—",
            streamUrl = url
        )
    }
}