package com.daniel.mobilepauker2.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;

/**
 * Created by dfritsch on 22.03.2018.
 * veesy.de
 * hs-augsburg
 */

public class EditCardActivity extends AppCompatActivity {
    private final Context context = this;
    private int cardPosition;
    private FlashCard flashCard;
    private EditText sideAEditText;
    private EditText sideBEditText;
    private String sideAText;
    private String sideBText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_card);

        cardPosition = getIntent().getIntExtra(Constants.CURSOR_POSITION, -1);

        if (cardPosition < 0) {
            Log.w("EditCardsActivity::OnCreate", "Card Position null " + cardPosition);
        } else {
            flashCard = ModelManager.instance().getCard(cardPosition);
        }

        if (flashCard == null) {
            Log.w("EditCardsActivity::OnCreate", "Flash Card set to null");
            Toast.makeText(context, getString(R.string.edit_cards_no_card_available), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            init();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sideAEditText != null && sideBEditText != null) {
            SettingsManager settingsManager = SettingsManager.instance();
            sideAEditText.setTextSize(Float.parseFloat(settingsManager.getStringPreference(context, SettingsManager.Keys.FONT_SIZE)));
            sideBEditText.setTextSize(Float.parseFloat(settingsManager.getStringPreference(context, SettingsManager.Keys.FONT_SIZE)));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null && imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);

        return true;
    }

    private void init() {
        sideAEditText = findViewById(R.id.eTSideA);
        sideBEditText = findViewById(R.id.eTSideB);

        sideAText = flashCard.getSideAText();
        sideBText = flashCard.getSideBText();

        sideAEditText.setText(flashCard.getSideAText());
        sideBEditText.setText(flashCard.getSideBText());
    }

    public void okClicked(View view) {
        if (sideAEditText.getText().toString().trim().isEmpty() || sideBEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(context, R.string.add_card_sides_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (cardPosition >= 0) {
            ModelManager.instance().editCard(
                    cardPosition,
                    sideAEditText.getText().toString(),
                    sideBEditText.getText().toString());
            PaukerManager.instance().setSaveRequired(true);
            setResult(RESULT_OK);
            finish();
        }
    }

    public void resetCardSides(View view) {
        sideAEditText.setText(sideAText);
        sideBEditText.setText(sideBText);
    }

    public void settings(MenuItem item) {
        startActivity(new Intent(context, SettingsActivity.class));
    }
}
