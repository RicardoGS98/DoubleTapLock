package com.doubletaplock.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op. We don't listen to any events; the service exists solely
        // as a handle for performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN).
    }

    override fun onInterrupt() {
        // No-op.
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun lockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    companion object {
        @Volatile
        private var instance: LockAccessibilityService? = null

        fun lockNow(): Boolean {
            val svc = instance ?: return false
            svc.lockScreen()
            return true
        }

    }
}
