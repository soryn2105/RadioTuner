package com.sorin.radiotuner.data.repository

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
    suspend fun search(query: String): Result<List<Station>>
}