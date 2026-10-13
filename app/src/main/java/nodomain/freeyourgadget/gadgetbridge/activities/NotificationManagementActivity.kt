package nodomain.freeyourgadget.gadgetbridge.activities

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightSectionLabel
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightSwitchRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightText
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextInput
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextVariant
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBar
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarBackAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarCenter
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationManagementActivity : AbstractGBActivity() {
    private var refreshToken by mutableStateOf(0)
    private var notificationAccess by mutableStateOf(false)
    private var editingChoice by mutableStateOf<NotificationChoice?>(null)

    private val ringtonePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val selectedUri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (selectedUri != null) {
            GBApplication.getPrefs().preferences.edit().putString(GBPrefs.PING_TONE, selectedUri.toString()).apply()
            refreshToken++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        editingChoice = choiceForSearchResult(intent.getStringExtra(AbstractSettingsActivityV2.EXTRA_PREF_HIGHLIGHT))
        refreshNotificationAccess()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (editingChoice != null) editingChoice = null else finish()
            }
        })
        setContentView(ComposeView(this).apply {
            setContent {
                LightTheme {
                    AdvancedNotificationSettings(
                        refreshToken = refreshToken,
                        notificationAccess = notificationAccess,
                        editingChoice = editingChoice,
                        onBack = {
                            if (editingChoice != null) editingChoice = null else finish()
                        },
                        onEditChoice = { editingChoice = it },
                        onOpenApplicationSettings = ::openApplicationNotificationSettings,
                        onOpenNotificationAccess = ::openNotificationAccess,
                        onChooseRingtone = ::chooseRingtone,
                        onOpenApplications = {
                            startActivity(Intent(this@NotificationManagementActivity, AppBlacklistActivity::class.java))
                        },
                        onSaveString = ::saveString,
                        onSaveBoolean = ::saveBoolean,
                        onSaveTime = ::saveTime,
                        onSaveCallDelay = ::saveCallDelay,
                    )
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationAccess()
    }

    private fun refreshNotificationAccess() {
        notificationAccess = PermissionsUtils.checkPermission(
            this,
            PermissionsUtils.CUSTOM_PERM_NOTIFICATION_LISTENER,
        )
        refreshToken++
    }

    private fun openNotificationAccess() {
        PermissionsUtils.requestPermission(this, PermissionsUtils.CUSTOM_PERM_NOTIFICATION_LISTENER)
    }

    private fun openApplicationNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            })
        }
    }

    private fun chooseRingtone() {
        val preferences = GBApplication.getPrefs().preferences
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()
        ringtonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.pref_title_ping_tone))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                Uri.parse(preferences.getString(GBPrefs.PING_TONE, defaultUri) ?: defaultUri),
            )
        })
    }

    private fun saveString(key: String, value: String) {
        GBApplication.getPrefs().preferences.edit().putString(key, value).apply()
        refreshToken++
    }

    private fun saveBoolean(key: String, value: Boolean) {
        GBApplication.getPrefs().preferences.edit().putBoolean(key, value).apply()
        refreshToken++
    }

    private fun saveTime(key: String, value: LocalTime) {
        GBApplication.getPrefs().preferences.edit()
            .putString(key, value.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)))
            .apply()
        refreshToken++
    }

    private fun saveCallDelay(value: String) {
        GBApplication.getPrefs().preferences.edit().putString("notification_delay_calls", value).apply()
    }

    private fun choiceForSearchResult(key: String?): NotificationChoice? = when (key) {
        "notification_mode_calls" -> NotificationChoice(
            "notification_mode_calls", R.string.pref_title_notifications_call, R.array.notification_mode_toggle,
            R.array.notification_mode_values_toggle, "always",
        )
        "notification_mode_sms" -> NotificationChoice(
            "notification_mode_sms", R.string.pref_title_notifications_sms, R.array.notification_mode,
            R.array.notification_mode_values, "when_screen_off",
        )
        "notification_mode_pebblemsg" -> NotificationChoice(
            "notification_mode_pebblemsg", R.string.pref_title_notifications_pebblemsg, R.array.notification_mode,
            R.array.notification_mode_values, "when_screen_off",
        )
        "notifications_timeout" -> NotificationChoice(
            "notifications_timeout", R.string.pref_title_notifications_timeout, R.array.notifications_timeout,
            R.array.notifications_timeout_values, "0",
        )
        "notification_list_is_blacklist" -> NotificationChoice(
            "notification_list_is_blacklist", R.string.pref_title_notification_use_as, R.array.notification_list_is_blacklist_names,
            R.array.notification_list_is_blacklist_values, "true",
        )
        "pref_call_privacy_mode" -> NotificationChoice(
            "pref_call_privacy_mode", R.string.pref_title_call_privacy_mode, R.array.pref_call_privacy_mode,
            R.array.pref_call_privacy_mode_values, getString(R.string.p_call_privacy_mode_off),
        )
        "pref_message_privacy_mode" -> NotificationChoice(
            "pref_message_privacy_mode", R.string.pref_title_message_privacy_mode, R.array.pref_message_privacy_mode,
            R.array.pref_message_privacy_mode_values, getString(R.string.p_message_privacy_mode_off),
        )
        else -> null
    }
}

