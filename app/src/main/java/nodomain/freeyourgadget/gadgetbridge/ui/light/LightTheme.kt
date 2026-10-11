package nodomain.freeyourgadget.gadgetbridge.ui.light

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration

/** Tokens for the monochrome Light UI. Values are deliberately independent of Material. */
@Immutable
data class LightColors(
    val background: Color,
    val content: Color,
    val contentSecondary: Color,
    val divider: Color = contentSecondary,
)

@Immutable
data class LightTypography(
    val title: TextStyle,
    val heading: TextStyle,
    val copy: TextStyle,
    val button: TextStyle,
    val detail: TextStyle,
)

object LightThemeColors {
    val Dark = LightColors(Color.Black, Color.White, Color(0xFFBBBBBB))
    val Light = LightColors(Color.White, Color.Black, Color(0xFF666666))
}

private val DefaultTypography = LightTypography(
    title = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Light, lineHeight = 42.sp),
    heading = TextStyle(fontSize = 30.sp, lineHeight = 38.sp),
    copy = TextStyle(fontSize = 30.sp, lineHeight = 45.sp),
    button = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.75.sp),
    detail = TextStyle(fontSize = 20.sp, lineHeight = 29.sp),
)

val LocalLightColors = staticCompositionLocalOf { LightThemeColors.Dark }
val LocalLightTypography = staticCompositionLocalOf { DefaultTypography }

object LightThemeTokens {
    val colors: LightColors
        @Composable get() = LocalLightColors.current
    val typography: LightTypography
        @Composable get() = LocalLightTypography.current
}

@Composable
fun LightTheme(
    colors: LightColors = LightThemeColors.Dark,
    typography: LightTypography = DefaultTypography,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLightColors provides colors,
        LocalLightTypography provides typography,
        content = content,
    )
}

/** The 27-column horizontal grid used by the Light frontend. */
object LightGrid {
    const val WIDTH = 27
    const val HEIGHT = 31
}

@Composable
fun Float.lightGridUnitsAsDp(): Dp =
    (LocalConfiguration.current.screenWidthDp.toFloat() / LightGrid.WIDTH * this).dp
