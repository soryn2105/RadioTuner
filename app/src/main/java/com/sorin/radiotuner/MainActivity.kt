package com.sorin.radiotuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sorin.radiotuner.ui.RadioScreen
import com.sorin.radiotuner.ui.theme.RadioTunerTheme
import com.sorin.radiotuner.ui.viewmodel.RadioViewModelFactory
import com.sorin.radiotuner.RadioTunerApplication


/**
 * (RO)
 * Singura activitate a aplicatiei. Nu contine nici o logica de business -
 * doar monteaza Compose si da mai departe fabrica de ViewModel
 * (construita din AppContainer-ul aplicatiei).
 *
 * (EN)
 * The only activity of the application. It does not contain any business logic -
 * it just mounts Compose and passes on the ViewModel factory
 * (built from the application's AppContainer).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as RadioTunerApplication).container
        val factory = RadioViewModelFactory(container)

        setContent {
            RadioTunerTheme {
                RadioScreen(factory = factory)
            }
        }
    }
}