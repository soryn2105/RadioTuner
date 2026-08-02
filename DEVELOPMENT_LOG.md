# Project Development Log - RadioTuner

This document summarizes the changes, fixes, and architectural adjustments made to restore the project to a buildable and functional state.

## 1. Build System & Dependency Restoration

### Version Catalog (`libs.versions.toml`)
- **Compose BOM Support**: Introduced `androidx-compose-bom` (version `2024.06.00`) to manage Compose library versions consistently.
- **Kotlin 2.0 Upgrade**: Updated Kotlin to `2.0.21` to support the new **Compose Compiler** plugin.
- **New Aliases**: Added missing aliases for `activity-compose`, `ui`, `ui-graphics`, `lifecycle-runtime-compose`, and `lifecycle-viewmodel-compose`.
- **Extended Icons**: Explicitly added `androidx.compose.material:material-icons-extended` to provide specialized radio and player icons.
- **Fixed Typos**: Corrected `ktlinx-coroutines-android` to `kotlinx-coroutines-android`.

### Gradle Configuration
- **Root `build.gradle.kts`**: Defined the Kotlin and Compose Compiler plugins.
- **App `build.gradle.kts`**: 
    - Enabled `buildFeatures { compose = true }`.
    - Applied `org.jetbrains.kotlin.plugin.compose`.
    - Migrated hardcoded dependency strings to Version Catalog (`libs.*`) aliases.
    - Fixed the coroutines dependency typo.

## 2. UI & Component Fixes

### General Compose Corrections
- **Compiler Compliance**: Fixed multiple `@Composable invocations can only happen from the context of a @Composable function` errors by converting helper classes/functions into proper Composable functions.
- **Delegate Imports**: Added `import androidx.compose.runtime.getValue` and `setValue` for `remember`ed state properties (e.g., in `TuningDial.kt`).
- **Standardization**: Removed accidental `androidx.wear.compose` imports in the mobile app, replacing them with standard Material 3 components.

### Specific Components
- **`LanguagePickerDialog.kt`**: Fully rewritten. Fixed logic for language selection, corrected typos in string resource references, and replaced `android.widget.RadioButton` (View) with `androidx.compose.material3.RadioButton` (Compose).
- **`SearchBar.kt`**: Fixed the function signature to correctly accept `Modifier` and handle query changes via `(String) -> Unit`.
- **`PlayerControls.kt`**: Renamed internal `RadioButton` to `RoundButton` to avoid name collisions with the Material library and fixed parameter types.
- **`RdsTicker.kt`**: Fixed invalid Kotlin syntax in the parameter list (`text: Text: String?`).

## 3. Data & Network Layer

### Networking
- **`RadioBrowserDto.kt`**: Removed duplicate `SerializedName` imports.
- **`NetworkModule.kt`**: 
    - Fixed the API interface reference to `RadioBrowserApi`.
    - Corrected syntax errors in the Retrofit builder chain.
    - Fixed type mismatch for `HttpLoggingInterceptor` levels.
- **`NetworkStationRepository.kt`**: Corrected the mapping logic from DTOs to the domain `Station` model.

### Persistence
- **`FavoritesDataStore.kt`**: 
    - Added missing `stringPreferencesKey` import.
    - Resolved `fromJson` ambiguity by providing explicit type tokens for GSON list parsing.
    - Fixed DataStore initialization to use the correct context.

## 4. Resource Management

### Strings (`strings.xml`)
A comprehensive set of missing string resources was added to support the multi-language UI, including:
- Menu and Drawer labels (`menu_title`, `menu_open`, etc.).
- Playback status indicators (`status_on_air`, `status_buffering`, `status_error`).
- Language names for the picker (`language_romaniana`, `language_english`, etc.).
- Ticker labels (`rds_label`, `rds_stopped`).

## 5. Landscape Crash & Ripple API Fix
- **Issue**: App crashed in landscape due to `IndicationNodeFactory` mismatch in `Modifier.clickable`.
- **Cause**: Mismatched Compose versions (Foundation 1.7.0+ vs older Material 3).
- **Fix**: Aligned all Compose libraries via `compose-bom:2024.06.00`. Removed all explicit Compose versions in `libs.versions.toml`.
- **Experimental API**: Added `@OptIn(ExperimentalFoundationApi::class)` for `basicMarquee` in `RdsTicker.kt`.

## 6. Summary of Verified Build
The project build was verified using `./gradlew app:assembleDebug`. All initial compiler errors and the landscape runtime crash have been resolved.

---
*Last Updated: 2026-08-01*

## 7. Addendum — Review Pass (Claude)
Two small issues found after reviewing the uploaded, working build:
- **Missing `ACCESS_NETWORK_STATE` permission** in `AndroidManifest.xml`. `ConnectivityObserver` calls
  `ConnectivityManager.getNetworkCapabilities()` / registers a `NetworkCallback`, both of which require
  this permission — without it, offline detection could throw a `SecurityException` or silently fail at
  runtime. **Fixed**: permission added next to `INTERNET`.
- **Stray duplicate resource folders** nested inside `res/values/` (`values/values`, `values/values-en`,
  `values/values-fr`, `values/xml`) — leftovers from merging the delivered package into the existing
  project, likely copied one level too deep. Harmless (AAPT doesn't scan nested subfolders), but messy.
  **Fixed**: removed; the real `res/values-en/`, `res/values-fr/`, `res/xml/` at the top level of `res/`
  are untouched and correct.
- **Bug critic: căutarea nu găsea niciodată niciun post.** Cauza: în
  `NetworkModule.kt`, adresa serverului API avea o literă greșită —
  `"del.api.radio-browser.info"` (litera L) în loc de
  `"de1.api.radio-browser.info"` (cifra 1). Domeniul cu litera L nu
  există, deci fiecare cerere eșua silențios. **Fixed**, plus am adăugat
  un header `User-Agent` (Radio Browser API îl recomandă explicit în
  documentație, fără el unele cereri pot fi respinse).
- **Erorile de rețea erau invizibile în UI.** `RadioUiState.errorMessage`
  exista deja, dar nu era afișat nicăieri — o căutare eșuată arăta
  identic cu "niciun rezultat", fără niciun indiciu. **Fixed**: acum
  apare un banner roșu cu mesajul erorii, deasupra listelor.

## 8. Descoperire automată de servere + failover
- **Motivație**: userul a cerut ca aplicația să poată găsi orice post din
  lume. Clarificare importantă: un singur server Radio Browser are deja
  toată baza mondială — schimbarea de aici e despre *fiabilitate*, nu
  despre acoperire.
- **`RadioBrowserServers.kt` (nou)**: descoperă serverele active via
  `https://all.api.radio-browser.info/json/servers`, cu listă de rezervă
  hardcodată dacă discovery-ul eșuează.
- **`NetworkModule.kt`**: nu mai construiește UN client fix pentru UN
  server — construiește Retrofit pentru orice host primit ca parametru.
- **`NetworkStationRepository.kt`**: la căutare, încearcă serverele pe
  rând (amestecate aleatoriu); dacă unul eșuează, trece automat la
  următorul (failover), fără să oprească toată căutarea.
- **`AppContainer.kt`**: actualizat constructorul apelat (semnătura s-a
  schimbat — `NetworkStationRepository()` fără parametri acum).
