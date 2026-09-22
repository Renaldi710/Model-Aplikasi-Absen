# Repository Guidelines

## Project Overview
Absen Perhutani (`Model-Aplikasi-Absen`) is an Android client wrapper for Perhutani's DANI web-based attendance system (`https://dani.perhutani.co.id/login`). It provides a native app experience using an embedded full-screen WebView (no browser UI), complete with network connectivity verification, Cloudflare challenge support via persistent cookies, offline fallback handling, and retry capability.

## Architecture & Data Flow
- **Pattern**: Single-Activity Native Wrapper with embedded `android.webkit.WebView` (no external browser, no `androidx.browser` dependency).
- **Module Structure**: Single-module Android project (`:app`).
- **Data Flow**:
  1. `MainActivity.onCreate()` initializes views (`WebView`, `ProgressBar`, error container, retry button) from `activity_main.xml`, then calls `setupWebView()` (JS + DOM storage + `LOAD_DEFAULT` cache + persistent `CookieManager`) and `setupBackNavigation()` (`onBackPressedDispatcher` → `webView.goBack()`).
  2. Network status is evaluated via `ConnectivityManager.getNetworkCapabilities()`:
     - **Connected**: Shows content view and calls `webView.loadUrl("https://dani.perhutani.co.id/login")`. Top horizontal `ProgressBar` tracks load via `WebChromeClient.onProgressChanged`. `WebViewClient` keeps navigation in-app (`shouldOverrideUrlLoading = false`), flushes cookies on `onPageFinished`/`onPause`, and shows error view only for fatal main-frame errors (`ERROR_HOST_LOOKUP`, `ERROR_CONNECT`, `ERROR_TIMEOUT`, `ERROR_UNKNOWN`) so Cloudflare JS challenges (HTTP 403/503 with body) still render normally.
     - **Disconnected**: Hides progress spinner and WebView, displays the error view, and binds the retry button to re-check network (`loadUrl` on first load, `reload()` afterwards).
- **UI Architecture**: Active runtime interface uses classic Android View/XML layouts (`activity_main.xml`), while Jetpack Compose Material 3 theme scaffolding (`ui/theme/`) is configured for future Compose integration.

## Key Directories
- `app/src/main/java/com/renaldi/absenperhutani/`: Core Kotlin source code.
  - `MainActivity.kt`: Main launcher activity, WebView setup, connectivity checks, and back navigation.
  - `ui/theme/`: Jetpack Compose Material 3 theme files (`Theme.kt`, `Color.kt`, `Type.kt`).
- `app/src/main/res/`: Android application resources.
  - `layout/`: View layouts (`activity_main.xml`).
  - `values/`: Strings, color definitions, and XML themes (`strings.xml`, `colors.xml`, `themes.xml`).
  - `drawable/` & `mipmap-*/`: App icons and vector assets.
- `app/src/test/java/com/renaldi/absenperhutani/`: Host JVM unit tests (`*Test.kt`, `*UnitTest.kt`).
- `app/src/androidTest/java/com/renaldi/absenperhutani/`: On-device instrumented tests (`*InstrumentedTest.kt`).
- `gradle/`: Build configuration.
  - `libs.versions.toml`: Centralized Gradle Version Catalog.
  - `wrapper/`: Gradle wrapper binaries and properties.

## Development Commands
All builds and tasks use the Gradle wrapper (`./gradlew`):

```bash
# Build APKs
./gradlew assembleDebug       # Build debug APK (app/build/outputs/apk/debug/)
./gradlew assembleRelease     # Build unsigned/signed release APK

# Run Tests
./gradlew test                # Run host JVM unit tests
./gradlew testDebugUnitTest   # Run debug variant unit tests
./gradlew connectedAndroidTest # Run instrumented tests on connected device/emulator

# Quality & Verification
./gradlew lint                # Run Android Lint analysis
./gradlew lintDebug           # Run lint on debug variant
./gradlew clean               # Clean build cache and build artifacts
```

