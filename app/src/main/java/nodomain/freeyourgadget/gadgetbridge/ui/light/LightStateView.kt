package nodomain.freeyourgadget.gadgetbridge.ui.light

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView

/** Small bridge for legacy adapters that need Light state copy without moving their behavior. */
object LightStateView {
    @JvmStatic
    fun bind(view: ComposeView, message: String) {
        view.setContent {
            LightTheme {
                LightStateMessage(
                    message,
                    Modifier.fillMaxWidth().padding(horizontal = 1f.lightGridUnitsAsDp()),
                )
            }
        }
    }
}
