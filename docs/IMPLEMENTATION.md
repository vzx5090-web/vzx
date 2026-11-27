# Implementation notes

## Facts recovered from the uploaded Octane launcher

The uploaded OctanePlayerLauncher.exe hash matched Octane Bootstrapper 1.3.1. Its source identifies the Studio package as:

- Package directory: Studio/2021
- Executable: RobloxStudioBeta.exe
- Version endpoint: https://octane.wtf/setup/studio/2021/version.txt
- ZIP name: {version}-studio.zip

The Windows launcher also passes the Octane negotiate URL at https://octane.wtf/Login/Negotiate.ashx and supports octane-studio: deep links with ticket and task parameters.

## Android implementation

The dedicated Android page creates or selects a Wine container, downloads the live ZIP from the official Octane endpoint, extracts it with path-traversal checks, validates the PE executable signature, creates a Wine desktop shortcut, and starts that shortcut in Winlator's X server.

The URI handler restricts ticket characters and numeric asset/place/universe identifiers before passing command-line values to Studio.

## Not claimed

No claim is made that Roblox Studio is fully compatible with Wine or every Android GPU. CI verifies that the Android app compiles and that the APK signature is valid. Device-level launch, authentication, editing, and publishing tests are still needed.
