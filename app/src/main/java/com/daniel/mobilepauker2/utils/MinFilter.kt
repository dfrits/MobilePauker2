package com.daniel.mobilepauker2.utils

import android.app.AlertDialog
import android.content.DialogInterface
import android.preference.EditTextPreference
import android.text.Editable
import android.text.TextWatcher

/**
 * Created by Daniel on 17.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class MinFilter(private val preference: EditTextPreference) : TextWatcher {
    override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) {
    }

    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
    ) {
    }

    override fun afterTextChanged(text: Editable) {
        val dialog = preference.dialog
        if (dialog is AlertDialog) {
            val button =
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            if (text.length == 0) {
                button.isEnabled = false
                return
            }
            try {
                val input = text.toString().toInt()
                button.isEnabled = input >= 1
            } catch (e: Exception) {
                button.isEnabled = false
            }
        }
    }

}