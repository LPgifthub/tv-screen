package uk.lloyd.tvhome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/** Plain text in the app's fonts. */
@Composable
fun Txt(
    text: String,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = AppFonts.sans,
    spacing: TextUnit = TextUnit.Unspecified,
    italic: Boolean = false,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            fontFamily = family,
            letterSpacing = spacing,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textAlign = align,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Rich text (mixed styles) in the app's fonts. */
@Composable
fun RichTxt(text: AnnotatedString, style: TextStyle, modifier: Modifier = Modifier) {
    BasicText(text = text, modifier = modifier, style = style, maxLines = 1)
}

/**
 * Anything you can select with the remote: grows slightly and gets a brass ring when focused,
 * and runs onClick when OK is pressed.
 */
@Composable
fun TvButton(
    shape: Shape,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    idleBorder: Color? = null,
    focusRequester: FocusRequester? = null,
    focusedScale: Float = 1.045f,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, label = "focusScale")
    val requester = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    val border = when {
        focused -> Modifier.border(3.dp, Palette.Brass, shape)
        idleBorder != null -> Modifier.border(1.dp, idleBorder, shape)
        else -> Modifier
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (focused) 14.dp else 0.dp, shape, clip = false)
            .clip(shape)
            .background(background)
            .then(border)
            .then(requester)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = contentAlignment,
    ) {
        content(focused)
    }
}

enum class IconKind { Film, Flame, Sunrise, Power, House, Chevron, Cloud, Sun, Rain }

/** Simple line icons drawn in code, so there are no image files to manage. */
@Composable
fun LineIcon(kind: IconKind, color: Color, size: Dp = 14.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(block: Path.() -> Unit) = drawPath(Path().apply(block), color, style = stroke)

        when (kind) {
            IconKind.Film -> {
                drawRoundRect(
                    color, topLeft = Offset(3 * s, 6 * s), size = Size(18 * s, 13 * s),
                    cornerRadius = CornerRadius(2 * s), style = stroke,
                )
                line { moveTo(3 * s, 10 * s); lineTo(21 * s, 10 * s) }
                line { moveTo(8 * s, 6 * s); lineTo(10 * s, 10 * s) }
                line { moveTo(14 * s, 6 * s); lineTo(16 * s, 10 * s) }
            }
            IconKind.Flame -> line {
                moveTo(12 * s, 3 * s)
                cubicTo(13 * s, 6.5f * s, 17 * s, 8.5f * s, 17 * s, 13 * s)
                cubicTo(17 * s, 16 * s, 14.8f * s, 18.5f * s, 12 * s, 18.5f * s)
                cubicTo(9.2f * s, 18.5f * s, 7 * s, 16 * s, 7 * s, 13 * s)
                cubicTo(7 * s, 10.5f * s, 8.5f * s, 9.5f * s, 9.5f * s, 8 * s)
                cubicTo(10 * s, 9.5f * s, 11 * s, 10 * s, 12 * s, 10 * s)
                cubicTo(12 * s, 8 * s, 11 * s, 5.5f * s, 12 * s, 3 * s)
                close()
            }
            IconKind.Sunrise -> {
                line { moveTo(4 * s, 18 * s); lineTo(20 * s, 18 * s) }
                drawArc(
                    color, 180f, 180f, false,
                    topLeft = Offset(7 * s, 13 * s), size = Size(10 * s, 10 * s), style = stroke,
                )
                line { moveTo(12 * s, 4 * s); lineTo(12 * s, 7 * s) }
                line { moveTo(5.2f * s, 8.2f * s); lineTo(7.2f * s, 10.2f * s) }
                line { moveTo(18.8f * s, 8.2f * s); lineTo(16.8f * s, 10.2f * s) }
            }
            IconKind.Power -> {
                line { moveTo(12 * s, 3 * s); lineTo(12 * s, 11 * s) }
                drawArc(
                    color, -60f, 300f, false,
                    topLeft = Offset(5 * s, 5.5f * s), size = Size(14 * s, 14 * s), style = stroke,
                )
            }
            IconKind.House -> line {
                moveTo(3 * s, 11 * s); lineTo(12 * s, 4 * s); lineTo(21 * s, 11 * s)
                moveTo(5 * s, 10 * s); lineTo(5 * s, 20 * s); lineTo(19 * s, 20 * s); lineTo(19 * s, 10 * s)
            }
            IconKind.Chevron -> line {
                moveTo(9 * s, 6 * s); lineTo(15 * s, 12 * s); lineTo(9 * s, 18 * s)
            }
            IconKind.Sun -> {
                drawCircle(color, radius = 4.5f * s, center = Offset(12 * s, 12 * s), style = stroke)
                line { moveTo(12 * s, 2.5f * s); lineTo(12 * s, 4.5f * s) }
                line { moveTo(12 * s, 19.5f * s); lineTo(12 * s, 21.5f * s) }
                line { moveTo(2.5f * s, 12 * s); lineTo(4.5f * s, 12 * s) }
                line { moveTo(19.5f * s, 12 * s); lineTo(21.5f * s, 12 * s) }
                line { moveTo(5.3f * s, 5.3f * s); lineTo(6.7f * s, 6.7f * s) }
                line { moveTo(17.3f * s, 17.3f * s); lineTo(18.7f * s, 18.7f * s) }
                line { moveTo(18.7f * s, 5.3f * s); lineTo(17.3f * s, 6.7f * s) }
                line { moveTo(6.7f * s, 17.3f * s); lineTo(5.3f * s, 18.7f * s) }
            }
            IconKind.Rain -> {
                line {
                    moveTo(7 * s, 15 * s)
                    cubicTo(4.5f * s, 15 * s, 3.5f * s, 13 * s, 3.5f * s, 11.5f * s)
                    cubicTo(3.5f * s, 9.5f * s, 5 * s, 8 * s, 7 * s, 8 * s)
                    cubicTo(7.8f * s, 5.5f * s, 9.8f * s, 4 * s, 12.2f * s, 4 * s)
                    cubicTo(15.2f * s, 4 * s, 17.4f * s, 6.2f * s, 17.6f * s, 9 * s)
                    cubicTo(19.4f * s, 9.2f * s, 20.5f * s, 10.5f * s, 20.5f * s, 12 * s)
                    cubicTo(20.5f * s, 13.7f * s, 19.2f * s, 15 * s, 17.5f * s, 15 * s)
                    close()
                }
                line { moveTo(8 * s, 18 * s); lineTo(7 * s, 21 * s) }
                line { moveTo(12.5f * s, 18 * s); lineTo(11.5f * s, 21 * s) }
                line { moveTo(17 * s, 18 * s); lineTo(16 * s, 21 * s) }
            }
            IconKind.Cloud -> {
                line { moveTo(12 * s, 2.5f * s); lineTo(12 * s, 4.5f * s) }
                line { moveTo(5.3f * s, 5.3f * s); lineTo(6.7f * s, 6.7f * s) }
                line { moveTo(2.5f * s, 12 * s); lineTo(4.5f * s, 12 * s) }
                line {
                    moveTo(8 * s, 20 * s); lineTo(17 * s, 20 * s)
                    cubicTo(19 * s, 20 * s, 20.5f * s, 18.5f * s, 20.5f * s, 16.5f * s)
                    cubicTo(20.5f * s, 14.5f * s, 19 * s, 13 * s, 17 * s, 13 * s)
                    lineTo(16.4f * s, 13 * s)
                    cubicTo(15.6f * s, 10.6f * s, 13.5f * s, 9.5f * s, 11.5f * s, 9.5f * s)
                    cubicTo(9 * s, 9.5f * s, 7.2f * s, 11.6f * s, 7 * s, 14.5f * s)
                    cubicTo(5.3f * s, 14.8f * s, 5 * s, 16.3f * s, 5 * s, 17 * s)
                    cubicTo(5 * s, 18.7f * s, 6.3f * s, 20 * s, 8 * s, 20 * s)
                }
            }
        }
    }
}

