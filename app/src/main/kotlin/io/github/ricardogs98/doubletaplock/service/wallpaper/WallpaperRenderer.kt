package io.github.ricardogs98.doubletaplock.service.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

private val BITMAP_PAINT = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

// Cover-fits the bitmap into the surface and pans horizontally through any
// slack created by a wide source. Vertical slack is always centered. Tall
// or screen-shaped bitmaps don't pan (slack collapses to 0).
fun renderCoverCrop(
    canvas: Canvas,
    bitmap: Bitmap?,
    surfaceW: Int,
    surfaceH: Int,
    xOffset: Float,
) {
    canvas.drawColor(Color.BLACK)
    if (bitmap == null || surfaceW <= 0 || surfaceH <= 0) return
    val src = Rect(0, 0, bitmap.width, bitmap.height)
    val dst = computeCoverDst(bitmap.width, bitmap.height, surfaceW, surfaceH, xOffset)
    canvas.drawBitmap(bitmap, src, dst, BITMAP_PAINT)
}

private fun computeCoverDst(bw: Int, bh: Int, sw: Int, sh: Int, xOffset: Float): Rect {
    if (bw <= 0 || bh <= 0) return Rect(0, 0, sw, sh)
    val scale = maxOf(sw.toFloat() / bw, sh.toFloat() / bh)
    val outW = (bw * scale).toInt()
    val outH = (bh * scale).toInt()
    val left = ((sw - outW) * xOffset.coerceIn(0f, 1f)).toInt()
    val top = (sh - outH) / 2
    return Rect(left, top, left + outW, top + outH)
}
