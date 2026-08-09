package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorin.radiotuner.R
import com.sorin.radiotuner.data.model.CountryOption
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.ui.theme.Amber
import com.sorin.radiotuner.ui.theme.Graphite700
import com.sorin.radiotuner.ui.theme.Graphite800
import com.sorin.radiotuner.ui.theme.Muted

private enum class ListTab { FOUND, PREMIUM, FAVORITE }

/**
 * (RO)
 * Panoul din stânga (landscape) sau din interiorul sertarului (portret):
 * căutare + filtru de țară + cele două liste comutabile. O singură
 * componentă, refolosită în ambele moduri, ca UI-ul să nu diveargă în timp.
 * (EN)
 * The left panel (landscape) or inside the drawer (portrait):
 * search + country filter + the two switchable lists. A single component,
 * reused in both ways, so the UI doesn't diverge over time.
 */
@Composable
fun StationListPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    foundStations: List<Station>,
    favoriteStations: List<Station>,
    curatedStations: List<Station>,
    currentStationId: String?,
    onSelectStation: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    countries: List<CountryOption> = emptyList(),
    selectedCountry: CountryOption? = null,
    onCountrySelected: (CountryOption?) -> Unit = {},
    canLoadMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onAddManualClick: () -> Unit = {},
    onEditStation: (Station) -> Unit = {},
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(ListTab.FOUND) }
    var showCountryDialog by remember { mutableStateOf(false) }
    val favoriteIds = favoriteStations.map { it.id }.toSet()

    Column(modifier = modifier.fillMaxHeight().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SearchBar(query = query, onQueryChange = onQueryChange)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onAddManualClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Graphite700)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add manual", tint = Amber)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Filtru de țară — buton care deschide selectorul; arată alegerea curentă.
        OutlinedButton(
            onClick = { showCountryDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = selectedCountry?.name ?: "Toate țările",
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(0xFF3A2420), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "⚠ $errorMessage",
                    color = androidx.compose.ui.graphics.Color(0xFFFF8A80),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Graphite800, RoundedCornerShape(8.dp))
                .padding(3.dp)
        ) {
            TabButton(
                label = "GĂSITE",
                selected = tab == ListTab.FOUND,
                onClick = { tab = ListTab.FOUND },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                label = "ROMÂNIA",
                selected = tab == ListTab.PREMIUM,
                onClick = { tab = ListTab.PREMIUM },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                label = "FAVORITE",
                selected = tab == ListTab.FAVORITE,
                onClick = { tab = ListTab.FAVORITE },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        val listToShow = when(tab) {
            ListTab.FOUND -> foundStations
            ListTab.PREMIUM -> curatedStations
            ListTab.FAVORITE -> favoriteStations
        }
        
        val emptyText = when(tab) {
            ListTab.FOUND -> stringResource(R.string.empty_no_results)
            ListTab.PREMIUM -> "Niciun post în lista de bază."
            ListTab.FAVORITE -> stringResource(R.string.empty_no_favorites)
        }

        if (listToShow.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(emptyText, color = Muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn {
                items(listToShow, key = { it.id }) { station ->
                    StationRow(
                        station = station,
                        isActive = station.id == currentStationId,
                        isFavorite = favoriteIds.contains(station.id),
                        onClick = { onSelectStation(station) },
                        onToggleFavorite = { onToggleFavorite(station) },
                        onEdit = if (tab == ListTab.FAVORITE || tab == ListTab.PREMIUM) { { onEditStation(station) } } else null
                    )
                }

                // "Încarcă mai multe" — apare doar la tab-ul GĂSITE, doar
                // dacă mai există posturi de adus. Înlocuiește limita
                // fixă de rezultate cu ceva controlat de utilizator.
                if (tab == ListTab.FOUND && canLoadMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                OutlinedButton(onClick = onLoadMore) {
                                    Text("Încarcă mai multe", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCountryDialog) {
        CountryPickerDialog(
            countries = countries,
            selectedName = selectedCountry?.name,
            onDismiss = { showCountryDialog = false },
            onCountrySelected = { country ->
                onCountrySelected(country)
                showCountryDialog = false
            }
        )
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
