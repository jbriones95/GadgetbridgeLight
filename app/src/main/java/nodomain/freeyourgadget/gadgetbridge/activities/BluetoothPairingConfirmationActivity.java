package nodomain.freeyourgadget.gadgetbridge.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nodomain.freeyourgadget.gadgetbridge.R;

public class BluetoothPairingConfirmationActivity extends AbstractGBActivity {
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_PAIRING_VARIANT = "pairing_variant";
    public static final String EXTRA_PAIRING_KEY = "pairing_key";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String address = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
        final int variant = getIntent().getIntExtra(EXTRA_PAIRING_VARIANT, BluetoothDevice.PAIRING_VARIANT_PIN);
        final int key = getIntent().getIntExtra(EXTRA_PAIRING_KEY, 0);
        final BluetoothDevice device = address == null
                ? null
                : BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

        if (device == null) {
            finish();
            return;
        }

        final String passkey = String.format(java.util.Locale.ROOT, "%06d", key);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.bluetooth_pairing_confirmation_title)
                .setMessage(getString(R.string.bluetooth_pairing_confirmation_message, device.getName(), passkey))
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    reject(device);
                    finish();
                })
                .setPositiveButton(R.string.bluetooth_pairing_confirmation_confirm, (dialog, which) -> {
                    confirm(device, variant, passkey);
                    finish();
                })
                .show();
    }

    private void confirm(BluetoothDevice device, int variant, String passkey) {
        if (variant == BluetoothDevice.PAIRING_VARIANT_PIN
                || variant == 7 /* PAIRING_VARIANT_PIN_16_DIGITS */) {
            device.setPin(passkey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } else {
            device.setPairingConfirmation(true);
        }
    }

    private void reject(BluetoothDevice device) {
        device.setPairingConfirmation(false);
    }
}
