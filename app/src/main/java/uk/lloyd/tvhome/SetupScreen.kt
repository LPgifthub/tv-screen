package uk.lloyd.tvhome

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * The one-time setup command, as a single line to paste into an ADB shell:
 * 1. makes TV Home the default home app (the main Home button method);
 * 2. allows the backup Accessibility service, which Android greys out for sideloaded apps;
 * 3. lets TV Home switch that backup service back on by itself after a restart.
 */
const val SETUP_COMMAND =
    "cmd package set-home-activity uk.lloyd.tvhome/.MainActivity; " +
        "appops set uk.lloyd.tvhome ACCESS_RESTRICTED_SETTINGS allow; " +
        "pm grant uk.lloyd.tvhome android.permission.WRITE_SECURE_SETTINGS"

@Composable
fun SetupScreen(
    latestBuild: Int?,
    footballKeySet: Boolean,
    paletteMode: PaletteMode,
    onInstallUpdate: () -> Unit,
    onEnterFootballKey: () -> Unit,
    onCyclePalette: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onTestHomePanel: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    var tick by remember { mutableStateOf(0) }
    var backupWanted by remember { mutableStateOf(Prefs.backupWanted(context)) }

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
    val installed = Updater.installedBuild(context)
    val updateReady = latestBuild != null && latestBuild > installed
    val defaultHome = isDefaultHome(context)
    val serviceRunning = HomeButtonService.instance != null
    val selfRepair = ServiceGuard.canSelfRepair(context)

    Row(
        Modifier.fillMaxSize().background(Palette.Bg).padding(horizontal = 44.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt("Setup", 30.sp, Palette.Ink, family = AppFonts.serif)

            Heading("Home button")
            StatusLine("Default home app", if (defaultHome) "TV Home ✓" else otherHomeLabel(context), good = defaultHome)
            StatusLine(
                "Backup service",
                when {
                    serviceRunning -> "Running ✓"
                    !backupWanted -> "Switched off in Setup"
                    else -> "Not running"
                },
                good = serviceRunning,
            )
            StatusLine("Backup switches itself back on", if (selfRepair) "Yes ✓" else "Not yet – run the command", good = selfRepair)

            Txt("One-time command (paste it in one go – see the README for how):", 11.sp, Palette.Ink)
            Txt(
                SETUP_COMMAND, 9.5.sp, Palette.OnNight,
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.Night)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                family = FontFamily.Monospace,
            )

            Heading("Scores and updates")
            StatusLine("Football scores key", if (footballKeySet) "Saved ✓" else "Not added yet", good = footballKeySet)
            StatusLine(
                "TV Home build",
                when {
                    latestBuild == null -> "$installed (checking for newer…)"
                    updateReady -> "$installed – build $latestBuild ready to install"
                    else -> "$installed – up to date ✓"
                },
                good = latestBuild != null && !updateReady,
            )
            StatusLine("Last screen seen by service", Prefs.lastWindow(context), good = null)
        }

        Column(Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Txt(" ", 30.sp, Palette.Bg, family = AppFonts.serif)
            SetupButton(if (updateReady) "Install update" else "Check for update / reinstall", first, onInstallUpdate)
            SetupButton(if (footballKeySet) "Change football scores key" else "Add football scores key", null, onEnterFootballKey)
            SetupButton(paletteMode.label, null, onCyclePalette)
            SetupButton(if (backupWanted) "Backup service: on" else "Backup service: off", null) {
                backupWanted = !backupWanted
                Prefs.setBackupWanted(context, backupWanted)
                if (!ServiceGuard.sync(context) && !selfRepair) onOpenAccessibility()
            }
            SetupButton("Test: open Google Home panel", null, onTestHomePanel)
            SetupButton("Open Accessibility settings", null, onOpenAccessibility)
            SetupButton("Open Android settings", null, onOpenAndroidSettings)
            SetupButton("Back to home", null, onBack)
        }
    }
}

@Composable
private fun Heading(text: String) {
    Txt(text, 13.sp, Palette.Accent, Modifier.padding(top = 4.dp), weight = FontWeight.Bold)
}

@Composable
private fun StatusLine(label: String, value: String, good: Boolean?) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Txt(label, 10.5.sp, Palette.Muted, Modifier.weight(0.95f))
        Txt(
            value, 10.5.sp,
            when (good) {
                true -> Palette.Accent
                false -> Palette.Live
                null -> Palette.Ink
            },
            Modifier.weight(1.35f), weight = FontWeight.SemiBold, maxLines = 2,
        )
    }
}

@Composable
private fun SetupButton(label: String, focusRequester: FocusRequester?, onClick: () -> Unit) {
    TvButton(
        shape = RoundedCornerShape(9.dp),
        background = Palette.TileGreen,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(38.dp),
        focusRequester = focusRequester,
        focusedScale = 1.03f,
    ) {
        Txt(label, 11.sp, Palette.OnTile, Modifier.padding(horizontal = 10.dp), weight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Which other app is the home screen right now. */
private fun otherHomeLabel(context: Context): String =
    runCatching {
        val pm = context.packageManager
        val info = pm.resolveActivity(
            android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME),
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo ?: return@runCatching "Unknown"
        if (info.packageName == "android") "Not chosen yet – run the command"
        else info.applicationInfo.loadLabel(pm).toString() + " – run the command"
    }.getOrDefault("Unknown")
