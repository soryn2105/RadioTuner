package com.sorin.radiotuner

import android.app.Application
import com.sorin.radiotuner.di.AppContainer

/**
 * (RO)
 * Punctul de start al aplicației. Creează AppContainer o singură dată,
 * cât timp trăiește procesul — MainActivity îl citește de aici.
 *
 * (EN)
 * The entry point of the application. Creates AppContainer only once,
 * while the process is alive — MainActivity reads it from here.
 */

class RadioTunerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}