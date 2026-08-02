package com.sorin.radiotuner.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * (RO)
 * Interfața Retrofit pentru Radio Browser API (radio-browser.info) —
 * un director public, gratuit, de posturi radio online, fără cheie API.
 * Notă: API-ul are mai multe servere-oglindă, toate cu aceeași bază de
 * date mondială — vezi RadioBrowserServers.kt pentru descoperire + failover.
 *
 * (EN)
 * Retrofit interface for Radio Browser API (radio-browser.info) —
 * a public, free directory of online radio stations, without an API key.
 * Note: The API has multiple mirror servers, all sharing the same global
 * database — see RadioBrowserServers.kt for discovery + failover.
 */
interface RadioBrowserApi {

    /**
     * (RO) country e opțional (null = fără filtru de țară) — text simplu,
     * exact numele întors de json/countries, nu un cod ISO. offset
     * susține paginarea: prima pagină offset=0, a doua offset=limit ș.a.m.d.
     *
     * (EN) country is optional (null = no country filter) — plain text,
     * exactly the name returned by json/countries, not an ISO code.
     * offset supports pagination: first page offset=0, second page
     * offset=limit, etc.
     */
    @GET("json/stations/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("country") country: String? = null,
        @Query("countrycode") countryCode: String? = null,
        @Query("language") language: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("hidebroken") hideBroken: Boolean = false,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioBrowserStationDto>

    /** Lista completă de țări cu posturi indexate, pentru selector. */
    @GET("json/countries")
    suspend fun listCountries(): List<RadioBrowserCountryDto>
}
