package nodomain.freeyourgadget.gadgetbridge.ui.light

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.unit.dp

enum class LightTextVariant { Title, Heading, Copy, Button, Detail }

@Composable
fun LightText(
    text: String,
    variant: LightTextVariant = LightTextVariant.Copy,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val style: TextStyle = when (variant) {
        LightTextVariant.Title -> LightThemeTokens.typography.title
        LightTextVariant.Heading -> LightThemeTokens.typography.heading
        LightTextVariant.Copy -> LightThemeTokens.typography.copy
        LightTextVariant.Button -> LightThemeTokens.typography.button
        LightTextVariant.Detail -> LightThemeTokens.typography.detail
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = (if (textAlign == null) style else style.copy(textAlign = textAlign)).copy(
            color = if (secondary) LightThemeTokens.colors.contentSecondary else LightThemeTokens.colors.content,
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

data class LightTopBarAction(
    val label: String,
    val contentDescription: String = label,
    val onClick: (() -> Unit)? = null,
)

sealed interface LightTopBarCenter {
    data class Text(val value: String, val onClick: (() -> Unit)? = null) : LightTopBarCenter
    data class TwoLine(val first: String, val second: String, val onClick: (() -> Unit)? = null) : LightTopBarCenter
}

@Composable
fun LightTopBar(
    leftAction: LightTopBarAction? = null,
    center: LightTopBarCenter? = null,
    rightAction: LightTopBarAction? = null,
    modifier: Modifier = Modifier,
) {
    val height = LightSpacing.control
    Box(modifier.fillMaxWidth().height(height).padding(horizontal = 1f.lightGridUnitsAsDp())) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            LightTopBarActionView(leftAction, Modifier.widthIn(max = 72.dp))
            Spacer(Modifier.weight(1f))
            LightTopBarActionView(rightAction, Modifier.widthIn(max = 72.dp))
        }
        Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            when (val value = center) {
                is LightTopBarCenter.Text -> LightText(
                    value.value, LightTextVariant.Button,
                    Modifier.widthIn(max = 160.dp).clickable(enabled = value.onClick != null) { value.onClick?.invoke() },
                    textAlign = TextAlign.Center,
                )
                is LightTopBarCenter.TwoLine -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LightText(value.first, LightTextVariant.Detail)
                    LightText(value.second, LightTextVariant.Detail)
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun LightTopBarActionView(action: LightTopBarAction?, modifier: Modifier) {
    if (action == null) return
    LightText(
        text = action.label,
        variant = LightTextVariant.Button,
        modifier = modifier
            .heightIn(min = LightSpacing.control)
            .clickable(enabled = action.onClick != null) { action.onClick?.invoke() }
            .padding(horizontal = 4.dp),
    )
}

@Composable
fun LightRow(
    title: String,
    detail: String? = null,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = LightSpacing.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            LightText(title)
            if (detail != null) LightText(detail, LightTextVariant.Detail, secondary = true)
        }
        if (trailing != null) LightText(
            trailing,
            LightTextVariant.Detail,
            Modifier.widthIn(max = 96.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun LightSectionLabel(text: String, modifier: Modifier = Modifier) {
    LightText(text, LightTextVariant.Detail, modifier.padding(top = LightSpacing.section, bottom = LightSpacing.textGap), secondary = true)
}

@Composable
fun LightStateMessage(message: String, modifier: Modifier = Modifier) {
    LightText(message, modifier = modifier.padding(vertical = LightSpacing.section))
}

data class LightMetric(
    val label: String,
    val value: String,
    val detail: String? = null,
    val target: String? = null,
    val progress: Float? = null,
)

@Composable
fun LightMetricRow(metric: LightMetric, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = LightSpacing.rowVertical)) {
        Row(Modifier.fillMaxWidth()) {
            LightText(metric.label, LightTextVariant.Detail, Modifier.weight(1f))
            LightText(metric.value, modifier = Modifier.widthIn(max = 160.dp), textAlign = TextAlign.End)
        }
        if (metric.target != null && metric.progress != null) {
            LightText("${metric.value} / ${metric.target}", LightTextVariant.Detail, secondary = true)
            Spacer(Modifier.height(LightSpacing.textGap))
            LightProgressBar(metric.progress)
        } else if (metric.progress != null) {
            if (metric.detail != null) LightText(metric.detail, LightTextVariant.Detail, secondary = true)
            Spacer(Modifier.height(LightSpacing.textGap))
            LightProgressBar(metric.progress)
        } else if (metric.detail != null) {
            LightText(metric.detail, LightTextVariant.Detail, secondary = true)
        }
    }
}

@Composable
fun LightDetailFrame(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 1f.lightGridUnitsAsDp())) {
        LightText(title, LightTextVariant.Heading)
        if (subtitle != null) {
            LightText(subtitle, LightTextVariant.Detail, secondary = true)
        }
        content()
    }
}

@Composable
fun LightMetricSection(
    title: String,
    metrics: List<LightMetric>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        LightSectionLabel(title)
        if (metrics.isEmpty()) {
            LightStateMessage("No data for this section")
        } else {
            metrics.forEach { LightMetricRow(it) }
        }
    }
}

@Composable
fun LightChartFrame(
    title: String,
    hasData: Boolean,
    emptyMessage: String = "No chart data available",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 1f.lightGridUnitsAsDp())) {
        LightSectionLabel(title)
        if (hasData) content() else LightStateMessage(emptyMessage)
    }
}

@Composable
fun LightProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.5f.lightGridUnitsAsDp()), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(0.1f.lightGridUnitsAsDp()).background(LightThemeTokens.colors.content))
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(0.5f.lightGridUnitsAsDp()).background(LightThemeTokens.colors.content))
    }
}

@Composable
fun LightScrollContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        content = content,
    )
}
