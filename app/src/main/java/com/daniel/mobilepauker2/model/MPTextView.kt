package com.daniel.mobilepauker2.model

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.daniel.mobilepauker2.model.pauker_native.CardSide
import com.daniel.mobilepauker2.model.pauker_native.Font

class MPTextView : AppCompatTextView {
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    constructor(context: Context?) : super(context) {}

    fun setCard(cardside: CardSide?) {
        text = cardside.getText()
        setFont(cardside.getFont())
    }

    fun setFont(font: Font?) {
        ModelManager.Companion.instance()!!.setFont(font, this)
    }
}