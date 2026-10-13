package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.AlertDialog
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.ComposeView
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ActionSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.CategorySetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.GroupSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.InfoSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ListEntry
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ListSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ScreenSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.SeekBarSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.SwitchSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.TextSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.TimeSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DateSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.FilePickerSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.FolderPickerSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.MultiSelectSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.SortableListSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.XmlScreenSetting
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightSwitchRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightScrollContainer
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightText
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextVariant
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBar
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarBackAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarCenter
import nodomain.freeyourgadget.gadgetbridge.util.Prefs

class LightDeviceSettingsActivity : AbstractGBActivity() {
    private lateinit var device: GBDevice
    private lateinit var rootItems: List<DeviceSetting>
    private var items by mutableStateOf<List<DeviceSetting>>(emptyList())
    private var title by mutableStateOf("")
    private var refreshToken by mutableStateOf(0)
    private val history = ArrayDeque<Pair<String, List<DeviceSetting>>>()
    private var pendingFilePicker: FilePickerSetting? = null
    private var pendingFolderPicker: FolderPickerSetting? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE)
            ?: error("Missing device")
        title = getString(R.string.pref_header_device_spec_settings)
        rootItems = device.deviceCoordinator.getDeviceSettings(device)?.items
            ?: error("Missing DSL settings")
        val requestedKey = intent.getStringExtra(DeviceSettingsActivity.EXTRA_PREF_SCREEN)
        val requested = requestedKey?.let { findSettingContainer(rootItems, it) }
        items = requested?.second ?: rootItems
        requested?.first?.let { title = it }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (history.isEmpty()) finish() else popScreen()
            }
        })
        setContentView(ComposeView(this).apply {
            setContent {
                LightTheme {
                LightDeviceSettingsContent(
                    title = title,
                    items = items,
                    device = device,
                    refreshToken = refreshToken,
                    onBack = { if (history.isEmpty()) finish() else popScreen() },
                    onOpenScreen = ::pushScreen,
                    onChanged = ::preferenceChanged,
                    onFilePicker = ::launchFilePicker,
                    onFolderPicker = ::launchFolderPicker,
                )
                }
            }
        })
    }

    private fun pushScreen(screen: ScreenSetting) {
        if (screen.xmlSubScreens.isNotEmpty()) {
            DeviceSettingsActivity.startLegacy(this, device, screen.key)
            return
        }
        history.addLast(title to items)
        title = getString(screen.title)
        items = screen.children
    }

    private fun popScreen() {
        val previous = history.removeLast()
        title = previous.first
        items = previous.second
    }

    private fun preferenceChanged(key: String) {
        GBApplication.deviceService(device).onSendConfiguration(key)
        refreshToken++
    }

    private fun launchFilePicker(setting: FilePickerSetting) {
        pendingFilePicker = setting
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = setting.mimeTypes.firstOrNull() ?: "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, setting.mimeTypes.toTypedArray())
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }, REQUEST_FILE)
    }

    private fun launchFolderPicker(setting: FolderPickerSetting) {
        pendingFolderPicker = setting
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_FOLDER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            REQUEST_FILE -> {
                val uris = buildList {
                    data.data?.let(::add)
                    data.clipData?.let { clip -> repeat(clip.itemCount) { add(clip.getItemAt(it).uri) } }
                }.distinct()
                pendingFilePicker?.onPicked?.invoke(this, device, uris)
                pendingFilePicker = null
            }
            REQUEST_FOLDER -> {
                val setting = pendingFolderPicker ?: return
                val uri = data.data ?: return
                if (setting.persistUriPermission) {
                    val grantedFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    if (grantedFlags != 0) contentResolver.takePersistableUriPermission(uri, grantedFlags)
                }
                GBApplication.getDevicePrefs(device).preferences.edit().putString(setting.key, uri.toString()).apply()
                setting.onPicked?.invoke(this, device, uri)
                preferenceChanged(setting.key)
                pendingFolderPicker = null
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context, device: GBDevice) {
            context.startActivity(Intent(context, LightDeviceSettingsActivity::class.java).apply {
                putExtra(GBDevice.EXTRA_DEVICE, device)
            })
        }

        @JvmStatic
        fun start(context: Context, device: GBDevice, prefKey: String) {
            context.startActivity(Intent(context, LightDeviceSettingsActivity::class.java).apply {
                putExtra(GBDevice.EXTRA_DEVICE, device)
                putExtra(DeviceSettingsActivity.EXTRA_PREF_SCREEN, prefKey)
                putExtra(DeviceSettingsActivity.EXTRA_PREF_HIGHLIGHT, prefKey)
            })
        }

        private const val REQUEST_FILE = 4101
        private const val REQUEST_FOLDER = 4102
    }
}

