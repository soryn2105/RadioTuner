# Landscape Crash Fix Documentation

## Issue
The application crashed immediately upon switching to landscape mode or when starting in landscape mode.

### Error Details
```
java.lang.IllegalArgumentException: clickable only supports IndicationNodeFactory instances provided to LocalIndication, but Indication was provided instead.
```

### Root Cause
The crash was caused by a mismatch in **Jetpack Compose library versions**. Specifically:
1.  The project was pulling in `androidx.compose.foundation` version **1.7.0+** (either explicitly or as a transitive dependency).
2.  The `androidx.compose.material3` or `material` libraries were at an older version (managed by a previous BOM or explicit versions like `1.4.0`/`1.6.2`).
3.  In Compose **1.7.0**, the Ripple API was refactored. `Modifier.clickable` now expects an `IndicationNodeFactory` for the ripple effect. If it receives a legacy `Indication` (like the one provided by Material 3 1.2.x), it throws the `IllegalArgumentException`.

Landscape mode triggered this because the `StationListPanel` (which contains many `clickable` rows) is rendered *immediately* in landscape, whereas in portrait it is hidden inside a drawer and only rendered when the menu is opened.

## Solution
The fix involved **version alignment** in the `libs.versions.toml` file:

1.  **Alignment via BOM**: All explicit versions for Compose libraries (`foundation-layout`, `material`, `material3`, `ui`, `ui-graphics`, `material-icons-extended`) were removed.
2.  **BOM Enforcement**: The project now relies strictly on the `androidx.compose:compose-bom:2024.06.00` for all Compose-related dependencies.
3.  **Result**: This ensures all Compose libraries are at version **1.6.8**, where the Ripple API is consistent and does not require `IndicationNodeFactory`.

## Applied Changes

### `libs.versions.toml`
- Removed `foundationLayout = "1.11.4"` (invalid/conflicting version).
- Removed `material3 = "1.4.0"` (invalid/conflicting version).
- Removed `composeMaterialCore = "1.6.2"`.
- Removed explicit version `1.7.0` from `androidx-compose-material-icons-extended`.
- Removed version references from all `androidx-compose-*` library definitions to let the BOM manage them.

### `RdsTicker.kt`
- Added `@OptIn(ExperimentalFoundationApi::class)` to support `Modifier.basicMarquee()`, which is experimental in Compose 1.6.8.

---
*Verified build: Successful (app:assembleDebug)*
