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

## Build
Requires JDK 17+ and the Android SDK (API 35). Open the project in Android Studio, or run:

```
./gradlew assembleDebug
```

The APK is created as `app/build/outputs/apk/debug/my-location.apk`. The GitHub Actions workflow also builds it and
attaches the APK as a build artifact.

## Release
Either push a version tag (e.g. `git tag v1.0 && git push origin v1.0`), or run the "Build APK"
workflow manually from the Actions tab with a `release_tag` (e.g. `v1.0`). The workflow creates a
GitHub release with `my-location.apk` attached.
