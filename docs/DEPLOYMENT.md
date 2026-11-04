# PocketQwen deployment checklist

This branch contains the Android app, a static project/privacy site, and optional Vercel API routes. A checked-in route is not a live service: deploy and test it before enabling network features in the Android app.

## 1. Deploy the static site with GitHub Pages

1. Open the repository's **Settings → Pages**.
2. Set **Build and deployment → Source** to **GitHub Actions**.
3. Add a Pages workflow that publishes only `index.html` and `privacy.html` (or configure a static-site provider to publish the repository root).
4. Confirm the generated site URL and open both the home page and `/privacy.html`.
5. Use the final privacy URL in the Play Store listing and in-app privacy link.

The static privacy page is a launch draft. Replace the support-contact placeholder with a monitored contact address and verify the final data practices before launch.

## 2. Deploy the API routes to Vercel

Import `jarumba-code/qwen-pocket-` into Vercel and use the repository root as the project root. The routes are:

- `POST /api/search`
- `POST /api/report`

Configure these environment variables in Vercel:

- `TAVILY_API_KEY`: server-side key for the optional search proxy. Never put this key in Android source or the APK.
- `REPORT_WEBHOOK_URL`: HTTPS endpoint controlled by the project operator that accepts report JSON. Do not use a public unauthenticated spreadsheet URL.

Redeploy after adding or changing environment variables.

### Search request

```json
{ "query": "latest Android 16 KB page size requirements" }
```

The route returns up to four title, HTTPS URL and snippet records. The Android client must present Web Search as an explicit user action, clearly label returned sources, and send only the query. It must not silently send chat history.

### Report request

```json
{
  "category": "bug",
  "details": "The app closes when I open settings.",
  "excerpt": "Optional selected response excerpt",
  "appVersion": "1.1.0"
}
```

Allowed categories: `bug`, `incorrect_answer`, `safety`, `privacy`, `other`. The route validates method, payload size and fields, then forwards the report to the configured HTTPS webhook. The receiving service's access controls and retention policy must be configured separately.

## 3. Android release requirements

- CI uses the pinned `llama.cpp` revision and downloads the model with a SHA-256 check.
- Android SDK 36 and NDK 29.0.14206865 are configured for the current CI toolchain.
- The native shared library requests 16 KB maximum ELF page alignment. Verify the generated APK/AAB with Android's 16 KB page-size checks; a linker flag alone is not proof.
- The current release signing config is the debug key for experiment installs only. Replace it with a protected upload key before Play Store release. Never commit a keystore or passwords.
- Run `./gradlew testDebugUnitTest assembleDebug` and `./gradlew assembleRelease` in CI. Install the artifact on physical devices and test model loading, memory pressure, cancellation, theme switching, edit/resend, copy, and long-response truncation.

## 4. Privacy and rollout

Ordinary model inference is local. Search and report submission are separate network actions. Keep both unavailable until the corresponding backend is deployed, tested, and reflected accurately in the app privacy disclosure and Play Data safety form. Do not claim that the app is fully offline when an online feature is being used.
