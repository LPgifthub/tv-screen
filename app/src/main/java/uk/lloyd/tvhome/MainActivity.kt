package uk.lloyd.tvhome

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Home, AllApps, Setup }

class MainActivity : ComponentActivity() {

    private val screen = mutableStateOf(Screen.Home)

    /** Goes up each time TV Home comes back on screen, so the app list refreshes. */
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashGuard.install(this)
        if (CrashGuard.shouldFallBack(this)) {
            CrashGuard.clear(this)
            if (CrashGuard.openGoogleHome(this)) {
                toast("TV Home hit a problem, so Google's home screen is open for now. Press Home to try TV Home again.")
                finish()
                return
            }
        }
        AppFonts.init(this)
        Palette.evening = shouldUseEvening(Prefs.paletteMode(this))
        // Match the window to the palette so there's no cream flash when opening in the evening.
        window.setBackgroundDrawable(ColorDrawable(Palette.Bg.toArgb()))
        setContent { App(screen, resumeTick.intValue) }
    }

    override fun onResume() {
        super.onResume()
        ServiceGuard.sync(this)
        resumeTick.intValue++
    }

    /** True while TV Home is the screen being shown (not behind another app). */
    fun isOnScreen(): Boolean = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    /** Pressing Home while on another TV Home page goes back to the main screen. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        screen.value = Screen.Home
    }

    /**
     * "All home controls": asks the system to open its side panel, where the Google Home
     * panel lives. Whether this works on the Streamer is part of the Stage 1 test.
     */
    fun openHomeControls() {
        Prefs.setGrace(this, 20_000)
        val opened = HomeButtonService.instance?.openSidePanel() ?: false
        if (!opened) {
            toast("Open the Google Home panel from your remote's dashboard for now – this button is finished in Stage 2")
        }
    }

    /** For a main button whose app isn't installed: open its Play Store search. */
    fun openPlayStore(appName: String) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(appName) + "&c=apps"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            toast("$appName isn't installed – get it from the Play Store")
        }
    }

    fun openSettings(action: String) {
        Prefs.setGrace(this, 20_000)
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast("Couldn't open that settings page")
        }
    }

    /**
     * Asks for the football-data.org key in a standard TV text box.
     * Tip: the Google TV remote in the Google Home app on your phone can type or paste into it.
     */
    fun askFootballKey(onSaved: (String) -> Unit) {
        val input = EditText(this).apply {
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            hint = "Paste or type your key"
            setText(Prefs.footballKey(this@MainActivity))
        }
        val box = FrameLayout(this).apply {
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Football scores key")
            .setMessage("Get a free key at football-data.org (it's emailed to you), then enter it here.")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val key = input.text.toString().trim()
                Prefs.setFootballKey(this, key)
                onSaved(key)
                toast(if (key.isEmpty()) "Key removed" else "Key saved – scores load in a few seconds")
            }
            .setNegativeButton("Cancel", null)
            .show()
        input.requestFocus()
    }
}

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

