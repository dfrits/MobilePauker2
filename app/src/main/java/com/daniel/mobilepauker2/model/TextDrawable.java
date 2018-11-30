package com.daniel.mobilepauker2.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.annotation.NonNull;

public class TextDrawable extends ShapeDrawable {
    private String s;
    private int size = 120;

    public TextDrawable(String text) {
        this(text, Color.BLACK);
    }

    public TextDrawable(int backColor) {
        this("", backColor);
    }

    private TextDrawable(String text, int backColor) {
        super(new OvalShape());
        s = text;
        getPaint().setColor(backColor);
        setIntrinsicHeight(size);
        setIntrinsicWidth(size);
        setBounds(0, 0, size, size);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        Paint paint = getPaint();
        if (!s.isEmpty()) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(size / 2);
            Rect bounds = new Rect();
            paint.getTextBounds(s, 0, s.length(), bounds);
            int theight = bounds.height();
            int twidth = bounds.width();
            canvas.drawText(s, (size / 2) - (twidth / 2), (size / 2) + (theight / 2), paint);
        } else {
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(size / 2, size / 2, size / 2, paint);
        }
    }

    public void setBold(boolean bool) {
        int bold = bool ? Typeface.BOLD : Typeface.NORMAL;
        getPaint().setTypeface(Typeface.create(Typeface.DEFAULT, bold));
    }

    public void setItalic(boolean bool) {
        int italic = bool ? Typeface.ITALIC : Typeface.NORMAL;
        getPaint().setTypeface(Typeface.create(Typeface.DEFAULT, italic));
    }
}
