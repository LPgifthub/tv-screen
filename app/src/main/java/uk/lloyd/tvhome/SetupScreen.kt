package uk.lloyd.tvhome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
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

const val GUIDE_URL = "https://github.com/LPgifthub/tv-screen"
const val FOOTBALL_SIGNUP_URL = "https://www.football-data.org/"

/** True when the Home button part of setup is fully done. */
fun homeButtonReady(context: android.content.Context): Boolean =
    isDefaultHome(context) && ServiceGuard.canSelfRepair(context)

/** Setup as a checklist that ticks itself off, with a QR code for whatever is next. */
@Composable
fun SetupScreen(
    latestBuild: Int?,
    footballKeySet: Boolean,
    paletteMode: PaletteMode,
    onInstallUpdate: () -> Unit,
    onEnterFootballKey: () -> Unit,
    onCyclePalette: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onTestHomePanel: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    var tick by remember { mutableStateOf(0) }
    var backupWanted by remember { mutableStateOf(Prefs.backupWanted(context)) }
    var autoUpdate by remember { mutableStateOf(Prefs.autoUpdate(context)) }

    // Re-check everything every 2 seconds, so ticks appear as soon as each step is done.
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
    val homeDone = defaultHome && selfRepair

    Row(
        Modifier.fillMaxSize().background(Palette.Bg).padding(horizontal = 44.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // Left: the checklist.
        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Txt("Setup", 30.sp, Palette.Ink, family = AppFonts.serif)
                Txt(
                    if (homeDone && footballKeySet) "All done ✓" else "Each step ticks itself off when it's done",
                    11.sp, if (homeDone && footballKeySet) Palette.Accent else Palette.Muted,
                    Modifier.padding(bottom = 6.dp),
                )
            }

            StepCard(1, "Home button", homeDone) {
                Check("TV Home is the default home app", defaultHome)
                Check("Backup Home button service running", serviceRunning)
                Check("Backup switches itself back on after a restart", selfRepair)
                if (!homeDone) {
                    Txt(
                        "Scan the code on the right with your Pixel camera, tap Copy, and paste it into Bugjaeger's shell. " +
                            "Pairing steps are in the guide.",
                        10.5.sp, Palette.Muted,
                    )
                }
            }

            StepCard(2, "Football scores", footballKeySet) {
                Check("Free football-data.org key saved", footballKeySet)
                if (!footballKeySet && homeDone) {
                    Txt("Scan the code to sign up, then press \"Add football scores key\".", 10.5.sp, Palette.Muted)
                }
            }

            StepCard(3, "Updates", !updateReady && latestBuild != null) {
                Check(
                    when {
                        latestBuild == null -> "Build $installed – checking GitHub…"
                        updateReady -> "Build $installed – build $latestBuild is ready"
                        else -> "Build $installed – up to date"
                    },
                    !updateReady && latestBuild != null,
                )
                Txt(
                    if (Updater.canUpdateSilently(context)) "New builds install by themselves while TV Home is on screen."
                    else "The next update asks you to confirm once; after that they install by themselves.",
                    10.5.sp, Palette.Muted,
                )
            }

            Txt("Service last saw: ${Prefs.lastWindow(context)}", 9.sp, Palette.Muted, maxLines = 1)
        }

        // Right: next-step QR code and the controls.
        Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val (qrText, qrTitle, qrHint) = when {
                !homeDone -> Triple(SETUP_COMMAND, "Step 1 – setup command", "Scan, tap Copy, paste into Bugjaeger's shell")
                !footballKeySet -> Triple(FOOTBALL_SIGNUP_URL, "Step 2 – free football key", "Scan to open football-data.org on your phone")
                else -> Triple(GUIDE_URL, "Guide and troubleshooting", "Scan to open the guide on your phone")
            }
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Card)
                    .border(1.dp, Palette.Border, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QrCode(qrText, 112.dp, qrTitle)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Txt(qrTitle, 12.sp, Palette.Ink, weight = FontWeight.Bold)
                    Txt(qrHint, 10.5.sp, Palette.Muted)
                }
            }

            SetupButton(if (updateReady) "Install update now" else "Check for update / reinstall", first, onInstallUpdate)
            SetupButton(if (footballKeySet) "Change football scores key" else "Add football scores key", null, onEnterFootballKey)
            SetupButton(paletteMode.label, null, onCyclePalette)
            SetupButton(if (autoUpdate) "Automatic updates: on" else "Automatic updates: off", null) {
                autoUpdate = !autoUpdate
                Prefs.setAutoUpdate(context, autoUpdate)
            }
            SetupButton(if (backupWanted) "Backup Home service: on" else "Backup Home service: off", null) {
                backupWanted = !backupWanted
                Prefs.setBackupWanted(context, backupWanted)
                if (!ServiceGuard.sync(context) && !selfRepair) onOpenAccessibility()
            }
            SetupButton("Test: open Google Home panel", null, onTestHomePanel)
            SetupButton("Back to home", null, onBack)
        }
    }
}

@Composable
private fun StepCard(number: Int, title: String, done: Boolean, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Card)
            .border(1.dp, if (done) Palette.Accent.copy(alpha = 0.5f) else Palette.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(if (done) Palette.TileGreen else Palette.Chip),
                contentAlignment = Alignment.Center,
            ) {
                Txt(if (done) "✓" else number.toString(), 10.sp, if (done) Palette.OnTile else Palette.Ink, weight = FontWeight.Bold)
            }
            Txt(title, 13.sp, Palette.Ink, weight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun Check(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Txt(if (ok) "✓" else "○", 11.sp, if (ok) Palette.Accent else Palette.Muted, weight = FontWeight.Bold)
        Txt(label, 11.sp, if (ok) Palette.Ink else Palette.Muted)
    }
}

@Composable
private fun SetupButton(label: String, focusRequester: FocusRequester?, onClick: () -> Unit) {
    TvButton(
        shape = RoundedCornerShape(9.dp),
        background = Palette.TileGreen,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(36.dp),
        focusRequester = focusRequester,
        focusedScale = 1.03f,
    ) {
        Txt(label, 11.sp, Palette.OnTile, Modifier.padding(horizontal = 10.dp), weight = FontWeight.SemiBold, maxLines = 1)
    }
}
