package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorin.radiotuner.R
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.ui.theme.Amber
import com.sorin.radiotuner.ui.theme.Graphite700
import com.sorin.radiotuner.ui.theme.Graphite800
import com.sorin.radiotuner.ui.theme.Muted

private enum class ListTab { FOUND, FAVORITE }

/**
 * (RO)
 * Panoul din stânga (landscape) sau din interiorul sertarului (portret):
 * căutare + cele două liste comutabile. O singură componentă, refolosită
 * în ambele moduri, ca UI-ul să nu diveargă în timp.
 * (EN)
 * The left panel (landscape) or inside the drawer (portrait):
 * search + the two switchable lists. A single component, reused
 * in both ways, so the UI doesn't diverge over time.
 */

@Composable
fun StationListPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    foundStations: List<Station>,
    favoriteStations: List<Station>,
    currentStationId: String?,
    onSelectStation: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(ListTab.FOUND) }
    val favoriteIds = favoriteStations.map { it.id }.toSet()

    Column(modifier = modifier.fillMaxHeight().padding(16.dp)) {
        SearchBar(query = query, onQueryChange = onQueryChange)

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Graphite800, RoundedCornerShape(8.dp))
                .padding(3.dp)
        ) {
            TabButton(
                label = stringResource(R.string.tab_found, foundStations.size),
                selected = tab == ListTab.FOUND,
                onClick = { tab = ListTab.FOUND },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                label = stringResource(R.string.tab_favorites, favoriteStations.size),
                selected = tab == ListTab.FAVORITE,
                onClick = { tab = ListTab.FAVORITE },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        val listToShow = if (tab == ListTab.FOUND) foundStations else favoriteStations
        val emptyText =
            if (tab == ListTab.FOUND) stringResource(R.string.empty_no_results) else stringResource(
                R.string.empty_no_favorites
            )

        if (listToShow.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(emptyText, color = Muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn {
                items(listToShow, key = { it.id })
                { station ->
                    StationRow(
                        station = station,
                        isActive = station.id == currentStationId,
                        isFavorite = favoriteIds.contains(station.id),
                        onClick = { onSelectStation(station) },
                        onToggleFavorite = { onToggleFavorite(station) }
                    )
                }
            }
        }
    }
}


@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Graphite700 else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Amber else Muted
        )
    }
}