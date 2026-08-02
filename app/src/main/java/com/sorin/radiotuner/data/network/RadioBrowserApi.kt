package com.sorin.radiotuner.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * (RO)
 * Interfața Retrofit pentru Radio Browser API (radio-browser.info) —
 * un director public, gratuit, de posturi radio online, fără cheie API.
 * Notă: API-ul are mai multe servere-oglindă, toate cu aceeași bază de
 * date mondială. Nu mai folosim un singur server fix — vezi
 * RadioBrowserServers.kt pentru descoperirea automată + failover.
 *
 *  (EN)
 * Retrofit interface for Radio Browser API (radio-browser.info) —
 * a public, free directory of online radio stations, without an API key.
 * Note: The API has multiple mirror servers, all sharing the same global
 * database. We no longer pin a single fixed server — see
 * RadioBrowserServers.kt for automatic discovery + failover.
 */
interface RadioBrowserApi {
    @GET("json/stations/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("limit") limit : Int = 50,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<RadioBrowserStationDto>
}