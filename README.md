# Octane Studio Android

This branch builds an Android integration around the actual Octane Studio Windows package. It is not the old Studio Mobile editor prototype and does not substitute a web editor for Studio.

## What it does

- Builds on the pinned Winlator Android source, which provides Wine and Box64 for running x86-64 Windows programs on ARM64 Android.
- Adds a dedicated Octane Studio screen in the Android app.
- Reads the current Studio version from Octane's bootstrapper endpoint:
  https://octane.wtf/setup/studio/2021/version.txt
- Downloads the matching package:
  https://octane.wtf/setup/studio/2021/{version}-studio.zip
- Extracts the official package safely into a Wine C: folder and checks that RobloxStudioBeta.exe is present and has a Windows PE signature.
- Creates a Wine desktop shortcut and launches the actual Windows executable through Winlator's X server.
- Accepts Octane's octane-studio: URI launch data and forwards validated ticket/task values to Studio.

The official Octane payload is fetched at install time. It is not bundled, renamed, or replaced by a clone.

## Build and source

GitHub Actions checks out this integration and the pinned Winlator Android source, applies the patch, builds an ARM64 debug APK, verifies its APK signature, and uploads the APK with its checksum and license.

- Winlator Android source: https://github.com/brunodev85/winlator-app
- Pinned source commit: f8a211ce7fd776de9a2642b45d166016876eaa86
- Octane Bootstrapper source: https://github.com/unwllnd/Octane-Bootstrapper

## Compatibility limits

This is not a native Android port of Roblox Studio. It runs the real Windows Octane Studio package under Wine/Box64. Startup, Octane authentication, GPU rendering, editing, and publishing may still fail depending on the Windows build, Wine compatibility, and the device GPU. A successful APK build proves the Android package compiled; it does not prove Studio works on every phone. Device-level testing is required.

Internet is required for the initial Studio download and for Octane login/online Roblox features.

## Licensing

The Android compatibility runtime is based on Winlator and is licensed under LGPL-2.1. The build artifact includes the upstream license. The pinned upstream source is public at the link above; this branch contains the integration patch. Octane's Windows Studio payload remains Octane's own package and is retrieved from its official endpoint.
