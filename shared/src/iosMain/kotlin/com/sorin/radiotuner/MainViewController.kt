package com.sorin.radiotuner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeUIViewController
import com.sorin.radiotuner.player.IosRadioPlayer
import com.sorin.radiotuner.ui.components.PlayerControls
import com.sorin.radiotuner.ui.components.RdsTicker
import com.sorin.radiotuner.ui.theme.Amber
import com.sorin.radiotuner.ui.theme.Graphite900
import com.sorin.radiotuner.ui.theme.RadioTunerTheme
import platform.UIKit.UIViewController

/**
 * (RO)
 * Punctul de intrare Compose pentru iOS.
 * Afișează controalele și ticker-ul RDS folosind componentele comune Compose.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val iosPlayer = remember { IosRadioPlayer() }
    val playbackState by iosPlayer.playbackState.collectAsState()
    val rdsInfo by iosPlayer.rdsInfo.collectAsState()

    RadioTunerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Graphite900)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AMBER TUNER",
                    fontSize = 24.sp,
                    color = Amber
                )
                Spacer(Modifier.height(24.dp))
                RdsTicker(
                    text = rdsInfo?.text,
                    isAvailable = rdsInfo?.isAvailable ?: false
                )
                Spacer(Modifier.height(32.dp))
                PlayerControls(
                    playbackState = playbackState,
                    isFavorite = false,
                    onPrevious = { },
                    onPlayStop = {
                        if (playbackState is com.sorin.radiotuner.player.PlaybackState.Playing) {
                            iosPlayer.stop()
                        }
                    },
                    onNext = { },
                    onToggleFavorite = { }
                )
            }
        }
    }
}
