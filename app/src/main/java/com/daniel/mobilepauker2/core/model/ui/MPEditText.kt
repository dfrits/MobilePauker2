package com.daniel.mobilepauker2.core.model.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.pauker_native.CardSide
import com.daniel.mobilepauker2.pauker_native.Font

class MPEditText : AppCompatEditText {
    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setCard(cardside: CardSide?) {
        setText(cardside?.text)
        setFont(cardside?.font)
    }

    fun setFont(font: Font?) {
        ModelManager.instance().setFont(font, this)
    }
}