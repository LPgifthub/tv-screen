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
    football: FootballView,
    updateReady: Boolean,
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

    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        Column(
            Modifier.fillMaxSize().padding(start = 44.dp, end = 44.dp, top = 26.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Header(weather)

            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Label and the two small buttons share one line, leaving room for the cards below.
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.weight(1f)) { SectionLabel("WATCH") }
                        PillButton("All apps", onAllApps)
                        PillButton(if (updateReady) "Setup · update ready" else "Setup", onSetup)
                    }

                    val favourites = Apps.favourites(apps)
                    favourites.chunked(3).forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                            row.forEachIndexed { colIndex, (name, app) ->
                                val brown = (rowIndex * 3 + colIndex) % 2 == 1
                                AppTile(
                                    name = name,
                                    app = app,
                                    background = if (brown) Palette.TileBrown else Palette.TileGreen,
                                    modifier = Modifier.weight(1f).aspectRatio(16f / 9f),
                                    focusRequester = if (rowIndex == 0 && colIndex == 0) firstTile else null,
                                    onClick = { onOpenApp(name, app) },
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        InfoCard("Today", Modifier.weight(0.85f).fillMaxHeight()) {
                            Txt(
                                "Your calendar appears here once it's connected (Stage 4).",
                                11.5.sp, Palette.Muted,
                            )
                        }
                        InfoCard("Next up", Modifier.weight(1.25f).fillMaxHeight()) {
                            football.lines.forEachIndexed { i, line ->
                                TeamRow(line, badgeIndex = i)
                            }
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt(date, 11.sp, Palette.Muted, weight = FontWeight.SemiBold, spacing = 2.2.sp)
            RichTxt(
                buildAnnotatedString {
                    append("Good ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Palette.Accent)) { append(part) }
                },
                TextStyle(fontFamily = AppFonts.serif, fontSize = 36.sp, color = Palette.Ink),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Palette.Card)
                    .border(1.dp, Palette.Border, RoundedCornerShape(50))
                    .padding(start = 11.dp, end = 15.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                LineIcon(weather?.icon ?: IconKind.Cloud, Palette.Accent, 24.dp)
                Column {
                    Txt(weather?.let { "${it.tempC}°" } ?: "–°", 16.sp, Palette.Ink, weight = FontWeight.SemiBold)
                    Txt("Horsford · ${weather?.summary ?: "updating"}", 10.sp, Palette.Muted)
                    val detail = weather?.let { w ->
                        listOfNotNull(
                            if (w.highC != null && w.lowC != null) "${w.highC}° / ${w.lowC}°" else null,
                            w.rainChance?.let { "$it% rain" },
                        ).joinToString(" · ")
                    }
                    if (!detail.isNullOrEmpty()) Txt(detail, 10.sp, Palette.Muted)
                }
            }
            Txt(time, 50.sp, Palette.Accent, family = AppFonts.serif)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Txt(text, 10.sp, Palette.Muted, weight = FontWeight.SemiBold, spacing = 2.sp)
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
                Txt(name, 17.sp, Palette.OnTile, weight = FontWeight.Bold)
                if (app == null) Txt("Not installed – press OK to get it", 9.5.sp, Palette.OnTileMuted)
            }
        }
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    TvButton(
        shape = RoundedCornerShape(50),
        background = Palette.ButtonFill,
        idleBorder = Palette.ButtonBorder,
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        focusedScale = 1.06f,
    ) {
        Txt(label, 11.sp, Palette.Ink, Modifier.padding(horizontal = 16.dp), weight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoCard(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .clip(Rounded12)
            .background(Palette.Card)
            .border(1.dp, Palette.Border, Rounded12)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Txt(title, 17.sp, Palette.Ink, family = AppFonts.serif)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Divider))
        content()
    }
}

@Composable
private fun TeamRow(line: TeamLine, badgeIndex: Int) {
    val badge = when (badgeIndex) {
        3 -> Palette.Brass
        1 -> Palette.TileBrown
        else -> Palette.TileGreen
    }
    val badgeText = if (badgeIndex == 3) Color(0xFF2A1C14) else Palette.OnTile
    Row(
        Modifier.fillMaxWidth().height(21.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(badge), contentAlignment = Alignment.Center) {
            Txt(line.initials, 7.sp, badgeText, weight = FontWeight.Bold)
        }
        Txt(line.label, 11.5.sp, Palette.Ink, Modifier.weight(0.75f), weight = FontWeight.SemiBold, maxLines = 1)
        Row(
            Modifier.weight(1.6f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
        ) {
            if (line.live) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Palette.Live))
            }
            Txt(
                line.detail, 11.sp,
                when {
                    line.live -> Palette.Live
                    line.result -> Palette.Accent
                    else -> Palette.Muted
                },
                weight = if (line.live || line.result) FontWeight.Bold else FontWeight.Normal,
                align = TextAlign.End, maxLines = 1,
            )
        }
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
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt("Home", 24.sp, Palette.Ink, Modifier.weight(1f), family = AppFonts.serif)
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Palette.Chip)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Txt("Heating · Stage 2", 10.sp, Palette.Accent, weight = FontWeight.SemiBold)
                }
            }

            // Outdoor camera tile: live view is added in Stage 3.
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(11.dp)).background(Palette.Night),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt("Front garden", 12.sp, Palette.OnNight, weight = FontWeight.SemiBold)
                    Txt("Live camera view arrives in Stage 3", 10.sp, Palette.OnNightMuted, align = TextAlign.Center)
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
                background = Palette.TileBrown,
                onClick = onHomeControls,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                focusedScale = 1.03f,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    LineIcon(IconKind.House, Palette.BrassLight, 15.dp)
                    Txt("All home controls", 12.sp, Palette.OnTile, Modifier.weight(1f), weight = FontWeight.SemiBold)
                    LineIcon(IconKind.Chevron, Palette.OnTile, 13.dp)
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
        background = if (primary) Palette.TileGreen else Palette.ButtonFill.copy(alpha = 0.9f),
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
            LineIcon(icon, if (primary) Palette.BrassLight else Palette.Brass, 15.dp)
            Txt(label, 12.sp, if (primary) Palette.OnTile else Palette.Ink, weight = FontWeight.SemiBold)
        }
    }
}
