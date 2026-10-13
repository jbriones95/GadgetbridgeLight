package nodomain.freeyourgadget.gadgetbridge.activities.app_specific_notifications

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.database.AppSpecificNotificationSettingsRepository
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.entities.AppSpecificNotificationSetting
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightSectionLabel
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightText
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextVariant
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBar
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarBackAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarCenter

private data class NotificationPatternOption(val value: String?, val label: String)

private enum class NotificationPatternField(val titleResource: Int) {
    LED(R.string.per_app_notification_led_pattern),
    VIBRATION(R.string.per_app_notification_vibration_pattern),
    REPETITION(R.string.per_app_notification_vibration_repetition),
}

class AppSpecificNotificationSettingsDetailActivity : AbstractGBActivity() {
    private lateinit var device: GBDevice
    private lateinit var coordinator: DeviceCoordinator
    private lateinit var repository: AppSpecificNotificationSettingsRepository
    private lateinit var packageId: String
    private lateinit var appTitle: String
    private var ledOptions by mutableStateOf<List<NotificationPatternOption>>(emptyList())
    private var vibrationOptions by mutableStateOf<List<NotificationPatternOption>>(emptyList())
    private var repetitionOptions by mutableStateOf<List<NotificationPatternOption>>(emptyList())
    private var selectedLed by mutableStateOf<String?>(null)
    private var selectedVibration by mutableStateOf<String?>(null)
    private var selectedRepetition by mutableStateOf<String?>(null)
    private var hasSavedSettings by mutableStateOf(false)
    private var editingField by mutableStateOf<NotificationPatternField?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE) ?: run {
            finish()
            return
        }
        packageId = intent.getStringExtra(AppSpecificNotificationSettingsActivity.STRING_EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }
        appTitle = intent.getStringExtra(AppSpecificNotificationSettingsActivity.STRING_EXTRA_PACKAGE_TITLE) ?: packageId
        coordinator = device.deviceCoordinator
        repository = AppSpecificNotificationSettingsRepository(device)

        ledOptions = if (coordinator.supportsNotificationLedPatterns(device)) {
            patternOptions(coordinator.getNotificationLedPatterns())
        } else emptyList()
        vibrationOptions = if (coordinator.supportsNotificationVibrationPatterns(device)) {
            patternOptions(coordinator.getNotificationVibrationPatterns())
        } else emptyList()
        repetitionOptions = if (coordinator.supportsNotificationVibrationRepetitionPatterns(device)) {
            patternOptions(coordinator.getNotificationVibrationRepetitionPatterns())
        } else emptyList()

        repository.getSettingsForAppId(packageId)?.let { saved ->
            hasSavedSettings = true
            selectedLed = saved.ledPattern?.takeIf { value -> ledOptions.any { it.value == value } }
            selectedVibration = saved.vibrationPattern?.takeIf { value -> vibrationOptions.any { it.value == value } }
            selectedRepetition = saved.vibrationRepetition?.takeIf { value -> repetitionOptions.any { it.value == value } }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (editingField != null) editingField = null else finish()
            }
        })
        setContentView(ComposeView(this).apply {
            setContent {
                LightTheme {
                    DeviceAppNotificationDetailScreen(
                        deviceName = device.aliasOrName,
                        appTitle = appTitle,
                        ledOptions = ledOptions,
                        vibrationOptions = vibrationOptions,
                        repetitionOptions = repetitionOptions,
                        selectedLed = selectedLed,
                        selectedVibration = selectedVibration,
                        selectedRepetition = selectedRepetition,
                        hasSavedSettings = hasSavedSettings,
                        editingField = editingField,
                        onBack = {
                            if (editingField != null) editingField = null else finish()
                        },
                        onEditField = { editingField = it },
                        onSelectPattern = ::selectPattern,
                        onSave = ::saveSettings,
                        onDelete = ::deleteSettings,
                    )
                }
            }
        })
    }

    private fun patternOptions(patterns: Array<AbstractNotificationPattern>): List<NotificationPatternOption> =
        listOf(NotificationPatternOption(null, getString(R.string.pref_default))) + patterns.map {
            NotificationPatternOption(it.value, it.getUserReadableName(applicationContext))
        }

    private fun selectPattern(field: NotificationPatternField, value: String?) {
        when (field) {
            NotificationPatternField.LED -> selectedLed = value
            NotificationPatternField.VIBRATION -> selectedVibration = value
            NotificationPatternField.REPETITION -> selectedRepetition = value
        }
        editingField = null
    }

    private fun saveSettings() {
        repository.setSettingsForAppId(
            packageId,
            AppSpecificNotificationSetting(packageId, 0, selectedLed, selectedVibration, selectedRepetition),
        )
        finish()
    }

    private fun deleteSettings() {
        repository.setSettingsForAppId(packageId, null)
        finish()
    }
}