/** Soft marble veining for the Home panel, matching the worktops. */
@Composable
fun MarbleVeins(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val fx = size.width / 600f
        val fy = size.height / 800f
        fun vein(color: Color, width: Float, block: Path.() -> Unit) =
            drawPath(Path().apply(block), color, style = Stroke(width = width * fx))

        vein(Palette.Vein, 2.2f) {
            moveTo(-30 * fx, 90 * fy)
            cubicTo(90 * fx, 60 * fy, 170 * fx, 170 * fy, 300 * fx, 140 * fy)
            cubicTo(430 * fx, 110 * fy, 520 * fx, 40 * fy, 640 * fx, 110 * fy)
        }
        vein(Palette.VeinSoft, 1f) {
            moveTo(-30 * fx, 104 * fy)
            cubicTo(100 * fx, 80 * fy, 180 * fx, 180 * fy, 300 * fx, 156 * fy)
        }
        vein(Palette.VeinSoft, 1f) {
            moveTo(200 * fx, 160 * fy)
            cubicTo(230 * fx, 230 * fy, 180 * fx, 300 * fy, 240 * fx, 380 * fy)
        }
        vein(Palette.Vein, 1.6f) {
            moveTo(-30 * fx, 560 * fy)
            cubicTo(80 * fx, 500 * fy, 200 * fx, 640 * fy, 340 * fx, 570 * fy)
            cubicTo(480 * fx, 500 * fy, 520 * fx, 460 * fy, 640 * fx, 520 * fy)
        }
        vein(Palette.VeinSoft, 1f) {
            moveTo(340 * fx, 570 * fy)
            cubicTo(380 * fx, 630 * fy, 330 * fx, 700 * fy, 400 * fx, 760 * fy)
        }
        vein(Palette.Vein, 1.2f) {
            moveTo(90 * fx, 820 * fy)
            cubicTo(160 * fx, 720 * fy, 290 * fx, 750 * fy, 380 * fx, 680 * fy)
            cubicTo(470 * fx, 610 * fy, 520 * fx, 620 * fy, 640 * fx, 650 * fy)
        }
        vein(Palette.VeinSoft, 1f) {
            moveTo(430 * fx, -10 * fy)
            cubicTo(400 * fx, 70 * fy, 480 * fx, 120 * fy, 455 * fx, 210 * fy)
            cubicTo(430 * fx, 300 * fy, 500 * fx, 300 * fy, 470 * fx, 360 * fy)
        }
    }
}
