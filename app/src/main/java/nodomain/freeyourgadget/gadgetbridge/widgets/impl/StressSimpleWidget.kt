package nodomain.freeyourgadget.gadgetbridge.widgets.impl

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StressFragment
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetDataScope
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetConfig
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightMetric

/**
 * Current/average stress as a simple gauge.
 */
object StressSimpleWidget : GaugeWidget<StressData?>() {
    override val id = "stress_simple"
    override val label = R.string.menuitem_stress
    override val name = R.string.menuitem_stress_simple
    override val icon = R.drawable.ic_stress
    override val chartTab = "stress"

    override fun isSupportedBy(device: GBDevice): Boolean =
        device.deviceCoordinator.supportsStressMeasurement(device)

    override suspend fun loadData(scope: WidgetDataScope, config: WidgetConfig): StressData? = computeStress(scope)

    override fun lightMetric(context: Context, data: StressData?, title: String): LightMetric {
        if (data == null) return LightMetric(title, context.getString(R.string.stats_empty_value))
        val status = StressFragment.StressType.fromStress(data.value, data.ranges).getLabel(context)
        return LightMetric(title, data.value.toString(), detail = status, progress = data.value / 100f)
    }

    override fun draw(context: Context, gaugeValue: TextView, gaugeBar: ImageView, data: StressData?) {
        if (data == null) {
            drawSimpleGauge(gaugeBar, 0, -1f)
            return
        }

        val color = StressFragment.StressType.fromStress(data.value, data.ranges).getColor(context)
        gaugeValue.text = data.value.toString()
        drawSimpleGauge(gaugeBar, color, data.value / 100f)
    }
}
