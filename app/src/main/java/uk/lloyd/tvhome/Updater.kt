package uk.lloyd.tvhome

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lets TV Home update itself from GitHub.
 *
 * Installs go through Android's PackageInstaller with "no user action needed". The first
 * update (TV Home was installed by Downloader) asks you to confirm once; from then on TV Home
 * is its own installer, and Android lets it update itself silently.
 */
object Updater {
    private const val REPO = "LPgifthub/tv-screen"
    private const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"
    private const val APK_URL = "https://github.com/$REPO/releases/latest/download/tv-home.apk"

    fun installedBuild(context: Context): Int =
        runCatching {
            PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0),
            ).toInt()
        }.getOrDefault(0)

    /** True once TV Home installed itself at least once, so later updates can be silent. */
    fun canUpdateSilently(context: Context): Boolean = runCatching {
        val pm = context.packageManager
        val installer = if (Build.VERSION.SDK_INT >= 30) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        installer == context.packageName && pm.canRequestPackageInstalls()
    }.getOrDefault(false)

    /** The newest build number on GitHub, or null if it can't be checked right now. */
    suspend fun latestBuild(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(LATEST_API).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "tv-home-app")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val name = JSONObject(body).optString("name")
                Regex("build (\\d+)").find(name)?.groupValues?.get(1)?.toInt()
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** Downloads the newest build and hands it to Android's installer. */
    suspend fun downloadAndInstall(context: Context): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            // First time only: Android asks you to allow TV Home to install its updates.
            context.toast("Allow TV Home to install updates, then press Back and try again")
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return false
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apk = File(dir, "tv-home.apk")
                val conn = URL(APK_URL).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                try {
                    conn.inputStream.use { input -> apk.outputStream().use { input.copyTo(it) } }
                } finally {
                    conn.disconnect()
                }

                val installer = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(context.packageName)
                    if (Build.VERSION.SDK_INT >= 31) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }
                val sessionId = installer.createSession(params)
                installer.openSession(sessionId).use { session ->
                    apk.inputStream().use { input ->
                        session.openWrite("tv-home.apk", 0, apk.length()).use { out ->
                            input.copyTo(out)
                            session.fsync(out)
                        }
                    }
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                    val callback = PendingIntent.getBroadcast(
                        context, sessionId, Intent(context, InstallResultReceiver::class.java), flags,
                    )
                    session.commit(callback.intentSender)
                }
                true
            }.getOrDefault(false)
        }
    }
}

/** Hears back from Android's installer: shows the confirm screen if one is needed, or reports a failure. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) {
                    runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> context.toast(
                "Update didn't install: " +
                    (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "unknown reason"),
            )
        }
    }
}
