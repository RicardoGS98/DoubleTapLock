package com.doubletaplock.app.service.wallpaper

import android.view.Choreographer
import kotlin.math.abs
import kotlin.math.exp

// Critically-damped exponential follower for a [0, 1] offset.
//
// Why this exists: the launcher delivers onOffsetsChanged sparsely (often
// just a handful of callbacks per page swipe — sometimes only the final
// destination), so driving draw() straight off those callbacks looks like
// a teleport. Instead, we treat each callback as a new *target* and let a
// Choreographer-driven loop interpolate the actual rendered offset toward
// it at display refresh rate. setTarget mid-flight just retargets the
// existing animation, which keeps continuous swipes fluid.
//
// Must be used from a thread with a Looper (the wallpaper engine's main
// thread). onFrame fires on every frame while converging, plus once on
// the final settled value.
class OffsetAnimator(
    initial: Float,
    private val onFrame: (Float) -> Unit,
) : Choreographer.FrameCallback {

    private val choreographer: Choreographer = Choreographer.getInstance()

    private var current: Float = initial.coerceIn(0f, 1f)
    private var target: Float = current
    private var running: Boolean = false
    private var lastFrameNanos: Long = 0L

    val value: Float get() = current

    fun setTarget(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (clamped == target) return
        target = clamped
        if (!running) startLoop()
    }

    fun snapTo(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        stopLoop()
        target = clamped
        current = clamped
    }

    fun stop() {
        stopLoop()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val dt = if (lastFrameNanos == 0L) {
            FRAME_DT_FALLBACK
        } else {
            ((frameTimeNanos - lastFrameNanos) / NANOS_PER_SECOND).coerceAtMost(FRAME_DT_MAX)
        }
        lastFrameNanos = frameTimeNanos
        val alpha = 1f - exp(-dt / TAU_SECONDS)
        current += (target - current) * alpha
        if (abs(target - current) < EPSILON) {
            current = target
            stopLoop()
            onFrame(current)
        } else {
            onFrame(current)
            choreographer.postFrameCallback(this)
        }
    }

    private fun startLoop() {
        running = true
        lastFrameNanos = 0L
        choreographer.postFrameCallback(this)
    }

    private fun stopLoop() {
        if (running) {
            running = false
            choreographer.removeFrameCallback(this)
        }
        lastFrameNanos = 0L
    }

    private companion object {
        // Time constant — settles to within ~1% in ~5*tau. 80ms feels snappy
        // without being twitchy; raise to slow the follow, lower to tighten it.
        const val TAU_SECONDS = 0.080f
        const val EPSILON = 0.0005f
        const val FRAME_DT_FALLBACK = 1f / 60f
        const val FRAME_DT_MAX = 1f / 30f
        const val NANOS_PER_SECOND = 1_000_000_000f
    }
}
