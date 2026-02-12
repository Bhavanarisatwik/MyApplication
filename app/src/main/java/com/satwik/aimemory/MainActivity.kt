package com.satwik.aimemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.satwik.aimemory.navigation.AiMemoryNavigation
import com.satwik.aimemory.ui.theme.AiMemoryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiMemoryTheme {
                AiMemoryNavigation()
            }
        }
    }
}
