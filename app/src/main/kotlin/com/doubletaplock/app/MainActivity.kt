package com.doubletaplock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.doubletaplock.app.ui.MainScreen
import com.doubletaplock.app.ui.theme.DoubleTapLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoubleTapLockTheme {
                MainScreen()
            }
        }
    }
}
