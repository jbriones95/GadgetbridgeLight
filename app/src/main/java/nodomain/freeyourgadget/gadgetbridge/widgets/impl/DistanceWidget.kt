package nodomain.freeyourgadget.gadgetbridge.widgets.impl

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetColors
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetDataScope
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetConfig
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightMetric

/**
 * Total distance for the day against the user's distance goal.
 */
object DistanceWidget : GaugeWidget<DistanceWidget.Data>() {
    override val id = "distance"
    override val label = R.string.distance
    override val icon = R.drawable.ic_distance
    override val chartTab = "stepsweek"

    override fun isSupportedBy(device: GBDevice): Boolean =
        device.deviceCoordinator.supportsStepCounter(device)

    override suspend fun loadData(scope: WidgetDataScope, config: WidgetConfig): Data {
        val total = scope.distanceTotal()
        return Data(total, scope.distanceGoalFactor(total))
    }

    override fun lightMetric(context: android.content.Context, data: Data, title: String) =
        LightMetric(label = title, value = FormatUtils.getFormattedDistanceLabel(data.total.toDouble()), progress = data.goalFactor)

    data class Data(val total: Float, val goalFactor: Float)
}
