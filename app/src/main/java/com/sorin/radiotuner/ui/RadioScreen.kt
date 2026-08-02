package com.sorin.radiotuner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sorin.radiotuner.R
import com.sorin.radiotuner.player.PlaybackState
import com.sorin.radiotuner.ui.components.*
import com.sorin.radiotuner.ui.theme.*
import com.sorin.radiotuner.ui.viewmodel.RadioViewModel
import com.sorin.radiotuner.ui.viewmodel.RadioViewModelFactory
import com.sorin.radiotuner.util.AppLanguage
import com.sorin.radiotuner.util.LanguageManager

/**
 * (RO)
 * Ecranul principal. Comută complet layout-ul după orientare:
 *  - LANDSCAPE: panoul de căutare/liste stă fix pe stânga (ca înainte).
 *  - PORTRAIT: panoul e ascuns; apare doar peste player, ca un sertar,
 *    la apăsarea butonului hamburger, și se închide automat la selectarea
 *    unui post.
 * (EN)
 * The main screen. Composes the entire layout depending on the orientation:
 * - LANDSCAPE: the search/list panel is fixed on the left side (before the player).
 * - PORTRAIT: the panel is hidden; it appears over the player, as a drawer,
 * when the hamburger button is pressed, and closes automatically when a station is selected.
 * @param factory Factory for the view model.
 */
@Composable
fun RadioScreen(factory: RadioViewModelFactory) {
    val viewModel: RadioViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showLanguageDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Graphite900)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                isOnline = state.isOnline,
                showMenuButton = !isLandscape,
                onMenuClick = { viewModel.toggleMenu(true) },
                onLanguageClick = { showLanguageDialog = true }
            )

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    StationListPanel(
                        query = state.query,
                        onQueryChange = viewModel::onQueryChange,
                        foundStations = state.foundStations,
                        favoriteStations = state.favoriteStations,
                        currentStationId = state.currentStation?.id,
                        onSelectStation = viewModel::selectStation,
                        onToggleFavorite = viewModel::toggleFavorite,
                        modifier = Modifier.width(330.dp)
                    )
                    PlayerPane(state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
                }
            } else {
                PlayerPane(state = state, viewModel = viewModel, modifier = Modifier.fillMaxSize())
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
                    // fundal semi-transparent, clic = închide
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
                            Text(stringResource(R.string.menu_title), color = Cream, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.toggleMenu(false) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.menu_close), tint = Cream)
                            }
                        }
                        StationListPanel(
                            query = state.query,
                            onQueryChange = viewModel::onQueryChange,
                            foundStations = state.foundStations,
                            favoriteStations = state.favoriteStations,
                            currentStationId = state.currentStation?.id,
                            onSelectStation = viewModel::selectStation,
                            onToggleFavorite = viewModel::toggleFavorite,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentTag = LanguageManager.currentLanguageTag(),
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                LanguageManager.setLanguage(language)
                showLanguageDialog = false
            }
        )
    }
}

@Composable
private fun AppHeader(
    isOnline: Boolean,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onLanguageClick: () -> Unit
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
                    Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_open), tint = Cream)
                }
            }
            Icon(Icons.Default.Radio, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.app_name).uppercase(),
                color = Cream,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLanguageClick) {
                Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language_picker_title), tint = Muted)
            }
            Icon(
                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isOnline) LedGreen else HeartRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (isOnline) stringResource(R.string.status_online) else stringResource(R.string.status_offline),
                fontSize = 10.sp,
                color = Muted
            )
        }
    }
}

@Composable
private fun PlayerPane(state: com.sorin.radiotuner.ui.viewmodel.RadioUiState, viewModel: RadioViewModel, modifier: Modifier) {
    val isPlaying = state.playbackState is PlaybackState.Playing
    val isFavorite = state.currentStation?.let { current -> state.favoriteStations.any { it.id == current.id } } ?: false

    val statusLabel = when (state.playbackState) {
        is PlaybackState.Playing -> stringResource(R.string.status_on_air)
        is PlaybackState.Buffering -> stringResource(R.string.status_buffering)
        is PlaybackState.Error -> stringResource(R.string.status_error)
        else -> stringResource(R.string.status_idle)
    }

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TuningDial(statusLabel = statusLabel, isPlaying = isPlaying)

        Spacer(Modifier.height(16.dp))

        Text(
            text = state.currentStation?.name ?: stringResource(R.string.no_station_selected),
            color = Cream,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        if (state.currentStation != null) {
            Text(
                text = "${state.currentStation.genre} · ${state.currentStation.country}",
                color = Muted,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(20.dp))

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