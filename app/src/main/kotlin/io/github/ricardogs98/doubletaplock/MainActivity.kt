package io.github.ricardogs98.doubletaplock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.ricardogs98.doubletaplock.ui.MainScreen
import io.github.ricardogs98.doubletaplock.ui.theme.DoubleTapLockTheme

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
