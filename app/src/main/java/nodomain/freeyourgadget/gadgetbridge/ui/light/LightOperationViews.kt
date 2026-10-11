package nodomain.freeyourgadget.gadgetbridge.ui.light

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Light presentation bridges for legacy operation activities. They contain no operation logic. */
class LightOperationStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private var message by mutableStateOf("")
    private var error by mutableStateOf(false)

    init {
        addView(ComposeView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setContent {
                LightText(
                    message,
                    variant = LightTextVariant.Detail,
                    modifier = Modifier.fillMaxWidth().padding(vertical = LightSpacing.rowVertical),
                    secondary = !error,
                    textAlign = TextAlign.Center,
                )
            }
        })
    }

    @JvmOverloads
    fun setText(value: CharSequence?, isError: Boolean = false) {
        message = value?.toString().orEmpty()
        error = isError
    }

    fun getText(): CharSequence = message
}

class LightOperationProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private var indeterminateState by mutableStateOf(true)
    private var progress by mutableFloatStateOf(0f)

    init {
        addView(ComposeView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setContent {
                if (indeterminateState) {
                    LightText("...", LightTextVariant.Detail, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                } else {
                    LightProgressBar(progress, Modifier.fillMaxWidth().padding(vertical = LightSpacing.rowVertical))
                }
            }
        })
    }

    fun setIndeterminate(value: Boolean) {
        indeterminateState = value
    }

    fun setProgress(value: Int) {
        progress = (value / 100f).coerceIn(0f, 1f)
        indeterminateState = false
    }
}
