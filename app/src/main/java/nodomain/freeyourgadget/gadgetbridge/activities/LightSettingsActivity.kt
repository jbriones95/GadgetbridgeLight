package nodomain.freeyourgadget.gadgetbridge.activities

import android.content.Intent
import android.os.Bundle
import android.app.Activity
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ComposeView
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardPreferencesActivity
import nodomain.freeyourgadget.gadgetbridge.activities.NotificationManagementActivity
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightScrollContainer
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightText
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextVariant
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBar
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarBackAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarCenter

class LightSettingsActivity : AbstractGBActivity() {
    private var destination by mutableStateOf(Destination.Home)
    private var notificationAccess by mutableStateOf(false)
    private var preferenceRefreshToken by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        updateNotificationAccess()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (destination == Destination.Home) finish() else destination = Destination.Home
            }
        })
        setContentView(ComposeView(this).apply {
            setContent {
            LightTheme {
                when (val current = destination) {
                    Destination.Home -> SettingsHome(
                        onBack = { finish() },
                        onUnits = { destination = Destination.Units },
                        onNotifications = { destination = Destination.Notifications },
                        onLegacy = ::openLegacy,
                    )
                    Destination.Units -> UnitsSettings(
                        onBack = { destination = Destination.Home },
                    )
                    Destination.Notifications -> NotificationsSettings(
                        onBack = { destination = Destination.Home },
                        onLegacy = ::openLegacy,
                        listenerEnabled = notificationAccess,
                        refreshToken = preferenceRefreshToken,
                    )
                }
            }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateNotificationAccess()
    }

    private fun updateNotificationAccess() {
        notificationAccess = PermissionsUtils.checkPermission(
            this,
            PermissionsUtils.CUSTOM_PERM_NOTIFICATION_LISTENER,
        )
        preferenceRefreshToken++
    }

    private fun openLegacy(screenKey: String) {
        startActivity(Intent(this, SettingsActivity::class.java).apply {
            putExtra(AbstractSettingsActivityV2.EXTRA_PREF_SCREEN, screenKey)
        })
    }

    private enum class Destination { Home, Units, Notifications }
}

@Composable
private fun SettingsHome(
    onBack: () -> Unit,
    onUnits: () -> Unit,
    onNotifications: () -> Unit,
    onLegacy: (String) -> Unit,
) {
    val context = LocalContext.current
    SettingsScaffold(context.getString(R.string.action_settings), onBack) {
        LightRow(context.getString(R.string.pref_title_unit_system), context.getString(R.string.light_settings_units_summary), onClick = onUnits)
        LightRow(context.getString(R.string.pref_header_datetime), onClick = { onLegacy("pref_key_datetime") })
        LightRow(context.getString(R.string.pref_header_notifications), onClick = onNotifications)
        LightRow(context.getString(R.string.pref_header_dashboard), onClick = {
            context.startActivity(Intent(context, DashboardPreferencesActivity::class.java))
        })
        LightRow(context.getString(R.string.light_settings_more), context.getString(R.string.light_settings_more_summary), onClick = {
            onLegacy("pref_screen_user_interface")
        })
    }
}

@Composable
private fun NotificationsSettings(
    onBack: () -> Unit,
    onLegacy: (String) -> Unit,
    listenerEnabled: Boolean,
    refreshToken: Int,
) {
    val context = LocalContext.current
    val preferences = GBApplication.getPrefs().preferences
    var screenOn by remember(refreshToken) { mutableStateOf(preferences.getBoolean("notifications_generic_whenscreenon", false)) }
    var lowPriority by remember(refreshToken) { mutableStateOf(preferences.getBoolean("notifications_ignore_low_priority", true)) }
    var workProfile by remember(refreshToken) { mutableStateOf(preferences.getBoolean("notifications_ignore_work_profile", false)) }
    SettingsScaffold(context.getString(R.string.pref_header_notifications), onBack) {
        LightRow(
            title = context.getString(R.string.light_notification_access),
            detail = if (listenerEnabled) context.getString(R.string.on) else context.getString(R.string.off),
            onClick = {
                (context as? Activity)?.let {
                    PermissionsUtils.requestPermission(it, PermissionsUtils.CUSTOM_PERM_NOTIFICATION_LISTENER)
                }
            },
        )
        LightRow(
            title = context.getString(R.string.light_notifications_when_screen_on),
            detail = if (screenOn) context.getString(R.string.on) else context.getString(R.string.off),
            onClick = {
                screenOn = !screenOn
                saveBoolean("notifications_generic_whenscreenon", screenOn)
            },
        )
        LightRow(
            title = context.getString(R.string.pref_title_notifications_ignore_low_priority),
            detail = if (lowPriority) context.getString(R.string.on) else context.getString(R.string.off),
            onClick = {
                lowPriority = !lowPriority
                saveBoolean("notifications_ignore_low_priority", lowPriority)
            },
        )
        LightRow(
            title = context.getString(R.string.pref_title_notifications_ignore_work_profile),
            detail = if (workProfile) context.getString(R.string.on) else context.getString(R.string.off),
            onClick = {
                workProfile = !workProfile
                saveBoolean("notifications_ignore_work_profile", workProfile)
            },
        )
        LightRow(context.getString(R.string.pref_applications_settings), context.getString(R.string.light_notifications_applications_summary), onClick = {
            context.startActivity(Intent(context, AppBlacklistActivity::class.java))
        })
        LightRow(context.getString(R.string.advanced_settings), context.getString(R.string.light_notifications_advanced_summary), onClick = {
            context.startActivity(Intent(context, NotificationManagementActivity::class.java))
        })
    }
}

@Composable
private fun UnitsSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = GBApplication.getPrefs().preferences
    var distance by remember { mutableStateOf(preferences.getString("unit_distance", "metric") ?: "metric") }
    var temperature by remember { mutableStateOf(preferences.getString("unit_temperature", "celsius") ?: "celsius") }
    var weight by remember { mutableStateOf(preferences.getString("unit_weight", "kilogram") ?: "kilogram") }
    var nautical by remember { mutableStateOf(preferences.getBoolean("units_nautical", true)) }

    SettingsScaffold(context.getString(R.string.pref_title_unit_system), onBack) {
        UnitChoice(
            title = context.getString(R.string.pref_title_unit_distance),
            selected = distance,
            entries = context.resources.getStringArray(R.array.pref_entries_unit_system),
            values = context.resources.getStringArray(R.array.pref_values_unit_system),
            onSelected = { value -> distance = value; saveUnit("unit_distance", value) },
        )
        UnitChoice(
            title = context.getString(R.string.pref_title_unit_temperature),
            selected = temperature,
            entries = context.resources.getStringArray(R.array.temperature_scales),
            values = context.resources.getStringArray(R.array.temperature_scales_values),
            onSelected = { value -> temperature = value; saveUnit("unit_temperature", value) },
        )
        UnitChoice(
            title = context.getString(R.string.pref_title_unit_weight),
            selected = weight,
            entries = context.resources.getStringArray(R.array.weight_scale_units),
            values = context.resources.getStringArray(R.array.weight_scale_values),
            onSelected = { value -> weight = value; saveUnit("unit_weight", value) },
        )
        LightRow(
            title = context.getString(R.string.pref_title_nautical_units),
            detail = if (nautical) context.getString(R.string.on) else context.getString(R.string.off),
            onClick = {
                nautical = !nautical
                saveBoolean("units_nautical", nautical)
            },
        )
    }
}

@Composable
private fun UnitChoice(
    title: String,
    selected: String,
    entries: Array<String>,
    values: Array<String>,
    onSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    LightText(title, LightTextVariant.Detail, secondary = true)
    values.forEachIndexed { index, value ->
        LightRow(
            title = entries[index],
            detail = if (value == selected) context.getString(R.string.light_selected) else null,
            onClick = { onSelected(value) },
        )
    }
}

@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    androidx.compose.foundation.layout.Column(modifier = Modifier) {
        LightTopBar(
            leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back), onBack),
            center = LightTopBarCenter.Text(title),
        )
        LightScrollContainer(modifier = Modifier.weight(1f)) { content() }
    }
}

private fun saveUnit(key: String, value: String) {
    GBApplication.getPrefs().preferences.edit().putString(key, value).apply()
    android.os.Handler(android.os.Looper.getMainLooper()).post {
        GBApplication.deviceService().onSendConfiguration(key)
    }
}

private fun saveBoolean(key: String, value: Boolean) {
    GBApplication.getPrefs().preferences.edit().putBoolean(key, value).apply()
}
