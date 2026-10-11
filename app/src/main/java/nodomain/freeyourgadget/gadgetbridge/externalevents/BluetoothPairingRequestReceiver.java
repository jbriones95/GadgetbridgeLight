/*  Copyright (C) 2017-2024 Andreas Shimokawa, Daniel Dakhno, Daniele Gobbetti,
    João Paulo Barraca, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.activities.BluetoothPairingConfirmationActivity;

public class BluetoothPairingRequestReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(BluetoothPairingRequestReceiver.class);

    final DeviceCommunicationService service;

    public BluetoothPairingRequestReceiver(DeviceCommunicationService service) {
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();


        if (!BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            return;
        }
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            return;
        }

        final int pairingVariant = intent.getIntExtra(
                BluetoothDevice.EXTRA_PAIRING_VARIANT,
                BluetoothDevice.PAIRING_VARIANT_PIN
        );
        final int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, 0);
        Log.i("BluetoothPairingRequestReceiver", "Pairing request for " + device.getAddress()
                + " variant=" + pairingVariant + " key=" + pairingKey);

        Intent confirmationIntent = new Intent(context, BluetoothPairingConfirmationActivity.class)
                .putExtra(BluetoothPairingConfirmationActivity.EXTRA_DEVICE_ADDRESS, device.getAddress())
                .putExtra(BluetoothPairingConfirmationActivity.EXTRA_PAIRING_VARIANT, pairingVariant)
                .putExtra(BluetoothPairingConfirmationActivity.EXTRA_PAIRING_KEY, pairingKey)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(confirmationIntent);

        GBDevice gbDevice = null;
        try {
            gbDevice = service.getDeviceByAddress(device.getAddress());

            // Do not abort the system pairing broadcast here. Android owns the PIN and
            // numeric-confirmation UI; consuming this request prevents that UI from appearing.
            LOG.debug("Allowing system pairing confirmation for {}", gbDevice.getName());
        } catch (DeviceCommunicationService.DeviceNotFoundException e) {
            LOG.warn("exception in onReceive", e);
        }
    }
}
