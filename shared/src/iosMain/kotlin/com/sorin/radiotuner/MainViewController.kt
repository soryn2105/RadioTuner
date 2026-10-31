package com.sorin.radiotuner

import androidx.compose.material3.Text
import androidx.compose.ui.window.ComposeUIViewController
import com.sorin.radiotuner.player.IosRadioPlayer
import platform.UIKit.UIViewController

/**
 * (RO)
 * Punctul de intrare Compose pentru iOS.
 * Exportă ecranul aplicației într-un UIViewController standard Apple.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    Text("RadioTuner iOS - Compose Multiplatform")
}
