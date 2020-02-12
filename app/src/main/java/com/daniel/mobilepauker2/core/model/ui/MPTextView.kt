package com.daniel.mobilepauker2.core.model.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.pauker_native.CardSide
import com.daniel.mobilepauker2.pauker_native.Font

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
        text = cardside?.text
        setFont(cardside?.font)
    }

    fun setFont(font: Font?) {
        ModelManager.instance()
            .setFont(font, this)
    }
}