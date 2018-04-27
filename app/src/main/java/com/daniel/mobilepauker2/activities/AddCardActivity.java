package com.daniel.mobilepauker2.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.SettingsManager;

/**
 * Created by dfritsch on 22.03.2018.
 * veesy.de
 * hs-augsburg
 */

public class AddCardActivity extends AppCompatActivity {
    private final Context context = this;
    private EditText sideAEditText;
    private EditText sideBEditText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_card);

        sideAEditText = findViewById(R.id.eTSideA);
        sideBEditText = findViewById(R.id.eTSideB);
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

    @Override
    public void onBackPressed() {
        String sideAText = sideAEditText.getText().toString();
        String sideBText = sideBEditText.getText().toString();

        if (sideAText.isEmpty() && sideBText.isEmpty()) {
            finish();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Card is not added yet. Do you really want to leave?")
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
        }
    }

    public void okClicked(View view) {
        String sideAText = sideAEditText.getText().toString();
        String sideBText = sideBEditText.getText().toString();

        if (!sideAText.isEmpty() && !sideBText.isEmpty()) {
            ModelManager.instance().addCard(sideAText, sideBText, "-1", "-1", "false");
            Toast.makeText(context, R.string.card_added, Toast.LENGTH_SHORT).show();

            sideAEditText.setText("");
            sideBEditText.setText("");
            PaukerManager.instance().setSaveRequired(true);
        } else {
            Toast.makeText(context, R.string.add_card_side_empty, Toast.LENGTH_SHORT).show();
        }
    }

    public void resetCardSides(View view) {
        sideAEditText.setText("");
        sideBEditText.setText("");
    }


    public void settings(MenuItem item) {
        startActivity(new Intent(context, SettingsActivity.class));
    }
}
