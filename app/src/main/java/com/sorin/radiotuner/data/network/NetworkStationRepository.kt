package com.sorin.radiotuner.data.network

import com.sorin.radiotuner.data.model.CountryOption
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.StationRepository
import java.net.HttpURLConnection
import java.net.URL

/**
 * (RO)
 * Implementarea REALĂ a StationRepository, bazată pe Radio Browser API.
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
                if (attempt == serverList.size / 2) {
                    ensureServers(forceRefresh = true)
                    serverList = servers.ifEmpty { RadioBrowserServers.FALLBACK }
                }
            }
        }
        
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
            // (RO) Pentru a găsi TOATE posturile, folosim o singură căutare puternică.
            // (EN) To find ALL stations, we use a single powerful search.
            val searchLimit = 100
            
            // Dacă utilizatorul a selectat România, folosim codul "RO" care e cel mai precis.
            val results = api.search(
                name = query, 
                country = if (countryCode == null) country else null, 
                countryCode = countryCode, 
                language = language, 
                offset = offset,
                limit = searchLimit
            )

            // (RO) Nu mai filtrăm nimic automat pentru a asigura că apar toate cele ~1000 de posturi.
            results.mapNotNull { it.toStationOrNull() }
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