@Composable
private fun DeviceAppNotificationDetailScreen(
    deviceName: String,
    appTitle: String,
    ledOptions: List<NotificationPatternOption>,
    vibrationOptions: List<NotificationPatternOption>,
    repetitionOptions: List<NotificationPatternOption>,
    selectedLed: String?,
    selectedVibration: String?,
    selectedRepetition: String?,
    hasSavedSettings: Boolean,
    editingField: NotificationPatternField?,
    onBack: () -> Unit,
    onEditField: (NotificationPatternField?) -> Unit,
    onSelectPattern: (NotificationPatternField, String?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    if (editingField != null) {
        val options = when (editingField) {
            NotificationPatternField.LED -> ledOptions
            NotificationPatternField.VIBRATION -> vibrationOptions
            NotificationPatternField.REPETITION -> repetitionOptions
        }
        val selected = when (editingField) {
            NotificationPatternField.LED -> selectedLed
            NotificationPatternField.VIBRATION -> selectedVibration
            NotificationPatternField.REPETITION -> selectedRepetition
        }
        Column(Modifier.fillMaxSize()) {
            LightTopBar(
                leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back)) { onEditField(null) },
                center = LightTopBarCenter.Text(context.getString(editingField.titleResource)),
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    LightRow(
                        title = option.label,
                        detail = if (option.value == selected) context.getString(R.string.light_selected) else null,
                        onClick = { onSelectPattern(editingField, option.value) },
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        LightTopBar(
            leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back), onBack),
            center = LightTopBarCenter.Text(context.getString(R.string.prefs_wena3_notification_per_app_settings_title)),
            rightAction = LightTopBarAction(
                label = context.getString(R.string.save),
                contentDescription = context.getString(R.string.save),
                onClick = onSave,
            ),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            LightText(appTitle, LightTextVariant.Heading)
            LightText(deviceName, LightTextVariant.Detail, secondary = true)
            if (ledOptions.isEmpty() && vibrationOptions.isEmpty() && repetitionOptions.isEmpty()) {
                LightText(
                    context.getString(R.string.per_app_notification_no_patterns),
                    LightTextVariant.Detail,
                    secondary = true,
                )
            }
            if (ledOptions.isNotEmpty()) {
                LightSectionLabel(context.getString(R.string.per_app_notification_led_pattern))
                NotificationPatternRow(ledOptions, selectedLed, onClick = { onEditField(NotificationPatternField.LED) })
            }
            if (vibrationOptions.isNotEmpty()) {
                LightSectionLabel(context.getString(R.string.per_app_notification_vibration_pattern))
                NotificationPatternRow(vibrationOptions, selectedVibration, onClick = { onEditField(NotificationPatternField.VIBRATION) })
            }
            if (repetitionOptions.isNotEmpty()) {
                LightSectionLabel(context.getString(R.string.per_app_notification_vibration_repetition))
                NotificationPatternRow(repetitionOptions, selectedRepetition, onClick = { onEditField(NotificationPatternField.REPETITION) })
            }
            if (hasSavedSettings) {
                LightRow(
                    title = context.getString(R.string.per_app_notification_delete),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun NotificationPatternRow(
    options: List<NotificationPatternOption>,
    selectedValue: String?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val valueLabel = options.firstOrNull { it.value == selectedValue }?.label
        ?: context.getString(R.string.pref_default)
    LightRow(valueLabel, onClick = onClick)
}
