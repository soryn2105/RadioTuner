package com.sorin.radiotuner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sorin.radiotuner.BuildConfig
import com.sorin.radiotuner.R
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.player.PlaybackState
import com.sorin.radiotuner.ui.components.*
import com.sorin.radiotuner.ui.theme.*
import com.sorin.radiotuner.ui.viewmodel.RadioViewModel
import com.sorin.radiotuner.ui.viewmodel.RadioViewModelFactory

/**
 * (RO)
 * Ecranul principal.
 */
@Composable
fun RadioScreen(factory: RadioViewModelFactory) {
    val viewModel: RadioViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showAboutDialog by remember { mutableStateOf(false) }
    var showManualStationDialog by remember { mutableStateOf(false) }
    var stationToEdit by remember { mutableStateOf<Station?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Graphite900)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                isOnline = state.isOnline,
                showMenuButton = !isLandscape,
                onMenuClick = { viewModel.toggleMenu(true) },
                onAboutClick = { showAboutDialog = true }
            )

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    StationListPanel(
                        query = state.query,
                        onQueryChange = viewModel::onQueryChange,
                        foundStations = state.foundStations,
                        favoriteStations = state.favoriteStations,
                        selectedTab = state.selectedTab,
                        onTabSelected = viewModel::onTabSelected,
                        currentStationId = state.currentStation?.id,
                        onSelectStation = viewModel::selectStation,
                        onToggleFavorite = viewModel::toggleFavorite,
                        countries = state.countries,
                        selectedCountry = state.selectedCountry,
                        onCountrySelected = viewModel::onCountrySelected,
                        canLoadMore = state.canLoadMore,
                        isLoadingMore = state.isLoadingMore,
                        onLoadMore = viewModel::loadMore,
                        onAddManualClick = { showManualStationDialog = true },
                        onEditStation = { stationToEdit = it },
                        errorMessage = state.errorMessage,
                        modifier = Modifier.width(330.dp)
                    )
                    PlayerPane(
                        state = state,
                        viewModel = viewModel,
                        onEditStation = { stationToEdit = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                PlayerPane(
                    state = state,
                    viewModel = viewModel,
                    onEditStation = { stationToEdit = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Sertarul suprapus, doar în portret.
        if (!isLandscape) {
            AnimatedVisibility(
                visible = state.isMenuOpen,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it })
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { viewModel.toggleMenu(false) }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.86f)
                            .background(Graphite900)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MENIU", color = Cream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            IconButton(onClick = { viewModel.toggleMenu(false) }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Cream)
                            }
                        }
                        StationListPanel(
                            query = state.query,
                            onQueryChange = viewModel::onQueryChange,
                            foundStations = state.foundStations,
                            favoriteStations = state.favoriteStations,
                            selectedTab = state.selectedTab,
                            onTabSelected = viewModel::onTabSelected,
                            currentStationId = state.currentStation?.id,
                            onSelectStation = viewModel::selectStation,
                            onToggleFavorite = viewModel::toggleFavorite,
                            countries = state.countries,
                            selectedCountry = state.selectedCountry,
                            onCountrySelected = viewModel::onCountrySelected,
                            canLoadMore = state.canLoadMore,
                            isLoadingMore = state.isLoadingMore,
                            onLoadMore = viewModel::loadMore,
                            onAddManualClick = { showManualStationDialog = true },
                            onEditStation = { stationToEdit = it },
                            errorMessage = state.errorMessage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Despre Aplicație", fontSize = 20.sp) },
            text = {
                Column {
                    Text("Versiune: ${BuildConfig.VERSION_NAME}", fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Dezvoltat de Sorin", fontSize = 16.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", fontSize = 16.sp)
                }
            }
        )
    }

    if (showManualStationDialog) {
        ManualStationDialog(
            onDismiss = { showManualStationDialog = false },
            onConfirm = { name, url ->
                viewModel.addManualStation(name, url)
                showManualStationDialog = false
            }
        )
    }

    if (stationToEdit != null) {
        ManualStationDialog(
            initialName = stationToEdit!!.name,
            initialUrl = stationToEdit!!.streamUrl,
            isEditing = true,
            onDismiss = { stationToEdit = null },
            onConfirm = { name, url ->
                viewModel.updateManualStation(stationToEdit!!, name, url)
                stationToEdit = null
            }
        )
    }
}

@Composable
private fun AppHeader(
    isOnline: Boolean,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showMenuButton) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Cream)
                }
            }
            Icon(Icons.Default.Radio, contentDescription = null, tint = Amber, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "AMBER TUNER",
                color = Cream,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAboutClick) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
            }
            Icon(
                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isOnline) LedGreen else HeartRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (isOnline) "ONLINE" else "OFFLINE",
                fontSize = 12.sp,
                color = Muted
            )
        }
    }
}

@Composable
private fun PlayerPane(
    state: com.sorin.radiotuner.ui.viewmodel.RadioUiState,
    viewModel: RadioViewModel,
    onEditStation: (Station) -> Unit,
    modifier: Modifier
) {
    val isPlaying = state.playbackState is PlaybackState.Playing
    val isFavorite = state.currentStation?.let { current -> state.favoriteStations.any { it.id == current.id } } ?: false
    var showDetails by remember { mutableStateOf(false) }

    val statusLabel = when (state.playbackState) {
        is PlaybackState.Playing -> "ON AIR"
        is PlaybackState.Buffering -> "BUFFERING..."
        is PlaybackState.Error -> "ERROR"
        else -> "STOPPED"
    }

    val statusColor = when (state.playbackState) {
        is PlaybackState.Playing -> LedGreen
        is PlaybackState.Buffering -> Amber
        is PlaybackState.Error -> HeartRed
        else -> Muted
    }

    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // (RO) Status Badge - separat de logo
        Surface(
            color = statusColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
        ) {
            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(24.dp))

        TuningDial(
            isPlaying = isPlaying,
            imageUrl = state.currentStation?.imageUrl
        )

        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = state.currentStation?.name ?: "Niciun post selectat",
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (state.currentStation != null) {
                    Text(
                        text = "${state.currentStation.genre} · ${state.currentStation.country}",
                        color = Muted,
                        fontSize = 16.sp
                    )
                }
            }
            
            if (state.currentStation != null) {
                IconButton(onClick = { showDetails = !showDetails }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = if (showDetails) Amber else Muted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        if (state.currentStation != null) {
            Spacer(Modifier.height(16.dp))
            StationDetails(
                station = state.currentStation,
                isVisible = showDetails,
                onEditClick = if (isFavorite) { { onEditStation(state.currentStation) } } else null
            )
        }

        Spacer(Modifier.height(24.dp))

        PlayerControls(
            playbackState = state.playbackState,
            isFavorite = isFavorite,
            onPrevious = viewModel::skipToPrevious,
            onPlayStop = viewModel::togglePlayStop,
            onNext = viewModel::skipToNext,
            onToggleFavorite = { state.currentStation?.let(viewModel::toggleFavorite) }
        )

        Spacer(Modifier.height(24.dp))

        RdsTicker(text = state.rdsInfo?.text, isAvailable = state.rdsInfo?.isAvailable == true)
    }
}
