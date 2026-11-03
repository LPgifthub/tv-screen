package uk.lloyd.tvhome

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Keeps the backup Home button service switched on.
 *
 * Android TV 14 can switch accessibility services off after a restart. If the one-time
 * setup command granted TV Home the "write secure settings" permission, TV Home can
 * switch its own service back on at start-up and whenever it opens. Without that
 * permission this does nothing and the service is switched on by hand in Accessibility.
 */
object ServiceGuard {
    private const val COMPONENT = "uk.lloyd.tvhome/uk.lloyd.tvhome.HomeButtonService"
    private const val SHORT_COMPONENT = "uk.lloyd.tvhome/.HomeButtonService"

    fun canSelfRepair(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun isEnabledInSettings(context: Context): Boolean {
        val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return list.split(':').any { it.equals(COMPONENT, true) || it.equals(SHORT_COMPONENT, true) }
    }

    /** Makes the service match what Setup asks for. Returns true if it changed anything. */
    fun sync(context: Context): Boolean = runCatching {
        if (!canSelfRepair(context)) return@runCatching false
        val resolver = context.contentResolver
        val current = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val others = current.split(':').filter {
            it.isNotBlank() && !it.equals(COMPONENT, true) && !it.equals(SHORT_COMPONENT, true)
        }
        val wanted = Prefs.backupWanted(context)
        val alreadyOn = isEnabledInSettings(context)
        if (wanted == alreadyOn) return@runCatching false
        val next = if (wanted) others + COMPONENT else others
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, next.joinToString(":"))
        if (wanted) Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        true
    }.getOrDefault(false)
}

/** After the TV restarts, put the backup service back on if it was switched off. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ServiceGuard.sync(context)
        }
    }
}

/** True when TV Home is the TV's default home app (the preferred Home button method). */
fun isDefaultHome(context: Context): Boolean = runCatching {
    context.packageManager.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        PackageManager.MATCH_DEFAULT_ONLY,
    )?.activityInfo?.packageName == context.packageName
}.getOrDefault(false)
