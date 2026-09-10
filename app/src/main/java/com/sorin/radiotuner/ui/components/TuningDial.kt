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
 * Cadranul rotativ al playerului.
 * Acum conține DOAR logo-ul postului, separat de starea redării.
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
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dialRotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(240.dp)
            .shadow(24.dp, CircleShape, ambientColor = Amber.copy(alpha = 0.3f))
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Graphite800, Graphite900)
                )
            )
            .border(2.dp, Amber.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // (RO) Inelul decorativ (ca un disc de vinil)
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
        )

        // (RO) Logo-ul care se rotește
        Box(
            modifier = Modifier
                .fillMaxSize(0.75f)
                .rotate(currentRotation)
                .clip(CircleShape)
                .background(Color.Black)
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
