package nodomain.freeyourgadget.gadgetbridge.ui.light

data class LightMenuAction(
    val id: String,
    val title: String,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val iconRes: Int = 0,
)
