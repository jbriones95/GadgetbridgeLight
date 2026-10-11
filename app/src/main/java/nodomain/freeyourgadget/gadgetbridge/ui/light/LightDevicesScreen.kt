package nodomain.freeyourgadget.gadgetbridge.ui.light

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nodomain.freeyourgadget.gadgetbridge.R

@Composable
fun LightDevicesScreen(
    devices: List<LightDeviceUiState>,
    onDeviceClick: (String) -> Unit,
    onDeviceMenu: (String) -> Unit,
    menuAddress: String? = null,
    menuActions: List<LightMenuAction> = emptyList(),
    onDeviceAction: (String) -> Unit = {},
    onDismissDeviceMenu: () -> Unit = {},
    onQuickAction: (String, String) -> Unit = { _, _ -> },
    heartRateAddress: String? = null,
    onDismissHeartRate: () -> Unit = {},
    findAddress: String? = null,
    findPerEarbud: Boolean = false,
    findTarget: String? = null,
    onFindTarget: (String) -> Unit = {},
    onFindStop: () -> Unit = {},
) {
    if (devices.isEmpty()) {
        LightEmptyState(
            title = stringResource(R.string.light_devices_no_devices),
            message = stringResource(R.string.light_devices_connect_hint),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(devices, key = { it.address }) { device ->
            LightDeviceRow(
                device,
                onClick = { onDeviceClick(device.address) },
                onMenuClick = { onDeviceMenu(device.address) },
                onQuickAction = { actionId -> onQuickAction(device.address, actionId) },
            )
        }
    }
    val selectedDevice = devices.firstOrNull { it.address == menuAddress }
    if (selectedDevice != null) {
        LightDeviceActionsDialog(
            deviceName = selectedDevice.name,
            actions = menuActions,
            onAction = onDeviceAction,
            onDismiss = onDismissDeviceMenu,
        )
    }
    val heartRateDevice = devices.firstOrNull { it.address == heartRateAddress }
    if (heartRateDevice != null) {
        LightHeartRateDialog(heartRateDevice, onDismissHeartRate)
    }
    val findDevice = devices.firstOrNull { it.address == findAddress }
    if (findDevice != null) {
        LightFindDeviceDialog(
            device = findDevice,
            perEarbud = findPerEarbud,
            target = findTarget,
            onTarget = onFindTarget,
            onStop = onFindStop,
        )
    }
}

@Composable
private fun LightDeviceActionsDialog(
    deviceName: String,
    actions: List<LightMenuAction>,
    onAction: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightThemeTokens.colors.background),
        ) {
            LightTopBar(
                leftAction = LightTopBarBackAction(stringResource(R.string.light_action_back), onDismiss),
                center = LightTopBarCenter.Text(stringResource(R.string.light_device_actions_title)),
            )
            LightSectionLabel(deviceName, Modifier.padding(horizontal = 16.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(actions, key = { it.id }) { action ->
                    LightDeviceActionRow(action) { onAction(action.id) }
                }
            }
        }
    }
}

@Composable
private fun LightDeviceActionRow(action: LightMenuAction, onClick: () -> Unit) {
    val description = if (action.enabled) action.title else "${action.title}, ${stringResource(R.string.off)}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics {
                role = Role.Button
                contentDescription = description
                if (!action.enabled) disabled()
            }
            .clickable(enabled = action.enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        if (action.iconRes != 0) {
            androidx.compose.foundation.Image(
                painter = painterResource(action.iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(
                    if (action.enabled) LightThemeTokens.colors.content else LightThemeTokens.colors.contentSecondary,
                ),
            )
            Spacer(Modifier.width(12.dp))
        }
        LightText(
            action.title,
            modifier = Modifier.weight(1f),
            secondary = !action.enabled,
        )
    }
}

@Composable
private fun LightDeviceRow(
    device: LightDeviceUiState,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    onQuickAction: (String) -> Unit,
) {
    val moreDescription = stringResource(R.string.light_action_more)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .semantics {
                        role = Role.Button
                        stateDescription = device.state
                    },
            ) {
                LightText(device.name, LightTextVariant.Heading)
                LightText(
                    text = device.state,
                    variant = LightTextVariant.Detail,
                    secondary = !device.connected,
                )
                if (device.battery != null) {
                    LightText(device.battery, LightTextVariant.Detail, secondary = true)
                }
            }
            Spacer(Modifier.width(8.dp))
            LightText(
                text = stringResource(R.string.light_action_more),
                variant = LightTextVariant.Button,
                modifier = Modifier
                    .padding(8.dp)
                    .widthIn(min = 48.dp)
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onMenuClick)
                    .semantics {
                        role = Role.Button
                        contentDescription = moreDescription
                    },
            )
        }
        if (device.quickActions.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                device.quickActions.forEach { action ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { onQuickAction(action.id) }
                            .semantics {
                                role = Role.Button
                                contentDescription = action.description
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(action.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(LightThemeTokens.colors.content),
                        )
                        Spacer(Modifier.width(6.dp))
                        LightText(action.label, LightTextVariant.Button, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun LightHeartRateDialog(device: LightDeviceUiState, onDismiss: () -> Unit) {
    val message = if (device.heartRate != null) {
        stringResource(R.string.light_heart_rate_result, device.heartRate)
    } else {
        stringResource(R.string.light_heart_rate_measuring)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .background(LightThemeTokens.colors.background)
                .padding(horizontal = 12.dp),
        ) {
            LightTopBar(
                leftAction = LightTopBarBackAction(stringResource(R.string.light_action_back), onDismiss),
                center = LightTopBarCenter.Text(device.name),
            )
            LightText(message, LightTextVariant.Heading, Modifier.padding(vertical = 32.dp))
        }
    }
}

@Composable
private fun LightFindDeviceDialog(
    device: LightDeviceUiState,
    perEarbud: Boolean,
    target: String?,
    onTarget: (String) -> Unit,
    onStop: () -> Unit,
) {
    val title = stringResource(R.string.light_find_device_title, device.name)
    Dialog(
        onDismissRequest = onStop,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .background(LightThemeTokens.colors.background)
                .padding(horizontal = 12.dp),
        ) {
            LightTopBar(
                leftAction = LightTopBarBackAction(stringResource(R.string.light_action_back), onStop),
                center = LightTopBarCenter.Text(title),
            )
            if (target == null) {
                if (perEarbud) {
                    LightRow(stringResource(R.string.light_find_target_all), onClick = { onTarget("ALL") })
                    LightRow(stringResource(R.string.light_find_target_left), onClick = { onTarget("LEFT") })
                    LightRow(stringResource(R.string.light_find_target_right), onClick = { onTarget("RIGHT") })
                } else {
                    LightRow(stringResource(R.string.controlcenter_find_device), onClick = { onTarget("ALL") })
                }
            } else {
                LightText(
                    stringResource(R.string.light_find_device_active),
                    LightTextVariant.Copy,
                    Modifier.padding(vertical = 20.dp),
                )
                LightRow(stringResource(R.string.light_find_device_stop), onClick = onStop)
            }
        }
    }
}