private data class NotificationChoice(
    val key: String,
    val titleRes: Int,
    val entriesRes: Int,
    val valuesRes: Int,
    val defaultValue: String,
)

@Composable
private fun AdvancedNotificationSettings(
    refreshToken: Int,
    notificationAccess: Boolean,
    editingChoice: NotificationChoice?,
    onBack: () -> Unit,
    onEditChoice: (NotificationChoice?) -> Unit,
    onOpenApplicationSettings: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onChooseRingtone: () -> Unit,
    onOpenApplications: () -> Unit,
    onSaveString: (String, String) -> Unit,
    onSaveBoolean: (String, Boolean) -> Unit,
    onSaveTime: (String, LocalTime) -> Unit,
    onSaveCallDelay: (String) -> Unit,
) {
    val context = LocalContext.current
    if (editingChoice != null) {
        val entries = context.resources.getStringArray(editingChoice.entriesRes)
        val values = context.resources.getStringArray(editingChoice.valuesRes)
        val currentValue = GBApplication.getPrefs().getString(editingChoice.key, editingChoice.defaultValue)
        Column(Modifier.fillMaxSize()) {
            LightTopBar(
                leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back)) { onEditChoice(null) },
                center = LightTopBarCenter.Text(context.getString(editingChoice.titleRes)),
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                entries.forEachIndexed { index, entry ->
                    LightRow(
                        title = entry,
                        detail = if (values[index] == currentValue) context.getString(R.string.light_selected) else null,
                        onClick = {
                            onSaveString(editingChoice.key, values[index])
                            onEditChoice(null)
                        },
                    )
                }
            }
        }
        return
    }

    val preferences = GBApplication.getPrefs().preferences
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val tone = remember(refreshToken) {
        runCatching {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()
            val uri = Uri.parse(preferences.getString(GBPrefs.PING_TONE, defaultUri) ?: defaultUri)
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        }.getOrNull() ?: context.getString(R.string.not_set)
    }
    val callPrivacyDefault = context.getString(R.string.p_call_privacy_mode_off)
    var quietHoursEnabled by remember(refreshToken) {
        mutableStateOf(preferences.getBoolean("notification_times_enabled", false))
    }

    Column(Modifier.fillMaxSize()) {
        LightTopBar(
            leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back), onBack),
            center = LightTopBarCenter.Text(context.getString(R.string.title_activity_notification_management)),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            LightSectionLabel(context.getString(R.string.pref_header_notification_basics))
            LightRow(
                title = context.getString(R.string.pref_title_notifications_generic),
                detail = if (notificationAccess) context.getString(R.string.on) else context.getString(R.string.off),
                onClick = onOpenNotificationAccess,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LightRow(
                    title = context.getString(R.string.pref_title_notifications_generic_settings),
                    onClick = onOpenApplicationSettings,
                )
            }
            NotificationToggle("notifications_generic_whenscreenon", R.string.pref_title_whenscreenon, false, refreshToken, onSaveBoolean)
            NotificationToggle("notifications_generic_when_screen_saver", R.string.pref_title_notifications_when_screen_saver, true, refreshToken, onSaveBoolean)

            LightSectionLabel(context.getString(R.string.pref_header_notification_filters))
            NotificationToggle("notifications_ignore_low_priority", R.string.pref_title_notifications_ignore_low_priority, true, refreshToken, onSaveBoolean, R.string.pref_summary_notifications_ignore_low_priority)
            NotificationToggle("notifications_ignore_work_profile", R.string.pref_title_notifications_ignore_work_profile, false, refreshToken, onSaveBoolean, R.string.pref_summary_notifications_ignore_work_profile)
            NotificationToggle("notifications_ignore_other_users", R.string.pref_title_notifications_ignore_other_users, false, refreshToken, onSaveBoolean, R.string.pref_summary_notifications_ignore_other_users)
            NotificationToggle("notification_prefer_long_text", R.string.pref_title_notification_prefer_long_text, true, refreshToken, onSaveBoolean, R.string.pref_summary_notification_prefer_long_text)
            NotificationToggle("notification_cache_while_disconnected", R.string.pref_title_notification_cache_while_disconnected, false, refreshToken, onSaveBoolean, R.string.pref_summary_notification_cache_while_disconnected)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                NotificationToggle(
                    "minimize_priority",
                    R.string.pref_title_minimize_priority,
                    false,
                    refreshToken,
                    onSaveBoolean,
                    R.string.pref_summary_minimize_priority_off,
                    R.string.pref_summary_minimize_priority_on,
                )
            }

            LightSectionLabel(context.getString(R.string.notification_calls_messages_section))
            LightRow(context.getString(R.string.pref_title_ping_tone), tone, onClick = onChooseRingtone)
            NotificationChoiceRow(
                NotificationChoice("notification_mode_calls", R.string.pref_title_notifications_call, R.array.notification_mode_toggle, R.array.notification_mode_values_toggle, "always"),
                refreshToken,
                onEditChoice,
            )
            NotificationToggle("notification_support_voip_calls", R.string.pref_title_support_voip_calls, false, refreshToken, onSaveBoolean)
            NotificationTextPreference(refreshToken, onSaveCallDelay)
            NotificationChoiceRow(
                NotificationChoice("notification_mode_sms", R.string.pref_title_notifications_sms, R.array.notification_mode, R.array.notification_mode_values, "when_screen_off"),
                refreshToken,
                onEditChoice,
            )
            NotificationChoiceRow(
                NotificationChoice("notification_mode_pebblemsg", R.string.pref_title_notifications_pebblemsg, R.array.notification_mode, R.array.notification_mode_values, "when_screen_off"),
                refreshToken,
                onEditChoice,
            )
            NotificationChoiceRow(
                NotificationChoice("notifications_timeout", R.string.pref_title_notifications_timeout, R.array.notifications_timeout, R.array.notifications_timeout_values, "0"),
                refreshToken,
                onEditChoice,
            )
            NotificationToggle("notification_filter", R.string.pref_title_notification_filter, false, refreshToken, onSaveBoolean, R.string.pref_summary_notification_filter)

            LightSectionLabel(context.getString(R.string.pref_title_notification_times_enabled))
            NotificationToggle(
                "notification_times_enabled",
                R.string.pref_title_notification_times_enabled,
                false,
                refreshToken,
                { key, value ->
                    quietHoursEnabled = value
                    onSaveBoolean(key, value)
                },
                R.string.pref_summary_notification_times_enabled,
            )
            NotificationTimeRow("notification_times_start", R.string.mi2_prefs_do_not_disturb_start, "08:00", quietHoursEnabled, refreshToken, is24Hour, timeFormat, onSaveTime)
            NotificationTimeRow("notification_times_end", R.string.mi2_prefs_do_not_disturb_end, "22:00", quietHoursEnabled, refreshToken, is24Hour, timeFormat, onSaveTime)

            LightSectionLabel(context.getString(R.string.pref_header_notification_application_settings))
            NotificationChoiceRow(
                NotificationChoice("notification_list_is_blacklist", R.string.pref_title_notification_use_as, R.array.notification_list_is_blacklist_names, R.array.notification_list_is_blacklist_values, "true"),
                refreshToken,
                onEditChoice,
            )
            NotificationToggle("notification_media_ignores_application_list", R.string.pref_title_notification_media_ignores_application_list, false, refreshToken, onSaveBoolean, R.string.pref_summary_notification_media_ignores_application_list)
            LightRow(context.getString(R.string.pref_applications_settings), onClick = onOpenApplications)

            LightSectionLabel(context.getString(R.string.pref_header_privacy))
            NotificationChoiceRow(
                NotificationChoice("pref_call_privacy_mode", R.string.pref_title_call_privacy_mode, R.array.pref_call_privacy_mode, R.array.pref_call_privacy_mode_values, callPrivacyDefault),
                refreshToken,
                onEditChoice,
            )
            NotificationChoiceRow(
                NotificationChoice("pref_message_privacy_mode", R.string.pref_title_message_privacy_mode, R.array.pref_message_privacy_mode, R.array.pref_message_privacy_mode_values, context.getString(R.string.p_message_privacy_mode_off)),
                refreshToken,
                onEditChoice,
            )
        }
    }
}

