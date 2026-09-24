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
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sorin.radiotuner.BuildConfig
import com.sorin.radiotuner.R
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.player.PlaybackState
import com.sorin.radiotuner.ui.components.*
import com.sorin.radiotuner.ui.theme.*
import com.sorin.radiotuner.ui.viewmodel.RadioUiState
import com.sorin.radiotuner.ui.viewmodel.RadioViewModel
import com.sorin.radiotuner.ui.viewmodel.RadioViewModelFactory
import com.sorin.radiotuner.update.UpdateChecker
import com.sorin.radiotuner.update.UpdateInfo
import com.sorin.radiotuner.update.UpdateInstaller
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * (RO)
 * Ecranul principal.
 */
@Composable
fun RadioScreen(factory: RadioViewModelFactory) {
    val viewModel: RadioViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showManualStationDialog by remember { mutableStateOf(false) }
    var stationToEdit by remember { mutableStateOf<Station?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Graphite900)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader()

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
                        onCheckUpdate = {
                            scope.launch {
                                val info = UpdateChecker.check(context)
                                if (info != null) {
                                    updateInfo = info
                                } else {
                                    Toast.makeText(context, "Aplicația este la zi.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                PlayerPane(
                    state = state,
                    viewModel = viewModel,
                    onEditStation = { stationToEdit = it },
                    onCheckUpdate = {
                        scope.launch {
                            val info = UpdateChecker.check(context)
                            if (info != null) {
                                updateInfo = info
                            } else {
                                Toast.makeText(context, "Aplicația este la zi.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
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
                            Text("MENIU", color = Cream, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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

    if (showManualStationDialog) {
        ManualStationDialog(
            onDismiss = { showManualStationDialog = false },
            onConfirm = { name, url, img, gen, ctr, web ->
                viewModel.addManualStation(name, url, img, gen, ctr, web)
                showManualStationDialog = false
            }
        )
    }

    if (stationToEdit != null) {
        ManualStationDialog(
            station = stationToEdit,
            isEditing = true,
            onDismiss = { stationToEdit = null },
            onConfirm = { name, url, img, gen, ctr, web ->
                viewModel.updateManualStation(stationToEdit!!, name, url, img, gen, ctr, web)
                stationToEdit = null
            }
        )
    }

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Actualizare disponibilă") },
            text = { Text("Versiunea ${info.version} este disponibilă. Vrei să o instalezi?") },
            confirmButton = {
                TextButton(onClick = {
                    UpdateInstaller.downloadAndInstall(context, info.apkUrl)
                    updateInfo = null
                }) {
                    Text("Instalează")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) {
                    Text("Mai târziu")
                }
            }
        )
    }
}

@Composable
private fun rememberCurrentTime(): String {
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = formatter.format(Date())
            delay(1000L)
        }
    }
    return currentTime
}

@Composable
private fun AppHeader() {
    // Bara de sus goală (rămâne spațiul, dar fără elemente vizuale)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
    )
}

@Composable
private fun PlayerPane(
    state: RadioUiState,
    viewModel: RadioViewModel,
    onEditStation: (Station) -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isPlaying = state.playbackState is PlaybackState.Playing
    val isFavorite = state.currentStation?.let { current -> state.favoriteStations.any { it.id == current.id } } ?: false
    var showDetails by remember { mutableStateOf(false) }
    val currentTime = rememberCurrentTime()
    val isOnline = state.isOnline

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

    Surface(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        color = Graphite800,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.35f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Bara creată în interiorul frame-ului playerului ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isLandscape) {
                        IconButton(
                            onClick = { viewModel.toggleMenu(true) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = Cream, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(Icons.Default.Radio, contentDescription = null, tint = Amber, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AMBER TUNER",
                        color = Cream,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Ceas
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Amber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = currentTime,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Cream
                        )
                    }

                    // Network / Wifi
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) LedGreen else HeartRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isOnline) "ONLINE" else "OFFLINE",
                            fontSize = 11.sp,
                            color = Muted
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Logo+Cadran în stânga
                Box(modifier = Modifier.size(170.dp)) {
                    TuningDial(
                        isPlaying = isPlaying,
                        imageUrl = state.currentStation?.imageUrl
                    )
                }

                Spacer(Modifier.width(20.dp))

                // Coloana din dreapta
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 1. Badge-ul de State
                    Surface(
                        color = statusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2. Postul de radio + Gen/Țară
                    Text(
                        text = state.currentStation?.name ?: "Niciun post selectat",
                        color = Cream,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        maxLines = 2,
                        lineHeight = 34.sp
                    )
                    
                    if (state.currentStation != null) {
                        Text(
                            text = "${state.currentStation.genre} · ${state.currentStation.country}",
                            color = Muted,
                            fontSize = 16.sp
                        )
                        
                        Spacer(Modifier.height(12.dp))

                        // 3. Butonul Info (sub nume)
                        IconButton(
                            onClick = { showDetails = !showDetails },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = if (showDetails) Amber else Muted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Informațiile (StationDetails) - Apar sub grupul centrat
            if (state.currentStation != null) {
                AnimatedVisibility(visible = showDetails) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        StationDetails(
                            station = state.currentStation,
                            isVisible = true,
                            onEditClick = if (isFavorite) { { onEditStation(state.currentStation) } } else null,
                            onCheckUpdate = onCheckUpdate
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Butoanele Playerului
            PlayerControls(
                playbackState = state.playbackState,
                isFavorite = isFavorite,
                onPrevious = viewModel::skipToPrevious,
                onPlayStop = viewModel::togglePlayStop,
                onNext = viewModel::skipToNext,
                onToggleFavorite = { state.currentStation?.let(viewModel::toggleFavorite) }
            )

            Spacer(Modifier.height(40.dp))

            // RDS-ul (la bază)
            RdsTicker(text = state.rdsInfo?.text, isAvailable = state.rdsInfo?.isAvailable == true)
        }
    }
}
