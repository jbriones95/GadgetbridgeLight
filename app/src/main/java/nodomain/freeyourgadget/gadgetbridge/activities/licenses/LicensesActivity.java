/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.licenses;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;

public class LicensesActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(LicensesActivity.class);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);

        final View back = findViewById(R.id.licenses_back);
        back.setOnClickListener(v -> finish());

        final RecyclerView listView = findViewById(R.id.listView);
        listView.setLayoutManager(new LinearLayoutManager(this));

        final List<License> licenses;
        try (InputStream in = getAssets().open("licenses/licenses.json")) {
            final Type listType = new TypeToken<List<License>>() {
            }.getType();
            licenses = new Gson().fromJson(new InputStreamReader(in), listType);
            licenses.sort((a, b) -> {
                return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
            });
        } catch (final IOException e) {
            LOG.error("Failed to read licenses", e);
            finish();
            return;
        }

        final LicensesAdapter appListAdapter = new LicensesAdapter(this, licenses);

        listView.setAdapter(appListAdapter);
    }

}
