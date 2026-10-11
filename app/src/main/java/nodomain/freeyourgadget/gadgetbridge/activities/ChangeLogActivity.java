package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GBChangeLog;

public class ChangeLogActivity extends AbstractGBActivity {
    public static final String EXTRA_FULL = "full";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changelog);

        final GBChangeLog changeLog = GBChangeLog.createChangeLog(this);
        final WebView content = findViewById(R.id.changelog_content);
        content.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        content.setVerticalScrollBarEnabled(false);
        content.loadDataWithBaseURL(null, changeLog.getLightLog(getIntent().getBooleanExtra(EXTRA_FULL, true)),
                "text/html", "UTF-8", null);

        final View close = findViewById(R.id.changelog_close);
        final View back = findViewById(R.id.changelog_back);
        close.setOnClickListener(view -> closeLog(changeLog));
        back.setOnClickListener(view -> closeLog(changeLog));
    }

    private void closeLog(GBChangeLog changeLog) {
        changeLog.markLightLogRead();
        finish();
    }

    @Override
    public void onBackPressed() {
        final GBChangeLog changeLog = GBChangeLog.createChangeLog(this);
        closeLog(changeLog);
    }
}
