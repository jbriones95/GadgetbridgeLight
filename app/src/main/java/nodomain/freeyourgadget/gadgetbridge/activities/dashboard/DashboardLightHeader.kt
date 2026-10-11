package nodomain.freeyourgadget.gadgetbridge.activities.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import nodomain.freeyourgadget.gadgetbridge.R
import androidx.compose.ui.platform.ViewCompositionStrategy
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBar
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarCenter

class DashboardLightHeaderState(
    date: String,
    canGoForward: Boolean,
) {
    var date by mutableStateOf(date)
    var canGoForward by mutableStateOf(canGoForward)
}

object DashboardLightHeader {
    @JvmStatic
    fun bind(
        view: ComposeView,
        state: DashboardLightHeaderState,
        onPreviousDay: () -> Unit,
        onNextDay: () -> Unit,
    ) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            DashboardLightHeaderContent(state, onPreviousDay, onNextDay)
        }
    }
}

@Composable
private fun DashboardLightHeaderContent(
    state: DashboardLightHeaderState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    LightTheme {
        val context = LocalContext.current
        LightTopBar(
            leftAction = LightTopBarAction(
                context.getString(R.string.light_action_previous_short),
                context.getString(R.string.datestep_prev_day),
                onPreviousDay,
            ),
            center = LightTopBarCenter.Text(state.date),
            rightAction = LightTopBarAction(
                context.getString(R.string.light_action_next_short),
                context.getString(R.string.datestep_next_day),
                onClick = if (state.canGoForward) onNextDay else null,
                enabled = state.canGoForward,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
