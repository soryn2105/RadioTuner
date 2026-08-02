package com.sorin.radiotuner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorin.radiotuner.ui.theme.Amber
import com.sorin.radiotuner.ui.theme.AmberDim
import com.sorin.radiotuner.ui.theme.Cream
import com.sorin.radiotuner.ui.theme.Graphite900
import com.sorin.radiotuner.ui.theme.Muted

/**
 * (RO)
 * Cadranul central al playerului — pur decorativ, dar reflectă starea:
 * se rotește și se luminează doar cât timp postul chiar redă.
 * Notă: radio-ul prin internet nu are o frecvență reală de emisie (aia
 * ține de FM), deci afișăm un status text ("ON AIR" / "OFF"), nu o
 * frecvență inventată — ar fi informație falsă.
 * (EN)
 * The central dial of the player — purely decorative, but reflects the state:
 * it rotates and lights up only while the station is playing.
 * Note: radio over the internet does not have a real emission frequency
 * (it's just a guess), so we display a status text ("ON AIR" / "OFF"),
 * not a real frequency — it's misleading information.
 */
@Composable
fun TuningDial(statusLabel: String, isPlaying: Boolean) {
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
        Box(
            modifier = Modifier
                .size(132.dp)
                .background(Graphite900, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(statusLabel, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Cream)
                }
            }
        }
    }
}