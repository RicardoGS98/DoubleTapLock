package com.doubletaplock.app.service

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.FileObserver
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
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

// Tap forwarding is launcher-dependent: Pixel Launcher forwards taps on empty
// wallpaper areas via WallpaperManager.COMMAND_TAP, so we listen there instead
// of enabling raw touch events (which would forward every motion event and
// waste battery). Other launchers may not forward taps at all.
class DoubleTapWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = WallpaperEngine()

    private inner class WallpaperEngine : Engine() {

        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private val wallpaperFile: File = File(filesDir, WALLPAPER_FILE_NAME)

        private var bitmap: Bitmap? = null
        private var loadedTimestamp: Long = 0L
        private var surfaceWidth: Int = 0
        private var surfaceHeight: Int = 0
        private var visible: Boolean = false

        @Volatile private var lastTapAt: Long = 0L

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
                if (needsReload()) reloadAndDraw() else draw()
            }
            // When not visible: do nothing — no draw, no decode.
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

        override fun onCommand(
            action: String?,
            x: Int,
            y: Int,
            z: Int,
            extras: Bundle?,
            resultRequested: Boolean
        ): Bundle? {
            // Stay synchronous and fast: timestamp comparison + lockNow only.
            if (action == WallpaperManager.COMMAND_TAP) {
                val now = SystemClock.uptimeMillis()
                val previous = lastTapAt
                if (previous != 0L && now - previous <= DOUBLE_TAP_THRESHOLD_MS) {
                    lastTapAt = 0L
                    LockAccessibilityService.lockNow()
                } else {
                    lastTapAt = now
                }
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onDestroy() {
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
                val newBitmap = loadBitmap(wallpaperFile, surfaceWidth, surfaceHeight)
                recycleBitmap()
                bitmap = newBitmap
                loadedTimestamp = ts
                draw()
            }
        }

        private suspend fun loadBitmap(file: File, width: Int, height: Int): Bitmap? {
            if (!file.exists() || width <= 0 || height <= 0) return null
            return withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                val options = BitmapFactory.Options().apply {
                    inSampleSize = computeSampleSize(
                        bounds.outWidth, bounds.outHeight, width, height
                    )
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                runCatching {
                    BitmapFactory.decodeFile(file.absolutePath, options)
                }.getOrNull()
            }
        }

        private fun computeSampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
            var sample = 1
            var w = srcW
            var h = srcH
            while (w / 2 >= dstW && h / 2 >= dstH) {
                w /= 2
                h /= 2
                sample *= 2
            }
            return sample
        }

        private fun recycleBitmap() {
            val previous = bitmap
            bitmap = null
            previous?.recycle()
        }

        private fun draw() {
            if (!visible) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                canvas.drawColor(Color.BLACK)
                val current = bitmap
                if (current != null) {
                    val src = Rect(0, 0, current.width, current.height)
                    val dst = computeCenterCropDst(
                        current.width, current.height, surfaceWidth, surfaceHeight
                    )
                    canvas.drawBitmap(current, src, dst, BITMAP_PAINT)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun computeCenterCropDst(bw: Int, bh: Int, sw: Int, sh: Int): Rect {
            if (bw <= 0 || bh <= 0 || sw <= 0 || sh <= 0) return Rect(0, 0, sw, sh)
            val scale = maxOf(sw.toFloat() / bw, sh.toFloat() / bh)
            val outW = (bw * scale).toInt()
            val outH = (bh * scale).toInt()
            val left = (sw - outW) / 2
            val top = (sh - outH) / 2
            return Rect(left, top, left + outW, top + outH)
        }
    }

    companion object {
        private val BITMAP_PAINT = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    }
}
