package de.daniel.mobilepauker2.models

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import de.daniel.mobilepauker2.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor() {

    fun setFont(font: Font?, cardSide: TextView) {
        val mFont = font ?: Font()
        val textSize: Int = mFont.textSize
        cardSide.textSize = if (textSize > 16) textSize.toFloat() else 16.toFloat()
        cardSide.setTextColor(mFont.textColor)
        val bold: Boolean = mFont.isBold
        val italic: Boolean = mFont.isItalic

        if (bold && italic) cardSide.setTypeface(
            Typeface.create(
                mFont.family,
                Typeface.BOLD_ITALIC
            )
        ) else if (bold) cardSide.setTypeface(
            Typeface.create(
                mFont.family,
                Typeface.BOLD
            )
        ) else if (italic) cardSide.setTypeface(
            Typeface.create(
                mFont.family,
                Typeface.ITALIC
            )
        ) else cardSide.typeface = Typeface.create(mFont.family, Typeface.NORMAL)

        val backgroundColor: Int = mFont.backgroundColor

        if (backgroundColor != -1) cardSide.background = createBoxBackground(backgroundColor)
        else cardSide.setBackgroundResource(R.drawable.box_background)
    }

    private fun createBoxBackground(backgroundColor: Int): GradientDrawable {
        val background = GradientDrawable()
        background.shape = GradientDrawable.RECTANGLE
        background.cornerRadius = 2f
        background.setStroke(3, Color.BLACK)
        background.setColor(backgroundColor)
        return background
    }
}