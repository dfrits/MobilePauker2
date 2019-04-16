package com.daniel.mobilepauker2.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.utils.Constants;

import static com.daniel.mobilepauker2.PaukerManager.showToast;

/**
 * Created by dfritsch on 22.03.2018.
 * veesy.de
 * hs-augsburg
 */

public class AddCardActivity extends AEditCardActivity {
    private MenuItem checkBox;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        flashCard = new FlashCard();
    }

    @Override
    public void onBackPressed() {
        String sideAText = sideAEditText.getText().toString();
        String sideBText = sideBEditText.getText().toString();

        if (sideAText.isEmpty() && sideBText.isEmpty()) {
            resetCardAndFinish();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.finish_add_card_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resetCardAndFinish();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_card, menu);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        checkBox = menu.findItem(R.id.mKeepOpen);
        checkBox.setChecked(pref.getBoolean(Constants.KEEP_OPEN_KEY, true));

        return true;
    }

    public void okClicked(View view) {
        String sideAText = sideAEditText.getText().toString();
        String sideBText = sideBEditText.getText().toString();

        if (!sideAText.isEmpty() && !sideBText.isEmpty()) {
            ModelManager.instance().addCard(flashCard, sideAText, sideBText);
            showToast((Activity) context, R.string.card_added, Toast.LENGTH_SHORT);

            sideAEditText.setText("");
            sideBEditText.setText("");
            PaukerManager.instance().setSaveRequired(true);
            sideAEditText.requestFocus();
            sideAEditText.setSelection(0, 0);

            if (checkBox != null && !checkBox.isChecked()) finish();
        } else {
            showToast((Activity) context, R.string.add_card_sides_empty_error, Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void resetCardSides(View view) {
        flashCard = new FlashCard();
        super.resetCardSides(view);
    }

    public void mKeepOpenClicked(MenuItem item) {
        boolean isChecked = checkBox.isChecked();
        checkBox.setChecked(!isChecked);
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(Constants.KEEP_OPEN_KEY, !isChecked).apply();
    }
}
