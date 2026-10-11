/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.welcome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import nodomain.freeyourgadget.gadgetbridge.R;

public class WelcomePageIndicator extends View {
    private ViewPager2 viewPager;
    private int pageCount;
    private int color;

    private Paint trackPaint;
    private Paint progressPaint;
    private float currentPosition = 0.0f;

    private ValueAnimator dotAnimator;

    public WelcomePageIndicator(Context context) {
        super(context);
        init();
    }

    public WelcomePageIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        determineColor(context, attrs);
        init();
    }

    public WelcomePageIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        determineColor(context, attrs);
        init();
    }

    private void determineColor(Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WelcomePageIndicator);
        color = a.getColor(R.styleable.WelcomePageIndicator_page_indicator_color, Color.BLACK);
        a.recycle();
    }

    private void init() {
        trackPaint = new Paint();
        trackPaint.setColor(Color.argb(70, Color.red(color), Color.green(color), Color.blue(color)));
        trackPaint.setStrokeWidth(1);
        trackPaint.setAntiAlias(true);
        progressPaint = new Paint();
        progressPaint.setColor(color);
        progressPaint.setStrokeWidth(3);
        progressPaint.setAntiAlias(true);
    }

    public void setViewPager(ViewPager2 viewPager) {
        this.viewPager = viewPager;
        this.pageCount = viewPager.getAdapter().getItemCount();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                animateIndicator(position);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                currentPosition = position + positionOffset;
                invalidate();
            }
        });
        invalidate();
    }

    private void animateIndicator(int position) {
        float targetPosition = position;
        if (dotAnimator != null && dotAnimator.isRunning()) {
            dotAnimator.cancel();
        }
        dotAnimator = ValueAnimator.ofFloat(currentPosition, targetPosition);
        dotAnimator.addUpdateListener(animation -> {
            currentPosition = (float) animation.getAnimatedValue();
            invalidate();
        });
        dotAnimator.setDuration(300);
        dotAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (viewPager == null || pageCount == 0) {
            return;
        }

        float segmentWidth = getWidth() / (float) pageCount;
        float gap = Math.max(2f, segmentWidth * 0.12f);
        float y = getHeight() / 2f;
        for (int page = 0; page < pageCount; page++) {
            float start = page * segmentWidth + gap;
            float end = (page + 1) * segmentWidth - gap;
            canvas.drawLine(start, y, end, y, trackPaint);
            if (page <= Math.round(currentPosition)) {
                canvas.drawLine(start, y, end, y, progressPaint);
            }
        }
    }
}
