package com.doubletaplock.app.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.doubletaplock.app.service.DoubleTapWallpaperService
import com.doubletaplock.app.service.LockAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val WALLPAPER_FILE_NAME = "wallpaper.jpg"

private enum class StepState { Completed, Active, Locked }

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wallpaperFile = remember(context) { File(context.filesDir, WALLPAPER_FILE_NAME) }

    var step1Done by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var step2Done by remember { mutableStateOf(wallpaperFile.exists()) }
    var step3Done by remember { mutableStateOf(isOurLiveWallpaperActive(context)) }

    val lifecycle = (context as ComponentActivity).lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                step1Done = isAccessibilityServiceEnabled(context)
                step2Done = wallpaperFile.exists()
                step3Done = isOurLiveWallpaperActive(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        wallpaperFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                step2Done = wallpaperFile.exists()
            }
        }
    }

    val accessibilityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        step1Done = isAccessibilityServiceEnabled(context)
    }

    val liveWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        step3Done = isOurLiveWallpaperActive(context)
    }

    val allDone = step1Done && step2Done && step3Done
    val activeIndex = when {
        !step1Done -> 1
        !step2Done -> 2
        !step3Done -> 3
        else -> 0
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 24.dp, end = 24.dp, top = 32.dp)
        ) {
            Text("Double Tap Lock", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Bloquea tu pantalla con doble toque en el escritorio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            if (allDone) {
                CompletionBanner()
            }

            StepCard(
                number = 1,
                title = "Activar servicio de bloqueo",
                description = "Necesario para bloquear la pantalla",
                activeLabel = "Activar",
                completedLabel = "Revisar",
                state = stateFor(1, activeIndex, step1Done),
                onAction = { accessibilityLauncher.launch(buildAccessibilitySettingsIntent()) }
            )
            Spacer(Modifier.height(12.dp))
            StepCard(
                number = 2,
                title = "Elegir imagen de fondo",
                description = "Cualquier foto de tu galería",
                activeLabel = "Elegir",
                completedLabel = "Cambiar",
                state = stateFor(2, activeIndex, step2Done),
                onAction = {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            StepCard(
                number = 3,
                title = "Activar fondo de pantalla",
                description = "Establecerlo como live wallpaper",
                activeLabel = "Activar",
                completedLabel = "Reactivar",
                state = stateFor(3, activeIndex, step3Done),
                onAction = { liveWallpaperLauncher.launch(buildSetLiveWallpaperIntent(context)) }
            )
        }
    }
}

private fun stateFor(number: Int, activeIndex: Int, done: Boolean): StepState =
    when {
        done -> StepState.Completed
        number == activeIndex -> StepState.Active
        else -> StepState.Locked
    }

@Composable
private fun CompletionBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Listo. Doble toque para bloquear.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    description: String,
    activeLabel: String,
    completedLabel: String,
    state: StepState,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepIndicator(number = number, state = state)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when (state) {
                StepState.Completed -> FilledTonalButton(onClick = onAction) {
                    Text(completedLabel)
                }
                StepState.Active -> Button(onClick = onAction) {
                    Text(activeLabel)
                }
                StepState.Locked -> Button(onClick = {}, enabled = false) {
                    Text(activeLabel)
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(number: Int, state: StepState) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val base = Modifier
        .size(40.dp)
        .clip(CircleShape)
    Box(
        modifier = when (state) {
            StepState.Completed, StepState.Active -> base.background(primary)
            StepState.Locked -> base.border(2.dp, outline, CircleShape)
        },
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            StepState.Completed -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = onPrimary
            )
            StepState.Active -> Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = onPrimary
            )
            StepState.Locked -> Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = outline
            )
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, LockAccessibilityService::class.java)
        .flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

private fun isOurLiveWallpaperActive(context: Context): Boolean {
    val wm = WallpaperManager.getInstance(context) ?: return false
    val info = wm.wallpaperInfo ?: return false
    val ours = ComponentName(context, DoubleTapWallpaperService::class.java)
    return info.component == ours
}

private fun buildAccessibilitySettingsIntent(): Intent =
    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

private fun buildSetLiveWallpaperIntent(context: Context): Intent =
    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
        putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, DoubleTapWallpaperService::class.java)
        )
    }
