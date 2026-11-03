package uk.lloyd.tvhome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Home button takeover, backup method.
 *
 * The preferred method is making TV Home the default home app (one command, see Setup),
 * which needs no running service. This service is the fallback: it brings TV Home to the
 * front when the Home button is pressed, or when Google's own home screen appears.
 * It never reads what is on screen.
 *
 * Short press Home = TV Home. Long press Home = the system side panel (Google Home panel).
 */
class HomeButtonService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: HomeButtonService? = null
            private set

        private const val LONG_PRESS_MS = 600L
    }

    /** Google's own home screens, as "package/ActivityClass", read from the TV itself. */
    private var stockHomeScreens: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        stockHomeScreens = findStockHomeScreens()
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
            val held = event.eventTime - event.downTime
            if (held >= LONG_PRESS_MS) openSidePanel() else openTvHome()
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val cls = event.className?.toString().orEmpty()
        val key = "$pkg/$cls"
        Prefs.setLastWindow(this, key)

        if (stockHomeScreens.isEmpty()) stockHomeScreens = findStockHomeScreens()
        // Only Google's actual home screen, never its search results, panels or other pages.
        if (key !in stockHomeScreens) return
        if (!Prefs.watchScreens(this)) return
        // Just opened something of Google's on purpose: leave it alone for a moment.
        if (System.currentTimeMillis() < Prefs.graceUntil(this)) return
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

    /** Opens the system side panel, where the Google Home panel lives. Returns false if refused. */
    fun openSidePanel(): Boolean {
        Prefs.setGrace(this, 20_000)
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    private fun findStockHomeScreens(): Set<String> =
        runCatching {
            packageManager
                .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .map { it.activityInfo }
                .filter { it.packageName != packageName }
                .map { "${it.packageName}/${it.name}" }
                .toSet()
        }.getOrDefault(emptySet())
}
