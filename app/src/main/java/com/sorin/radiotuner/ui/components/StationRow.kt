package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.sorin.radiotuner.R
import com.sorin.radiotuner.data.model.Station
import com.sorin.radiotuner.ui.theme.*

/**
 * (RO)
 * Un singur rând din listele "găsite" / "favorite".
 * Nu știe în care listă se află — primește doar datele și callback-urile,
 * complet reutilizabil.
 * (EN)
 * A single row in the "found" / "favorite" lists.
 * Doesn't know which list it's in — just gets the data and the callbacks,
 * complete reusable.
 */
@Composable
fun StationRow(
    station: Station,
    isActive: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Amber.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // (RO) Logo post (Favicon)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Graphite800)
        ) {
            AsyncImage(
                model = station.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_radio_placeholder),
                placeholder = painterResource(R.drawable.ic_radio_placeholder),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = Cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${station.genre} · ${station.country}",
                    fontSize = 11.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (station.bitrate != null && station.bitrate > 0) {
                    Text(
                        text = " · ${station.bitrate}k",
                        fontSize = 10.sp,
                        color = Amber.copy(alpha = 0.7f),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) HeartRed else Muted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
