package nodomain.freeyourgadget.gadgetbridge.activities.app_specific_notifications

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightText
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextInput
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextVariant
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBar
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarBackAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarCenter
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils

class AppSpecificNotificationSettingsActivity : AbstractGBActivity() {
    private var refreshToken by mutableStateOf(0)
    private lateinit var device: GBDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE) ?: run {
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        setContentView(ComposeView(this).apply {
            setContent {
                LightTheme {
                    AppSpecificNotificationAppsScreen(
                        deviceName = device.aliasOrName,
                        refreshToken = refreshToken,
                        onBack = ::finish,
                        onOpenApp = { app ->
                            startActivity(Intent(this@AppSpecificNotificationSettingsActivity, AppSpecificNotificationSettingsDetailActivity::class.java).apply {
                                putExtra(STRING_EXTRA_PACKAGE_NAME, app.packageName)
                                putExtra(STRING_EXTRA_PACKAGE_TITLE, app.label)
                                putExtra(GBDevice.EXTRA_DEVICE, device)
                            })
                        },
                    )
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshToken++
    }

    companion object {
        const val STRING_EXTRA_PACKAGE_NAME = "packageName"
        const val STRING_EXTRA_PACKAGE_TITLE = "packageTitle"
    }
}

private data class DeviceNotificationApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@Composable
private fun AppSpecificNotificationAppsScreen(
    deviceName: String,
    refreshToken: Int,
    onBack: () -> Unit,
    onOpenApp: (DeviceNotificationApp) -> Unit,
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    val apps = remember(refreshToken) {
        val isBlacklist = GBApplication.getPrefs().getString("notification_list_is_blacklist", "true") == "true"
        NotificationUtils.getAllApplications(context)
            .filter { packageName ->
                val blocked = GBApplication.appIsNotifBlacklisted(packageName) ||
                    GBApplication.appIsPebbleBlacklisted(GBApplication.packageNameToPebbleMsgSender(packageName))
                if (isBlacklist) !blocked else blocked
            }
            .map { packageName ->
                DeviceNotificationApp(
                    packageName = packageName,
                    label = NotificationUtils.getApplicationLabel(context, packageName) ?: packageName,
                    icon = NotificationUtils.getAppIcon(context, packageName),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
    val visibleApps = apps.filter {
        search.isBlank() || it.label.contains(search, ignoreCase = true) || it.packageName.contains(search, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        LightTopBar(
            leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back), onBack),
            center = LightTopBarCenter.Text(context.getString(R.string.prefs_wena3_notification_per_app_settings_title)),
        )
        LightText(deviceName, LightTextVariant.Detail, secondary = true, modifier = Modifier.padding(horizontal = 14.dp))
        LightTextInput(value = search, onValueChange = { search = it }, hint = context.getString(R.string.search))
        LazyColumn(Modifier.weight(1f)) {
            if (visibleApps.isEmpty()) item {
                LightText(
                    context.getString(R.string.no_applications_found),
                    LightTextVariant.Detail,
                    secondary = true,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            items(visibleApps, key = { it.packageName }) { app ->
                Row(
                    Modifier.fillMaxWidth()
                        .semantics {
                            role = Role.Button
                            contentDescription = context.getString(R.string.notification_app_row_description, app.label, context.getString(R.string.appmanager_app_edit))
                        }
                        .clickable { onOpenApp(app) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.icon)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        LightText(app.label)
                        LightText(app.packageName, LightTextVariant.Detail, secondary = true, maxLines = 1)
                    }
                    LightText(context.getString(R.string.appmanager_app_edit), LightTextVariant.Detail)
                }
            }
        }
    }
}

@Composable
private fun AppIcon(drawable: Drawable?) {
    if (drawable == null) return
    AndroidView(
        factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        modifier = Modifier.size(40.dp),
        update = { it.setImageDrawable(drawable) },
    )
}
