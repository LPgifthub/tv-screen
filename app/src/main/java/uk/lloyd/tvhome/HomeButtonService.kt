package uk.lloyd.tvhome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Home button takeover, method 1.
 *
 * Google's own home screen stays installed (so the Google Home features keep working);
 * this service simply brings TV Home to the front whenever the Home button is pressed
 * or Google's home screen appears. It never reads what is on screen.
 */
class HomeButtonService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: HomeButtonService? = null
            private set

        val STOCK_LAUNCHERS = setOf(
            "com.google.android.apps.tv.launcherx", // Google TV home screen
            "com.google.android.tvlauncher",        // older Android TV home screen
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_UP) {
            Prefs.markHomeKey(this)
            openTvHome()
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val cls = event.className?.toString().orEmpty()
        Prefs.setLastWindow(this, "$pkg / $cls")

        if (pkg !in STOCK_LAUNCHERS) return
        if (!Prefs.watchScreens(this)) return
        // Just opened Google's Home panel on purpose: leave it alone for a moment.
        if (System.currentTimeMillis() < Prefs.graceUntil(this)) return
        // Only full screens, not side panels or pop-ups.
        if (!cls.endsWith("Activity")) return
        openTvHome()
    }

    override fun onInterrupt() {}

    fun openTvHome() {
        val intent = Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        startActivity(intent)
    }
}
