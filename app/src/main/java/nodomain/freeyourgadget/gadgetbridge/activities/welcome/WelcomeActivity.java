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

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityWelcomeBinding;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class WelcomeActivity extends AbstractGBActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        final ActivityWelcomeBinding binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Configure ViewPager2 with fragment adapter and default fragment
        WelcomeFragmentsPagerAdapter pagerAdapter = new WelcomeFragmentsPagerAdapter(this);
        binding.welcomeViewpager.setAdapter(pagerAdapter);
        // Setup is a deliberate sequence on LP3. Pages advance only through NEXT and BACK.
        binding.welcomeViewpager.setUserInputEnabled(false);
        binding.welcomeViewpager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        binding.welcomeViewpager.setHorizontalScrollBarEnabled(false);
        if (binding.welcomeViewpager.getChildCount() > 0) {
            final View pagerContent = binding.welcomeViewpager.getChildAt(0);
            pagerContent.setHorizontalScrollBarEnabled(false);
            pagerContent.setOverScrollMode(View.OVER_SCROLL_NEVER);
            final float[] touchStart = new float[2];
            pagerContent.setOnTouchListener((view, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN -> {
                        touchStart[0] = event.getX();
                        touchStart[1] = event.getY();
                        return false;
                    }
                    case MotionEvent.ACTION_MOVE -> {
                        final float dx = Math.abs(event.getX() - touchStart[0]);
                        final float dy = Math.abs(event.getY() - touchStart[1]);
                        return dx > 16f && dx > dy;
                    }
                    default -> {
                        return false;
                    }
                }
            });
        }

        // Set up welcome page indicator
        binding.welcomePageIndicator.setViewPager(binding.welcomeViewpager);

        binding.welcomeBack.setOnClickListener(view -> {
            final int currentPage = binding.welcomeViewpager.getCurrentItem();
            if (currentPage > 0) {
                binding.welcomeViewpager.setCurrentItem(currentPage - 1);
            }
        });
        binding.welcomeNext.setOnClickListener(view -> {
            final int currentPage = binding.welcomeViewpager.getCurrentItem();
            if (currentPage < pagerAdapter.getItemCount() - 1) {
                if (currentPage == 1) {
                    // Continuing means the user chose to finish setup without granting every
                    // optional permission. They can enable individual permissions when needed.
                    GBApplication.getPrefs().getPreferences().edit()
                            .putBoolean("permission_pestering", false)
                            .apply();
                }
                binding.welcomeViewpager.setCurrentItem(currentPage + 1);
            }
        });
        binding.welcomeViewpager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.welcomeBack.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
                binding.welcomeNext.setVisibility(position == pagerAdapter.getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);
                binding.welcomeNext.setText(getString(position == 1
                        ? R.string.first_start_permissions_continue
                        : R.string.light_action_next));
            }
        });
        binding.welcomeBack.setVisibility(View.INVISIBLE);
    }

    private static class WelcomeFragmentsPagerAdapter extends FragmentStateAdapter {
        public WelcomeFragmentsPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> new WelcomeFragmentIntro();
                case 1 -> new WelcomeFragmentPermissions();
                default -> new WelcomeFragmentGetStarted();
            };
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
