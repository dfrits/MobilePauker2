package com.daniel.mobilepauker2.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.pauker_native.Font;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;

import static com.daniel.mobilepauker2.PaukerManager.showToast;

/**
 * Created by dfritsch on 22.03.2018.
 */

public class EditCardActivity extends AEditCardActivity {
    private int cardPosition;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cardPosition = getIntent().getIntExtra(Constants.CURSOR_POSITION, -1);

        if (cardPosition < 0) {
            Log.w("EditCardsActivity::OnCreate", "Card Position null " + cardPosition);
        } else {
            flashCard = ModelManager.instance().getCard(cardPosition);
        }

        if (flashCard == null) {
            Log.w("EditCardsActivity::OnCreate", "Flash Card set to null");
            showToast((Activity) context, getString(R.string.edit_cards_no_card_available), Toast.LENGTH_SHORT);
            finish();
        } else {
            init();
        }
    }

    private void init() {
        //SideA
        String text = flashCard.getSideAText();
        Font font = flashCard.getFrontSide().getFont();
        font = font == null ? new Font() : font;
        initSideAText = text;
        initSideATSize = font.getTextSize();
        initSideATColor = font.getTextColor();
        initSideABColor = font.getBackgroundColor();
        initSideABold = font.isBold();
        initSideAItalic = font.isItalic();
        sideAEditText.setCard(flashCard.getFrontSide());
        initIsRepeatedByTyping = flashCard.isRepeatedByTyping();

        //SideB
        text = flashCard.getSideBText();
        font = flashCard.getReverseSide().getFont();
        font = font == null ? new Font() : font;
        initSideBText = text;
        initSideBTSize = font.getTextSize();
        initSideBTColor = font.getTextColor();
        initSideBBColor = font.getBackgroundColor();
        initSideBBold = font.isBold();
        initSideBItalic = font.isItalic();
        sideBEditText.setCard(flashCard.getReverseSide());
    }

    public void okClicked(View view) {
        if (sideAEditText.getText().toString().trim().isEmpty() || sideBEditText.getText().toString().trim().isEmpty()) {
            showToast((Activity) context, R.string.add_card_sides_empty_error, Toast.LENGTH_SHORT);
            return;
        }
        if (cardPosition >= 0) {
            ModelManager.instance().editCard(
                    cardPosition,
                    sideAEditText.getText().toString(),
                    sideBEditText.getText().toString());
            if (detectChanges()) {
                PaukerManager.instance().setSaveRequired(true);
                setResult(RESULT_OK);
            }
            finish();
        }
    }
}
