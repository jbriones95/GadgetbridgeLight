package nodomain.freeyourgadget.gadgetbridge.activities

import android.os.Bundle
import android.widget.Toast
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
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilter
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterDao
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterEntry
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterEntryDao
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightRow
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightText
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextInput
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTextVariant
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTheme
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBar
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarBackAction
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightTopBarCenter
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils
import org.apache.commons.lang3.StringUtils

class NotificationFilterActivity : AbstractGBActivity() {
    private var filterMode by mutableStateOf(NOTIFICATION_FILTER_MODE_NONE)
    private var filterSubMode by mutableStateOf(NOTIFICATION_FILTER_SUBMODE_ANY)
    private var words by mutableStateOf("")
    private var filterPackageName: String? = null
    private var notificationFilter: NotificationFilter? = null
    private var filterEntryIds: List<Long> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        filterPackageName = intent.getStringExtra(AppBlacklistActivity.STRING_EXTRA_PACKAGE_NAME)
            ?.takeIf { StringUtils.isNotBlank(it) }
            ?.lowercase()
        if (filterPackageName == null) {
            finish()
            return
        }

        try {
            loadFilter(filterPackageName!!)
        } catch (exception: Exception) {
            android.util.Log.e("NotificationFilterActivity", "Error accessing notification filter", exception)
            Toast.makeText(this, R.string.notification_filter_load_error, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        setContentView(ComposeView(this).apply {
            setContent {
                LightTheme {
                    NotificationFilterScreen(
                        appName = NotificationUtils.getApplicationLabel(this@NotificationFilterActivity, filterPackageName!!)
                            ?: filterPackageName!!,
                        mode = filterMode,
                        subMode = filterSubMode,
                        words = words,
                        onBack = ::finish,
                        onModeChange = { filterMode = it },
                        onSubModeChange = { filterSubMode = it },
                        onWordsChange = { words = it },
                        onSave = ::saveFilter,
                    )
                }
            }
        })
    }

    private fun loadFilter(appPackageName: String) {
        GBApplication.acquireDB().use { db ->
            val filterDao = db.daoSession.notificationFilterDao
            val entryDao = db.daoSession.notificationFilterEntryDao
            val filter = filterDao.queryBuilder()
                .where(NotificationFilterDao.Properties.AppIdentifier.eq(appPackageName))
                .unique()
            val loadedFilter = filter ?: NotificationFilter().apply { appIdentifier = appPackageName }
            notificationFilter = loadedFilter
            filterMode = loadedFilter.notificationFilterMode
            filterSubMode = loadedFilter.notificationFilterSubMode

            if (loadedFilter.id == null) {
                filterEntryIds = emptyList()
                words = ""
            } else {
                val entries = entryDao.queryBuilder()
                    .where(NotificationFilterEntryDao.Properties.NotificationFilterId.eq(loadedFilter.id))
                    .list()
                filterEntryIds = entries.mapNotNull { it.id }
                words = entries.joinToString(separator = "\n") { it.notificationFilterContent ?: "" }
            }
        }
    }

    private fun saveFilter() {
        if (filterMode != NOTIFICATION_FILTER_MODE_NONE && StringUtils.isBlank(words)) {
            Toast.makeText(this, R.string.toast_notification_filter_words_empty_hint, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            GBApplication.acquireDB().use { db ->
                val filterDao = db.daoSession.notificationFilterDao
                val entryDao = db.daoSession.notificationFilterEntryDao
                val filter = requireNotNull(notificationFilter)
                filter.notificationFilterMode = filterMode
                filter.notificationFilterSubMode = filterSubMode
                entryDao.deleteByKeyInTx(filterEntryIds)
                val filterId = filterDao.insertOrReplace(filter)

                if (filterMode != NOTIFICATION_FILTER_MODE_NONE) {
                    words.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { word ->
                            val entry = NotificationFilterEntry().apply {
                                notificationFilterContent = word
                                notificationFilterId = filterId
                            }
                            entryDao.insert(entry)
                        }
                }
            }
            Toast.makeText(this, R.string.toast_notification_filter_saved_successfully, Toast.LENGTH_SHORT).show()
            finish()
        } catch (exception: Exception) {
            android.util.Log.e("NotificationFilterActivity", "Error saving notification filter", exception)
            Toast.makeText(this, R.string.notification_filter_save_error, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val NOTIFICATION_FILTER_MODE_NONE = 0
        const val NOTIFICATION_FILTER_MODE_WHITELIST = 1
        const val NOTIFICATION_FILTER_MODE_BLACKLIST = 2
        const val NOTIFICATION_FILTER_SUBMODE_ANY = 0
        const val NOTIFICATION_FILTER_SUBMODE_ALL = 1
    }
}

@Composable
private fun NotificationFilterScreen(
    appName: String,
    mode: Int,
    subMode: Int,
    words: String,
    onBack: () -> Unit,
    onModeChange: (Int) -> Unit,
    onSubModeChange: (Int) -> Unit,
    onWordsChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val filterModes = context.resources.getStringArray(R.array.notification_filter_modes_entries)
    val subModes = context.resources.getStringArray(R.array.notification_filter_submodes_entries)
    Column(Modifier.fillMaxSize()) {
        LightTopBar(
            leftAction = LightTopBarBackAction(context.getString(R.string.light_action_back), onBack),
            center = LightTopBarCenter.Text(context.getString(R.string.title_activity_notification_filter)),
            rightAction = LightTopBarAction(
                label = context.getString(R.string.save),
                contentDescription = context.getString(R.string.save),
                onClick = onSave,
            ),
        )
        Column(
            Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            LightText(appName, LightTextVariant.Heading)
            LightText(context.getString(R.string.filter_mode), LightTextVariant.Heading)
            filterModes.forEachIndexed { index, label ->
                LightRow(
                    title = label,
                    detail = if (mode == index) context.getString(R.string.light_selected) else null,
                    onClick = { onModeChange(index) },
                )
            }

            if (mode != NotificationFilterActivity.NOTIFICATION_FILTER_MODE_NONE) {
                LightText(context.getString(R.string.mode_configuration), LightTextVariant.Heading)
                subModes.forEachIndexed { index, label ->
                    LightRow(
                        title = label,
                        detail = if (subMode == index) context.getString(R.string.light_selected) else null,
                        onClick = { onSubModeChange(index) },
                    )
                }
                LightText(
                    context.getString(R.string.edittext_notification_filter_words_hint),
                    LightTextVariant.Detail,
                    secondary = true,
                )
                LightTextInput(
                    value = words,
                    onValueChange = onWordsChange,
                    hint = context.getString(R.string.edittext_notification_filter_words_hint),
                    singleLine = false,
                )
            }
        }
    }
}
