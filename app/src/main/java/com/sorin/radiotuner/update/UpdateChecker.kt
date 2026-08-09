package com.sorin.radiotuner.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * (RO) Model pentru informațiile despre actualizare returnate de Worker.
 */
data class UpdateInfo(
    val version: String,
    val apkUrl: String
)

/**
 * (RO)
 * Verifică dacă există o versiune nouă pe serverul public (Cloudflare Worker).
 * Worker-ul interoghează Gitea în fundal și returnează versiunea curentă și link-ul de download.
 */
object UpdateChecker {

    private const val UPDATE_URL =
        "https://app-updates.soryn2105.workers.dev/update?app=radiotuner"

    /**
     * (RO) Returnează UpdateInfo dacă există o versiune mai nouă, sau null în caz contrar.
     */
    suspend fun check(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(UPDATE_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(body)
            if (json.has("error")) return@withContext null

            val remoteVersion = json.getString("version") // ex: "1.0.7"
            val apkUrl = json.getString("apk_url")

            // (RO) Obținem versiunea instalată curent
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = packageInfo.versionName

            // (RO) Comparăm versiunile (textual)
            if (remoteVersion != currentVersion) {
                UpdateInfo(remoteVersion, apkUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null // Eșuăm silențios pentru a nu întrerupe experiența utilizatorului
        }
    }
}
