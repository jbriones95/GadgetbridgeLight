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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.compose.ui.platform.ComposeView;


import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapterv2;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.FindDeviceTarget;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightStateView;
import nodomain.freeyourgadget.gadgetbridge.ui.light.LightDevicesView;

public class DevicesFragment extends Fragment implements HeaderShadePage {

    private DeviceManager deviceManager;
    private GBDeviceAdapterv2 mGBDeviceAdapter;
    private RecyclerView deviceListView;
    private View fab;
    private ComposeView emptyState;
    private ComposeView lightDeviceList;
    List<GBDevice> deviceList;
    private  HashMap<String, DailyTotals> deviceActivityHashMap = new HashMap();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
            switch (Objects.requireNonNull(action)) {
                case DeviceManager.ACTION_DEVICES_CHANGED:
                case GBApplication.ACTION_NEW_DATA:
                    if (action.equals(GBApplication.ACTION_NEW_DATA)) {
                        createRefreshTask("get activity data", requireContext(), device).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    if (device != null) {
                        // Refresh only this device
                        refreshSingleDevice(device);
                    } else {
                        refreshPairedDevices();
                    }

                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    handleRealtimeSample(device, intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
            }
        }
    };

    private void handleRealtimeSample(GBDevice device, Serializable extra) {
        final int heartRate;
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            heartRate = sample.getHeartRate();
        } else if (extra instanceof HeartRateSample) {
            heartRate = ((HeartRateSample) extra).getHeartRate();
        } else {
            return;
        }
        if (HeartRateUtils.getInstance().isValidHeartRateValue(heartRate)) {
            if (device != null) {
                refreshSingleDevice(device);
                LightDevicesView.updateHeartRate(lightDeviceList, device.getAddress(), heartRate);
            } else {
                refreshPairedDevices();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View currentView = inflater.inflate(R.layout.fragment_devices, container, false);

        deviceManager = ((GBApplication) getActivity().getApplication()).getDeviceManager();

        deviceListView = currentView.findViewById(R.id.deviceListView);
        emptyState = currentView.findViewById(R.id.no_devices_text);
        LightStateView.bind(emptyState, getString(R.string.info_no_devices_connected));
        lightDeviceList = currentView.findViewById(R.id.light_device_list);
        deviceListView.setVisibility(View.GONE);
        deviceListView.setHasFixedSize(true);
        deviceListView.setLayoutManager(new LinearLayoutManager(currentView.getContext()));

        deviceList = deviceManager.getDevices();
        LightDevicesView.bind(
                lightDeviceList,
                currentView.getContext(),
                deviceList,
                new Consumer<String>() {
                    @Override
                    public void accept(String address) {
                        openDevice(address);
                    }
                },
                new Consumer<String>() {
                    @Override
                    public void accept(String address) {
                        showDeviceActions(address);
                    }
                },
                new java.util.function.BiConsumer<String, String>() {
                    @Override
                    public void accept(String address, String actionId) {
                        performLightDeviceAction(address, actionId);
                    }
                },
                new java.util.function.BiConsumer<String, String>() {
                    @Override
                    public void accept(String address, String actionId) {
                        performLightQuickAction(address, actionId);
                    }
                },
                new java.util.function.BiConsumer<String, String>() {
                    @Override
                    public void accept(String address, String target) {
                        startLightFindDevice(address, target);
                    }
                },
                new java.util.function.BiConsumer<String, String>() {
                    @Override
                    public void accept(String address, String target) {
                        stopLightFindDevice(address, target);
                    }
                }
        );
        mGBDeviceAdapter = new GBDeviceAdapterv2(currentView.getContext(), deviceList, deviceActivityHashMap);
        mGBDeviceAdapter.setHasStableIds(true);

        deviceListView.setAdapter(this.mGBDeviceAdapter);

        // get activity data asynchronously, this fills the deviceActivityHashMap
        // and calls refreshPairedDevices() → notifyDataSetChanged
        deviceListView.post(new Runnable() {
            @Override
            public void run() {
                if (getContext() != null) {
                    createRefreshTask("get activity data", getContext(), null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });

        fab = currentView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDiscoveryActivity();
            }
        });

        showFabIfNecessary();

        /* uncomment to enable fixed-swipe to reveal more actions

        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT , ItemTouchHelper.RIGHT) {
            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if(dX>50)
                    dX = 50;
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                GB.toast(getBaseContext(), "onMove", Toast.LENGTH_LONG, GB.ERROR);

                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                GB.toast(getBaseContext(), "onSwiped", Toast.LENGTH_LONG, GB.ERROR);

            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                        RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                        int actionState, boolean isCurrentlyActive) {
            }
        });

        swipeToDismissTouchHelper.attachToRecyclerView(deviceListView);
        */

        registerForContextMenu(deviceListView);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_NEW_DATA);
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filterLocal);

        refreshPairedDevices();

