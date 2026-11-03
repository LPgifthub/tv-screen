package uk.lloyd.tvhome

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Home, AllApps, Setup }

class MainActivity : ComponentActivity() {

    private val screen = mutableStateOf(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppFonts.init(this)
        setContent { App(screen) }
    }

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
}

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

@Composable
fun App(screenState: MutableState<Screen>) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    var apps by remember { mutableStateOf<List<TvApp>>(emptyList()) }
    var weather by remember { mutableStateOf<Weather?>(null) }
    var latestBuild by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val screen = screenState.value
    val updateReady = latestBuild?.let { it > Updater.installedBuild(context) } ?: false

    // Check GitHub for a newer build at start-up, every 6 hours, and whenever Setup opens.
    LaunchedEffect(screen == Screen.Setup) {
        while (true) {
            Updater.latestBuild()?.let { latestBuild = it }
            delay(6 * 60 * 60 * 1000L)
        }
    }

    // Reload the app list whenever we come back to a page (catches newly installed apps).
    LaunchedEffect(screen) {
        apps = withContext(Dispatchers.IO) { runCatching { Apps.load(context) }.getOrDefault(apps) }
    }

    // Weather every 30 minutes.
    LaunchedEffect(Unit) {
        while (true) {
            WeatherService.fetch()?.let { weather = it }
            delay(30 * 60 * 1000L)
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
            updateReady = updateReady,
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
            onInstallUpdate = {
                scope.launch {
                    context.toast("Downloading the newest TV Home…")
                    if (!Updater.downloadAndInstall(context)) {
                        context.toast("Update didn't download – check the Wi-Fi and try again")
                    }
                }
            },
            onOpenAccessibility = { activity?.openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            onOpenAndroidSettings = { activity?.openSettings(Settings.ACTION_SETTINGS) },
            onTestHomePanel = { activity?.openHomeControls() },
            onBack = { screenState.value = Screen.Home },
        )
    }
}
