package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.api.NetworkClient
import com.example.util.MangaNotificationManager

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.data.SettingsRepository

import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        init {
            try {
                android.system.Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
            } catch (_: Exception) {}
        }
    }

    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            android.system.Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
        } catch (_: Exception) {}

        super.onCreate(savedInstanceState)

        // Pre-create WebView and Chromium cache directories to prevent simple_file_enumerator opendir errors
        try {
            val webViewCacheDirs = listOf(
                File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js"),
                File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm"),
                File(cacheDir, "WebView/Default/HTTP Cache"),
                File(cacheDir, "WebView/Default/Code Cache/js"),
                File(cacheDir, "WebView/Default/Code Cache/wasm")
            )
            for (dir in webViewCacheDirs) {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
        } catch (e: Exception) {
            // Ignore directory creation errors
        }

        // Initialize Notification Channels
        MangaNotificationManager.initChannels(applicationContext)

        // Check & request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val imageLoader = NetworkClient.getImageLoader(applicationContext)
        Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            val settingsRepo = remember { SettingsRepository(this) }
            val themeMode by settingsRepo.themeMode.collectAsState(initial = "DARK")

            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
