package com.sorin.radiotuner.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** (RO)
 * Construiește o singură dată instanța Retrofit + OkHttp, folosită de
 * NetworkStationRepository. Fiind izolată aici, dacă vrem să schimbăm
 * serverul-oglindă sau să adăugăm cache HTTP, modificăm doar acest fișier.
 *
 ** (EN)
 *  Builds a single instance of Retrofit + OkHttp, used by NetworkStationRepository.
 *  Being isolated here, if we want to change the mirror server or add HTTP cache,
 *  we only modify this file.
 */

object NetworkModule {
    private const val BASE_URL = "https://del.api.radio-browser.info/"

    fun provideRadioBrowserApi(): RadioBrowserApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioBrowserApi::class.java)
    }
}