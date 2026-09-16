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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorin.radiotuner.BuildConfig
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.ui.theme.*

/**
 * (RO)
 * Afișează informații detaliate unificate: datele postului + datele aplicației.
 * Include funcții de copiere, editare și verificare actualizări.
 */
@Composable
fun StationDetails(
    station: Station,
    isVisible: Boolean,
    onEditClick: (() -> Unit)? = null,
    onCheckUpdate: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Secțiunea Post Radio ---
            Text("DETALII POST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber.copy(alpha = 0.7f))
            
            InfoRow(
                icon = Icons.Default.Language,
                label = "Site Web",
                value = station.homepage ?: "Nespecificat"
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        icon = Icons.Default.Link,
                        label = "Adresă Flux",
                        value = station.streamUrl
                    )
                }
                
                Row {
                    if (onEditClick != null) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Amber, modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(
                        onClick = {
                            copyToClipboard(context, station.streamUrl)
                            Toast.makeText(context, "Link copiat!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Amber.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            InfoRow(
                icon = Icons.Default.Speed,
                label = "Calitate",
                value = if (station.bitrate != null && station.bitrate > 0) "${station.bitrate} kbps" else "Necunoscută"
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            // --- Secțiunea Aplicație & Dezvoltator ---
            Text("DESPRE APLICAȚIE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber.copy(alpha = 0.7f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        icon = Icons.Default.Info,
                        label = "Versiune Instalată",
                        value = BuildConfig.VERSION_NAME
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Dezvoltator",
                        value = "Sorin"
                    )
                }
                
                Button(
                    onClick = onCheckUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = Graphite700),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp), tint = Amber)
                    Spacer(Modifier.width(6.dp))
                    Text("UPDATE", fontSize = 11.sp, color = Cream)
                }
            }
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
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(label, fontSize = 11.sp, color = Muted)
            Text(
                text = value,
                fontSize = 14.sp,
                color = Cream,
                maxLines = 2
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Radio Stream URL", text)
    clipboard.setPrimaryClip(clip)
}
