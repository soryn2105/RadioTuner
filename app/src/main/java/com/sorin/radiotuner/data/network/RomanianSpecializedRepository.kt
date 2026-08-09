package com.sorin.radiotuner.data.network

import com.sorin.radiotuner.data.model.CountryOption
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.repository.StationRepository

/**
 * (RO)
 * Un repository specializat pentru România.
 * Strategie:
 * 1. Menține o listă locală (curată) de posturi "Premium" (Kiss FM, ZU, etc.).
 * 2. Când utilizatorul caută fără filtru sau caută specific România, injectează aceste posturi la început.
 * 3. Folosește NetworkStationRepository ca sursă "de adâncime" pentru a găsi orice altceva din lume.
 *
 * (EN)
 * A specialized repository for Romania.
 * Strategy:
 * 1. Maintains a local (clean) list of "Premium" stations (Kiss FM, ZU, etc.).
 * 2. When the user searches without a filter or specifically for Romania, it injects these stations at the beginning.
 * 3. Uses NetworkStationRepository as a "deep search" source to find anything else in the world.
 */
class RomanianSpecializedRepository(
    private val networkRepository: NetworkStationRepository = NetworkStationRepository()
) : StationRepository {

    override suspend fun search(
        query: String,
        country: String?,
        countryCode: String?,
        language: String?,
        offset: Int
    ): Result<List<Station>> {
        // (RO) Dacă filtrul de țară e pe România, forțăm și filtrul de limbă română pentru a găsi mai multe
        val effectiveLanguage = if (countryCode == "RO" || (country != null && country.contains("Romania", ignoreCase = true))) {
            "romanian"
        } else {
            language
        }

        // (RO) Obținem rezultatele globale de la API
        val networkResult = networkRepository.search(query, country, countryCode, effectiveLanguage, offset)

        // (RO) Dacă suntem pe prima pagină (offset 0), injectăm lista noastră specială
        if (offset == 0) {
            val curated = filterCurated(query, country, countryCode)
            
            return if (networkResult.isSuccess) {
                networkResult.map { networkStations ->
                    // (RO) Combinăm cele două liste, punând cele "Premium" primele și eliminând duplicatele
                    (curated + networkStations).distinctBy { it.name.lowercase().trim() }
                }
            } else {
                // (RO) Chiar dacă rețeaua a eșuat, returnăm măcar lista locală curated
                Result.success(curated)
            }
        }

        return networkResult
    }

    override suspend fun listCountries(): Result<List<CountryOption>> {
        return networkRepository.listCountries()
    }

    override fun getCuratedStations(): List<Station> {
        return CuratedRomanianStations.STATIONS
    }

    /**
     * (RO) Filtrează lista locală de posturi "Premium" în funcție de termenul căutat.
     */
    private fun filterCurated(query: String, country: String?, countryCode: String?): List<Station> {
        // (RO) Dacă filtrul de țară e setat pe altceva decât România, nu injectăm nimic
        val isRomaniaFilter = country?.contains("Romania", ignoreCase = true) == true || 
                             countryCode?.equals("RO", ignoreCase = true) == true ||
                             (country == null && countryCode == null)

        if (!isRomaniaFilter) return emptyList()

        if (query.isBlank()) return CuratedRomanianStations.STATIONS

        return CuratedRomanianStations.STATIONS.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.genre.contains(query, ignoreCase = true)
        }
    }
}
