package de.daniel.mobilepauker2.models.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.core.graphics.drawable.toBitmap

class BitmapDrawable(private val icon: Drawable) :
    ShapeDrawable(OvalShape()) {
    private val size = 120

    init {
        paint.color = Color.WHITE
        intrinsicHeight = size
        intrinsicWidth = size
        setBounds(0, 0, size, size)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val paint = paint
        val bitmap = icon.toBitmap(size, size)

        paint.color = Color.BLACK
        canvas.drawBitmap(
            bitmap,
            0f,
            0f,
            paint
        )
    }
}