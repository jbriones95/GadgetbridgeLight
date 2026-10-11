package nodomain.freeyourgadget.gadgetbridge.ui.light

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

val LocalLightColors = staticCompositionLocalOf { LightThemeColors.Dark }
val LocalLightTypography = staticCompositionLocalOf {
    LightTypography(
        title = TextStyle(fontSize = 24.sp),
        heading = TextStyle(fontSize = 20.sp),
        copy = TextStyle(fontSize = 16.sp),
        button = TextStyle(fontSize = 14.sp),
        detail = TextStyle(fontSize = 12.sp),
    )
}

object LightThemeTokens {
    val colors: LightColors
        @Composable get() = LocalLightColors.current
    val typography: LightTypography
        @Composable get() = LocalLightTypography.current
}

@Composable
fun LightTheme(
    colors: LightColors = LightThemeColors.Dark,
    typography: LightTypography? = null,
    content: @Composable () -> Unit,
) {
    val resolvedTypography = typography ?: rememberLightTypography()
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLightColors provides colors,
        LocalLightTypography provides resolvedTypography,
        content = content,
    )
}

@Composable
private fun rememberLightTypography(): LightTypography {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val context = LocalContext.current
    val fontFamily = remember(context) { resolveLightFontFamily() }
    fun scaled(designPx: Float) = (designPx * screenHeightDp / 600f).sp
    return LightTypography(
        title = TextStyle(fontSize = scaled(115f), fontFamily = fontFamily, fontWeight = FontWeight.Light, lineHeight = scaled(126.5f)),
        heading = TextStyle(fontSize = scaled(38f), fontFamily = fontFamily, fontWeight = FontWeight.Normal, lineHeight = scaled(51f)),
        copy = TextStyle(fontSize = scaled(30f), fontFamily = fontFamily, fontWeight = FontWeight.Normal, lineHeight = scaled(45f)),
        button = TextStyle(fontSize = scaled(25f), fontFamily = fontFamily, fontWeight = FontWeight.Medium, letterSpacing = scaled(0.75f), lineHeight = scaled(29f)),
        detail = TextStyle(fontSize = scaled(20f), fontFamily = fontFamily, fontWeight = FontWeight.Normal, lineHeight = scaled(29f)),
    )
}

private fun resolveLightFontFamily(): FontFamily {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return FontFamily.Default
    return runCatching {
        val fonts = android.graphics.fonts.SystemFonts.getAvailableFonts()
            .filter { it.file?.name?.startsWith("Akkurat", ignoreCase = true) == true }
            .mapNotNull { font ->
                val file = font.file ?: return@mapNotNull null
                Font(
                    file = file,
                    weight = FontWeight(font.style.weight),
                    style = if (font.style.slant != 0) FontStyle.Italic else FontStyle.Normal,
                )
            }
        if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
    }.getOrDefault(FontFamily.Default)
}

/** The 27-column horizontal grid used by the Light frontend. */
object LightGrid {
    const val WIDTH = 27
    const val HEIGHT = 31
}

/** Fixed vertical rhythm; the horizontal grid must not make rows taller on wider phones. */
object LightSpacing {
    val rowVertical = 6.dp
    val section = 16.dp
    val textGap = 4.dp
    val control = 40.dp
}

@Composable
fun Float.lightGridUnitsAsDp(): Dp =
    (LocalConfiguration.current.screenWidthDp.toFloat() / LightGrid.WIDTH * this).dp
