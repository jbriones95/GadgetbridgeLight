package nodomain.freeyourgadget.gadgetbridge.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.ui.viewinterop.AndroidView
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightSwitchRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightText
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextInput
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextVariant
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBar
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarBackAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarCenter
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils

class AppBlacklistActivity : AbstractGBActivity() {
    private var refreshToken by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        setContentView(ComposeView(this).apply {
            setContent {
                LightTheme {
                    AppBlacklistScreen(
                        refreshToken = refreshToken,
                        onBack = ::finish,
                        onOpenFilter = { packageName ->
                            startActivity(Intent(this@AppBlacklistActivity, NotificationFilterActivity::class.java).apply {
                                putExtra(STRING_EXTRA_PACKAGE_NAME, packageName)
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
    }
}

private data class NotificationApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val notificationBlocked: Boolean,
    val pebbleBlocked: Boolean,
)

@Composable
private fun AppBlacklistScreen(
    refreshToken: Int,
    onBack: () -> Unit,
    onOpenFilter: (String) -> Unit,
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var revision by remember(refreshToken) { mutableStateOf(0) }
    val apps = remember(refreshToken, revision) {
        NotificationUtils.getAllApplications(context)
            .map { packageName ->
                NotificationApp(
                    packageName = packageName,
                    label = NotificationUtils.getApplicationLabel(context, packageName) ?: packageName,
                    icon = NotificationUtils.getAppIcon(context, packageName),
                    notificationBlocked = GBApplication.appIsNotifBlacklisted(packageName),
                    pebbleBlocked = GBApplication.appIsPebbleBlacklisted(
                        GBApplication.packageNameToPebbleMsgSender(packageName),
                    ),
                )
            }
            .sortedWith(compareByDescending<NotificationApp> { it.notificationBlocked || it.pebbleBlocked }.thenBy { it.label.lowercase() })
    }
    val visibleApps = apps.filter {
        search.isBlank() || it.label.contains(search, ignoreCase = true) || it.packageName.contains(search, ignoreCase = true)
    }
    val isBlacklist = GBApplication.getPrefs().getString("notification_list_is_blacklist", "true") == "true"

    Column(Modifier.fillMaxSize()) {
        LightTopBar(
            leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back), onBack),
            center = LightTopBarCenter.Text(context.getString(R.string.notification_apps_title)),
        )
        LightTextInput(
            value = search,
            onValueChange = { search = it },
            hint = context.getString(R.string.search),
        )
        LazyColumn(Modifier.weight(1f)) {
            item {
                LightText(
                    context.getString(R.string.light_notifications_applications_summary),
                    LightTextVariant.Detail,
                    secondary = true,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
                LightRow(
                    title = context.getString(R.string.check_all_applications),
                    onClick = {
                        GBApplication.setAppsNotifBlackList(NotificationUtils.getAllApplications(context).toSet())
                        revision++
                    },
                )
                LightRow(
                    title = context.getString(R.string.uncheck_all_applications),
                    onClick = {
                        GBApplication.setAppsNotifBlackList(emptySet())
                        revision++
                    },
                )
            }
            if (visibleApps.isEmpty()) item {
                LightText(
                    context.getString(R.string.no_applications_found),
                    LightTextVariant.Detail,
                    secondary = true,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            items(visibleApps, key = { it.packageName }) { app ->
                AppBlacklistRow(
                    app = app,
                    configureLabel = context.getString(R.string.appmanager_app_edit),
                    onToggleNotifications = {
                        if (app.notificationBlocked) GBApplication.removeFromAppsNotifBlacklist(app.packageName)
                        else GBApplication.addAppToNotifBlacklist(app.packageName)
                        revision++
                    },
                    onTogglePebble = {
                        if (app.pebbleBlocked) GBApplication.removeFromAppsPebbleBlacklist(app.packageName)
                        else GBApplication.addAppToPebbleBlacklist(app.packageName)
                        revision++
                    },
                    onConfigure = {
                        val canConfigure = if (isBlacklist) app.notificationBlocked.not() else app.notificationBlocked
                        if (canConfigure) onOpenFilter(app.packageName)
                        else android.widget.Toast.makeText(
                            context,
                            context.getString(if (isBlacklist) R.string.toast_app_must_not_be_selected else R.string.toast_app_must_be_selected),
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }
        }
    }
}

@Composable
private fun AppBlacklistRow(
    app: NotificationApp,
    configureLabel: String,
    onToggleNotifications: () -> Unit,
    onTogglePebble: () -> Unit,
    onConfigure: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        LightSwitchRow(
            title = app.label,
            detail = app.packageName,
            checked = app.notificationBlocked,
            onCheckedChange = { onToggleNotifications() },
            leadingContent = { AppIcon(app.icon) },
        )
        LightSwitchRow(
            title = context.getString(R.string.notification_app_pebble),
            checked = app.pebbleBlocked,
            onCheckedChange = { onTogglePebble() },
        )
        LightRow(title = configureLabel, onClick = onConfigure)
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
