package de.daniel.mobilepauker2.models.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.lesson.card.CardSide
import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.models.ModelManager
import javax.inject.Inject

class MPEditText : AppCompatEditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    @Inject
    lateinit var modelManager: ModelManager

    init {
        (context.applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
    }

    fun setCard(cardSide: CardSide) {
        setText(cardSide.text)
        setFont(cardSide.font)
    }

    fun setFont(font: Font?) {
        modelManager.setFont(font, this)
    }
}