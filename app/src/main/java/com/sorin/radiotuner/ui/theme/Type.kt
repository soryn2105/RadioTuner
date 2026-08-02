package com.sorin.radiotuner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * (RO)
 * Tipografia generală (texte, titluri de post).
 * (EN)
 * General typography (texts, post titles).
 */
val RadioTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    )
)

/**
 * (RO)
 * Fontul este monospace, folosit punctual pentru frecvență și ticker-ul RDS.
 * (EN)
 * Monospace style, used for frequency and RDS ticker.
 */
val DisplayMonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp
)