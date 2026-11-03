package uk.lloyd.tvhome

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/** The room palette: cream, forest green, chocolate, marble and brass. */
object Palette {
    val Cream = Color(0xFFF2EBDD)
    val CreamLight = Color(0xFFFAF6EE)
    val Marble = Color(0xFFF8F4EC)
    val Vein = Color(0xFFD6C7AE)
    val VeinSoft = Color(0xFFE2D6C1)
    val Forest = Color(0xFF29443A)
    val Chocolate = Color(0xFF3A271D)
    val Brass = Color(0xFFB48A52)
    val BrassLight = Color(0xFFE2C595)
    val Muted = Color(0xFF6E5848)
    val Border = Color(0xFFE4D9C6)
    val ButtonBorder = Color(0xFFC9B99E)
    val Divider = Color(0xFFEAE0CF)
    val Chip = Color(0xFFEDE4D3)
    val Night = Color(0xFF221A15)
    val OnDark = Color(0xFFF7F1E6)
    val OnDarkMuted = Color(0xFFBFAF97)
}

/**
 * Instrument Serif for the big moments, Manrope for everything else.
 * The font files are downloaded by the automatic build; if they are missing
 * the app falls back to the TV's own serif and sans fonts rather than crashing.
 */
object AppFonts {
    var serif: FontFamily = FontFamily.Serif
        private set
    var sans: FontFamily = FontFamily.SansSerif
        private set

    @OptIn(ExperimentalTextApi::class)
    fun init(context: Context) {
        val assets = context.assets
        val files = runCatching { assets.list("fonts")?.toSet() }.getOrNull() ?: emptySet()

        if ("InstrumentSerif-Regular.ttf" in files) {
            val fonts = mutableListOf(Font("fonts/InstrumentSerif-Regular.ttf", assets))
            if ("InstrumentSerif-Italic.ttf" in files) {
                fonts += Font("fonts/InstrumentSerif-Italic.ttf", assets, FontWeight.Normal, FontStyle.Italic)
            }
            serif = FontFamily(fonts)
        }

        if ("Manrope.ttf" in files) {
            // Manrope is a variable font: one file, every weight.
            sans = FontFamily(listOf(400, 500, 600, 700).map { w -> Font("fonts/Manrope.ttf", assets, FontWeight(w)) })
        }
    }
}
