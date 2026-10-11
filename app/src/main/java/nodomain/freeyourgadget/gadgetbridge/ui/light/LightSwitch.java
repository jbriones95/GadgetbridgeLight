package nodomain.freeyourgadget.gadgetbridge.ui.light;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;

import com.google.android.material.materialswitch.MaterialSwitch;

/** Light SDK-style line and circle toggle with SwitchCompat behavior and accessibility. */
public class LightSwitch extends MaterialSwitch {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public LightSwitch(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setTrackDrawable(new ColorDrawable(Color.TRANSPARENT));
        setThumbDrawable(new ColorDrawable(Color.TRANSPARENT));
        setShowText(false);
        setMinimumWidth((int) (40 * density));
        setMinimumHeight((int) (40 * density));
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        final float centerY = getHeight() / 2f;
        final float stroke = 2.2f * density;
        final float radius = 6f * density;
        final float left = 5f * density;
        final float offCenter = 11f * density;
        final float onCenter = 29f * density;
        final float right = 35f * density;

        paint.setColor(resolveContentColor());
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStyle(Paint.Style.STROKE);
        if (isChecked()) {
            canvas.drawLine(left, centerY, onCenter - radius, centerY, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(onCenter, centerY, radius, paint);
        } else {
            canvas.drawLine(offCenter + radius, centerY, right, centerY, paint);
            canvas.drawCircle(offCenter, centerY, radius, paint);
            paint.setColor(resolveBackgroundColor());
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(offCenter, centerY, radius - stroke, paint);
        }
    }

    private int resolveContentColor() {
        final android.util.TypedValue value = new android.util.TypedValue();
        if (getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, value, true)) {
            return value.resourceId == 0 ? value.data : getResources().getColor(value.resourceId, getContext().getTheme());
        }
        return Color.WHITE;
    }

    private int resolveBackgroundColor() {
        final android.util.TypedValue value = new android.util.TypedValue();
        if (getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, value, true)) {
            return value.resourceId == 0 ? value.data : getResources().getColor(value.resourceId, getContext().getTheme());
        }
        return Color.BLACK;
    }
}
