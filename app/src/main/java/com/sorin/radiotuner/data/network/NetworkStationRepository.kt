package com.sorin.radiotuner.data.network

import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.StationRepository

/**
 * (RO)
 * Implementarea REALĂ a StationRepository, bazată pe Radio Browser API.
 * RadioViewModel nu știe (și nu trebuie să știe) că aceasta e sursa —
 * el vede doar interfața StationRepository.
 *
 * Nu mai depinde de UN singur server fix. La prima căutare, descoperă
 * lista de servere active (RadioBrowserServers.discover()) și le
 * amestecă aleatoriu — dacă serverul curent nu răspunde, trece automat
 * la următorul din listă (failover), fără să oprească total căutarea.
 *
 * (EN)
 * The REAL implementation of StationRepository, based on the Radio
 * Browser API. RadioViewModel does not know (and should not know) that
 * this is the source — it only sees the StationRepository interface.
 *
 * No longer depends on a single fixed server. On the first search, it
 * discovers the list of active servers (RadioBrowserServers.discover())
 * and shuffles them — if the current server doesn't respond, it
 * automatically moves to the next one in the list (failover), instead
 * of failing the whole search.
 */
class NetworkStationRepository : StationRepository {

    private var servers: List<String> = emptyList()
    private var serverIndex = 0

    private suspend fun ensureServers() {
        if (servers.isEmpty()) {
            servers = RadioBrowserServers.discover().shuffled()
        }
    }

    override suspend fun search(query: String): Result<List<Station>> {
        ensureServers()
        val serverList = servers.ifEmpty { RadioBrowserServers.FALLBACK }

        var lastError: Exception? = null

        // Încercăm pe rând fiecare server disponibil, pornind de la cel
        // curent — dacă unul eșuează (server jos, timeout etc.), trecem
        // la următorul, în loc să dăm eroare imediat.
        repeat(serverList.size) { attempt ->
            val host = serverList[(serverIndex + attempt) % serverList.size]
            try {
                val api = NetworkModule.provideRadioBrowserApi(host)
                val stations = api.search(name = query)
                serverIndex = (serverIndex + attempt) % serverList.size // rămânem pe serverul care a mers
                return Result.success(stations.mapNotNull { it.toStationOrNull() })
            } catch (e: Exception) {
                lastError = e
                // continuăm la următorul server din listă
            }
        }

        return Result.failure(lastError ?: Exception("Niciun server Radio Browser nu a răspuns"))
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
