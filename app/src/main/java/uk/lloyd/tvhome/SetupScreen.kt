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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/** Setup and test page: Home button status, and what the Stage 1 test needs to report back. */
@Composable
fun SetupScreen(
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
    val serviceOn = HomeButtonService.instance != null
    val lastKey = Prefs.lastHomeKey(context)

    Row(
        Modifier.fillMaxSize().background(Palette.Cream).padding(horizontal = 44.dp, vertical = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt("Setup", 34.sp, Palette.Chocolate, family = AppFonts.serif)
            Txt("Home button", 14.sp, Palette.Forest, weight = FontWeight.Bold)
            Txt(
                "To make the remote's Home button open TV Home, press \"Open Accessibility settings\", " +
                    "choose \"TV Home – Home button\" and switch it on. Then press Home on the remote to test.",
                11.sp, Palette.Chocolate,
            )
            StatusLine("Home button service", if (serviceOn) "On" else "Off")
            StatusLine(
                "Home key last caught",
                if (lastKey == 0L) "Not yet" else DateFormat.getTimeInstance().format(Date(lastKey)),
            )
            StatusLine("Last Google screen seen", Prefs.lastWindow(context))
            StatusLine("App version", versionName(context))
            Txt(
                "If Home still opens Google's screen with the service on, note the \"Last Google screen seen\" line " +
                    "and send it to Claude. That tells us which backup method to use.",
                9.5.sp, Palette.Muted,
            )
        }

        Column(Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt(" ", 34.sp, Palette.Cream, family = AppFonts.serif)
            SetupButton("Open Accessibility settings", first, onOpenAccessibility)
            SetupButton(
                if (watch) "Screen watching: On" else "Screen watching: Off",
                null,
            ) {
                watch = !watch
                Prefs.setWatchScreens(context, watch)
            }
            SetupButton("Test: open Google Home panel", null, onTestHomePanel)
            SetupButton("Open Android settings", null, onOpenAndroidSettings)
            SetupButton("Back to home", null, onBack)
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
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Txt(label, 10.sp, Palette.Muted, Modifier.weight(0.9f))
        Txt(value, 10.sp, Palette.Chocolate, Modifier.weight(1.4f), weight = FontWeight.SemiBold, maxLines = 2)
    }
}

@Composable
private fun SetupButton(label: String, focusRequester: FocusRequester?, onClick: () -> Unit) {
    TvButton(
        shape = RoundedCornerShape(9.dp),
        background = Palette.Forest,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        focusRequester = focusRequester,
        focusedScale = 1.03f,
    ) {
        Txt(label, 11.sp, Palette.OnDark, weight = FontWeight.SemiBold)
    }
}

private fun versionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: "unknown"
