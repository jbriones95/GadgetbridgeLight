package nodomain.freeyourgadget.gadgetbridge.ui.light

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ViewCompositionStrategy
import nodomain.freeyourgadget.gadgetbridge.R
import java.util.function.Consumer

object LightMainNavigation {
    @JvmStatic
    fun bindTopBar(
        view: ComposeView,
        dashboard: Boolean,
        onCalendar: Runnable,
        onDashboardWidgets: Runnable,
    ) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            LightTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                LightTopBar(
                    leftAction = if (dashboard) {
                        LightTopBarAction(
                            label = "",
                            contentDescription = context.getString(R.string.menuitem_calendar),
                            onClick = onCalendar::run,
                            iconRes = R.drawable.ic_calendar_month,
                        )
                    } else null,
                    center = LightTopBarCenter.Text(
                        context.getString(if (dashboard) R.string.bottom_nav_dashboard else R.string.bottom_nav_devices),
                    ),
                    rightAction = if (dashboard) {
                        LightTopBarAction(
                            label = context.getString(R.string.light_dashboard_edit),
                            contentDescription = context.getString(R.string.dashboard_settings),
                            onClick = onDashboardWidgets::run,
                        )
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    @JvmStatic
    fun bindBottomBar(view: ComposeView, selectedIndex: Int, onSelected: Consumer<Int>) {
        val state = LightNavigationState(selectedIndex)
        view.tag = state
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            LightTheme {
                LightNavigationBar(state, onSelected)
            }
        }
    }

    @JvmStatic
    fun select(view: ComposeView, selectedIndex: Int) {
        (view.tag as? LightNavigationState)?.selectedIndex = selectedIndex
    }
}

private class LightNavigationState(selected: Int) {
    var selectedIndex by mutableIntStateOf(selected)
}

@Composable
private fun LightNavigationBar(state: LightNavigationState, onSelected: Consumer<Int>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entries = listOf(
        Triple(R.drawable.ic_dashboard, R.string.bottom_nav_dashboard, 0),
        Triple(R.drawable.ic_devices_other, R.string.bottom_nav_devices, 1),
        Triple(R.drawable.ic_settings, R.string.action_settings, 2),
    )
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEach { (iconRes, labelRes, index) ->
            val isSelected = state.selectedIndex == index
            val label = context.getString(labelRes)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = label
                        selected = isSelected
                    }
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = {
                            state.selectedIndex = index
                            onSelected.accept(index)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).padding(1.dp),
                    colorFilter = ColorFilter.tint(
                        if (isSelected) LightThemeTokens.colors.content else LightThemeTokens.colors.contentSecondary,
                    ),
                )
            }
        }
    }
}
