package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sorin.radiotuner.player.PlaybackState
import com.sorin.radiotuner.ui.theme.*

@Composable
fun PlayerControls(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    onPrevious: () -> Unit,
    onPlayStop: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isPlaying = playbackState is PlaybackState.Playing

    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundButton(onClick = onPrevious, size = 42.dp) {
            Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Cream, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        RoundButton(onClick = onPlayStop, size = 58.dp, filled = true) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Graphite900,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        RoundButton(onClick = onNext, size = 42.dp) {
            Icon(Icons.Default.SkipNext, contentDescription = null, tint = Cream, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        RoundButton(onClick = onToggleFavorite, size = 42.dp) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) HeartRed else Cream,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun RoundButton(
    onClick: () -> Unit,
    size: Dp,
    filled: Boolean = false,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .background(if (filled) Amber else Graphite700, CircleShape)
            .border(1.dp, if (filled) Amber else Color(0xFF33363A), CircleShape)
    ) {
        content()
    }
}
