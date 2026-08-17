package com.sorin.radiotuner.player

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sorin.radiotuner.RadioTunerApplication
import com.sorin.radiotuner.data.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * (RO)
 * Serviciu de fundal care permite integrarea cu Android Auto.
 * Implementează MediaLibraryService pentru a permite mașinii să răsfoiască posturile.
 * (EN)
 * Background service that enables Android Auto integration.
 * Implements MediaLibraryService to allow the car to browse stations.
 */
class PlaybackService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val ROOT_ID = "[root]"
        private const val FAVORITES_ID = "[favorites]"
        private const val PREMIUM_ID = "[premium]"
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        @OptIn(UnstableApi::class)
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setTitle("Radio Tuner")
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        @OptIn(UnstableApi::class)
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return when (parentId) {
                ROOT_ID -> {
                    val favoritesFolder = MediaItem.Builder()
                        .setMediaId(FAVORITES_ID)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Favorite")
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                                .build()
                        )
                        .build()
                    val premiumFolder = MediaItem.Builder()
                        .setMediaId(PREMIUM_ID)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("România")
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                                .build()
                        )
                        .build()
                    Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(favoritesFolder, premiumFolder), params))
                }
                FAVORITES_ID -> {
                    val favoritesRepo = (application as RadioTunerApplication).container.favoritesRepository
                    val future = SettableFuture<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        val stations = favoritesRepo.observeFavorites().first()
                        val mediaItems = stations.map { it.toMediaItem() }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params))
                    }
                    future
                }
                PREMIUM_ID -> {
                    val stationRepo = (application as RadioTunerApplication).container.stationRepository
                    val stations = stationRepo.getCuratedStations()
                    val mediaItems = stations.map { it.toMediaItem() }
                    Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params))
                }
                else -> Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
        }
    }

    private fun Station.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setSubtitle(genre)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .setArtworkUri(imageUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        super.onDestroy()
    }

    // Simple SettableFuture helper
    private class SettableFuture<T> : com.google.common.util.concurrent.AbstractFuture<T>() {
        public override fun set(value: T): Boolean = super.set(value)
        public override fun setException(throwable: Throwable): Boolean = super.setException(throwable)
    }
}
