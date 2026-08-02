package com.sorin.radiotuner.data.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * (RO)
 * Descoperă lista de servere-oglindă Radio Browser active ACUM, în loc
 * să depindem de o singură adresă fixă (exact bug-ul pe care l-am
 * reparat: un server fix, cu o literă greșită, oprea toată căutarea).
 *
 * Toate serverele-oglindă au aceeași bază de date mondială de posturi —
 * asta nu "deblochează" mai multe posturi, ci face aplicația rezistentă
 * la căderea temporară a unui singur server.
 *
 * Radio Browser expune un endpoint dedicat exact pentru asta:
 * https://all.api.radio-browser.info/json/servers — returnează serverele
 * curent active. Dacă acest endpoint însuși e indisponibil, cădem pe o
 * listă de rezervă (servere cunoscute, stabile istoric).
 *
 * (EN)
 * Discovers the list of Radio Browser mirror servers active RIGHT NOW,
 * instead of depending on one fixed address (exactly the bug we fixed:
 * one fixed server, with a typo'd letter, broke all searching).
 *
 * All mirror servers share the same worldwide station database — this
 * doesn't "unlock" more stations, it makes the app resilient to any
 * single server going down temporarily.
 *
 * Radio Browser exposes a dedicated endpoint for exactly this purpose:
 * https://all.api.radio-browser.info/json/servers — returns the currently
 * active servers. If that endpoint itself is unreachable, we fall back to
 * a reserve list of historically stable, well-known mirrors.
 */
object RadioBrowserServers {

    private const val DISCOVERY_URL = "https://all.api.radio-browser.info/json/servers"

    /** Listă de rezervă — folosită doar dacă discovery-ul eșuează complet. */
    val FALLBACK = listOf(
        "de1.api.radio-browser.info",
        "de2.api.radio-browser.info",
        "at1.api.radio-browser.info",
        "nl1.api.radio-browser.info",
        "fr1.api.radio-browser.info"
    )

    private data class ServerEntry(@SerializedName("name") val name: String?)

    suspend fun discover(): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(DISCOVERY_URL)
                .header("User-Agent", "AmberTuner/1.0 (Android; educational project)")
                .build()

            OkHttpClient().newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrBlank()) return@withContext FALLBACK

                val type = object : TypeToken<List<ServerEntry>>() {}.type
                val servers: List<ServerEntry> = Gson().fromJson(body, type) ?: emptyList()
                val hosts = servers.mapNotNull { it.name }.filter { it.isNotBlank() }

                hosts.ifEmpty { FALLBACK }
            }
        } catch (e: Exception) {
            // Fără internet, DNS căzut, orice — cădem pe rezervă, nu blocăm căutarea.
            FALLBACK
        }
    }
}