        return currentView;
    }

    @Nullable
    @Override
    public View getHeaderShade() {
        final View view = getView();
        return view != null ? view.findViewById(R.id.devices_top_shade) : null;
    }

    private void launchDiscoveryActivity() {
        startActivity(new Intent(getActivity(), DiscoveryActivityV2.class));
    }

    private void openDevice(String address) {
        if (mGBDeviceAdapter == null || lightDeviceList == null) return;
        for (GBDevice device : deviceList) {
            if (address.equals(device.getAddress())) {
                mGBDeviceAdapter.openDevice(lightDeviceList, device);
                return;
            }
        }
    }

    private void showFabIfNecessary() {
        if (GBApplication.getPrefs().getBoolean("display_add_device_fab", true)) {
            fab.setVisibility(View.VISIBLE);
        } else {
            if (deviceManager.getDevices().size() < 1) {
                fab.setVisibility(View.VISIBLE);
            } else {
                fab.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGBDeviceAdapter != null) {
            mGBDeviceAdapter.onResume();
        }
        if (deviceManager != null && lightDeviceList != null) {
            deviceList = deviceManager.getDevices();
            refreshPairedDevices();
        }
    }

    @Override
    public void onDestroyView() {
        if (deviceListView != null) unregisterForContextMenu(deviceListView);
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        }
        super.onDestroyView();
    }

    private DailyTotals getSteps(GBDevice device, DBHandler db) {
        Calendar day = GregorianCalendar.getInstance();

        return DailyTotals.getDailyTotalsForDevice(device, day, db);
    }

    public void refreshPairedDevices() {
        if (mGBDeviceAdapter != null) {
            mGBDeviceAdapter.rebuildFolders();
            mGBDeviceAdapter.notifyDataSetChanged();
        }
        if (emptyState != null && deviceList != null) {
            emptyState.setVisibility(deviceList.isEmpty() ? View.VISIBLE : View.GONE);
            if (lightDeviceList == null) return;
            lightDeviceList.setVisibility(deviceList.isEmpty() ? View.GONE : View.VISIBLE);
            LightDevicesView.update(
                    lightDeviceList,
                    getContext(),
                    deviceList
            );
        }
    }

    public void refreshSingleDevice(final GBDevice device) {
        if (mGBDeviceAdapter != null) {
            mGBDeviceAdapter.refreshSingleDevice(device);
        }
        LightDevicesView.update(lightDeviceList, getContext(), deviceList);
    }

    private void showDeviceActions(String address) {
        if (mGBDeviceAdapter == null || lightDeviceList == null) return;
        for (GBDevice device : deviceList) {
            if (address.equals(device.getAddress())) {
                LightDevicesView.showActions(
                        lightDeviceList,
                        address,
                        mGBDeviceAdapter.getDeviceActionsForLight(lightDeviceList, device)
                );
                return;
            }
        }
    }

    private void performLightDeviceAction(String address, String actionId) {
        if (mGBDeviceAdapter == null || lightDeviceList == null) return;
        for (GBDevice device : deviceList) {
            if (address.equals(device.getAddress())) {
                mGBDeviceAdapter.performDeviceAction(lightDeviceList, device, actionId);
                return;
            }
        }
    }

    private void performLightQuickAction(String address, String actionId) {
        if (lightDeviceList == null) return;
        for (GBDevice device : deviceList) {
            if (!address.equals(device.getAddress())) continue;
            if ("heart_rate".equals(actionId)) {
                LightDevicesView.showHeartRate(lightDeviceList, address);
                GBApplication.deviceService(device).onHeartRateTest();
            } else if ("sync".equals(actionId)) {
                GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);
            } else if ("find_device".equals(actionId)) {
                if (device.getType() == DeviceType.VIBRATISSIMO) {
                    Intent intent = new Intent(getContext(), nodomain.freeyourgadget.gadgetbridge.activities.VibrationActivity.class);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivity(intent);
                    return;
                }
                LightDevicesView.showFindDialog(
                        lightDeviceList,
                        address,
                        device.getDeviceCoordinator().supportsFindDevicePerEarbud(device)
                );
            }
            return;
        }
    }

    private GBDevice findLightDevice(String address) {
        if (deviceList == null) return null;
        for (GBDevice device : deviceList) {
            if (address.equals(device.getAddress())) return device;
        }
        return null;
    }

    private void startLightFindDevice(String address, String targetName) {
        GBDevice device = findLightDevice(address);
        if (device == null) return;
        GBApplication.deviceService(device).onFindDevice(true, FindDeviceTarget.valueOf(targetName));
    }

    private void stopLightFindDevice(String address, String targetName) {
        GBDevice device = findLightDevice(address);
        if (device != null && targetName != null) {
            GBApplication.deviceService(device).onFindDevice(false, FindDeviceTarget.valueOf(targetName));
        }
    }

    public RefreshTask createRefreshTask(String task, Context context, GBDevice device) {
        return new RefreshTask(task, context, device);
    }

    public class RefreshTask extends DBAccess {
        private final GBDevice device;

        public RefreshTask(final String task, final Context context, final GBDevice device) {
            super(task, context, false);
            this.device = device;
        }

        @Override
        protected void doInBackground(final DBHandler db) {
            if (device != null) {
                updateDevice(db, device);
            } else {
                for (GBDevice gbDevice : deviceList) {
                    updateDevice(db, gbDevice);
                }
            }
        }

        private void updateDevice(final DBHandler db, final GBDevice gbDevice) {
            final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
            final boolean showActivityCard = GBApplication.getDevicePrefs(gbDevice).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD, true);
            if ((coordinator.supportsStepCounter(gbDevice) || coordinator.supportsSleepMeasurement(gbDevice)) && showActivityCard) {
                final DailyTotals stepsAndSleepData = getSteps(gbDevice, db);
                deviceActivityHashMap.put(gbDevice.getAddress(), stepsAndSleepData);
            }
        }

        @Override
        protected void onPostExecute(final Object o) {
            if (device != null) {
                refreshSingleDevice(device);
            } else {
                refreshPairedDevices();
            }
        }
    }
}
