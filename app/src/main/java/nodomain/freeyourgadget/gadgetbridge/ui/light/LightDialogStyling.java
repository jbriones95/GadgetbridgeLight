package nodomain.freeyourgadget.gadgetbridge.ui.light;

import android.util.TypedValue;

import androidx.appcompat.app.AlertDialog;

/** Applies the flat Light action treatment without changing dialog behavior. */
public final class LightDialogStyling {
    private LightDialogStyling() {
    }

    public static void apply(final AlertDialog dialog) {
        dialog.setOnShowListener(ignored -> {
            final TypedValue colorValue = new TypedValue();
            dialog.getContext().getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorOnSurface, colorValue, true);
            final int color = colorValue.data;
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(color);
            }
        });
    }
}
