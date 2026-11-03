package uk.lloyd.tvhome

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/** The one-time command that makes TV Home the default home screen (preferred Home button method). */
const val SET_HOME_COMMAND = "cmd package set-home-activity uk.lloyd.tvhome/.MainActivity"

/** Only needed for the backup method: lets the Accessibility switch turn on for a sideloaded app. */
const val ALLOW_SERVICE_COMMAND = "appops set uk.lloyd.tvhome ACCESS_RESTRICTED_SETTINGS allow"

/** Setup and test page: Home button status, updates, and what to report back. */
@Composable
fun SetupScreen(
    latestBuild: Int?,
    onInstallUpdate: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onTestHomePanel: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    var tick by remember { mutableStateOf(0) }
    var watch by remember { mutableStateOf(Prefs.watchScreens(context)) }

    // Refresh the status lines every couple of seconds while this page is open.
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
        while (true) {
            delay(2_000)
            tick++
        }
    }

    @Suppress("UNUSED_VARIABLE") val refresh = tick
    val defaultHome = defaultHomeLabel(context)
    val serviceOn = HomeButtonService.instance != null
    val lastKey = Prefs.lastHomeKey(context)
    val installed = Updater.installedBuild(context)
    val updateReady = latestBuild != null && latestBuild > installed

    Row(
        Modifier.fillMaxSize().background(Palette.Cream).padding(horizontal = 44.dp, vertical = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(
            Modifier.weight(1.25f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Txt("Setup", 30.sp, Palette.Chocolate, family = AppFonts.serif)

            Txt("Home button", 13.sp, Palette.Forest, weight = FontWeight.Bold)
            StatusLine("Default home app", defaultHome)
            StatusLine("Backup Home button service", if (serviceOn) "On" else "Off")
            StatusLine(
                "Home key last caught by service",
                if (lastKey == 0L) "Not yet" else DateFormat.getTimeInstance().format(Date(lastKey)),
            )
            StatusLine("Last screen seen by service", Prefs.lastWindow(context))

            Txt("Recommended: make TV Home the default home app with this one command:", 10.sp, Palette.Chocolate)
            CommandBox(SET_HOME_COMMAND)
            Txt("Backup method, only if that doesn't stick: allow the service, then switch it on in Accessibility:", 10.sp, Palette.Chocolate)
            CommandBox(ALLOW_SERVICE_COMMAND)

            Txt("Updates", 13.sp, Palette.Forest, weight = FontWeight.Bold)
            StatusLine("Installed build", installed.toString())
            StatusLine(
                "Newest build",
                when {
                    latestBuild == null -> "Checking…"
                    updateReady -> "$latestBuild – ready to install"
                    else -> "$latestBuild – up to date"
                },
            )
        }

        Column(Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Txt(" ", 30.sp, Palette.Cream, family = AppFonts.serif)
            SetupButton(if (updateReady) "Install update" else "Check for update / reinstall", first, onInstallUpdate)
            SetupButton("Test: open Google Home panel", null, onTestHomePanel)
            SetupButton("Open Accessibility settings", null, onOpenAccessibility)
            SetupButton(
                if (watch) "Backup screen watching: On" else "Backup screen watching: Off",
                null,
            ) {
                watch = !watch
                Prefs.setWatchScreens(context, watch)
            }
            SetupButton("Open Android settings", null, onOpenAndroidSettings)
            SetupButton("Back to home", null, onBack)
            Txt(
                "Tip: long-press Home on the remote also opens the Google Home panel when the backup service is on.",
                9.sp, Palette.Muted,
            )
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.CreamLight)
            .border(1.dp, Palette.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Txt(label, 9.5.sp, Palette.Muted, Modifier.weight(0.9f))
        Txt(value, 9.5.sp, Palette.Chocolate, Modifier.weight(1.4f), weight = FontWeight.SemiBold, maxLines = 2)
    }
}

@Composable
private fun CommandBox(command: String) {
    Txt(
        command, 10.sp, Palette.OnDark,
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.Night)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        family = FontFamily.Monospace,
    )
}

@Composable
private fun SetupButton(label: String, focusRequester: FocusRequester?, onClick: () -> Unit) {
    TvButton(
        shape = RoundedCornerShape(9.dp),
        background = Palette.Forest,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(38.dp),
        focusRequester = focusRequester,
        focusedScale = 1.03f,
    ) {
        Txt(label, 10.5.sp, Palette.OnDark, weight = FontWeight.SemiBold)
    }
}

/** Which app the TV treats as its home screen right now. */
private fun defaultHomeLabel(context: Context): String =
    runCatching {
        val pm = context.packageManager
        val info = pm.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo ?: return@runCatching "Unknown"
        when (info.packageName) {
            context.packageName -> "TV Home ✓"
            "android" -> "Not chosen yet"
            else -> info.applicationInfo.loadLabel(pm).toString() + " (${info.packageName})"
        }
    }.getOrDefault("Unknown")
