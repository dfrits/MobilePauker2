package de.daniel.mobilepauker2.models.view

import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape

class TextDrawable(private val text: String, private val backColor: Int) :
    ShapeDrawable(OvalShape()) {
    private val size = 120

    constructor(text: String) : this(text, Color.BLACK)

    constructor(backColor: Int) : this("", backColor)

    init {
        paint.color = backColor
        intrinsicHeight = size
        intrinsicWidth = size
        setBounds(0, 0, size, size)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val paint = paint
        if (text.isNotEmpty()) {
            paint.color = Color.WHITE
            paint.textSize = (size / 2).toFloat()
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val theight = bounds.height()
            val twidth = bounds.width()
            canvas.drawText(
                text,
                ((size / 2) - (twidth / 2)).toFloat(),
                ((size / 2) + (theight / 2)).toFloat(),
                paint
            )
        } else {
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(
                (size / 2).toFloat(),
                (size / 2).toFloat(),
                (size / 2).toFloat(),
                paint
            )
        }
    }

    fun setBold(bool: Boolean) {
        val bold = if (bool) Typeface.BOLD else Typeface.NORMAL
        paint.typeface = Typeface.create(Typeface.DEFAULT, bold)
    }

    fun setItalic(bool: Boolean) {
        val italic = if (bool) Typeface.ITALIC else Typeface.NORMAL
        paint.typeface = Typeface.create(Typeface.DEFAULT, italic)
    }
}