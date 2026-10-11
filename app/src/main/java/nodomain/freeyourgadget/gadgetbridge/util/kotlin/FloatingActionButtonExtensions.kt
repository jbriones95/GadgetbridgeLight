package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import android.content.res.ColorStateList
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import nodomain.freeyourgadget.gadgetbridge.R

/** Use the same monochrome control treatment as the Light Phone shell. */
fun FloatingActionButton.applyAccentColors() {
    backgroundTintList = ColorStateList.valueOf(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
    imageTintList = ColorStateList.valueOf(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
}
