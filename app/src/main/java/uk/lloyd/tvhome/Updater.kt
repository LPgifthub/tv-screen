package uk.lloyd.tvhome

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Lets TV Home update itself from the GitHub download link, so Downloader is only needed once. */
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
            // First time only: Android asks you to allow TV Home to install updates.
            context.toast("Allow TV Home to install updates, then press Back and try again")
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return false
        }

        val file = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val out = File(dir, "tv-home.apk")
                val conn = URL(APK_URL).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                try {
                    conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
                } finally {
                    conn.disconnect()
                }
                out
            }.getOrNull()
        } ?: return false

        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }
}
