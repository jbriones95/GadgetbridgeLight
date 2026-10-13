/*  Copyright (C) 2026 oddballza

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
package nodomain.freeyourgadget.gadgetbridge.widgets.impl

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.test.TestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SleepScoreWidgetTest : TestBase() {
    /** Inflates the real widget view, binds [data] through the public API and returns the displayed value. */
    private fun render(data: SleepScoreWidget.Data): String {
        return SleepScoreWidget.lightMetric(context, data, context.getString(SleepScoreWidget.label)).value
    }

    @Test
    fun noScoreShowsEmptyValueNotSentinel() {
        val text = render(SleepScoreWidget.Data(-1))
        assertNotEquals("-1", text)
        assertEquals(context.getString(R.string.stats_empty_value), text)
    }

    @Test
    fun zeroScoreShowsEmptyValue() {
        assertEquals(context.getString(R.string.stats_empty_value), render(SleepScoreWidget.Data(0)))
    }

    @Test
    fun scoreIsShown() {
        assertEquals("80", render(SleepScoreWidget.Data(80)))
    }
}
