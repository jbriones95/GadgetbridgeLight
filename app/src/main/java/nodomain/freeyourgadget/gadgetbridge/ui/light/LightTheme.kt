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
    title = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Light, lineHeight = 28.sp),
    heading = TextStyle(fontSize = 20.sp, lineHeight = 24.sp),
    copy = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    button = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp, letterSpacing = 0.56.sp),
    detail = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
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

/** Fixed vertical rhythm; the horizontal grid must not make rows taller on wider phones. */
object LightSpacing {
    val rowVertical = 4.dp
    val section = 12.dp
    val textGap = 2.dp
    val control = 40.dp
}

@Composable
fun Float.lightGridUnitsAsDp(): Dp =
    (LocalConfiguration.current.screenWidthDp.toFloat() / LightGrid.WIDTH * this).dp
