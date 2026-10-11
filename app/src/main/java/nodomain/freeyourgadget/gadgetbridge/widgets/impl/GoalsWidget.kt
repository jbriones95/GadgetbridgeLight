package nodomain.freeyourgadget.gadgetbridge.widgets.impl

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils
import nodomain.freeyourgadget.gadgetbridge.widgets.GBWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetConfig
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetDataScope
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightMetric
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightMetricSection
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme

object GoalsWidget : GBWidget<GoalsWidget.Data> {
    override val id = "goals"
    override val label = R.string.pref_dashboard_widget_goals_chart_title
    override val icon = R.drawable.ic_done_all
    override val defaultColumns = 2
    override val allowedColumns = listOf(1, 2)

    override fun isSupportedBy(device: GBDevice): Boolean =
        device.deviceCoordinator.supportsActivityTracking(device)

    override fun settings(context: Context): DeviceSettingsSpec = deviceSettings {
        switchSetting(
            key = "legend",
            title = R.string.pref_dashboard_widget_show_legend_title,
            summary = R.string.pref_dashboard_widget_show_legend_summary,
            icon = R.drawable.ic_legend_toggle,
            defaultValue = true,
        )
    }

    override suspend fun loadData(scope: WidgetDataScope, config: WidgetConfig): Data {
        val steps = scope.stepsTotal()
        val distance = scope.distanceTotal()
        val activeMinutes = scope.activeMinutesTotal()
        val sleepMinutes = scope.sleepMinutesTotal()
        return Data(
            steps = steps,
            distanceMeters = distance,
            activeMinutes = activeMinutes,
            sleepMinutes = sleepMinutes,
            stepsFactor = scope.stepsGoalFactor(steps),
            distanceFactor = scope.distanceGoalFactor(distance),
            activeMinutesFactor = scope.activeMinutesGoalFactor(activeMinutes),
            sleepFactor = scope.sleepGoalFactor(sleepMinutes),
        )
    }

    override fun createView(inflater: LayoutInflater, parent: ViewGroup): View =
        ComposeView(inflater.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

    override fun bind(view: View, config: WidgetConfig, data: Data) {
        val composeView = view as ComposeView
        val context = view.context
        composeView.setContent {
            LightTheme {
                LightMetricSection(
                    title = config.title(context, this),
                    metrics = listOf(
                        LightMetric(context.getString(R.string.steps), java.text.NumberFormat.getInstance().format(data.steps), detail = percentIfEnabled(config, data.stepsFactor), progress = safeProgress(data.stepsFactor)),
                        LightMetric(context.getString(R.string.distance), formatDistance(data.distanceMeters), detail = percentIfEnabled(config, data.distanceFactor), progress = safeProgress(data.distanceFactor)),
                        LightMetric(context.getString(R.string.activity_list_summary_active_time), formatMinutes(data.activeMinutes), detail = percentIfEnabled(config, data.activeMinutesFactor), progress = safeProgress(data.activeMinutesFactor)),
                        LightMetric(context.getString(R.string.menuitem_sleep), formatMinutes(data.sleepMinutes), detail = percentIfEnabled(config, data.sleepFactor), progress = safeProgress(data.sleepFactor)),
                    ),
                )
            }
        }
    }

    private fun percent(factor: Float): String = "${(safeProgress(factor) * 100).toInt()}%"

    private fun percentIfEnabled(config: WidgetConfig, factor: Float): String? =
        if (config.prefs.getBoolean("legend", true)) percent(factor) else null

    private fun formatDistance(meters: Float): String = FormatUtils.getFormattedDistanceLabel(meters.toDouble())

    private fun formatMinutes(minutes: Long): String = "${minutes / 60}h ${minutes % 60}m"

    private fun safeProgress(value: Float): Float = value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f

    data class Data(
        val steps: Int,
        val distanceMeters: Float,
        val activeMinutes: Long,
        val sleepMinutes: Long,
        val stepsFactor: Float,
        val distanceFactor: Float,
        val activeMinutesFactor: Float,
        val sleepFactor: Float,
    )
}
