package nodomain.freeyourgadget.gadgetbridge.ui.light

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2
import nodomain.freeyourgadget.gadgetbridge.adapter.DeviceCardItemBinder
import nodomain.freeyourgadget.gadgetbridge.devices.cards.ActionCardItem
import nodomain.freeyourgadget.gadgetbridge.devices.cards.DeviceCardLayout
import nodomain.freeyourgadget.gadgetbridge.devices.cards.HeartRateCardItem
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

data class LightDeviceQuickAction(
    val id: String,
    val label: String,
    val description: String,
    val iconRes: Int,
)

data class LightDeviceUiState(
    val address: String,
    val name: String,
    val state: String,
    val connected: Boolean,
    val battery: String?,
    val quickActions: List<LightDeviceQuickAction>,
    val heartRate: Int?,
) {
    companion object {
        fun from(context: Context, device: GBDevice): LightDeviceUiState {
            val batteryLevel = device.getBatteryLevel(0)
            val cardItems = DeviceCardLayout.apply(device, device.deviceCoordinator.getCardItems(device))
            val quickActions = cardItems.filter { item ->
                (item is ActionCardItem && item.action.id in setOf("find_device", "sync")) || item is HeartRateCardItem
            }.map { item ->
                when (item) {
                    is ActionCardItem -> LightDeviceQuickAction(
                        item.action.id,
                        when (item.action.id) {
                            "find_device" -> context.getString(nodomain.freeyourgadget.gadgetbridge.R.string.light_quick_find)
                            "sync" -> context.getString(nodomain.freeyourgadget.gadgetbridge.R.string.light_quick_sync)
                            else -> item.action.getDescription(device, context)
                        },
                        item.action.getDescription(device, context),
                        item.action.getIcon(device),
                    )
                    is HeartRateCardItem -> LightDeviceQuickAction(
                        item.id,
                        context.getString(nodomain.freeyourgadget.gadgetbridge.R.string.light_quick_heart_rate),
                        context.getString(nodomain.freeyourgadget.gadgetbridge.R.string.controlcenter_get_heartrate_measurement),
                        nodomain.freeyourgadget.gadgetbridge.R.drawable.ic_heart,
                    )
                    else -> error("Filtered out unsupported device card item")
                }
            }
            val sample = (context as? ControlCenterv2)?.getCurrentHRSample(device)
            return LightDeviceUiState(
                address = device.address ?: device.aliasOrName ?: "unknown-device",
                name = device.aliasOrName ?: "Device",
                state = if (device.isBusy) device.busyTask ?: "BUSY" else device.getStateString(context),
                connected = device.isConnected || device.isInitialized,
                battery = batteryLevel.takeIf { it >= 0 }?.let { "$it%" },
                quickActions = quickActions,
                heartRate = sample?.heartRate?.takeIf { it > 0 },
            )
        }
    }
}
