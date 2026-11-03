package uk.lloyd.tvhome

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalTime

/**
 * The room palette, by role. Each colour has a day version (cream, forest green, chocolate,
 * marble) and a soft evening version (warm dark chocolate, muted sage, candle-brass).
 * Because [evening] is Compose state, flipping it repaints the whole screen automatically.
 */
object Palette {
    var evening by mutableStateOf(false)

    private fun pick(day: Long, eve: Long) = Color(if (evening) eve else day)

    /** Page background. */
    val Bg: Color get() = pick(0xFFF2EBDD, 0xFF2A211B)
    /** Cards (Today, Next up, status lines). */
    val Card: Color get() = pick(0xFFFAF6EE, 0xFF342920)
    /** The marble Home panel. */
    val Marble: Color get() = pick(0xFFF8F4EC, 0xFF382C23)
    val Vein: Color get() = pick(0xFFD6C7AE, 0xFF4F4034)
    val VeinSoft: Color get() = pick(0xFFE2D6C1, 0xFF45372B)

    /** Main text. */
    val Ink: Color get() = pick(0xFF3A271D, 0xFFF1E6D3)
    /** Quieter text. */
    val Muted: Color get() = pick(0xFF6E5848, 0xFFBCA88F)
    /** Green text: the clock, headings, highlights. */
    val Accent: Color get() = pick(0xFF29443A, 0xFF9DBFA9)

    /** Button and tile fills. */
    val TileGreen: Color get() = pick(0xFF29443A, 0xFF2F4B40)
    val TileBrown: Color get() = pick(0xFF3A271D, 0xFF4A3426)
    val OnTile: Color get() = pick(0xFFF7F1E6, 0xFFF1E6D3)
    val OnTileMuted: Color get() = pick(0xFFBFAF97, 0xFFBCA88F)

    /** Focus ring and icons. */
    val Brass: Color get() = pick(0xFFB48A52, 0xFFC9A26B)
    val BrassLight: Color get() = Color(0xFFE2C595)

    val Border: Color get() = pick(0xFFE4D9C6, 0xFF4A3B30)
    val ButtonBorder: Color get() = pick(0xFFC9B99E, 0xFF5E4B3D)
    val Divider: Color get() = pick(0xFFEAE0CF, 0xFF43362B)
    val Chip: Color get() = pick(0xFFEDE4D3, 0xFF43362B)
    val ButtonFill: Color get() = pick(0xFFFAF6EE, 0xFF3A2E25)

    /** The camera tile and command boxes: always dark. */
    val Night: Color get() = Color(0xFF221A15)
    val OnNight: Color get() = Color(0xFFF7F1E6)
    val OnNightMuted: Color get() = Color(0xFFBFAF97)

    /** Live score marker. */
    val Live: Color get() = pick(0xFFB24A30, 0xFFE07A5F)
}

enum class PaletteMode(val label: String) {
    Auto("Evening colours: automatic, 9pm–7am"),
    Day("Evening colours: off (always day)"),
    Evening("Evening colours: always on"),
}

fun shouldUseEvening(mode: PaletteMode, now: LocalTime = LocalTime.now()): Boolean = when (mode) {
    PaletteMode.Auto -> now.hour >= 21 || now.hour < 7
    PaletteMode.Day -> false
    PaletteMode.Evening -> true
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
