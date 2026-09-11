package com.sorin.radiotuner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sorin.radiotuner.R
import com.sorin.radiotuner.ui.theme.Amber
import com.sorin.radiotuner.ui.theme.Graphite800
import com.sorin.radiotuner.ui.theme.Graphite900

/**
 * (RO)
 * Cadranul rotativ luminat cu efect de degrade (glow).
 * Inelul exterior de lumină se rotește în degrade, în timp ce logo-ul din centru rămâne static.
 */
@Composable
fun TuningDial(
    isPlaying: Boolean,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowRotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // (RO) Inelul luminos în degrade care se rotește
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(currentRotation)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Amber.copy(alpha = 0.1f),
                            Amber.copy(alpha = 0.8f), // Partea cea mai luminoasă
                            Amber.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // (RO) Corpul principal al cadranului (acoperă centrul gradientului)
        Box(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Graphite800, Graphite900)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        )

        // (RO) Logo-ul postului - STATIC în centru
        Box(
            modifier = Modifier
                .fillMaxSize(0.65f)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Amber.copy(alpha = 0.4f), CircleShape)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Logo post",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_radio_placeholder),
                error = painterResource(R.drawable.ic_radio_placeholder),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
