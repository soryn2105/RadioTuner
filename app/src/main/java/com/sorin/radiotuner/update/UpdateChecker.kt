package com.sorin.radiotuner.update

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val apkUrl: String
)

object UpdateChecker {

    private const val UPDATE_URL =
        "https://app-updates.soryn2105.workers.dev/update?app=radiotuner"

    /**
     * Returnează UpdateInfo dacă există o versiune mai nouă decât cea instalată,
     * sau null dacă aplicația e deja la zi ori verificarea eșuează.
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

            val remoteVersion = json.getString("version")
            val apkUrl = json.getString("apk_url")

            val currentVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName

            if (remoteVersion != currentVersion) {
                UpdateInfo(remoteVersion, apkUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            // Fără conexiune, server oprit etc. — eșuăm silențios, nu blocăm aplicația
            null
        }
    }
}