## Code Conventions & Common Patterns
- **Language & Style**: Kotlin 2.0.21 using official Kotlin style conventions (`kotlin.code.style=official` in `gradle.properties`).
- **Naming Conventions**:
  - Activities: PascalCase ending in `Activity` (e.g., `MainActivity`).
  - Layouts: snake_case prefixed by component type (e.g., `activity_main.xml`).
  - Resources: snake_case (e.g., `ic_launcher_foreground.xml`, `@color/perhutani_green`).
- **Network Verification Pattern**:
  ```kotlin
  val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val network = connectivityManager.activeNetwork ?: return false
  val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
  return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  ```
- **WebView Setup & Error Filter Pattern**:
  ```kotlin
  settings.javaScriptEnabled = true
  settings.domStorageEnabled = true
  settings.cacheMode = WebSettings.LOAD_DEFAULT
  CookieManager.getInstance().apply {
      setAcceptCookie(true)
      setAcceptThirdPartyCookies(webView, true)
  }
  // Only fatal main-frame errors show the error view;
  // Cloudflare challenges (HTTP 403/503 with body) render normally.
  if (request?.isForMainFrame == true && error?.errorCode in FATAL_ERRORS) showError()
  ```
- **State Management**: Local activity view-state toggles between content (`webView.visibility`), loading (`progressBar.visibility` via `WebChromeClient.onProgressChanged`), and offline error state (`errorLayout.visibility`).

## Important Files
- `app/src/main/java/com/renaldi/absenperhutani/MainActivity.kt`: Primary application entry point, lifecycle controller, and network/browser dispatcher.
- `app/src/main/AndroidManifest.xml`: Declares permissions (`INTERNET`, `ACCESS_NETWORK_STATE`), screen orientation (`portrait`), and main launcher activity.
- `app/build.gradle.kts`: Module build configuration, SDK levels, dependencies, and Compose build features.
- `settings.gradle.kts`: Repository management (`google`, `mavenCentral`) and module inclusion.
- `gradle/libs.versions.toml`: Central version catalog for all dependencies (AGP, Kotlin, AndroidX, Compose BOM, testing).
- `README.md`: User-facing documentation, release notes, and keystore signing configuration instructions.

## Runtime/Tooling Preferences
- **Build System**: Gradle 9.2.1 (via `./gradlew`).
- **Daemon JVM**: Java 21 (managed via Foojay Toolchain Resolver in `gradle/gradle-daemon-jvm.properties`).
- **Bytecode Target**: Java 11 compatibility (`sourceCompatibility` and `targetCompatibility = JavaVersion.VERSION_11`).
- **Android SDK Targets**:
  - `compileSdk`: 36 (Android 16 / release 36.1)
  - `minSdk`: 24 (Android 7.0 Nougat)
  - `targetSdk`: 36
- **Android Gradle Plugin (AGP)**: 9.0.1.
- **Kotlin & Compose Compiler**: Kotlin 2.0.21 with Jetpack Compose Compiler plugin.
- **Environment & Secrets**: Release signing uses `keystore.properties` (never committed to git) referenced in `README.md`.

## Testing & QA
- **Frameworks**:
  - Host Unit Tests: JUnit 4 (`junit:junit:4.13.2`).
  - Instrumented Tests: AndroidX Test JUnit (`androidx.test.ext:junit:1.3.0`), Espresso Core (`androidx.test.espresso:espresso-core:3.7.0`), Compose UI Test (`androidx.compose.ui:ui-test-junit4`).
  - Test Runner: `androidx.test.runner.AndroidJUnitRunner`.
- **Conventions**:
  - Host tests live in `app/src/test/java/` and use standard JUnit 4 assertions (`assertEquals`).
  - Instrumented tests live in `app/src/androidTest/java/` annotated with `@RunWith(AndroidJUnit4::class)`.
- **Coverage & Mocking**:
  - No external mocking framework (e.g., MockK, Mockito) or code coverage plugin (e.g., JaCoCo, Kover) is currently configured.
  - Test verification relies on Gradle's test tasks and Android Lint checks.
