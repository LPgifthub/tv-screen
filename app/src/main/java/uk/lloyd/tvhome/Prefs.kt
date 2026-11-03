package uk.lloyd.tvhome

import android.content.Context

/** Small settings stored on the TV only. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("tvhome", Context.MODE_PRIVATE)

    /** While this time is in the future, the Home button service leaves Google's screens alone. */
    fun graceUntil(c: Context): Long = sp(c).getLong("grace", 0L)
    fun setGrace(c: Context, millis: Long) {
        sp(c).edit().putLong("grace", System.currentTimeMillis() + millis).apply()
    }

    /** Whether the service also watches for Google's home screen appearing (not just the Home key). */
    fun watchScreens(c: Context): Boolean = sp(c).getBoolean("watch", true)
    fun setWatchScreens(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("watch", on).apply()
    }

    /** The last screen the service saw, shown on the Setup page to help with testing. */
    fun lastWindow(c: Context): String = sp(c).getString("lastWindow", null) ?: "Nothing seen yet"
    fun setLastWindow(c: Context, value: String) {
        sp(c).edit().putString("lastWindow", value).apply()
    }

    /** When the service last caught the Home key itself. */
    fun lastHomeKey(c: Context): Long = sp(c).getLong("homeKey", 0L)
    fun markHomeKey(c: Context) {
        sp(c).edit().putLong("homeKey", System.currentTimeMillis()).apply()
    }
}
