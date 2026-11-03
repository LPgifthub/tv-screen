package uk.lloyd.tvhome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Rounded12 = RoundedCornerShape(12.dp)
private val Rounded9 = RoundedCornerShape(9.dp)

/** The main home screen, laid out to match the signed-off design. */
@Composable
fun HomeScreen(
    apps: List<TvApp>,
    weather: Weather?,
    onOpenApp: (name: String, app: TvApp?) -> Unit,
    onAllApps: () -> Unit,
    onSetup: () -> Unit,
    onHomeControls: () -> Unit,
    onComingSoon: (String) -> Unit,
) {
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstTile.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(Palette.Cream)) {
        Column(
            Modifier.fillMaxSize().padding(start = 44.dp, end = 44.dp, top = 34.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header(weather)

            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    SectionLabel("WATCH")
                    val favourites = Apps.favourites(apps)
                    favourites.chunked(3).forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                            row.forEachIndexed { colIndex, (name, app) ->
                                val dark = (rowIndex * 3 + colIndex) % 2 == 1
                                AppTile(
                                    name = name,
                                    app = app,
                                    background = if (dark) Palette.Chocolate else Palette.Forest,
                                    modifier = Modifier.weight(1f).aspectRatio(16f / 9f),
                                    focusRequester = if (rowIndex == 0 && colIndex == 0) firstTile else null,
                                    onClick = { onOpenApp(name, app) },
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PillButton("All apps", onAllApps)
                        PillButton("Setup", onSetup)
                    }
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        InfoCard("Today", Modifier.weight(1f).fillMaxHeight()) {
                            Txt(
                                "Your calendar appears here once it's connected (Stage 4).",
                                11.sp, Palette.Muted,
                            )
                        }
                        InfoCard("Next up", Modifier.weight(1f).fillMaxHeight()) {
                            TeamRow("NC", "Norwich City", Palette.Forest, Palette.OnDark)
                            TeamRow("NF", "Nottingham Forest", Palette.Chocolate, Palette.OnDark)
                            TeamRow("EN", "England", Palette.Forest, Palette.OnDark)
                            TeamRow("PL", "Premier League", Palette.Brass, Color(0xFF2A1C14))
                        }
                    }
                }

                HomePanel(
                    modifier = Modifier.width(300.dp).fillMaxHeight(),
                    onHomeControls = onHomeControls,
                    onComingSoon = onComingSoon,
                )
            }
        }
    }
}

@Composable
private fun Header(weather: Weather?) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    val date = now.format(DateTimeFormatter.ofPattern("EEEE · d MMMM", Locale.UK)).uppercase(Locale.UK)
    val time = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.UK))
    val part = when (now.hour) {
        in 0..11 -> "morning"
        in 12..17 -> "afternoon"
        else -> "evening"
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Txt(date, 10.sp, Palette.Muted, weight = FontWeight.SemiBold, spacing = 2.2.sp)
            RichTxt(
                buildAnnotatedString {
                    append("Good ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Palette.Forest)) { append(part) }
                },
                TextStyle(fontFamily = AppFonts.serif, fontSize = 44.sp, color = Palette.Chocolate),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Palette.CreamLight)
                    .border(1.dp, Palette.Border, RoundedCornerShape(50))
                    .padding(start = 10.dp, end = 13.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                LineIcon(IconKind.Cloud, Palette.Forest, 22.dp)
                Column {
                    Txt(weather?.let { "${it.tempC}°" } ?: "–°", 15.sp, Palette.Chocolate, weight = FontWeight.SemiBold)
                    Txt("Horsford · ${weather?.summary ?: "updating"}", 8.5.sp, Palette.Muted)
                }
            }
            Txt(time, 60.sp, Palette.Forest, family = AppFonts.serif)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Txt(text, 9.sp, Palette.Muted, weight = FontWeight.SemiBold, spacing = 2.sp)
}

@Composable
private fun AppTile(
    name: String,
    app: TvApp?,
    background: Color,
    modifier: Modifier,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    TvButton(
        shape = Rounded12,
        background = background,
        onClick = onClick,
        modifier = modifier,
        focusRequester = focusRequester,
    ) {
        val banner = app?.banner
        val icon = app?.icon
        when {
            banner != null -> Image(
                bitmap = banner,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(6.dp))
                }
                Txt(name, 17.sp, Palette.OnDark, weight = FontWeight.Bold)
                if (app == null) Txt("Not installed", 8.sp, Palette.OnDarkMuted)
            }
        }
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    TvButton(
        shape = RoundedCornerShape(50),
        background = Palette.CreamLight,
        idleBorder = Palette.ButtonBorder,
        onClick = onClick,
        modifier = Modifier.height(30.dp),
        focusedScale = 1.06f,
    ) {
        Txt(label, 10.sp, Palette.Chocolate, Modifier.padding(horizontal = 16.dp), weight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoCard(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .clip(Rounded12)
            .background(Palette.CreamLight)
            .border(1.dp, Palette.Border, Rounded12)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Txt(title, 18.sp, Palette.Chocolate, family = AppFonts.serif)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Divider))
        content()
    }
}

