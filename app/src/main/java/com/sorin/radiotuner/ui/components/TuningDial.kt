package com.sorin.radiotuner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sorin.radiotuner.R
import com.sorin.radiotuner.ui.theme.Amber
import com.sorin.radiotuner.ui.theme.Graphite800
import com.sorin.radiotuner.ui.theme.Graphite900
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * (RO)
 * Cadranul rotativ luminat.
 * Inelul exterior cu puncte se rotește, în timp ce logo-ul din centru rămâne static.
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
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dialRotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(240.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        // (RO) Fundalul cadranului
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(20.dp, CircleShape, ambientColor = Amber.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Graphite800, Graphite900)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
        )

        // (RO) Inelul luminos rotativ (puncte/led-uri)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(currentRotation)
        ) {
            val center = size / 2f
            val radius = (size.minDimension / 2f) - 10.dp.toPx()
            
            // Desenăm punctele de pe cadran
            for (i in 0 until 60) {
                val angleInDegrees = i * 6f
                val angleInRadians = angleInDegrees * (PI / 180f)
                val x = center.width + radius * cos(angleInRadians).toFloat()
                val y = center.height + radius * sin(angleInRadians).toFloat()
                
                val isMajor = i % 5 == 0
                val color = if (isMajor) Amber else Amber.copy(alpha = 0.3f)
                val dotRadius = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                
                drawCircle(
                    color = color,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }

            // Un arc luminos de accent
            drawArc(
                color = Amber.copy(alpha = 0.4f),
                startAngle = -90f,
                sweepAngle = 60f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(center.width - radius, center.height - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        // (RO) Logo-ul postului - STATIC în centru
        Box(
            modifier = Modifier
                .fillMaxSize(0.62f)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Amber.copy(alpha = 0.3f), CircleShape)
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
