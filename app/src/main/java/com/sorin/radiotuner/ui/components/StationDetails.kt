package com.sorin.radiotuner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.ui.theme.Amber
import com.sorin.radiotuner.ui.theme.Graphite800
import com.sorin.radiotuner.ui.theme.Muted
import com.sorin.radiotuner.ui.theme.Cream

/**
 * (RO)
 * Afișează informații detaliate despre postul selectat: site web, URL flux, bitrate.
 * (EN)
 * Displays detailed information about the selected station: website, stream URL, bitrate.
 */
@Composable
fun StationDetails(
    station: Station,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Graphite800.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoRow(
                icon = Icons.Default.Language,
                label = "Site Web",
                value = station.homepage ?: "Nespecificat"
            )
            InfoRow(
                icon = Icons.Default.Link,
                label = "Adresă Flux",
                value = station.streamUrl
            )
            InfoRow(
                icon = Icons.Default.Speed,
                label = "Calitate",
                value = if (station.bitrate != null && station.bitrate > 0) "${station.bitrate} kbps" else "Necunoscută"
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(label, fontSize = 10.sp, color = Muted)
            Text(
                text = value,
                fontSize = 12.sp,
                color = Cream,
                maxLines = 2
            )
        }
    }
}
