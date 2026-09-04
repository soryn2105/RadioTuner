# Project Development Log - RadioTuner

This document summarizes the changes, fixes, and architectural adjustments made to restore the project to a buildable and functional state.

## 1. Build System & Dependency Restoration
- **Compose BOM Support**: Introduced `androidx-compose-bom` (version `2024.06.00`) to manage Compose library versions consistently.
- **Kotlin 2.2 Upgrade**: Updated Kotlin and Compose Compiler to `2.2.10`.
- **New Aliases**: Added missing aliases for `activity-compose`, `ui`, `ui-graphics`, `lifecycle-runtime-compose`, and `lifecycle-viewmodel-compose`.
- **Extended Icons**: Explicitly added `androidx.compose.material:material-icons-extended`.

## 2. UI & Component Fixes
- **Compiler Compliance**: Fixed multiple `@Composable` context errors.
- **SearchBar.kt**: Fixed signature and query handling.
- **PlayerControls.kt**: Renamed buttons to avoid Material library collisions.

## 3. Data & Network Layer
- **NetworkModule.kt**: Fixed API interface references and Retrofit builder chain.
- **NetworkStationRepository.kt**: Corrected mapping logic and added failover support.
- **FavoritesDataStore.kt**: Resolved JSON parsing ambiguity and added DataStore initialization.

## 4. Landscape Crash & Ripple API Fix
- **Issue**: App crashed in landscape due to `IndicationNodeFactory` mismatch.
- **Fix**: Aligned all Compose libraries via BOM and added `@OptIn` for experimental marquee APIs.

## 5. Background Playback (MediaLibraryService)
- **Evolution**: Migrated from `MediaSessionService` to `MediaLibraryService` to support Android Auto.
- **Resilience**: Configured `ExoPlayer` with `WAKE_MODE_NETWORK` and automatic audio focus.
- **Permissions**: Added `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `POST_NOTIFICATIONS`.

## 6. Improved API Resilience & Discovery
- **Dynamic Discovery**: Implemented `RadioBrowserServers.kt` to find active mirrors in real-time.
- **Automatic Failover**: Added logic to switch between Germany, Austria, and Netherlands mirrors if one fails.
- **Timeouts**: Added 5s connection and 10s read timeouts to prevent app hangs.

## 7. Romanian Specialized Experience (Legacy)
- **Premium List**: Created `CuratedRomanianStations.kt` with a manually verified list of top stations.
- **Note**: This tab was later removed in favor of a cleaner UI, but the curated list remains available in Android Auto.

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

## 11. Advanced RDS & Romanian Support (v1.4.6+)
- **Problem**: Romanian stations (Kiss, ZU, Digi) were not sending artist/song info in newer builds.
- **Fix 1 (User-Agent)**: Configured a Chrome-like User-Agent in `PlaybackService.kt`. Without this, Romanian servers block metadata.
- **Fix 2 (Aggressive RDS)**: Rewrote RDS extractor in `ExoRadioPlayer.kt`. It now scans multiple fields (Title, Artist, Subtitle, ICY raw) and prioritizes dynamic data over static station names.
- **Fix 3 (Initial Metadata)**: Removed Title/Artist from initial `MediaItem` builder to prevent "polluting" the RDS stream with static data.

## 12. Android Auto (Global & Search)
- **Global Browsing**: Added folder navigation for all countries worldwide.
- **Search Support**: Enabled voice and keyboard search directly on the car's dashboard.
- **Station Cache**: Implemented a background cache to ensure stations found via search start playing immediately.
- **Metadata Sync**: Forced metadata updates to the car's cluster and center display when a new song is detected.

## 13. UI Optimization & Fixes
- **STOP Button**: Fixed a bug where the app wouldn't close. Now calling `clearMediaItems()` and `stopSelf()` on STOP ensures the notification disappears.
- **Larger Text**: Increased font sizes (up to 26sp) for better readability on high-resolution devices.
- **Persistence**: Application now remembers the last active tab (Found/Favorite) and the last listened station.
