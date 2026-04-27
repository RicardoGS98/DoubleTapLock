package io.github.ricardogs98.doubletaplock.service.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Decodes a bitmap downsampled to roughly cover (surfaceW × surfaceH) using
// RGB_565 to keep memory low on cheap devices (a 4000×1080 panorama lands
// around 8 MB). Returns null when the file is missing/unreadable or the
// surface size is unknown.
suspend fun loadCoverBitmap(file: File, surfaceW: Int, surfaceH: Int): Bitmap? {
    if (!file.exists() || surfaceW <= 0 || surfaceH <= 0) return null
    return withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        val options = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(
                bounds.outWidth, bounds.outHeight, surfaceW, surfaceH
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
