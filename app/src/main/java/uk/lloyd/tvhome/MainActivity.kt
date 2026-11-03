package uk.lloyd.tvhome

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        val opened = HomeButtonService.instance
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS) ?: false
        if (!opened) {
            toast("Open the Google Home panel from your remote's dashboard for now – this button is finished in Stage 2")
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
    val screen = screenState.value

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
            onOpenApp = { name, app ->
                if (app == null || !Apps.launch(context, app)) {
                    context.toast("$name isn't installed – get it from the Play Store")
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
            onOpenAccessibility = { activity?.openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            onOpenAndroidSettings = { activity?.openSettings(Settings.ACTION_SETTINGS) },
            onTestHomePanel = { activity?.openHomeControls() },
            onBack = { screenState.value = Screen.Home },
        )
    }
}
