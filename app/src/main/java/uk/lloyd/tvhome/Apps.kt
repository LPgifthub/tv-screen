package uk.lloyd.tvhome

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** An installed TV app, with its own artwork read from the device. */
data class TvApp(
    val label: String,
    val packageName: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    val launch: Intent,
)

/** One of the six main buttons, and how to recognise its app on the Streamer. */
data class Favourite(val name: String, val matches: (TvApp) -> Boolean)

object Apps {

    val FAVOURITES = listOf(
        Favourite("YouTube") { a ->
            val l = a.label.lowercase()
            l.contains("youtube") && !l.contains("kids") && !l.contains("music")
        },
        Favourite("Netflix") { a -> a.label.lowercase().contains("netflix") },
        Favourite("BBC iPlayer") { a -> a.label.lowercase().contains("iplayer") },
        Favourite("ITVX") { a -> a.label.lowercase().startsWith("itv") },
        Favourite("Disney+") { a -> a.label.lowercase().contains("disney") },
        Favourite("NOW") { a ->
            val l = a.label.lowercase().trim()
            l == "now" || l.startsWith("now ") || a.packageName.contains("nowtv")
        },
    )

    /** Every app on the TV's own app list, apart from this one. Slow-ish: call off the main thread. */
    fun load(context: Context): List<TvApp> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                val ai = ri.activityInfo
                val bannerDrawable = ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm)
                val banner = bannerDrawable?.let { d ->
                    runCatching { d.toBitmap(320, 180).asImageBitmap() }.getOrNull()
                }
                val icon = if (banner == null) {
                    runCatching { ai.loadIcon(pm).toBitmap(128, 128).asImageBitmap() }.getOrNull()
                } else null
                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setClassName(ai.packageName, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                TvApp(ri.loadLabel(pm).toString(), ai.packageName, banner, icon, launch)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** The six main buttons, each paired with its installed app (or null if it isn't installed). */
    fun favourites(all: List<TvApp>): List<Pair<String, TvApp?>> =
        FAVOURITES.map { f -> f.name to all.filter(f.matches).minByOrNull { it.label.length } }

    fun launch(context: Context, app: TvApp): Boolean =
        try {
            context.startActivity(app.launch)
            true
        } catch (e: Exception) {
            false
        }
}
