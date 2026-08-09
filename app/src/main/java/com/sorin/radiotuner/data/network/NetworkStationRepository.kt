package com.sorin.radiotuner.data.network

import com.sorin.radiotuner.data.model.CountryOption
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.StationRepository
import kotlinx.coroutines.*
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
            // (RO) Căutăm în paralel folosind criterii diferite pentru a găsi absolut tot
            val results = mutableListOf<RadioBrowserStationDto>()
            
            // 1. Căutare standard
            results += api.search(name = query, country = country, countryCode = countryCode, language = language, offset = offset)
            
            // 2. Dacă e vorba de România, forțăm o căutare și după limba română (pentru diaspora/acoperire maximă)
            if (countryCode == "RO" || country?.contains("Romania", ignoreCase = true) == true) {
                results += api.search(name = query, language = "romanian", offset = offset)
            }

            val stations = results
                .distinctBy { it.stationUuid }
                .mapNotNull { it.toStationOrNull() }

            // (RO) Validăm posturile în paralel (doar pentru prima pagină, pentru a nu încetini UI-ul)
            if (offset == 0 && stations.isNotEmpty()) {
                validateStations(stations)
            } else {
                stations
            }
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
     * (RO) Verifică în timp real dacă stream-urile funcționează.
     */
    private suspend fun validateStations(stations: List<Station>): List<Station> = coroutineScope {
        stations.map { station ->
            async(Dispatchers.IO) {
                if (isStreamReachable(station.streamUrl)) station else null
            }
        }.awaitAll().filterNotNull()
    }

    private fun isStreamReachable(url: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 2500
            connection.readTimeout = 2500
            // Verificăm doar dacă serverul răspunde cu succes
            val responseCode = connection.responseCode
            responseCode in 200..399
        } catch (e: Exception) {
            false
        } finally {
            connection?.disconnect()
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