@Composable
fun App(screenState: MutableState<Screen>, resumeTick: Int) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val scope = rememberCoroutineScope()
    val screen = screenState.value

    var apps by remember { mutableStateOf<List<TvApp>>(emptyList()) }
    var weather by remember { mutableStateOf<Weather?>(null) }
    var latestBuild by remember { mutableStateOf<Int?>(null) }
    var paletteMode by remember { mutableStateOf(Prefs.paletteMode(context)) }
    var footballKey by remember { mutableStateOf(Prefs.footballKey(context)) }
    var football by remember { mutableStateOf(FootballTracker.noKeyView()) }
    val updateReady = latestBuild?.let { it > Updater.installedBuild(context) } ?: false

    // App list: reload whenever a page changes or TV Home comes back on screen
    // (picks up apps installed from the Play Store in the meantime).
    LaunchedEffect(screen, resumeTick) {
        apps = withContext(Dispatchers.IO) { runCatching { Apps.load(context) }.getOrDefault(apps) }
    }

    // Weather: every 30 minutes; every 2 minutes until the first success (Wi-Fi can be slow after a restart).
    LaunchedEffect(Unit) {
        while (true) {
            val w = WeatherService.fetch()
            if (w != null) weather = w
            delay(if (w != null) 30 * 60_000L else 2 * 60_000L)
        }
    }

    // First launch: open the Setup checklist straight away.
    LaunchedEffect(Unit) {
        if (!Prefs.firstRunDone(context)) {
            Prefs.setFirstRunDone(context)
            screenState.value = Screen.Setup
        }
    }

    // Updates: check at start-up, every 6 hours, and whenever Setup opens.
    LaunchedEffect(screen == Screen.Setup) {
        while (true) {
            Updater.latestBuild()?.let { latestBuild = it }
            delay(6 * 60 * 60_000L)
        }
    }

    // Automatic updates: only while TV Home itself is on screen (never over a programme),
    // and only once Android lets TV Home update itself without asking.
    var autoTried by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(latestBuild, resumeTick) {
        val newest = latestBuild ?: return@LaunchedEffect
        if (newest <= Updater.installedBuild(context) || autoTried == newest) return@LaunchedEffect
        if (!Prefs.autoUpdate(context) || !Updater.canUpdateSilently(context)) return@LaunchedEffect
        delay(5_000)
        if (activity?.isOnScreen() == true) {
            autoTried = newest
            Updater.downloadAndInstall(context)
        }
    }

    // Day or evening colours: checked every 30 seconds.
    LaunchedEffect(paletteMode) {
        while (true) {
            Palette.evening = shouldUseEvening(paletteMode)
            delay(30_000)
        }
    }

    // Fixtures and live scores: every 10 minutes, every minute while a tracked match is on.
    LaunchedEffect(footballKey) {
        if (footballKey.isBlank()) {
            football = FootballTracker.noKeyView()
            return@LaunchedEffect
        }
        val tracker = FootballTracker(footballKey)
        while (true) {
            football = tracker.cycle()
            delay(tracker.nextDelayMillis())
        }
    }

    // Back never leaves the home screen; on other pages it returns home.
    BackHandler(enabled = true) {
        if (screen != Screen.Home) screenState.value = Screen.Home
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            apps = apps,
            weather = weather,
            football = football,
            updateReady = updateReady,
            setupNeeded = !homeButtonReady(context),
            onOpenApp = { name, app ->
                if (app == null) {
                    activity?.openPlayStore(name)
                } else if (!Apps.launch(context, app)) {
                    context.toast("Couldn't open $name")
                }
            },
            onAllApps = { screenState.value = Screen.AllApps },
            onSetup = { screenState.value = Screen.Setup },
            onHomeControls = { activity?.openHomeControls() },
            onComingSoon = { context.toast(it) },
        )
        Screen.AllApps -> AllAppsScreen(apps) { app ->
            if (!Apps.launch(context, app)) context.toast("Couldn't open ${app.label}")
        }
        Screen.Setup -> SetupScreen(
            latestBuild = latestBuild,
            footballKeySet = footballKey.isNotBlank(),
            paletteMode = paletteMode,
            onInstallUpdate = {
                scope.launch {
                    context.toast("Downloading the newest TV Home…")
                    if (!Updater.downloadAndInstall(context)) {
                        context.toast("Update didn't download – check the Wi-Fi and try again")
                    }
                }
            },
            onEnterFootballKey = { activity?.askFootballKey { footballKey = it } },
            onCyclePalette = {
                val next = PaletteMode.entries[(paletteMode.ordinal + 1) % PaletteMode.entries.size]
                paletteMode = next
                Prefs.setPaletteMode(context, next)
                Palette.evening = shouldUseEvening(next)
            },
            onOpenAccessibility = { activity?.openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            onTestHomePanel = { activity?.openHomeControls() },
            onBack = { screenState.value = Screen.Home },
        )
    }
}
