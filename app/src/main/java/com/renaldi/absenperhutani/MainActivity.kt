package com.renaldi.absenperhutani

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

class MainActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var retryButton: Button

    private val targetUrl = "https://dani.perhutani.co.id/login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        errorLayout = findViewById(R.id.errorLayout)
        retryButton = findViewById(R.id.retryButton)

        retryButton.setOnClickListener {
            if (isConnected()) {
                errorLayout.visibility = View.GONE
                openChrome()
            } else {
                showError()
            }
        }

        if (isConnected()) {
            openChrome()
        } else {
            showError()
        }
    }

    private fun openChrome() {
        progressBar.visibility = View.VISIBLE

        val colorScheme = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(Color.parseColor("#2E7D32"))
            .setNavigationBarColor(Color.parseColor("#2E7D32"))
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorScheme)
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()

        // Paksa pakai Chrome
        customTabsIntent.intent.setPackage("com.android.chrome")

        progressBar.visibility = View.GONE

        try {
            customTabsIntent.launchUrl(this, Uri.parse(targetUrl))
        } catch (e: Exception) {
            // Chrome tidak ada, fallback ke browser default
            val fallback = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorScheme)
                .setShowTitle(true)
                .build()
            fallback.launchUrl(this, Uri.parse(targetUrl))
        }
    }

    private fun showError() {
        progressBar.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
    }

    private fun isConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}