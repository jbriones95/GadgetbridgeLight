package nodomain.freeyourgadget.gadgetbridge.ui.light

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class LightWorkoutSection(
    val title: String,
    val metrics: List<LightMetric>,
)

@Composable
fun LightWorkoutSummary(
    sections: List<LightWorkoutSection>,
    modifier: Modifier = Modifier,
) {
    LightDetailFrame("SUMMARY", modifier = modifier) {
        if (sections.isEmpty()) {
            LightStateMessage("No summary data available")
        } else {
            sections.forEach { section ->
                LightMetricSection(section.title, section.metrics)
            }
        }
    }
}
