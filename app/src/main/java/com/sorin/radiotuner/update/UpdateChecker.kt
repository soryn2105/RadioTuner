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

            val remoteVersion = json.getString("version").replace("v", "").trim()
            val apkUrl = json.getString("apk_url")

            // (RO) Obținem versiunea instalată curent
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = (packageInfo.versionName ?: "0.0.0").replace("v", "").trim()

            // (RO) Comparăm versiunile (sematic) pentru a evita prompt-urile false
            if (isNewerVersion(remoteVersion, currentVersion)) {
                UpdateInfo(remoteVersion, apkUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * (RO) Compară două versiuni (ex: "1.1.0" vs "1.0.9").
     * Returnează true dacă 'remote' este mai mare decât 'local'.
     */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote == local) return false
        
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        
        val maxLength = maxOf(remoteParts.size, localParts.size)
        
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            
            if (r > l) return true
            if (r < l) return false
        }
        
        return false
    }
}