@Composable
private fun TeamRow(initials: String, name: String, badge: Color, badgeText: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(18.dp).clip(CircleShape).background(badge),
            contentAlignment = Alignment.Center,
        ) {
            Txt(initials, 6.5.sp, badgeText, weight = FontWeight.Bold)
        }
        Txt(name, 10.sp, Palette.Chocolate, Modifier.weight(1f), weight = FontWeight.SemiBold, maxLines = 1)
        Txt("Stage 4", 9.sp, Palette.Muted)
    }
}

@Composable
private fun HomePanel(
    modifier: Modifier,
    onHomeControls: () -> Unit,
    onComingSoon: (String) -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier
            .clip(shape)
            .background(Palette.Marble)
            .border(1.dp, Palette.Border, shape),
    ) {
        MarbleVeins(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt("Home", 24.sp, Palette.Chocolate, Modifier.weight(1f), family = AppFonts.serif)
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Palette.Chip)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Txt("Heating · Stage 2", 9.sp, Palette.Forest, weight = FontWeight.SemiBold)
                }
            }

            // Outdoor camera tile: live view is added in Stage 3.
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(11.dp)).background(Palette.Night),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt("Front garden", 11.sp, Palette.OnDark, weight = FontWeight.SemiBold)
                    Txt("Live camera view arrives in Stage 3", 8.5.sp, Palette.OnDarkMuted, align = TextAlign.Center)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SceneButton("Film night", IconKind.Film, primary = true, Modifier.weight(1f)) { onComingSoon("Scenes arrive in Stage 2") }
                SceneButton("Cosy", IconKind.Flame, primary = false, Modifier.weight(1f)) { onComingSoon("Scenes arrive in Stage 2") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SceneButton("Morning", IconKind.Sunrise, primary = false, Modifier.weight(1f)) { onComingSoon("Scenes arrive in Stage 2") }
                SceneButton("All off", IconKind.Power, primary = false, Modifier.weight(1f)) { onComingSoon("Scenes arrive in Stage 2") }
            }

            TvButton(
                shape = Rounded9,
                background = Palette.Chocolate,
                onClick = onHomeControls,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                focusedScale = 1.03f,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    LineIcon(IconKind.House, Palette.BrassLight, 14.dp)
                    Txt("All home controls", 11.sp, Palette.OnDark, Modifier.weight(1f), weight = FontWeight.SemiBold)
                    LineIcon(IconKind.Chevron, Palette.OnDark, 12.dp)
                }
            }
        }
    }
}

@Composable
private fun SceneButton(
    label: String,
    icon: IconKind,
    primary: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    TvButton(
        shape = Rounded9,
        background = if (primary) Palette.Forest else Palette.CreamLight.copy(alpha = 0.85f),
        idleBorder = if (primary) null else Palette.ButtonBorder,
        onClick = onClick,
        modifier = modifier.height(44.dp),
        focusedScale = 1.04f,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            LineIcon(icon, if (primary) Palette.BrassLight else Palette.Brass, 14.dp)
            Txt(label, 11.sp, if (primary) Palette.OnDark else Palette.Chocolate, weight = FontWeight.SemiBold)
        }
    }
}
