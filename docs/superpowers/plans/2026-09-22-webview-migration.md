# WebView Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate `Model-Aplikasi-Absen` from Chrome Custom Tabs to an embedded full-screen WebView with Cloudflare challenge support, persistent cookies, and a simplified senior-friendly UI.

**Architecture:** Single-activity native Android application (`MainActivity`) embedding a full-screen `WebView` in `activity_main.xml`. Uses a top horizontal `ProgressBar` for non-blocking page load/challenge display, configures `WebSettings` (JS + DOM storage + default User-Agent) and persistent `CookieManager` to handle Cloudflare WAF, and handles back-press navigation via `onBackPressedDispatcher`.

**Tech Stack:** Kotlin 2.0.21, Android SDK (compileSdk 36, minSdk 24), AndroidX AppCompat, Android `android.webkit.WebView`, Gradle 9.2.1.

## Global Constraints
- Target URL: `https://dani.perhutani.co.id/login`
- Default User-Agent MUST NOT be modified or overridden.
- Zero new library dependencies (WebView is part of the Android platform SDK).
- `androidx.browser:browser` MUST be completely removed from project dependencies.
- No GPS/camera permissions or code additions (YAGNI).

---

### Task 1: Remove `androidx.browser` Dependency

**Files:**
- Modify: `gradle/libs.versions.toml:13-26`
- Modify: `app/build.gradle.kts:52-65`

**Interfaces:**
- Consumes: None
- Produces: Clean dependency tree without unused browser CustomTabs library

- [ ] **Step 1: Remove `androidx-browser` from `gradle/libs.versions.toml`**

Remove the `androidx-browser` entry in the `[libraries]` table:
```toml
# Delete:
# androidx-browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
# And delete version pin:
# browser = "1.7.0"
```

- [ ] **Step 2: Remove `libs.androidx.browser` from `app/build.gradle.kts`**

Remove the line `implementation(libs.androidx.browser)` from the `dependencies` block.

- [ ] **Step 3: Verify Gradle builds without error**

Run: `./gradlew help`
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Update Layout for Full-Screen WebView & Senior-Friendly UX

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

**Interfaces:**
- Consumes: Views used by `MainActivity` (`R.id.webView`, `R.id.progressBar`, `R.id.errorLayout`, `R.id.retryButton`)
- Produces: Layout with full-screen `WebView`, top horizontal `ProgressBar`, and large retry button

- [ ] **Step 1: Replace layout with WebView, horizontal ProgressBar, and senior-friendly error layout**

Update `app/src/main/res/layout/activity_main.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FFFFFF">

    <!-- Fullscreen WebView -->
    <WebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone" />

    <!-- Top Horizontal ProgressBar for non-intrusive loading / Cloudflare challenges -->
    <ProgressBar
        android:id="@+id/progressBar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="4dp"
        android:layout_alignParentTop="true"
        android:progressTint="#2E7D32"
        android:visibility="visible" />

    <!-- Error / Offline Layout with large senior-friendly touch targets -->
    <LinearLayout
        android:id="@+id/errorLayout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:visibility="gone"
        android:padding="32dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="⚠️ Tidak ada koneksi internet"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#333333"
            android:gravity="center" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Periksa koneksi WiFi atau kuota data Anda, lalu tekan tombol di bawah."
            android:textSize="15sp"
            android:textColor="#666666"
            android:gravity="center" />

        <Button
            android:id="@+id/retryButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:minHeight="56dp"
            android:layout_marginTop="24dp"
            android:layout_marginStart="32dp"
            android:layout_marginEnd="32dp"
            android:text="Coba Lagi"
            android:textSize="18sp"
            android:textStyle="bold"
            android:backgroundTint="#2E7D32"
            android:textColor="#FFFFFF" />

    </LinearLayout>

</RelativeLayout>
```

- [ ] **Step 2: Verify XML layout valid**

Run: `./gradlew processDebugResources`
Expected: `BUILD SUCCESSFUL`

---

### Task 3: Refactor `MainActivity.kt` to Native WebView with Cloudflare Support

**Files:**
- Modify: `app/src/main/java/com/renaldi/absenperhutani/MainActivity.kt`

**Interfaces:**
- Consumes: `R.id.webView`, `R.id.progressBar`, `R.id.errorLayout`, `R.id.retryButton`
- Produces: Functional WebView with JS/DOM storage enabled, persistent cookies, top progress bar update, error filtering, and back navigation

- [ ] **Step 1: Implement WebView logic in `MainActivity.kt`**

Update `app/src/main/java/com/renaldi/absenperhutani/MainActivity.kt`:
```kotlin
package com.renaldi.absenperhutani

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var retryButton: Button

    private val targetUrl = "https://dani.perhutani.co.id/login"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorLayout = findViewById(R.id.errorLayout)
        retryButton = findViewById(R.id.retryButton)

        setupWebView()
        setupBackNavigation()

        retryButton.setOnClickListener {
            if (isConnected()) {
                showContent()
                webView.reload()
            } else {
                showError()
            }
        }

        if (isConnected()) {
            showContent()
            webView.loadUrl(targetUrl)
        } else {
            showError()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Cookie persistence for session and Cloudflare clearance
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                showContent()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && !isConnected()) {
                    showError()
                }
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    private fun showContent() {
        errorLayout.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }

    private fun showError() {
        progressBar.visibility = View.GONE
        webView.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
    }

    private fun isConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

- [ ] **Step 2: Build and compile debug APK**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

---

### Task 4: Lint & Verification

**Files:**
- Verification only

- [ ] **Step 1: Run Android Lint on debug variant**

Run: `./gradlew lintDebug`
Expected: `BUILD SUCCESSFUL` without fatal lint errors

- [ ] **Step 2: Verify generated APK exists**

Check: `app/build/outputs/apk/debug/app-debug.apk` exists