@Composable
private fun NotificationToggle(
    key: String,
    titleRes: Int,
    defaultValue: Boolean,
    refreshToken: Int,
    onSave: (String, Boolean) -> Unit,
    summaryRes: Int? = null,
    summaryOnRes: Int? = null,
) {
    val context = LocalContext.current
    val preferences = GBApplication.getPrefs().preferences
    var checked by remember(refreshToken) { mutableStateOf(preferences.getBoolean(key, defaultValue)) }
    LightSwitchRow(
        title = context.getString(titleRes),
        detail = (if (checked) summaryOnRes else null)?.let { context.getString(it) }
            ?: summaryRes?.let { context.getString(it) },
        checked = checked,
        onCheckedChange = { value ->
            checked = value
            onSave(key, value)
        },
    )
}

@Composable
private fun NotificationChoiceRow(
    choice: NotificationChoice,
    refreshToken: Int,
    onEdit: (NotificationChoice?) -> Unit,
) {
    val context = LocalContext.current
    val preferences = GBApplication.getPrefs().preferences
    val current = remember(refreshToken) {
        preferences.getString(choice.key, choice.defaultValue) ?: choice.defaultValue
    }
    val entries = context.resources.getStringArray(choice.entriesRes)
    val values = context.resources.getStringArray(choice.valuesRes)
    val summary = entries.getOrNull(values.indexOf(current))
    LightRow(context.getString(choice.titleRes), summary, onClick = { onEdit(choice) })
}

