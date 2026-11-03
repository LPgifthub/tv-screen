package uk.lloyd.tvhome

import android.content.Context
import android.content.Intent

/**
 * Safety net for a home screen app: if TV Home ever crashes three times within three minutes,
 * the next launch opens Google's own home screen instead, so the TV is never stuck.
 * Google's home screen is never switched off, so it is always there to fall back to.
 */
object CrashGuard {
    private const val WINDOW_MS = 3 * 60_000L
    private const val LIMIT = 3

    private fun sp(c: Context) = c.getSharedPreferences("crashguard", Context.MODE_PRIVATE)

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is GuardHandler) return
        Thread.setDefaultUncaughtExceptionHandler(GuardHandler(app, previous))
    }

    private class GuardHandler(
        private val app: Context,
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            runCatching { record(app) }
            previous?.uncaughtException(t, e)
        }
    }

    private fun recent(c: Context): List<Long> {
        val now = System.currentTimeMillis()
        return sp(c).getString("times", null).orEmpty()
            .split(',').mapNotNull { it.toLongOrNull() }
            .filter { now - it < WINDOW_MS }
    }

    private fun record(c: Context) {
        val times = (recent(c) + System.currentTimeMillis()).takeLast(LIMIT + 2)
        // commit(), not apply(): the process is about to end.
        sp(c).edit().putString("times", times.joinToString(",")).commit()
    }

    fun shouldFallBack(c: Context): Boolean = recent(c).size >= LIMIT

    fun clear(c: Context) {
        sp(c).edit().remove("times").apply()
    }

    /** Opens Google's own home screen directly. Returns false if it can't be found. */
    fun openGoogleHome(c: Context): Boolean = runCatching {
        val homes = c.packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo }
            .filter { it.packageName != c.packageName }
        val target = homes.firstOrNull { it.packageName.contains("launcher") } ?: homes.firstOrNull()
            ?: return@runCatching false
        c.startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClassName(target.packageName, target.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)
}
