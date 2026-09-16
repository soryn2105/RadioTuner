package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorin.radiotuner.R
import com.sorin.radiotuner.ui.theme.Amber
import com.sorin.radiotuner.ui.theme.LedGreen


/**
 * (RO)
 * Afișajul "RDS" — de fapt metadata ICY a stream-ului.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RdsTicker(text: String?, isAvailable: Boolean) {
    Column {
        Text(
            text = stringResource(R.string.rds_label),
            fontSize = 12.sp,
            color = Amber,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.padding(top = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1210), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF33363A), RoundedCornerShape(8.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = if (isAvailable && !text.isNullOrBlank()) text else stringResource(R.string.rds_stopped),
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                color = LedGreen,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}