@Composable
private fun NotificationTextPreference(refreshToken: Int, onSave: (String) -> Unit) {
    val context = LocalContext.current
    val preferences = GBApplication.getPrefs().preferences
    var delay by remember(refreshToken) {
        mutableStateOf(preferences.getString("notification_delay_calls", "0") ?: "0")
    }
    LightText(context.getString(R.string.pref_title_notification_delay_calls), LightTextVariant.Detail, secondary = true)
    LightTextInput(
        value = delay,
        onValueChange = { candidate ->
            if (candidate.length <= 2 && candidate.all(Char::isDigit)) {
                delay = candidate
                onSave(candidate)
            }
        },
        hint = context.getString(R.string.pref_summary_notification_delay_calls),
        keyboardType = KeyboardType.Number,
    )
}

@Composable
private fun NotificationTimeRow(
    key: String,
    titleRes: Int,
    defaultValue: String,
    enabled: Boolean,
    refreshToken: Int,
    is24Hour: Boolean,
    formatter: DateTimeFormatter,
    onSave: (String, LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val preferences = GBApplication.getPrefs().preferences
    val value = remember(refreshToken) {
        runCatching { LocalTime.parse(preferences.getString(key, defaultValue), formatter) }
            .getOrDefault(LocalTime.parse(defaultValue, formatter))
    }
    LightRow(
        context.getString(titleRes),
        value.format(formatter),
        onClick = if (enabled) {
            {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onSave(key, LocalTime.of(hour, minute)) },
                    value.hour,
                    value.minute,
                    is24Hour,
                ).show()
            }
        } else null,
    )
}
