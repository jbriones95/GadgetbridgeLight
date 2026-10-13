package nodomain.freeyourgadget.gadgetbridge.widgets.impl

import android.text.format.DateUtils
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightText
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextVariant
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightThemeTokens
import nodomain.freeyourgadget.gadgetbridge.ui.light.lightGridUnitsAsDp
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import java.util.Date

private data class ActivityDayRange(
    val label: String,
    val start: Long,
    val end: Long,
    val isYesterday: Boolean,
)

private data class ActivityTimelinePeriod(
    val label: String?,
    val start: Long,
    val end: Long,
    val startTimeLabel: String,
    val endTimeLabel: String,
    val reverse: Boolean = false,
    val dim: Boolean = false,
    val showCurrentTime: Boolean = false,
)

/** A compact, monochrome day rail with readable labels and no circular/chart chrome. */
@Composable
fun TodayActivityTimeline(
    data: TodayWidget.Data,
    mode24h: Boolean,
    reverse24h: Boolean,
    showYesterday: Boolean,
    dimYesterday: Boolean,
    showCurrentTime: Boolean,
) {
    val context = LocalContext.current
    val dayEnd = data.timeTo.toLong()
    val selectedDayStart = dayEnd - 86_400L
    val includeYesterday = showYesterday && data.chartTimeFrom < selectedDayStart
    val selectedDayLabel = if (DateUtils.isToday(dayEnd * 1000L)) {
        context.getString(R.string.activity_summary_today)
    } else {
        DateTimeUtils.formatDate(Date(selectedDayStart * 1000L), DateUtils.FORMAT_SHOW_WEEKDAY)
    }
    val days = buildList {
        if (includeYesterday) {
            add(ActivityDayRange(context.getString(R.string.activity_summary_yesterday), data.chartTimeFrom, selectedDayStart, true))
        }
        add(ActivityDayRange(selectedDayLabel, selectedDayStart, dayEnd, false))
    }
    val textColor = LightThemeTokens.colors.content
    val timeFormat = DateFormat.getTimeFormat(context)
    fun formattedTime(seconds: Long) = timeFormat.format(Date(seconds * 1000L))

    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 1f.lightGridUnitsAsDp(), vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            if (mode24h) {
                ActivityTimelinePeriodView(
                    data = data,
                    dayLabel = day.label,
                    period = ActivityTimelinePeriod(
                        label = null,
                        start = day.start,
                        end = day.end,
                        startTimeLabel = formattedTime(if (reverse24h) day.end + 1 else day.start + 1),
                        endTimeLabel = formattedTime(if (reverse24h) day.start + 1 else day.end + 1),
                        reverse = reverse24h,
                        dim = day.isYesterday && dimYesterday,
                        showCurrentTime = showCurrentTime && !day.isYesterday,
                    ),
                    foreground = textColor,
                )
            } else {
                val midpoint = day.start + (day.end - day.start) / 2
                ActivityTimelinePeriodView(
                    data = data,
                    dayLabel = day.label,
                    period = ActivityTimelinePeriod(
                        label = context.getString(R.string.today_widget_am),
                        start = day.start,
                        end = midpoint,
                        startTimeLabel = formattedTime(day.start + 1),
                        endTimeLabel = formattedTime(midpoint + 1),
                        dim = day.isYesterday && dimYesterday,
                        showCurrentTime = showCurrentTime && !day.isYesterday,
                    ),
                    foreground = textColor,
                )
                ActivityTimelinePeriodView(
                    data = data,
                    dayLabel = day.label,
                    period = ActivityTimelinePeriod(
                        label = context.getString(R.string.today_widget_pm),
                        start = midpoint,
                        end = day.end,
                        startTimeLabel = formattedTime(midpoint + 1),
                        endTimeLabel = formattedTime(day.end + 1),
                        dim = day.isYesterday && dimYesterday,
                        showCurrentTime = showCurrentTime && !day.isYesterday,
                    ),
                    foreground = textColor,
                )
            }
        }
    }
}

@Composable
private fun ActivityTimelinePeriodView(
    data: TodayWidget.Data,
    dayLabel: String,
    period: ActivityTimelinePeriod,
    foreground: Color,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        val heading = if (period.label == null) dayLabel else "$dayLabel · ${period.label}"
        LightText(heading, LightTextVariant.Detail, secondary = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LightText(period.startTimeLabel, LightTextVariant.Detail, secondary = true, maxLines = 1)
            LightText(period.endTimeLabel, LightTextVariant.Detail, secondary = true, maxLines = 1)
        }
        Canvas(
            Modifier.fillMaxWidth()
                .height(12.dp)
                .semantics {
                    contentDescription = context.getString(
                        R.string.today_widget_timeline_accessibility,
                        heading,
                        period.startTimeLabel,
                        period.endTimeLabel,
                    )
                },
        ) {
            val centerY = size.height / 2f
            val baseColor = foreground.copy(alpha = 0.18f)
            drawLine(baseColor, Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = 2.dp.toPx())
            for (tick in 0..4) {
                val x = size.width * tick / 4f
                drawLine(baseColor, Offset(x, centerY - 4.dp.toPx()), Offset(x, centerY + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
            }

            val duration = (period.end - period.start).coerceAtLeast(1L).toFloat()
            for (activity in data.activities) {
                if (activity.timeTo <= period.start || activity.timeFrom >= period.end) continue
                val from = activity.timeFrom.coerceAtLeast(period.start)
                val to = activity.timeTo.coerceAtMost(period.end)
                val first = ((from - period.start) / duration).coerceIn(0f, 1f)
                val last = ((to - period.start) / duration).coerceIn(0f, 1f)
                val startFraction = if (period.reverse) 1f - last else first
                val endFraction = if (period.reverse) 1f - first else last
                val color = foreground.copy(alpha = activityAlpha(activity.activityKind) * if (period.dim) 0.32f else 1f)
                drawLine(
                    color = color,
                    start = Offset(size.width * startFraction, centerY),
                    end = Offset(size.width * endFraction, centerY),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Butt,
                )
            }

            val now = System.currentTimeMillis() / 1000L
            if (period.showCurrentTime && now in period.start until period.end) {
                val fraction = ((now - period.start) / duration).coerceIn(0f, 1f)
                val x = size.width * if (period.reverse) 1f - fraction else fraction
                drawLine(foreground, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
            }
        }
    }
}

private fun activityAlpha(kind: ActivityKind): Float = when (kind) {
    ActivityKind.NOT_WORN -> 0.28f
    ActivityKind.NOT_MEASURED -> 0.45f
    ActivityKind.LIGHT_SLEEP, ActivityKind.SLEEP_ANY -> 0.58f
    ActivityKind.DEEP_SLEEP -> 0.78f
    ActivityKind.REM_SLEEP -> 0.9f
    ActivityKind.AWAKE_SLEEP -> 0.7f
    ActivityKind.ACTIVITY -> 0.86f
    ActivityKind.EXERCISE -> 1f
    else -> 0f
}
