/*  Copyright (C) 2016-2024 Andreas Shimokawa, Andrzej Surowiec, Arjan
    Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Ganblejs,
    gfwilliams, Gordon Williams, Johannes Tysiak, José Rebelo, marco.altomonte,
    Petr Vaněk, Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.welcome.WelcomeActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BarShade;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBChangeLog;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils;
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightMainNavigation;

//TODO: extend AbstractGBActivity, but it requires actionbar that is not available
public class ControlCenterv2 extends AppCompatActivity implements GBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ControlCenterv2.class);
    public static final long REALTIME_HR_SAMPLE_TTL_MS = 5_000L;
    public static final int MENU_REFRESH_CODE = 1;
    private boolean isLanguageInvalid = false;
    private boolean isThemeInvalid = false;
    private ViewPager2 viewPager;
    private ComposeView navigationView;
    private ComposeView topBarView;
    private FragmentStateAdapter pagerAdapter;
    private SwipeRefreshLayout swipeLayout;
    private AlertDialog clDialog;
    private BarShade.ScrollListener barShades;
    private View toolbarShade;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBApplication.ACTION_LANGUAGE_CHANGE:
                    setLanguage(GBApplication.getLanguage(), true);
                    break;
                case GBApplication.ACTION_THEME_CHANGE:
                    isThemeInvalid = true;
                    break;
                case GBApplication.ACTION_QUIT:
                    finish();
                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    handleRealtimeSample(device, intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null && !dev.isBusy()) {
                        swipeLayout.setRefreshing(false);
                    }
                    break;
            }
        }
    };
    private boolean pesterWithPermissions = true;
    private final Map<GBDevice, ActivitySample> currentHRSample = new HashMap<>();
    private final Map<GBDevice, Long> currentHRSampleReceivedAt = new HashMap<>();

    public ActivitySample getCurrentHRSample(final GBDevice device) {
        final ActivitySample sample = currentHRSample.get(device);

        if (!device.getDeviceCoordinator().supportsLiveOnlyHeartRateDisplay(device)) {
            return sample;
        }

        final Long receivedAt = currentHRSampleReceivedAt.get(device);

        if (sample == null || receivedAt == null) {
            return null;
        }

        if (SystemClock.elapsedRealtime() - receivedAt > REALTIME_HR_SAMPLE_TTL_MS) {
            currentHRSample.remove(device);
            currentHRSampleReceivedAt.remove(device);
            return null;
        }

        return sample;
    }

    private void setCurrentHRSample(final GBDevice device, ActivitySample sample) {
        if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())) {
            currentHRSample.put(device, sample);
            currentHRSampleReceivedAt.put(device, SystemClock.elapsedRealtime());
        }
    }

    private void handleRealtimeSample(final GBDevice device, Serializable extra) {
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            setCurrentHRSample(device, sample);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GBPrefs prefs = GBApplication.getPrefs();

        // Determine availability of device with activity tracking functionality
        boolean activityTrackerAvailable = false;
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice dev : devices) {
            if (dev.getDeviceCoordinator().supportsActivityTracking(dev)) {
                activityTrackerAvailable = true;
                break;
            }
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("cl")) {
                GBChangeLog cl = GBChangeLog.createChangeLog(this);
                try {
                    if (cl.hasChanges(false)) {
                        clDialog = cl.getMaterialLogDialog();
                    } else {
                        clDialog = cl.getMaterialFullLogDialog();
                    }
                    clDialog.show();
                } catch (Exception ex) {
                    GB.toast(getBaseContext(), getString(R.string.error_showing_changelog), Toast.LENGTH_LONG, GB.ERROR, ex);
                }
            }
        }

        // Initialize bottom navigation
        navigationView = findViewById(R.id.bottom_nav_bar);
        // Keep the primary Light-style navigation available on devices without activity tracking.
        if (prefs.getBoolean("display_bottom_navigation_bar", true)) {
            navigationView.setVisibility(View.VISIBLE);
        } else {
            navigationView.setVisibility(View.GONE);
        }
        LightMainNavigation.bindBottomBar(navigationView, 0, selected -> {
            if (selected == 0 || selected == 1) {
                viewPager.setCurrentItem(selected, true);
            } else if (selected == 2) {
                startActivityForResult(new Intent(this, LightSettingsActivity.class), MENU_REFRESH_CODE);
            }
        });

        // Use LightOS top-bar primitives for the app shell; feature actions are wired below.
        topBarView = findViewById(R.id.toolbar);
        // targetSdk 35 forces edge-to-edge. We intercept the insets on the ConstraintLayout
        // itself (main_content) and apply them directly to the toolbar.
        final View mainContent = findViewById(R.id.main_content);
        final int toolbarPaddingLeft = topBarView.getPaddingLeft();
        final int toolbarPaddingRight = topBarView.getPaddingRight();
        final int toolbarPaddingBottom = topBarView.getPaddingBottom();
        swipeLayout = findViewById(R.id.dashboard_swipe_layout);
        ViewCompat.setOnApplyWindowInsetsListener(mainContent, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            topBarView.setPadding(toolbarPaddingLeft, insets.top, toolbarPaddingRight, toolbarPaddingBottom);
            // LightOS owns the system navigation area; adding its inset to the app bar creates a
            // large empty block and pushes the Light-style destinations off the LP3 viewport.
            // #6894 - when the bottom nav is not visible, the FAB would show up behind the system navbar
            final int contentPaddingBottom = navigationView.getVisibility() == View.VISIBLE ? 0 : insets.bottom;
            swipeLayout.setPadding(swipeLayout.getPaddingLeft(), swipeLayout.getPaddingTop(), swipeLayout.getPaddingRight(), contentPaddingBottom);
            return windowInsets;
        });
        toolbarShade = findViewById(R.id.toolbar_shade);
        barShades = new BarShade.ScrollListener(findViewById(R.id.main_content));
        if (navigationView.getVisibility() == View.VISIBLE) {
            barShades.setBottomShade(findViewById(R.id.bottom_nav_shade));
        }

        // Configure ViewPager2 with fragment adapter and default fragment
        viewPager = findViewById(R.id.dashboard_viewpager);
        viewPager.setUserInputEnabled(false);
        pagerAdapter = new MainFragmentsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        final boolean dashboardIsDefault = prefs.getBoolean("dashboard_as_default_view", true) && activityTrackerAvailable;
        final int initialPage = dashboardIsDefault ? 0 : 1;
        viewPager.setCurrentItem(initialPage, false);
        LightMainNavigation.select(navigationView, initialPage);
        bindShellTopBar(initialPage == 0);
        // A page's fragment, and its view, are created after the page is selected
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull final FragmentManager fm, @NonNull final Fragment fragment,
                                              @NonNull final View view, final Bundle savedInstanceState) {
                if (fragment instanceof HeaderShadePage) {
                    barShades.addDivider(((HeaderShadePage) fragment).getHeaderShade());
                }
                bindTopShade();
            }

            @Override
            public void onFragmentViewDestroyed(@NonNull final FragmentManager fm, @NonNull final Fragment fragment) {
                bindTopShade();
            }
        }, false);
        bindTopShade();

        // Keep the selected Light destination and shell actions in sync with the page.
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                LightMainNavigation.select(navigationView, position);
                bindShellTopBar(position == 0);
                bindTopShade();
            }
        });

        // Make sure the SwipeRefreshLayout doesn't interfere with the ViewPager2
        viewPager.getChildAt(0).setOnTouchListener((v, event) -> {
            if (prefs.refreshOnSwipe()) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    swipeLayout.setEnabled(false);
                } else {
                    swipeLayout.setEnabled(true);
                }
            }
            return false;
        });

        // Set pull-down-to-refresh action
        swipeLayout.setEnabled(prefs.refreshOnSwipe());
        swipeLayout.setOnRefreshListener(() -> {
            if (prefs.refreshOnSwipe()) {
                List<GBDevice> devices1 = GBApplication.app().getDeviceManager().getDevices();
                final boolean anyConnected = devices1.stream().anyMatch(GBDevice::isInitialized);
                if (!anyConnected) {
                    // No devices are connected at all
                    GB.toast(getString(R.string.info_no_devices_connected), Toast.LENGTH_LONG, GB.WARN);
                    swipeLayout.setRefreshing(false);
                    return;
                }
                // Fetch activity for all connected devices
                GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);

                // Hide 'refreshing' animation immediately if no devices are connected that support sync
                final boolean anySupported = devices1.stream().filter(GBDevice::isInitialized)
                        .anyMatch(dev -> dev.getDeviceCoordinator().supportsDataFetching(dev));
                if (!anySupported) {
                    swipeLayout.setRefreshing(false);
                    GB.toast(getString(R.string.info_no_devices_to_sync), Toast.LENGTH_LONG, GB.WARN);
                }
            } else {
                swipeLayout.setRefreshing(false);
            }
        });

        // Set up local intent listener
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_LANGUAGE_CHANGE);
        filterLocal.addAction(GBApplication.ACTION_THEME_CHANGE);
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        // Open the Welcome flow on first run, only check permissions on next runs
        boolean firstRun = prefs.getBoolean("first_run", true);
        if (firstRun) {
            launchWelcomeActivity();
        } else {
            pesterWithPermissions = prefs.getBoolean("permission_pestering", true);
            if (pesterWithPermissions && !PermissionsUtils.checkAllPermissions(this)) {
                Intent permissionsIntent = new Intent(this, PermissionsActivity.class);
                permissionsIntent.putExtra(PermissionsActivity.ARG_SHOW_DO_NOT_ASK_BUTTON, true);
                startActivity(permissionsIntent);
            }
        }

        GBChangeLog cl = GBChangeLog.createChangeLog(this);
        boolean showChangelog = prefs.getBoolean("show_changelog", true);
        if (showChangelog && cl.isFirstRun() && cl.hasChanges(cl.isFirstRunEver())) {
            openChangeLog(cl.isFirstRunEver());
        }

        GBApplication.deviceService().requestDeviceInfo();
    }

    /**
     * Uses the shade of the visible page, if it has one of its own, instead of the shade below
     * the toolbar.
     */
    private void bindTopShade() {
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        final View headerShade = fragment instanceof HeaderShadePage
                ? ((HeaderShadePage) fragment).getHeaderShade() : null;
        barShades.setTopShade(headerShade != null ? headerShade : toolbarShade);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (clDialog != null) {
            outState.putBoolean("cl", clDialog.isShowing());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleShortcut(getIntent());
        if (viewPager != null) {
            final int page = viewPager.getCurrentItem();
            LightMainNavigation.select(navigationViewForCurrentPage(), page);
            bindShellTopBar(page == 0);
        }
        if (isLanguageInvalid || isThemeInvalid) {
            isLanguageInvalid = false;
            isThemeInvalid = false;
            recreate();
        }
    }

    private ComposeView navigationViewForCurrentPage() {
        return navigationView;
    }

    private void bindShellTopBar(final boolean dashboard) {
        LightMainNavigation.bindTopBar(
                topBarView,
                dashboard,
                this::openDashboardCalendar,
                () -> startActivity(new Intent(this, DashboardPreferencesActivity.class))
        );
    }

    private void openDashboardCalendar() {
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag("f0");
        if (fragment instanceof DashboardFragment) {
            ((DashboardFragment) fragment).openCalendar();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void openChangeLog(boolean full) {
        final Intent changelogIntent = new Intent(this, ChangeLogActivity.class);
        changelogIntent.putExtra(ChangeLogActivity.EXTRA_FULL, full);
        startActivity(changelogIntent);
    }


    private void launchWelcomeActivity() {
        startActivity(new Intent(this, WelcomeActivity.class));
    }

    private void handleShortcut(Intent intent) {
        if (ACTION_CONNECT.equals(intent.getAction())) {
            String btDeviceAddress = intent.getStringExtra("device");
            if (btDeviceAddress != null) {
                GBDevice candidate = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress);
                if (candidate != null && !candidate.isConnected()) {
                    GBApplication.deviceService(candidate).connect();
                }
            }
        }
    }

    @Override
    public void setLanguage(Locale language, boolean invalidateLanguage) {
        if (invalidateLanguage) {
            isLanguageInvalid = true;
        }
        AndroidUtils.setLanguage(this, language);
    }

    private class MainFragmentsPagerAdapter extends FragmentStateAdapter {
        public MainFragmentsPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new DashboardFragment();
            } else {
                return new DevicesFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
