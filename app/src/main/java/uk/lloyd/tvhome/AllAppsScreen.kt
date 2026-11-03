package uk.lloyd.tvhome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Every installed app, so nothing is out of reach once the Home button opens TV Home. */
@Composable
fun AllAppsScreen(apps: List<TvApp>, onOpen: (TvApp) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(150)
            runCatching { first.requestFocus() }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Palette.Cream).padding(horizontal = 44.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Txt("All apps", 34.sp, Palette.Chocolate, family = AppFonts.serif)
        Txt("Press Back to return home", 10.sp, Palette.Muted)
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                TvButton(
                    shape = RoundedCornerShape(10.dp),
                    background = if (index % 2 == 0) Palette.Forest else Palette.Chocolate,
                    onClick = { onOpen(app) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    focusRequester = if (index == 0) first else null,
                    focusedScale = 1.06f,
                ) {
                    val banner = app.banner
                    val icon = app.icon
                    if (banner != null) {
                        Image(
                            bitmap = banner,
                            contentDescription = app.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(6.dp),
                        ) {
                            if (icon != null) {
                                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.height(4.dp))
                            }
                            Txt(
                                app.label, 10.sp, Palette.OnDark,
                                weight = FontWeight.SemiBold, align = TextAlign.Center, maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}
