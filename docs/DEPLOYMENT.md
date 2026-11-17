# EKIP deployment notes

The Android study partner is designed to run inference locally on the device. This repository also contains an optional report-submission endpoint for feedback; the Android study flow does not call a cloud inference or search service.

## 1. Optional report endpoint

The Vercel route `POST /api/report` accepts bug reports and feedback when a project operator configures the server-side `POCKETQWEN_REPORT_WEBHOOK_URL` environment variable.

The receiving service must be controlled by the project operator and have appropriate access controls and retention settings. Do not use a public unauthenticated spreadsheet URL. The endpoint is not required for local study generation.

## 2. Android build and release

- CI uses a pinned `llama.cpp` revision and downloads the model with a SHA-256 check.
- Android SDK 36 and NDK 29.0.14206865 are configured for the current CI toolchain.
- The native shared library requests 16 KB maximum ELF page alignment. Verify the generated APK/AAB with Android's 16 KB page-size checks; a linker flag alone is not proof.
- The current release signing config uses the debug key for experiment installs only unless protected release-signing secrets are configured. This fallback is NOT suitable for Google Play production releases.
- Run `./gradlew testDebugUnitTest assembleDebug` and `./gradlew assembleRelease` in CI.
- Install the artifact on physical devices and test model loading, memory pressure, cancellation, theme switching, keyboard resizing, and long-response completion.

## 3. Privacy and network behaviour

The Ekip Study Partner's notes processing and model inference run locally. No internet permission is declared in the Android manifest. The optional report endpoint is a separate server route and is not used by the study flow.
