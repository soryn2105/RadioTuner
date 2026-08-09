package com.sorin.radiotuner.data.repository

import com.sorin.radiotuner.data.model.CountryOption
import com.sorin.radiotuner.data.model.Station

/**
 * (RO)
 *  Contract pentru sursa de posturi radio. RadioViewModel depinde DOAR
 *  de această interfață, niciodată de implementarea concretă din
 *  data.network — de aceea putem înlocui sau testa sursa de date fără
 *  să atingem UI-ul sau ViewModel-ul.
 *
 *  (EN)
 *  Contract for radio stations data source. RadioViewModel depends ONLY
 *  on this interface, not on the concrete implementation from data.network —
 *  that's why we can replace or test the data source without having to touch
 *  the UI or the ViewModel.
 */
interface StationRepository {
    /**
     * (RO) country = null înseamnă "toate țările", altfel numele exact
     * al țării (nu un cod). offset = 0 pentru prima pagină de rezultate,
     * offset = N pentru "încarcă mai multe".
     *
     * (EN) country = null means "all countries", otherwise the exact
     * country name (not a code). offset = 0 for the first page of
     * results, offset = N for "load more".
     */
    suspend fun search(
        query: String,
        country: String? = null,
        countryCode: String? = null,
        language: String? = null,
        offset: Int = 0
    ): Result<List<Station>>

    /** Lista de țări disponibile pentru filtrare, cu numărul de posturi din fiecare. */
    suspend fun listCountries(): Result<List<CountryOption>>

    /** (RO) Returnează lista de posturi "Premium" verificate manual. */
    fun getCuratedStations(): List<Station>
}
