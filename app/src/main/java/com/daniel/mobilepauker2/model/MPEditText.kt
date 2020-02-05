package com.daniel.mobilepauker2.model

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.daniel.mobilepauker2.model.pauker_native.CardSide
import com.daniel.mobilepauker2.model.pauker_native.Font

class MPEditText : AppCompatEditText {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
    }

    fun setCard(cardside: CardSide?) {
        setText(cardside.getText())
        setFont(cardside.getFont())
    }

    fun setFont(font: Font?) {
        ModelManager.Companion.instance()!!.setFont(font, this)
    }
}