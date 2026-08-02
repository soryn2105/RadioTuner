package com.sorin.radiotuner.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * (RO)
 * Limbile suportate manual din aplicație, independent de limba telefonului.
 * (EN)
 * The languages supported manually in the application, independent of the phone's language.
 */

enum class AppLanguage(val tag: String, val displayName: String) {
    ROMANIAN("ro", "Română"),
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    SYSTEM("", "Limba telefonului")
}

/**
 * (RO)
 * Aplică sau citește limba aleasă manual de utilizator.
 * Se bazează pe AppCompatDelegate.setApplicationLocales, mecanismul
 * oficial Android pentru suprascrierea limbii DOAR pentru o singură
 * aplicație. AndroidX reține automat alegerea între lansări — nu mai
 * trebuie să o salvăm noi separat.
 * (EN)
 * Applies or reads the language manually chosen by the user.
 * It relies on AppCompatDelegate.setApplicationLocales, the official
 * Android mechanism for overriding the language ONLY for a single
 * application. AndroidX automatically remembers the choice between launches
 */
object LanguageManager {

    fun setLanguage(language: AppLanguage) {
        val localeList = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun currentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) AppLanguage.SYSTEM.tag else locales[0]?.language.orEmpty()
    }
}