package com.sorin.radiotuner.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * (RO)
 * Interfața Retrofit pentru Radio Browser API (radio-browser.info) —
 * un director public, gratuit, de posturi radio online, fără cheie API.
 * Notă: API-ul are mai multe servere-oglindă; aici folosim unul fix
 * (vezi NetworkModule) — documentat ca limitare cunoscută în ARHITECTURA.md.
 *
 *  (EN)
 * Retrofit interface for Radio Browser API (radio-browser.info) —
 * a public, free directory of online radio stations, without an API key.
 * Note: The API has multiple mirror servers; here we use a fixed one
 * (see NetworkModule) — documented as a known limitation in ARHITECTURA
 */
interface RadioBrowserApi {
    @GET("json/stations/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("limit") limit : Int = 30,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<RadioBrowserStationDto>
}