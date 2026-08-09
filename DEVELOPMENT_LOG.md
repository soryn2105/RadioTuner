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

## 9. Filtru de tara + eliminarea limitei fixe de rezultate
- **Filtru de tara**: nou endpoint `json/countries` in `RadioBrowserApi`,
  model `CountryOption`, dialog `CountryPickerDialog` (cautare locala in
  lista de ~250 tari + numar de posturi per tara). Selectia trece prin
  `countrycode` in cererea de cautare.
- **Eliminarea limitei fixe**: in loc sa maresc pur si simplu `limit`
  (risc: liste uriase, incet de randat, abuz pe API-ul gratuit), am
  implementat paginare reala cu `offset` + buton "Incarca mai multe" in
  `StationListPanel`. Fiecare pagina = 50 de rezultate; utilizatorul
  decide cate pagini incarca, nu un numar fix in cod.
- **`StationRepository`**: semnatura `search()` schimbata — acum ia
  `countryCode` si `offset`. Am actualizat toate implementarile si
  punctele de apel (verificat cu grep, nimic ramas pe semnatura veche).
- **`NetworkStationRepository`**: extras un helper `withFailover<T>()`
  comun pentru cautare SI listarea tarilor, ca sa nu duplic logica de
  failover intre servere in doua locuri.

## 10. Fix: selectorul de tara aparea gol (nimic de selectat)
- **Cauza reala**: presupunere gresita despre raspunsul `/json/countries`
  — am construit `CountryOption` pe baza unui camp `iso_3166_1` care
  DE FAPT NU exista in raspunsul acelui endpoint (el intoarce doar
  `name` si `stationcount`). Fiecare tara pica silentios la mapare,
  lista ramanea goala, dialogul arata doar "Toate tarile".
- **Fix**: renuntat complet la codul ISO. Filtrarea foloseste acum
  parametrul `country` (text simplu, exact numele intors de API), nu
  `countrycode`. Schimbat in: `RadioBrowserDto`, `Station.kt`
  (CountryOption fara `code`), `RadioBrowserApi`, `StationRepository`,
  `NetworkStationRepository`, `RadioViewModel`, `CountryPickerDialog`.
- **Imbunatatire suplimentara**: inaltimea listei din dialog nu mai e
  un numar fix (420dp) — se calculeaza din inaltimea reala a ecranului
  (`LocalConfiguration`), ca sa nu fie taiata pe telefoane in landscape
  (ecran scund). Am adaugat si un mesaj clar daca lista de tari chiar
  nu s-a incarcat (in loc sa para "stricat" fara explicatie).

## 11. HLS and Cleartext Traffic Fix
- **Issue**: App crashed with `ClassNotFoundException` when playing HLS radio streams and blocked `http://` (non-HTTPS) streams.
- **Fix**: 
    - Added `media3-exoplayer-hls` dependency in Version Catalog and Build Gradle.
    - Enabled `android:usesCleartextTraffic="true"` in `AndroidManifest.xml`.
- **Details**: See [HLS_AND_CLEARTEXT_FIX.md](file:///C:/ProgrameAndroidStudio/RadioTuner/HLS_AND_CLEARTEXT_FIX.md).

## 12. Search Optimization (More Stations Found)
- **Issue**: App found fewer Romanian stations than other players.
- **Fix**:
    - Enabled `hidebroken=false` in API calls (allows seeing all stations, even if flagged as unstable).
    - Added `countrycode` support (searching by "RO" is more reliable than "Romania").
    - Added sorting by `clickcount` descending (shows popular/working stations first).
- **Changes**: Updated `RadioBrowserApi`, `StationRepository`, `NetworkStationRepository`, and `RadioViewModel`.

## 15. Background Playback Fix (MediaSessionService)
- **Issue**: Playback stopped when the screen turned off or the app went to the background.
- **Fix**:
    - Implemented `PlaybackService` (inheriting from `MediaSessionService`).
    - Configured `ExoPlayer` with `AudioAttributes` and `WakeMode` to prevent the CPU from sleeping.
    - Added a `MediaSession` to keep the app alive as a "Foreground Service".
    - Refactored `ExoRadioPlayer` to control the service via a `MediaController`.
    - Added required permissions to `AndroidManifest.xml` (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`).
- **Details**: New file `PlaybackService.kt`. Updated `ExoRadioPlayer.kt` and `AndroidManifest.xml`.

## 14. Romanian Specialized Repository (Premium List)
- **Motivation**: Users wanted a "very good" source for Romanian stations. Global APIs often contain duplicate or low-quality entries.
- **Implementation**: Created a **Hybrid Repository** strategy.
    - **Curated List**: Manually verified a list of top 12 Romanian stations (Radio ZU, Kiss FM, Europa FM, etc.) in `CuratedRomanianStations.kt`.
    - **Decorator Pattern**: `RomanianSpecializedRepository` wraps the global search. It injects the curated "Premium" stations at the top of results when searching for Romania or when the app first loads.
    - **Result**: Immediate access to the most popular/reliable Romanian stations while maintaining the ability to search for anything worldwide.
- **Details**: New files `CuratedRomanianStations.kt` and `RomanianSpecializedRepository.kt`. Updated `AppContainer.kt`.

## 13. Improved API Resilience (Failover & Timeouts)
- **Issue**: Intermittent `UnknownHostException` (e.g., `nl1.api.radio-browser.info`) or `SocketTimeoutException` broke searches.
- **Fix**:
    - Added 5s connection and 10s read timeouts to `NetworkModule`.
    - Enhanced `NetworkStationRepository` to force-refresh the server list if a full failover cycle fails.
    - Sanitized error messages to be user-friendly ("Check internet connection") instead of technical DNS errors.
- **Details**: Updated `NetworkModule`, `RadioBrowserServers`, and `NetworkStationRepository`.

## 16. Fixed Fatal Lint Errors (Resource Synchronization)
- **Issue**: Release build failed due to `ExtraTranslation` errors (strings present in translations but missing/mismatched in default locale).
- **Fix**: 
    - Renamed `stringsearch_placeholder` to `search_placeholder` in default `strings.xml`.
    - Renamed `language_romaniana` to `language_romanian` in default `strings.xml`.
    - Updated code references in `SearchBar.kt` and `LanguagePickerDialog.kt`.
- **Result**: Successfully verified with `./gradlew app:assembleRelease`.

## 18. Removed Multi-Language Support
- **Motivation**: User requested to simplify the app and remove additional language support.
- **Changes**:
    - Deleted `values-en` and `values-fr` resource folders.
    - Removed `LanguageManager.kt` and `LanguagePickerDialog.kt`.
    - Removed the language selection icon from the top bar in `RadioScreen.kt`.
    - Cleaned up `AndroidManifest.xml` by removing `android:localeConfig`.
    - Removed language-related strings from the default `strings.xml`.
- **Result**: App now only uses the default language (English) and is lighter.

## 17. Detailed Station Information & UI Polishing
- **Requirement**: Users wanted to see more info like website, stream URL, and bitrate.
- **Fix**: 
    - Implemented `StationDetails.kt` component with animated expand/collapse.
    - Added an "Info" (i) button next to the station name in the player.
    - Updated `Station` model to include `homepage`.
    - Enhanced `TuningDial.kt` to display the station's **logo (favicon)** in the center of the rotating dial using the Coil library.
- **Landscape Layout**: Redesigned the landscape player to a two-column layout (Dial on left, Controls/RDS/Details on right) with vertical scrolling support to prevent UI clipping.
