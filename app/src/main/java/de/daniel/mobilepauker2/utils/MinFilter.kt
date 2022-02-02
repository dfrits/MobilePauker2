package de.daniel.mobilepauker2.utils

import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class MinFilter(private val preference: DialogFragment) :
    TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(text: Editable) {
        val dialog = preference.dialog
        if (dialog is AlertDialog) {
            val button = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            if (text.isEmpty()) {
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