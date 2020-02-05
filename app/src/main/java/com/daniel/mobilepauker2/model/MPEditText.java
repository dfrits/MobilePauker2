package com.daniel.mobilepauker2.model;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import com.daniel.mobilepauker2.model.pauker_native.CardSide;
import com.daniel.mobilepauker2.model.pauker_native.Font;

public class MPEditText extends androidx.appcompat.widget.AppCompatEditText {

    public MPEditText(Context context) {
        super(context);
    }

    public MPEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MPEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCard(CardSide cardside) {
        setText(cardside.getText());
        setFont(cardside.getFont());
    }

    public void setFont(@Nullable Font font) {
        ModelManager.instance().setFont(font, this);
    }
}
