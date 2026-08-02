package com.sorin.radiotuner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sorin.radiotuner.ui.theme.*

/**
 * (RO)
 * Cadranul central al playerului — acum include și logo-ul postului (dacă e disponibil).
 * Se rotește și se luminează doar cât timp postul chiar redă.
 */
@Composable
fun TuningDial(statusLabel: String, isPlaying: Boolean, imageUrl: String? = null) {
    val transition = rememberInfiniteTransition(label = "dial")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Box(
        modifier = Modifier.size(168.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inelul luminos rotativ
        Box(
            modifier = Modifier
                .size(168.dp)
                .rotate(if (isPlaying) rotation else 0f)
                .background(
                    brush = Brush.sweepGradient(
                        colors = if (isPlaying) listOf(Amber, AmberDim, Color.Transparent, Color.Transparent)
                        else listOf(AmberDim.copy(alpha = 0.15f), AmberDim.copy(alpha = 0.15f))
                    ),
                    shape = CircleShape
                )
        )
        
        // Fundalul interior
        Box(
            modifier = Modifier
                .size(132.dp)
                .background(Graphite900, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Logo post (centrat)
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.2f)),
                    alpha = if (isPlaying) 1f else 0.5f
                )
            }

            // Etichetă Status (peste logo, dacă e pornit)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (imageUrl != null) Color.Black.copy(alpha = 0.3f) else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusLabel,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isPlaying) Cream else Muted
                )
            }
        }
    }
}
