package com.doubletaplock.app.service.wallpaper

// Single-shot detector: returns true exactly once when a tap lands within
// thresholdMs of the previous one. Resets after firing so a triple tap
// counts as one double tap, not two.
class DoubleTapDetector(private val thresholdMs: Long) {

    @Volatile
    private var lastTapAt: Long = 0L

    fun onTap(nowMs: Long): Boolean {
        val previous = lastTapAt
        if (previous != 0L && nowMs - previous <= thresholdMs) {
            lastTapAt = 0L
            return true
        }
        lastTapAt = nowMs
        return false
    }
}
