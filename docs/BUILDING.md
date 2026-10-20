# Building Wheelhouse

## Requirements

- JDK 17
- Android SDK with platform 35 (Android Studio installs this for you)

## Debug build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds use the package `io.github.pronzzz.wheelhouse.debug`, so they install next to a
release build.

## Tests

```bash
./gradlew testDebugUnitTest   # wheel maths, sorting, library grouping
./gradlew lintDebug           # includes the NewApi check that guards Android 5 support
```

## Release build

Release builds are minified with R8 (the APK is about 1.3 MB).

```bash
./gradlew assembleRelease
```

Without a signing key the release APK is signed with your local debug key, which is fine for
your own phones. To sign with a real key, create `keystore.properties` in the project root
(it is git-ignored):

```properties
storeFile=/absolute/path/to/wheelhouse-release.jks
storePassword=…
keyAlias=wheelhouse
keyPassword=…
```

Create a key once with:

```bash
keytool -genkeypair -v -keystore wheelhouse-release.jks -alias wheelhouse -keyalg RSA -keysize 4096 -validity 10000
```

Keep the keystore and its passwords safe: Android only installs updates signed with the same key.

## Continuous integration

`.github/workflows/android.yml` builds the debug APK, runs unit tests and lint on every push and
pull request, and uploads the APK as a workflow artifact.
