package com.satwik.aimemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.satwik.aimemory.navigation.AiMemoryNavigation
import com.satwik.aimemory.network.HealthCheckManager
import com.satwik.aimemory.ui.theme.AiMemoryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start backend health monitoring
        HealthCheckManager.start()

        setContent {
            AiMemoryTheme {
                AiMemoryNavigation()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Pause health checks when app is in background
        HealthCheckManager.stop()
    }

    override fun onStart() {
        super.onStart()
        // Resume health checks when app comes to foreground
        HealthCheckManager.start()
    }
}
