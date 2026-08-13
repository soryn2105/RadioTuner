# Project Development Log - RadioTuner

This document summarizes the changes, fixes, and architectural adjustments made to restore the project to a buildable and functional state.

## 1. Build System & Dependency Restoration
- **Compose BOM Support**: Introduced `androidx-compose-bom` (version `2024.06.00`) to manage Compose library versions consistently.
- **Kotlin 2.0 Upgrade**: Updated Kotlin to `2.0.21` to support the new **Compose Compiler** plugin.
- **New Aliases**: Added missing aliases for `activity-compose`, `ui`, `ui-graphics`, `lifecycle-runtime-compose`, and `lifecycle-viewmodel-compose`.
- **Extended Icons**: Explicitly added `androidx.compose.material:material-icons-extended`.

## 2. UI & Component Fixes
- **Compiler Compliance**: Fixed multiple `@Composable` context errors.
- **`LanguagePickerDialog.kt`**: Fully rewritten (later removed in favor of single-language support).
- **`SearchBar.kt`**: Fixed signature and query handling.
- **`PlayerControls.kt`**: Renamed buttons to avoid Material library collisions.

## 3. Data & Network Layer
- **`NetworkModule.kt`**: Fixed API interface references and Retrofit builder chain.
- **`NetworkStationRepository.kt`**: Corrected mapping logic and added failover support.
- **`FavoritesDataStore.kt`**: Resolved JSON parsing ambiguity and added DataStore initialization.

## 4. Landscape Crash & Ripple API Fix
- **Issue**: App crashed in landscape due to `IndicationNodeFactory` mismatch.
- **Fix**: Aligned all Compose libraries via BOM and added `@OptIn` for experimental marquee APIs.

## 5. Background Playback (MediaSessionService)
- **Issue**: Playback stopped when the screen turned off.
- **Fix**: Implemented `PlaybackService.kt` (`MediaSessionService`). Configured `ExoPlayer` with `WAKE_MODE_NETWORK` and automatic audio focus.
- **Permissions**: Added `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `POST_NOTIFICATIONS`.

## 6. Improved API Resilience & Discovery
- **Dynamic Discovery**: Implemented `RadioBrowserServers.kt` to find active mirrors in real-time.
- **Automatic Failover**: Added logic to switch between Germany, Austria, and Netherlands mirrors if one fails.
- **Timeouts**: Added 5s connection and 10s read timeouts to prevent app hangs.

## 7. Romanian Specialized Experience
- **Premium List**: Created `CuratedRomanianStations.kt` with a manually verified list of top stations.
- **Hybrid Repository**: `RomanianSpecializedRepository.kt` injects these premium stations at the top of search results.
- **Deep Search**: Merges results from Country ("RO") and Language ("romanian") filters to maximize findings.

## 8. Public In-App Update System
- **Worker Proxy**: Deployed a Cloudflare Worker to proxy Gitea (which is behind Cloudflare Access).
- **Auto-Update**: `UpdateChecker.kt` compares local version with Gitea releases and triggers a download/install dialog.

## 9. Automated Versioning
- **CI/CD**: Configured `release.yml` to pass `github.ref_name` as `versionName` and `github.run_number` as `versionCode`.
- **Logic**: Updated the app to use Semantic Versioning comparison (e.g., 1.1.0 > 1.0.9).

## 10. UI Enhancements & Information
- **Station Details**: Added an animated panel showing Website, Stream URL, and Bitrate.
- **Copy to Clipboard**: Added a button to copy the direct stream URL.
- **Logo Integration**: Integrated **Coil** library to show station favicons in lists and in the center of the rotating dial.
- **About Dialog**: Added an info icon to the header to show the current version and developer info.

## 11. UI Simplification, Larger Text & State Persistence
- **Changes**:
    - **Removed "ROMANIA" Tab**: Eliminated the dedicated premium category to simplify the interface.
    - **Persistence**: Added logic to save and restore the last selected tab (Found/Favorite) and the last listened station using DataStore.
    - **Larger Text**: Increased font sizes globally (Names: 17-26sp, Details: 14-16sp, Header: 20sp) for better readability.
    - **Unified Repository**: Reverted to `NetworkStationRepository` for a cleaner, standard search experience.
- **Details**: Updated `RadioUiState`, `RadioViewModel`, `StationListPanel`, and most UI components.

## 11. Dedicated "ROMÂNIA" Tab & Editable Streams
- **UI**: Added a third tab to the main panel: **GĂSITE**, **ROMÂNIA**, **FAVORITE**.
- **Editability**: Enabled editing (pencil icon) for Premium stations. Modified values are saved locally and override defaults.
- **Verified Streams**: Updated **Radio Prahova** (320kbps SSL) and the **Capital FM** network (Dance, Manele, Etno) with direct, high-quality SSL links.

## 12. Search Optimization (Full Results)
- **Station Count**: Disabled aggressive auto-validation that was hiding 60% of Romanian stations.
- **Limit Increase**: Increased search limit to 100 stations per page to reach the full ~1000+ Romanian station database faster.
