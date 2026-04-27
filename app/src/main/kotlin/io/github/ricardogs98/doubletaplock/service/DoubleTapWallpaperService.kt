package io.github.ricardogs98.doubletaplock.service

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.FileObserver
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import io.github.ricardogs98.doubletaplock.service.wallpaper.DoubleTapDetector
import io.github.ricardogs98.doubletaplock.service.wallpaper.OffsetAnimator
import io.github.ricardogs98.doubletaplock.service.wallpaper.loadCoverBitmap
import io.github.ricardogs98.doubletaplock.service.wallpaper.renderCoverCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val DOUBLE_TAP_THRESHOLD_MS = 300L
private const val WALLPAPER_FILE_NAME = "wallpaper.jpg"
private const val INITIAL_X_OFFSET = 0.5f

// Tap forwarding is launcher-dependent: Pixel Launcher forwards taps on empty
// wallpaper areas via WallpaperManager.COMMAND_TAP, so we listen there instead
// of enabling raw touch events (which would forward every motion event and
// waste battery). Other launchers may not forward taps at all.
class DoubleTapWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = WallpaperEngine()

    private inner class WallpaperEngine : Engine() {

        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private val wallpaperFile: File = File(filesDir, WALLPAPER_FILE_NAME)

        private val tapDetector = DoubleTapDetector(thresholdMs = DOUBLE_TAP_THRESHOLD_MS)
        // The animator drives draw() from Choreographer at refresh rate while
        // converging toward the latest target offset. See OffsetAnimator for why.
        private val offsetAnimator = OffsetAnimator(initial = INITIAL_X_OFFSET) { draw() }

        private var bitmap: Bitmap? = null
        // Cached so onComputeColors() — invoked by the system on a binder
        // thread — can return without touching the bitmap (which only the
        // Main-dispatcher coroutine is allowed to mutate/recycle).
        @Volatile
        private var cachedColors: WallpaperColors? = null
        private var loadedTimestamp: Long = 0L
        private var surfaceWidth: Int = 0
        private var surfaceHeight: Int = 0
        private var visible: Boolean = false
        // Set the moment a double tap is detected and stays true until the
        // wallpaper becomes visible again (i.e. the user unlocked). While
        // true, draw() is suppressed so the surface stays solid black —
        // otherwise an in-flight animator frame can repaint the image right
        // as the system lock animation begins, producing the visible flash.
        private var locking: Boolean = false

        private var loadJob: Job? = null
        private var fileObserver: FileObserver? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Watch the wallpaper file so a fresh pick reloads live without DataStore.
            fileObserver = object : FileObserver(
                wallpaperFile,
                CLOSE_WRITE or MOVED_TO or DELETE_SELF
            ) {
                override fun onEvent(event: Int, path: String?) {
                    scope.launch { if (visible) reloadAndDraw() }
                }
            }.also { it.startWatching() }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                // The user is back on the launcher — clear the lock latch and
                // resume normal rendering.
                locking = false
                if (needsReload()) reloadAndDraw() else draw()
            } else {
                // No drawing while hidden — and no animation either.
                offsetAnimator.stop()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            if (needsReload()) reloadAndDraw() else draw()
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            // While hidden, just remember the position so we re-appear at the
            // right place — no animation through pages the user can't see.
            if (visible) {
                offsetAnimator.setTarget(xOffset)
            } else {
                offsetAnimator.snapTo(xOffset)
            }
        }

        override fun onCommand(
            action: String?,
            x: Int,
            y: Int,
            z: Int,
            extras: Bundle?,
            resultRequested: Boolean
        ): Bundle? {
            // Stay synchronous and fast: timestamp comparison + lockNow only.
            if (action == WallpaperManager.COMMAND_TAP &&
                tapDetector.onTap(SystemClock.uptimeMillis())
            ) {
                triggerLock()
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onDestroy() {
            offsetAnimator.stop()
            fileObserver?.stopWatching()
            fileObserver = null
            scope.cancel()
            loadJob?.cancel()
            recycleBitmap()
            super.onDestroy()
        }

        private fun needsReload(): Boolean {
            val ts = if (wallpaperFile.exists()) wallpaperFile.lastModified() else 0L
            return ts != loadedTimestamp
        }

        private fun reloadAndDraw() {
            loadJob?.cancel()
            loadJob = scope.launch {
                val ts = if (wallpaperFile.exists()) wallpaperFile.lastModified() else 0L
                val newBitmap = loadCoverBitmap(wallpaperFile, surfaceWidth, surfaceHeight)
                // Sample colors off the main thread — fromBitmap() scans pixels.
                val newColors = newBitmap?.let {
                    withContext(Dispatchers.IO) { WallpaperColors.fromBitmap(it) }
                }
                recycleBitmap()
                bitmap = newBitmap
                loadedTimestamp = ts
                cachedColors = newColors
                // Tells the system to re-query onComputeColors() — Material You
                // reads from the user's photo instead of falling back to the
                // wallpaper.xml thumbnail.
                notifyColorsChanged()
                draw()
            }
        }

        override fun onComputeColors(): WallpaperColors? = cachedColors

        private fun recycleBitmap() {
            val previous = bitmap
            bitmap = null
            previous?.recycle()
        }

        private fun draw() {
            if (!visible || locking) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                // Hardware canvas (API 26+, minSdk is 31) renders the blit on
                // the GPU; software lockCanvas was the source of the choppy feel.
                canvas = holder.lockHardwareCanvas() ?: return
                renderCoverCrop(
                    canvas = canvas,
                    bitmap = bitmap,
                    surfaceW = surfaceWidth,
                    surfaceH = surfaceHeight,
                    xOffset = offsetAnimator.value,
                )
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        // Order matters: latch first so any racing animator frame is dropped,
        // then stop the animator, then paint solid black, *then* invoke the
        // system lock. The frame queued by unlockCanvasAndPost reaches the
        // surface before the lock animation begins, so the transition fades
        // from black to lockscreen instead of from launcher to lockscreen.
        private fun triggerLock() {
            locking = true
            offsetAnimator.stop()
            fillBlack()
            LockAccessibilityService.lockNow()
        }

        private fun fillBlack() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockHardwareCanvas() ?: return
                canvas.drawColor(Color.BLACK)
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