private fun findSettingContainer(items: List<DeviceSetting>, key: String): Pair<String?, List<DeviceSetting>>? {
    if (items.any { it.key == key }) return null to items
    items.forEach { setting ->
        when (setting) {
            is ScreenSetting -> {
                if (setting.key == key) return null to setting.children
                findSettingContainer(setting.children, key)?.let { return it }
            }
            is CategorySetting -> findSettingContainer(setting.children, key)?.let { return setting.titleText to it.second }
            is GroupSetting -> findSettingContainer(setting.children, key)?.let { return it }
            else -> Unit
        }
    }
    return null
}

@androidx.compose.runtime.Composable
private fun LightDeviceSettingsContent(
    title: String,
    items: List<DeviceSetting>,
    device: GBDevice,
    refreshToken: Int,
    onBack: () -> Unit,
    onOpenScreen: (ScreenSetting) -> Unit,
    onChanged: (String) -> Unit,
    onFilePicker: (FilePickerSetting) -> Unit,
    onFolderPicker: (FolderPickerSetting) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = GBApplication.getDevicePrefs(device)
    @Suppress("UNUSED_VARIABLE")
    val refresh = refreshToken
    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        LightTopBar(
            leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back), onBack),
            center = LightTopBarCenter.Text(title),
        )
        LightScrollContainer(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            items.forEach { setting ->
                RenderDeviceSetting(
                    setting,
                    context,
                    prefs,
                    device,
                    onOpenScreen,
                    onChanged,
                    onFilePicker = onFilePicker,
                    onFolderPicker = onFolderPicker,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RenderDeviceSetting(
    setting: DeviceSetting,
    context: Context,
    prefs: Prefs,
    device: GBDevice,
    onOpenScreen: (ScreenSetting) -> Unit,
    onChanged: (String) -> Unit,
    onFilePicker: (FilePickerSetting) -> Unit,
    onFolderPicker: (FolderPickerSetting) -> Unit,
) {
    if (setting.visibleWhen?.invoke(prefs) == false) return
    val enabled = setting.connectedOnly.not() || device.isInitialized
    fun dependencyEnabled(key: String?): Boolean = key == null || prefs.preferences.getBoolean(key, true)
    when (setting) {
        is CategorySetting -> {
            LightText(setting.titleText ?: setting.title.takeIf { it != 0 }?.let(context::getString).orEmpty(), LightTextVariant.Detail, secondary = true)
                setting.children.forEach { RenderDeviceSetting(it, context, prefs, device, onOpenScreen, onChanged, onFilePicker, onFolderPicker) }
        }
        is ScreenSetting -> LightRow(
            title = context.getString(setting.title),
            detail = if (setting.summary != 0) context.getString(setting.summary) else null,
            onClick = if (enabled && setting.enabled?.invoke(prefs) != false) {{ onOpenScreen(setting) }} else null,
        )
        is SwitchSetting -> {
            val current = prefs.preferences.getBoolean(setting.key, setting.defaultValue)
            LightSwitchRow(
                title = context.getString(setting.title),
                detail = when {
                    current && setting.summaryOn != 0 -> context.getString(setting.summaryOn)
                    !current && setting.summaryOff != 0 -> context.getString(setting.summaryOff)
                    setting.summary != 0 -> context.getString(setting.summary)
                    else -> null
                },
                checked = current,
                enabled = enabled && setting.enabled && dependencyEnabled(setting.dependency),
                onCheckedChange = { checked ->
                    runWithConfirmation(context, setting.confirmationMessage) {
                        prefs.preferences.edit().putBoolean(setting.key, checked).apply()
                        onChanged(setting.key)
                    }
                },
            )
        }
        is ListSetting -> {
            val entries = when {
                setting.entriesProvider != null -> setting.entriesProvider.invoke(prefs)
                setting.entries.isNotEmpty() -> setting.entries
                setting.entriesRes != 0 && setting.entryValuesRes != 0 -> {
                    val labels = context.resources.getStringArray(setting.entriesRes)
                    val values = context.resources.getStringArray(setting.entryValuesRes)
                    values.mapIndexedNotNull { index, value ->
                        labels.getOrNull(index)?.let { ListEntry.Text(value, it) }
                    }
                }
                else -> emptyList()
            }
            val current = prefs.preferences.getString(setting.key, setting.defaultValue) ?: setting.defaultValue
            val selectedIndex = entries.indexOfFirst { it.value == current }
            LightRow(
                title = context.getString(setting.title),
                detail = entries.getOrNull(selectedIndex)?.label(context) ?: current,
                onClick = if (enabled && dependencyEnabled(setting.dependency)) {{
                    if (entries.isNotEmpty()) {
                        val nextIndex = if (selectedIndex < 0) 0 else (selectedIndex + 1).mod(entries.size)
                        val next = entries[nextIndex]
                        setting.onValueChange?.invoke(androidx.preference.Preference(context), current, next.value)
                        prefs.preferences.edit().putString(setting.key, next.value).apply()
                        onChanged(setting.key)
                    }
                }} else null,
            )
        }
        is InfoSetting -> LightRow(
            title = setting.titleText ?: setting.title.takeIf { it != 0 }?.let(context::getString).orEmpty(),
            detail = setting.summaryProvider?.invoke(context, prefs)?.toString()
                ?: prefs.preferences.getString(setting.key, setting.defaultValue),
        )
        is ActionSetting -> LightRow(
            title = setting.titleText ?: setting.title.takeIf { it != 0 }?.let(context::getString).orEmpty(),
            detail = setting.summaryProvider?.invoke(context, prefs)?.toString()
                ?: if (setting.summary != 0) context.getString(setting.summary) else null,
            onClick = if (enabled && setting.enabled && dependencyEnabled(setting.dependency)) {{
                runWithConfirmation(context, setting.confirmationMessage) { setting.onClick?.invoke(context, device) }
            }} else null,
        )
        is SeekBarSetting -> {
            val current = prefs.preferences.getInt(setting.key, setting.defaultValue)
            LightRow(
                title = context.getString(setting.title),
                detail = (current * setting.scale).toString(),
                onClick = if (enabled && dependencyEnabled(setting.dependency)) {{
                    val next = if (current >= setting.max) setting.min else (current + setting.step).coerceAtMost(setting.max)
                    prefs.preferences.edit().putInt(setting.key, next).apply()
                    setting.onSharedPreferenceChanged?.invoke(next)
                    onChanged(setting.key)
                }} else null,
            )
        }
        is TimeSetting -> {
            val value = prefs.preferences.getString(setting.key, setting.defaultValue).orEmpty()
            LightRow(
                title = context.getString(setting.title),
                detail = formatTimeForDisplay(context, value),
                onClick = if (enabled && dependencyEnabled(setting.dependency)) {{
                    val parts = value.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                        val selected = String.format(Locale.ROOT, "%02d:%02d", selectedHour, selectedMinute)
                        prefs.preferences.edit().putString(setting.key, selected).apply()
                        setting.onSharedPreferenceChanged?.invoke(selected)
                        onChanged(setting.key)
                    }, hour, minute, android.text.format.DateFormat.is24HourFormat(context)).show()
                }} else null,
            )
        }
        is DateSetting -> {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val calendar = Calendar.getInstance()
            runCatching { calendar.time = format.parse(prefs.preferences.getString(setting.key, setting.defaultValue).orEmpty()) ?: calendar.time }
            LightRow(
                title = context.getString(setting.title),
                detail = format.format(calendar.time),
                onClick = if (enabled && dependencyEnabled(setting.dependency)) {{
                    DatePickerDialog(context, { _, year, month, day ->
                        val selected = Calendar.getInstance().apply { set(year, month, day) }
                        val value = format.format(selected.time)
                        prefs.preferences.edit().putString(setting.key, value).apply()
                        setting.onSharedPreferenceChanged?.invoke(value)
                        onChanged(setting.key)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                        if (setting.minDate > 0L) datePicker.minDate = setting.minDate
                        if (setting.maxDate < Long.MAX_VALUE) datePicker.maxDate = setting.maxDate
                    }.show()
                }} else null,
            )
        }
        is MultiSelectSetting -> {
            val entries = setting.entriesProvider?.invoke(prefs) ?: setting.entries
            val selected = prefs.preferences.getStringSet(setting.key, setting.defaultValue).orEmpty().toMutableSet()
            LightRow(
                title = context.getString(setting.title),
                detail = entries.filter { it.value in selected }.joinToString(", ") { it.label(context) }
                    .ifEmpty { context.getString(R.string.not_set) },
                onClick = if (enabled && dependencyEnabled(setting.dependency)) {{
                    val checked = entries.map { it.value in selected }.toBooleanArray()
                    android.app.AlertDialog.Builder(context)
                        .setTitle(setting.title)
                        .setMultiChoiceItems(entries.map { it.label(context) }.toTypedArray(), checked) { _, which, isChecked ->
                            if (isChecked) selected.add(entries[which].value) else selected.remove(entries[which].value)
                        }
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            prefs.preferences.edit().putStringSet(setting.key, selected).apply()
                            onChanged(setting.key)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }} else null,
            )
        }
        is SortableListSetting -> {
            val selected = prefs.preferences.getString(setting.key, setting.defaultValue.joinToString(",")).orEmpty()
            LightRow(
                title = context.getString(setting.title),
                detail = selected.split(',')
                    .mapNotNull { value -> setting.entries.firstOrNull { it.value == value }?.label(context) }
                    .joinToString(", ")
                    .ifEmpty { context.getString(R.string.not_set) },
                onClick = if (enabled && dependencyEnabled(setting.dependency)) {{
                    val values = selected.split(',').filter { it.isNotBlank() }.toMutableList()
                    if (values.size > 1) {
                        values.add(values.removeAt(0))
                        prefs.preferences.edit().putString(setting.key, values.joinToString(",")).apply()
                        onChanged(setting.key)
                    }
                }} else null,
            )
        }
        is TextSetting -> {
            val value = prefs.preferences.getString(setting.key, setting.defaultValue).orEmpty()
            LightRow(
                title = context.getString(setting.title),
                detail = value.ifEmpty { setting.defaultSummary?.invoke(context) },
                onClick = if (enabled && setting.enabled && dependencyEnabled(setting.dependency)) {{
                    val input = EditText(context).apply {
                        setText(value)
                        inputType = setting.inputType
                        setting.maxLength?.let { filters = arrayOf(android.text.InputFilter.LengthFilter(it)) }
                        setting.onBindEditText?.invoke(this)
                    }
                    AlertDialog.Builder(context)
                        .setTitle(context.getString(setting.title))
                        .setView(input)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val updated = input.text.toString()
                            prefs.preferences.edit().putString(setting.key, updated).apply()
                            setting.onSharedPreferenceChanged?.invoke(updated)
                            onChanged(setting.key)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }} else null,
            )
        }
        is XmlScreenSetting -> LightRow(
            title = context.getString(setting.screen.title),
            detail = context.getString(R.string.pref_header_device_spec_settings),
            onClick = if (enabled) {{
                DeviceSettingsActivity.startLegacy(context, device, setting.screen.key)
            }} else null,
        )
        is FilePickerSetting -> LightRow(
            title = context.getString(setting.title),
            detail = if (setting.summary != 0) context.getString(setting.summary) else null,
            onClick = if (enabled && dependencyEnabled(setting.dependency)) {{ onFilePicker(setting) }} else null,
        )
        is FolderPickerSetting -> LightRow(
            title = context.getString(setting.title),
            detail = prefs.preferences.getString(setting.key, null),
            onClick = if (enabled && dependencyEnabled(setting.dependency)) {{ onFolderPicker(setting) }} else null,
        )
        is GroupSetting -> setting.children.forEach { RenderDeviceSetting(it, context, prefs, device, onOpenScreen, onChanged, onFilePicker, onFolderPicker) }
        else -> Unit
    }
}

private fun runWithConfirmation(context: Context, message: Int, action: () -> Unit) {
    if (message == 0) {
        action()
        return
    }
    AlertDialog.Builder(context)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _, _ -> action() }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

private fun formatTimeForDisplay(context: Context, value: String): String {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return value
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return value
    return if (android.text.format.DateFormat.is24HourFormat(context)) {
        String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    } else {
        val suffix = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        String.format(Locale.ROOT, "%d:%02d %s", displayHour, minute, suffix)
    }
}

private fun ListEntry.label(context: Context): String = when (this) {
    is ListEntry.Res -> context.getString(label)
    is ListEntry.Text -> label
}

private fun Int.mod(value: Int): Int = if (value <= 0) 0 else this % value
