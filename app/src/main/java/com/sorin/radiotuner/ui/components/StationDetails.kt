package com.sorin.radiotuner.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.ui.theme.*

/**
 * (RO)
 * Afișează informații detaliate despre postul selectat: site web, URL flux, bitrate.
 * Include funcție de copiere pentru adresa de stream.
 */
@Composable
fun StationDetails(
    station: Station,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InfoRow(
                icon = Icons.Default.Language,
                label = "Site Web",
                value = station.homepage ?: "Nespecificat"
            )
            
            // Adresă Flux cu buton de copiere
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        icon = Icons.Default.Link,
                        label = "Adresă Flux",
                        value = station.streamUrl
                    )
                }
                IconButton(
                    onClick = {
                        copyToClipboard(context, station.streamUrl)
                        Toast.makeText(context, "Link copiat!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Amber.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(label, fontSize = 12.sp, color = Muted)
            Text(
                text = value,
                fontSize = 15.sp,
                color = Cream,
                maxLines = 3
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Radio Stream URL", text)
    clipboard.setPrimaryClip(clip)
}
