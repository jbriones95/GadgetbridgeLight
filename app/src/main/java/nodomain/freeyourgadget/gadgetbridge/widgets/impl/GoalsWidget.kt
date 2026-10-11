package nodomain.freeyourgadget.gadgetbridge.widgets.impl

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.widgets.GBWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetConfig
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetDataScope

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
        inflater.inflate(R.layout.dashboard_widget_goals, parent, false)

    override fun bind(view: View, config: WidgetConfig, data: Data) {
        val summary = view.findViewById<TextView>(R.id.dashboard_goals_summary)
        val context = view.context
        summary.text = listOf(
            "${context.getString(R.string.steps)}  ${data.steps}  ${percent(data.stepsFactor)}",
            "${context.getString(R.string.distance)}  ${formatDistance(data.distanceMeters)}  ${percent(data.distanceFactor)}",
            "${context.getString(R.string.activity_list_summary_active_time)}  ${formatMinutes(data.activeMinutes)}  ${percent(data.activeMinutesFactor)}",
            "${context.getString(R.string.menuitem_sleep)}  ${formatMinutes(data.sleepMinutes)}  ${percent(data.sleepFactor)}",
        ).joinToString("\n")
    }

    private fun percent(factor: Float): String = "${(factor * 100).toInt()}%"

    private fun formatDistance(meters: Float): String =
        if (meters >= 1000f) "%.1f km".format(meters / 1000f) else "${meters.toInt()} m"

    private fun formatMinutes(minutes: Long): String = "${minutes / 60}h ${minutes % 60}m"

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
