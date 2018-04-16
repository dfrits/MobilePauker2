package com.daniel.mobilepauker2.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;

/**
 * Created by Daniel on 17.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class MinFilter implements TextWatcher {
    private final EditTextPreference preference;

    public MinFilter(EditTextPreference preference) {
        this.preference = preference;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable text) {
        Dialog dialog = preference.getDialog();
        if (dialog instanceof AlertDialog) {
            Button button = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
            if (text.length() == 0) {
                button.setEnabled(false);
                return;
            }
            try {
                int input = Integer.parseInt(text.toString());
                button.setEnabled(input >= 1);
            } catch (Exception e) {
                button.setEnabled(false);
            }
        }
    }
}
