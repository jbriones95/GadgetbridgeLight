package nodomain.freeyourgadget.gadgetbridge.widgets.impl

import android.content.Context
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.toSpannable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightMetric
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightMetricRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer
import nodomain.freeyourgadget.gadgetbridge.widgets.GBWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetActions
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetConfig

/**
 * Base for a compact Light metric widget. Gauge-zone data is represented by a monochrome progress
 * bar plus text rather than a colored Material gauge.
 */
abstract class GaugeWidget<D> : GBWidget<D> {
    /**
     * Gauge widgets are always a single column.
     */
    override val allowedColumns: List<Int> = listOf(1)

    /**
     * Chart tab opened when the widget is tapped, or null if nothing should be opened.
     */
    protected open val chartTab: String? = null

    /**
     * Sets [nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity.EXTRA_MODE].
     **/
    protected open val chartMode: String = ""

    private val gaugeDrawer = GaugeDrawer(GaugeDrawer.Shape.HALF_CIRCLE)

    override fun createView(inflater: LayoutInflater, parent: ViewGroup): View =
        ComposeView(inflater.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

    final override fun bind(view: View, config: WidgetConfig, data: D) {
        val title = config.title(view.context, this)
        val metric = lightMetric(view.context, data, title)
        val progress = metric.progress?.takeIf { it.isFinite() && it >= 0f }?.coerceIn(0f, 1f)
        (view as ComposeView).setContent {
            LightTheme {
                LightMetricRow(metric.copy(progress = progress))
            }
        }
    }

    final override fun onClick(view: View, config: WidgetConfig, timestamp: Int) {
        val tab = chartTab ?: return
        WidgetActions.openChart(view.context, config, this, tab, label, chartMode, timestamp)
    }

    /** Legacy value adapter retained while individual data classes are cleaned up. */
    protected open fun draw(context: Context, gaugeValue: TextView, gaugeBar: ImageView, data: D) = Unit

    /** Every registered gauge supplies an explicit Light metric, including empty state and meaning. */
    abstract fun lightMetric(context: Context, data: D, title: String): LightMetric

    private fun shrinkUnits(gaugeValue: TextView) {
        val text = gaugeValue.text
        if (text is Spanned || text.none { it.isDigit() }) {
            return
        }
        val spannable = text.toSpannable()
        for (match in UNIT_REGEX.findAll(text)) {
            spannable.setSpan(
                RelativeSizeSpan(UNIT_SIZE),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        gaugeValue.text = spannable
    }

    protected fun drawSimpleGauge(gaugeBar: ImageView, color: Int, value: Float) {
        gaugeBar.tag = value
        gaugeDrawer.drawSimpleGauge(gaugeBar, color, value)
    }

    protected fun drawSegmentedGauge(
        gaugeBar: ImageView,
        colors: IntArray,
        segments: FloatArray,
        value: Float,
        fadeOutsideDot: Boolean,
        gapBetweenSegments: Boolean,
    ) {
        gaugeBar.tag = value
        gaugeDrawer.drawSegmentedGauge(gaugeBar, colors, segments, value, fadeOutsideDot, gapBetweenSegments)
    }

    private companion object {
        val UNIT_REGEX = Regex("[\\p{L}%]+(?:\\s+[\\p{L}%]+)*")
        const val UNIT_SIZE = 0.7f
    }
}
