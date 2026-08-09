package com.sorin.radiotuner.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * (RO)
 * Construiește instanțe Retrofit + OkHttp pentru Radio Browser API.
 * Nu mai construim UN client fix, pentru UN server fix — construim
 * clientul pentru orice host primit ca parametru, ca NetworkStationRepository
 * să poată trece de la un server la altul (failover) dacă unul nu răspunde.
 * Vezi RadioBrowserServers.kt pentru lista de servere.
 *
 * (EN)
 * Builds Retrofit + OkHttp instances for the Radio Browser API.
 * We no longer build ONE client for ONE fixed server — we build the
 * client for whatever host is passed in, so NetworkStationRepository can
 * fail over from one server to another if one doesn't respond.
 * See RadioBrowserServers.kt for the server list.
 */
object NetworkModule {

    private val sharedClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        // Radio Browser API cere explicit un User-Agent identificabil în
        // documentația lor — fără el, unele cereri pot fi respinse.
        val userAgentInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AmberTuner/1.0 (Android; educational project)")
                .build()
            chain.proceed(request)
        }
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(logging)
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /** Construiește un RadioBrowserApi pentru un host specific (ex. "de1.api.radio-browser.info"). */
    fun provideRadioBrowserApi(host: String): RadioBrowserApi {
        return Retrofit.Builder()
            .baseUrl("https://$host/")
            .client(sharedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioBrowserApi::class.java)
    }

    /** (RO) Construiește API-ul pentru Gitea (Updates). */
    fun provideGiteaUpdateApi(baseUrl: String): GiteaUpdateApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(sharedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GiteaUpdateApi::class.java)
    }
}
