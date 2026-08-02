# HLS and Cleartext Traffic Fix

## Issues Resolved
1. **Missing HLS Dependency**: Many radio stations use HLS (HTTP Live Streaming) formats like `.m3u8`. The app was crashing with `java.lang.ClassNotFoundException: androidx.media3.exoplayer.hls.HlsMediaSource$Factory` when trying to play these streams.
2. **Cleartext Traffic Blocked**: Android blocks non-HTTPS traffic by default. Some radio stations only provide `http://` streams, which caused `androidx.media3.datasource.HttpDataSource$CleartextNotPermittedException`.

## Changes Made
### 1. Dependency Update
- Added `media3-exoplayer-hls` to `gradle/libs.versions.toml`.
- Updated `app/build.gradle.kts` to include `libs.media3.exoplayer.hls`.
- Cleaned up hardcoded `1.4.1` versions in `app/build.gradle.kts` to use the version catalog (`1.10.1`).

### 2. Manifest Update
- Added `android:usesCleartextTraffic="true"` to the `<application>` tag in `app/src/main/AndroidManifest.xml`. This allows the app to play `http://` radio streams.

## Verification
- Ran `app:assembleDebug` - Build successful.
- The app should now be able to play both standard and HLS streams, even over non-encrypted HTTP.
