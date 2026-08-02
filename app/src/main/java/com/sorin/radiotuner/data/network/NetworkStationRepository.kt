package com.sorin.radiotuner.data.network

import com.sorin.radiotuner.data.model.CountryOption
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.StationRepository

/**
 * (RO)
 * Implementarea REALĂ a StationRepository, bazată pe Radio Browser API.
 * RadioViewModel nu știe (și nu trebuie să știe) că aceasta e sursa —
 * el vede doar interfața StationRepository.
 *
 * Nu depinde de UN singur server fix. La prima cerere, descoperă lista
 * de servere active (RadioBrowserServers.discover()) și le amestecă
 * aleatoriu — dacă serverul curent nu răspunde, trece automat la
 * următorul din listă (failover).
 *
 * (EN)
 * The REAL implementation of StationRepository, based on the Radio
 * Browser API. RadioViewModel does not know (and should not know) that
 * this is the source — it only sees the StationRepository interface.
 *
 * Doesn't depend on a single fixed server. On the first request, it
 * discovers the list of active servers (RadioBrowserServers.discover())
 * and shuffles them — if the current server doesn't respond, it moves
 * to the next one automatically (failover).
 */
class NetworkStationRepository : StationRepository {

    private var servers: List<String> = emptyList()
    private var serverIndex = 0

    private suspend fun ensureServers(forceRefresh: Boolean = false) {
        if (servers.isEmpty() || forceRefresh) {
            val discovered = RadioBrowserServers.discover()
            if (discovered.isNotEmpty()) {
                servers = discovered
                serverIndex = 0
            }
        }
    }

    /**
     * (RO) Rulează [block] pe rând pe fiecare server disponibil, pornind
     * de la cel curent — dacă unul eșuează, încearcă următorul, fără să
     * abandoneze cererea imediat. Reține serverul care a funcționat, ca
     * următoarea cerere să pornească direct de la el.
     *
     * (EN) Runs [block] against each available server in turn, starting
     * from the current one — if one fails, it tries the next, instead of
     * failing the request immediately. Remembers the server that worked,
     * so the next request starts from it directly.
     */
    private suspend fun <T> withFailover(block: suspend (RadioBrowserApi) -> T): Result<T> {
        ensureServers()
        var serverList = servers.ifEmpty { RadioBrowserServers.FALLBACK }
        var lastError: Exception? = null

        repeat(serverList.size) { attempt ->
            val host = serverList[(serverIndex + attempt) % serverList.size]
            try {
                val api = NetworkModule.provideRadioBrowserApi(host)
                val result = block(api)
                serverIndex = (serverIndex + attempt) % serverList.size
                return Result.success(result)
            } catch (e: Exception) {
                lastError = e
                // (RO) Dacă am încercat jumătate din listă și tot nu merge, forțăm un refresh la listă
                // (EN) If we've tried half the list and it's still not working, force a list refresh
                if (attempt == serverList.size / 2) {
                    ensureServers(forceRefresh = true)
                    serverList = servers.ifEmpty { RadioBrowserServers.FALLBACK }
                }
            }
        }
        
        // (RO) În loc să returnăm o eroare tehnică de DNS pentru un server anume (ex. nl1),
        // returnăm un mesaj mai clar dacă toate au eșuat.
        val message = if (lastError is java.net.UnknownHostException || lastError is java.net.ConnectException) {
            "Probleme de conexiune la serverele Radio Browser. Verifică internetul."
        } else {
            lastError?.message ?: "Serviciul Radio Browser este momentan indisponibil."
        }
        
        return Result.failure(Exception(message))
    }

    override suspend fun search(
        query: String,
        country: String?,
        countryCode: String?,
        language: String?,
        offset: Int
    ): Result<List<Station>> {
        return withFailover { api ->
            api.search(
                name = query, 
                country = country, 
                countryCode = countryCode, 
                language = language,
                offset = offset
            ).mapNotNull { it.toStationOrNull() }
        }
    }

    override suspend fun listCountries(): Result<List<CountryOption>> {
        return withFailover { api ->
            api.listCountries()
                .mapNotNull { it.toCountryOrNull() }
                .filter { it.stationCount > 0 }
                .sortedByDescending { it.stationCount }
        }
    }

    /**
     * (RO) Ignorăm intrările fără id sau fără URL de stream — nu sunt redabile.
     * (EN) We ignore entries without an id or without a stream URL — they are not playable.
     */
    private fun RadioBrowserStationDto.toStationOrNull(): Station? {
        val id = stationUuid ?: return null
        val url = urlResolved ?: return null
        return Station(
            id = id,
            name = name?.trim().takeUnless { it.isNullOrBlank() } ?: "Post necunoscut",
            genre = tags?.split(",")?.firstOrNull()?.trim()?.replaceFirstChar { it.uppercase() } ?: "—",
            country = country?.takeUnless { it.isBlank() } ?: "—",
            streamUrl = url,
            imageUrl = favicon?.takeUnless { it.isBlank() },
            bitrate = bitrate?.takeUnless { it == 0 },
            homepage = homepage?.takeUnless { it.isBlank() }
        )
    }

    private fun RadioBrowserCountryDto.toCountryOrNull(): CountryOption? {
        val label = name?.takeUnless { it.isBlank() } ?: return null
        return CountryOption(
            name = label,
            stationCount = stationCount ?: 0,
            code = countryCode?.takeUnless { it.isBlank() }
        )
    }
}
