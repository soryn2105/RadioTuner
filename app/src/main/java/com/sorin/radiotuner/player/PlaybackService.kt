package com.sorin.radiotuner.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sorin.radiotuner.MainActivity
import com.sorin.radiotuner.RadioTunerApplication
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.data.network.CuratedRomanianStations
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * (RO)
 * Serviciu de fundal optimizat pentru Android Auto.
 * Implementează MediaLibraryService pentru a permite mașinii să răsfoiască posturile
 * din toată lumea, să caute și să afișeze metadate complete pe bord.
 */
class PlaybackService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // (RO) Reținem rezultatele ultimei căutări globale pentru Android Auto
    private var lastSearchResults = listOf<Station>()

    companion object {
        private const val ROOT_ID = "[root]"
        private const val FAVORITES_ID = "[favorites]"
        private const val COUNTRIES_ID = "[countries]"
        private const val SEARCH_RESULTS_ID = "[search_results]"
        private const val CUSTOM_COMMAND_TOGGLE_FAVORITE = "com.sorin.radiotuner.TOGGLE_FAVORITE"
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
            .setHandleAudioBecomingNoisy(true)
            .build()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(pendingIntent)
            .build()
            
        // (RO) Listener pentru a menține sincronizarea metadatelor RDS cu bordul mașinii
        player.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                // (RO) Media3 gestionează automat trimiterea noilor titluri (RDS) către Android Auto
            }
        })
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
                        .setTitle("Amber Tuner Global")
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
            val container = (application as RadioTunerApplication).container
            
            return when {
                parentId == ROOT_ID -> {
                    val folders = listOf(
                        createFolder(FAVORITES_ID, "⭐ Favoritele Mele", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
                        createFolder(COUNTRIES_ID, "🌎 Răsfoiește după Țară", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                        createFolder(SEARCH_RESULTS_ID, "🔍 Rezultate Căutare", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    )
                    Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(folders), params))
                }
                parentId == FAVORITES_ID -> {
                    val future = SettableFuture<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        val stations = container.favoritesRepository.observeFavorites().first()
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(stations.map { it.toMediaItem() }), params))
                    }
                    future
                }
                parentId == COUNTRIES_ID -> {
                    val future = SettableFuture<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        container.stationRepository.listCountries().onSuccess { countries ->
                            val items = countries.take(100).map { country ->
                                MediaItem.Builder()
                                    .setMediaId("[country]${country.name}")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(country.name)
                                            .setSubtitle("${country.stationCount} posturi disponibile")
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                            .build()
                                    ).build()
                            }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                        }.onFailure { future.set(LibraryResult.ofError(SessionError.ERROR_IO)) }
                    }
                    future
                }
                parentId.startsWith("[country]") -> {
                    val countryName = parentId.removePrefix("[country]")
                    val future = SettableFuture<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        container.stationRepository.search("", countryName, null, null, 0).onSuccess { stations ->
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(stations.map { it.toMediaItem() }), params))
                        }.onFailure { future.set(LibraryResult.ofError(SessionError.ERROR_IO)) }
                    }
                    future
                }
                parentId == SEARCH_RESULTS_ID -> {
                    Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(lastSearchResults.map { it.toMediaItem() }), params))
                }
                else -> Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
        }

        @OptIn(UnstableApi::class)
        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            val container = (application as RadioTunerApplication).container
            serviceScope.launch {
                // (RO) Căutare globală în toată lumea (fără filtru de țară)
                container.stationRepository.search(query, null, null, null, 0).onSuccess { stations ->
                    lastSearchResults = stations
                    session.notifySearchResultChanged(browser, query, stations.size, params)
                }
            }
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        @OptIn(UnstableApi::class)
        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = lastSearchResults.map { it.toMediaItem() }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        @OptIn(UnstableApi::class)
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture<LibraryResult<MediaItem>>()
            serviceScope.launch {
                val station = findStationById(mediaId)
                if (station != null) {
                    future.set(LibraryResult.ofItem(station.toMediaItem(), null))
                } else {
                    future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                }
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture<MutableList<MediaItem>>()
            serviceScope.launch {
                val resolvedItems = mediaItems.map { item ->
                    findStationById(item.mediaId)?.toMediaItem() ?: item
                }.toMutableList()
                future.set(resolvedItems)
            }
            return future
        }

        @OptIn(UnstableApi::class)
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)

            val customCommand = SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY)
            availableSessionCommands.add(customCommand)

            val customLayout = listOf(
                CommandButton.Builder()
                    .setDisplayName("Favorite")
                    .setSessionCommand(customCommand)
                    .setIconResId(android.R.drawable.btn_star)
                    .build()
            )
            session.setCustomLayout(ImmutableList.copyOf(customLayout))

            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_SET_MEDIA_ITEM)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_GET_TIMELINE)
                .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                .build()

            return MediaSession.ConnectionResult.accept(availableSessionCommands.build(), playerCommands)
        }

        @OptIn(UnstableApi::class)
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_COMMAND_TOGGLE_FAVORITE) {
                serviceScope.launch {
                    val currentItem = session.player.currentMediaItem ?: return@launch
                    val station = findStationById(currentItem.mediaId) ?: return@launch
                    val favoritesRepo = (application as RadioTunerApplication).container.favoritesRepository
                    favoritesRepo.toggleFavorite(station)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    private suspend fun findStationById(id: String): Station? {
        val container = (application as RadioTunerApplication).container
        
        // 1. Căutăm în favorite
        val favorite = container.favoritesRepository.observeFavorites().first().find { it.id == id }
        if (favorite != null) return favorite
        
        // 2. Căutăm în lista de bază
        val curated = CuratedRomanianStations.STATIONS.find { it.id == id }
        if (curated != null) return curated
        
        // 3. Căutăm în ultimele rezultate de căutare
        return lastSearchResults.find { it.id == id }
    }

    private fun Station.toMediaItem(): MediaItem {
        // (RO) Metadate bogate pentru afișare pe bordul mașinii
        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setSubtitle("$genre · $country")
            .setArtist(genre)
            .setAlbumTitle(country)
            .setDisplayTitle(name)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setArtworkUri(imageUrl?.let { Uri.parse(it) })
            .setExtras(Bundle().apply {
                putString("bitrate", if (bitrate != null) "$bitrate kbps" else "Nespecificat")
                putString("homepage", homepage)
            })
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun createFolder(id: String, title: String, type: Int): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(type)
                    .build()
            ).build()
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

    private class SettableFuture<T> : com.google.common.util.concurrent.AbstractFuture<T>() {
        public override fun set(value: T): Boolean = super.set(value)
        public override fun setException(throwable: Throwable): Boolean = super.setException(throwable)
    }
}
