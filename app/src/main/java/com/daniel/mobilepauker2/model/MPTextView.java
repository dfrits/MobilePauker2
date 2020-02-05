package com.daniel.mobilepauker2.model;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import com.daniel.mobilepauker2.model.pauker_native.CardSide;
import com.daniel.mobilepauker2.model.pauker_native.Font;

public class MPTextView extends androidx.appcompat.widget.AppCompatTextView {

    public MPTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MPTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MPTextView(Context context) {
        super(context);
    }

    public void setCard(CardSide cardside) {
        setText(cardside.getText());

        setFont(cardside.getFont());
    }

    public void setFont(@Nullable Font font) {
        ModelManager.instance().setFont(font, this);
    }
}
