# MyLocation
An Android app to share my current location

## Features
- Bilingual UI: every label is shown in English and Hungarian, e.g. "Latitude (szélesség)".
- Requests location permission and asks to turn on location (GPS) when the app starts.
- Shows the current latitude/longitude and the precision:
  - **Good (jó)**: ±10 m or better
  - **Average (közepes)**: ±10–30 m
  - **Poor (gyenge)**: worse than ±30 m
- **Share location** button sends the position as text (e.g. to Messages), including a Google Maps link:

  ```
  My location: 47.497913, 19.040236 (accuracy ±5 m)
  https://www.google.com/maps/search/?api=1&query=47.497913,19.040236
  ```

## Quick mode ("Where are you?")
Send a family member the link https://mylocation.gasparnagy.com/where (e.g. in Viber). Tapping it
opens a small My Location dialog that waits for a good fix (at most 30 seconds, then it takes the
best one so far), copies the location text to the clipboard and closes. The user is back in the
chat and only has to paste and send.

The link opens the app automatically when `.well-known/assetlinks.json` is published on the website
(see below). Otherwise it can be enabled once per phone: Settings → Apps → My Location →
Open by default → add `mylocation.gasparnagy.com`. If the app is not installed, the link shows a
web page with instructions and a download link.

## Build
Requires JDK 17+ and the Android SDK (API 35). Open the project in Android Studio, or run:

```
./gradlew assembleDebug
```

Debug builds are created as `app/build/outputs/apk/debug/my-location.apk`. The GitHub Actions workflow also builds it and
attaches the APK as a build artifact.

## Signing
CI builds the release APK (`app/build/outputs/apk/release/my-location.apk`) signed with a fixed release
key, so new versions install as updates over older ones. The key comes from two repository secrets:

- `SIGNING_KEYSTORE_BASE64`: the PKCS12 keystore (key alias `mylocation`), base64-encoded
- `SIGNING_KEYSTORE_PASSWORD`: the keystore (and key) password

Without these secrets, normal CI builds fall back to a temporary debug key and releases fail.
Keep a backup of the keystore: if it is lost, users have to uninstall the app to install a new version.

Release key SHA-256 fingerprint:
`CC:62:9D:2D:A1:2D:07:C9:2C:7D:F2:5D:CE:55:4D:CC:E3:09:1D:70:08:85:D6:F1:88:46:06:D0:78:49:F1:19`

## Website
The `website/` folder holds the files for https://mylocation.gasparnagy.com. Upload its contents to
the web root of that domain:

- `index.html`: download page, always links to the APK of the latest GitHub release
- `where/index.html`: shown when the "Where are you?" link opens in a browser instead of the app
- `.well-known/assetlinks.json`: proves that the app may open `mylocation.gasparnagy.com` links.
  It must be reachable at `https://mylocation.gasparnagy.com/.well-known/assetlinks.json` over
  HTTPS, without redirects, with `Content-Type: application/json`. Some upload tools skip folders
  starting with a dot, so check that it was uploaded. If the release key changes, update the
  fingerprint in this file.

## Release
Either push a version tag (e.g. `git tag v1.1 && git push origin v1.1`), or run the "Build APK"
workflow manually from the Actions tab with a `release_tag` (e.g. `v1.1`). The workflow creates a
GitHub release with `my-location.apk` attached.
