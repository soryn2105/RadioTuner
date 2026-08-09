package com.sorin.radiotuner.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.sorin.radiotuner.BuildConfig
import com.sorin.radiotuner.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * (RO)
 * Gestionează verificarea și instalarea automată a actualizărilor din Gitea.
 * (EN)
 * Manages checking and automatically installing updates from Gitea.
 */
class UpdateManager(private val context: Context) {

    // (RO) URL-ul de bază al serverului Gitea (fără /api/v1/...)
    // IMPORTANT: Asigură-te că acest URL este accesibil de pe telefon (ex: IP-ul real sau un domeniu)
    private val GITEA_BASE_URL = "http://gitea:3000/" 

    private val api = NetworkModule.provideGiteaUpdateApi(GITEA_BASE_URL)

    /**
     * (RO) Verifică dacă există un release mai nou pe Gitea.
     */
    suspend fun checkForUpdates(onNewVersion: (tagName: String, downloadUrl: String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val latest = api.getLatestRelease()
                val latestTag = latest.tagName ?: return@withContext
                val currentTag = "v${BuildConfig.VERSION_NAME}"

                // (RO) Comparație simplă de versiuni
                if (latestTag != currentTag) {
                    val apkAsset = latest.assets?.find { it.name?.endsWith(".apk") == true }
                    apkAsset?.downloadUrl?.let { url ->
                        val absoluteUrl = if (url.startsWith("http")) url else "${GITEA_BASE_URL.removeSuffix("/")}$url"
                        onNewVersion(latestTag, absoluteUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * (RO) Descarcă APK-ul și pornește instalarea.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadAndInstall(url: String) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Actualizare RadioTuner")
            .setDescription("Se descarcă noua versiune...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(destination)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, filter)
        }
    }

    private fun installApk(file: File) {
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
