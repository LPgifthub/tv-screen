package uk.lloyd.tvhome

import android.content.Context

/** Small settings stored on the TV only. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("tvhome", Context.MODE_PRIVATE)

    /** While this time is in the future, the backup service leaves Google's screens alone. */
    fun graceUntil(c: Context): Long = sp(c).getLong("grace", 0L)
    fun setGrace(c: Context, millis: Long) {
        sp(c).edit().putLong("grace", System.currentTimeMillis() + millis).apply()
    }

    /** Whether the backup service also watches for Google's home screen appearing. */
    fun watchScreens(c: Context): Boolean = sp(c).getBoolean("watch", true)
    fun setWatchScreens(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("watch", on).apply()
    }

    /** Whether TV Home should keep its backup Home button service switched on. */
    fun backupWanted(c: Context): Boolean = sp(c).getBoolean("backupWanted", true)
    fun setBackupWanted(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("backupWanted", on).apply()
    }

    /** The last screen the backup service saw, shown on the Setup page to help with testing. */
    fun lastWindow(c: Context): String = sp(c).getString("lastWindow", null) ?: "Nothing seen yet"
    fun setLastWindow(c: Context, value: String) {
        sp(c).edit().putString("lastWindow", value).apply()
    }

    /** When the backup service last caught the Home key itself. */
    fun lastHomeKey(c: Context): Long = sp(c).getLong("homeKey", 0L)
    fun markHomeKey(c: Context) {
        sp(c).edit().putLong("homeKey", System.currentTimeMillis()).apply()
    }

    fun paletteMode(c: Context): PaletteMode =
        runCatching { PaletteMode.valueOf(sp(c).getString("palette", null) ?: "Auto") }
            .getOrDefault(PaletteMode.Auto)
    fun setPaletteMode(c: Context, mode: PaletteMode) {
        sp(c).edit().putString("palette", mode.name).apply()
    }

    /** Free football-data.org key, typed in once on the Setup page. */
    fun footballKey(c: Context): String = sp(c).getString("footballKey", null).orEmpty()
    fun setFootballKey(c: Context, key: String) {
        sp(c).edit().putString("footballKey", key.trim()).apply()
    }
}
