package nodomain.freeyourgadget.gadgetbridge.ui.light

import android.content.Context
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.function.Consumer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.WeakHashMap

object LightDevicesView {
    private val states = WeakHashMap<ComposeView, LightDevicesContentState>()

    @JvmStatic
    fun bind(
        view: ComposeView,
        context: Context,
        devices: List<GBDevice>,
        onDeviceClick: Consumer<String>,
        onDeviceMenu: Consumer<String>,
        onDeviceAction: java.util.function.BiConsumer<String, String>,
        onQuickAction: java.util.function.BiConsumer<String, String>,
        onFindTarget: java.util.function.BiConsumer<String, String>,
        onFindStop: java.util.function.BiConsumer<String, String>,
    ) {
        val state = LightDevicesContentState(snapshot(context, devices))
        states[view] = state
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            LightTheme {
                LightDevicesScreen(
                    devices = state.devices,
                    onDeviceClick = onDeviceClick::accept,
                    onQuickAction = onQuickAction::accept,
                    onDeviceMenu = { address ->
                        state.menuAddress = address
                        onDeviceMenu.accept(address)
                    },
                    menuAddress = state.menuAddress,
                    menuActions = state.menuActions,
                    onDeviceAction = { actionId ->
                        state.menuAddress?.let { address -> onDeviceAction.accept(address, actionId) }
                        state.menuAddress = null
                    },
                    onDismissDeviceMenu = { state.menuAddress = null },
                    heartRateAddress = state.heartRateAddress,
                    onDismissHeartRate = { state.heartRateAddress = null },
                    findAddress = state.findAddress,
                    findPerEarbud = state.findPerEarbud,
                    findTarget = state.findTarget,
                    onFindTarget = { target ->
                        state.findTarget = target
                        state.findAddress?.let { onFindTarget.accept(it, target) }
                    },
                    onFindStop = {
                        val address = state.findAddress
                        val target = state.findTarget
                        if (address != null && target != null) onFindStop.accept(address, target)
                        state.findAddress = null
                        state.findTarget = null
                    },
                )
            }
        }
    }

    @JvmStatic
    fun update(view: ComposeView?, context: Context?, devices: List<GBDevice>) {
        if (view == null || context == null) return
        states[view]?.devices = snapshot(context, devices)
    }

    @JvmStatic
    fun showActions(view: ComposeView?, address: String, actions: List<LightMenuAction>) {
        states[view]?.let {
            it.menuAddress = address
            it.menuActions = actions
        }
    }

    @JvmStatic
    fun showHeartRate(view: ComposeView?, address: String) {
        states[view]?.let { state ->
            state.heartRateAddress = address
            state.devices = state.devices.map { device ->
                if (device.address == address) device.copy(heartRate = null) else device
            }
        }
    }

    @JvmStatic
    fun updateHeartRate(view: ComposeView?, address: String, heartRate: Int) {
        states[view]?.let { state ->
            state.devices = state.devices.map { device ->
                if (device.address == address) device.copy(heartRate = heartRate) else device
            }
        }
    }

    @JvmStatic
    fun showFindDialog(view: ComposeView?, address: String, perEarbud: Boolean) {
        states[view]?.let {
            it.findAddress = address
            it.findPerEarbud = perEarbud
            it.findTarget = null
        }
    }

    private fun snapshot(context: Context, devices: List<GBDevice>): List<LightDeviceUiState> =
        devices.asSequence()
            .filter { it.address != null }
            .map { LightDeviceUiState.from(context, it) }
            .distinctBy { it.address }
            .toList()
}

private class LightDevicesContentState(devices: List<LightDeviceUiState>) {
    var devices by mutableStateOf(devices)
    var menuAddress by mutableStateOf<String?>(null)
    var menuActions by mutableStateOf<List<LightMenuAction>>(emptyList())
    var heartRateAddress by mutableStateOf<String?>(null)
    var findAddress by mutableStateOf<String?>(null)
    var findPerEarbud by mutableStateOf(false)
    var findTarget by mutableStateOf<String?>(null)
}
