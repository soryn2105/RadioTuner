package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sorin.radiotuner.data.model.CountryOption

/**
 * (RO)
 * Dialog de alegere a țării pentru filtrarea posturilor. Prima opțiune
 * e mereu "Toate țările" (country = null în ViewModel), apoi lista reală
 * de la Radio Browser API, cu un câmp de căutare local (util pentru
 * ~250 țări) și numărul de posturi lângă fiecare.
 *
 * Filtrarea se face după NUME de țară (nu cod ISO) — endpoint-ul
 * json/countries nu întoarce niciun cod, doar name + stationcount.
 *
 * Înălțimea listei se calculează din înălțimea reală a ecranului
 * (LocalConfiguration), nu e un număr fix — pe telefon în landscape,
 * ecranul e scund, iar o înălțime fixă mare "tăia" lista în afara
 * zonei vizibile/atingibile a dialogului.
 *
 * (EN)
 * Country picker dialog for filtering stations. The first option is
 * always "All countries" (country = null in the ViewModel), then the
 * real list from the Radio Browser API, with a local search field
 * (useful for ~250 countries) and the station count next to each one.
 *
 * Filtering is done by country NAME (not ISO code) — the json/countries
 * endpoint doesn't return any code, only name + stationcount.
 *
 * The list height is computed from the actual screen height
 * (LocalConfiguration), not a fixed number — on a phone in landscape,
 * the screen is short, and a large fixed height "cut off" the list
 * outside the dialog's visible/tappable area.
 */
@Composable
fun CountryPickerDialog(
    countries: List<CountryOption>,
    selectedName: String?,
    onDismiss: () -> Unit,
    onCountrySelected: (CountryOption?) -> Unit
) {
    var filterText by remember { mutableStateOf("") }
    val filtered = remember(countries, filterText) {
        if (filterText.isBlank()) countries
        else countries.filter { it.name.contains(filterText, ignoreCase = true) }
    }

    val configuration = LocalConfiguration.current
    val listHeight = (configuration.screenHeightDp * 0.5f).dp.coerceIn(160.dp, 360.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alege țara") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Caută țară…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                if (countries.isEmpty()) {
                    Text(
                        text = "Lista de țări nu s-a încărcat încă (sau serverul a fost temporar indisponibil). Poți căuta oricum, fără filtru de țară.",
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(listHeight)) {
                        item {
                            CountryRow(
                                label = "Toate țările",
                                count = null,
                                selected = selectedName == null,
                                onClick = { onCountrySelected(null) }
                            )
                        }
                        items(filtered, key = { it.name }) { country ->
                            CountryRow(
                                label = country.name,
                                count = country.stationCount,
                                selected = country.name == selectedName,
                                onClick = { onCountrySelected(country) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Închide") }
        }
    )
}

@Composable
private fun CountryRow(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (count != null) "$label ($count)" else label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
