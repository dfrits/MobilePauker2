package com.daniel.mobilepauker2.activities

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.daniel.mobilepauker2.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.model.FlashCard
import com.daniel.mobilepauker2.model.ModelManager
import com.daniel.mobilepauker2.utils.Constants

/**
 * Created by dfritsch on 22.03.2018.
 * veesy.de
 * hs-augsburg
 */
class AddCardActivity : AEditCardActivity() {
    private var checkBox: MenuItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flashCard = FlashCard()
    }

    override fun onBackPressed() {
        val sideAText = sideAEditText!!.text.toString()
        val sideBText = sideBEditText!!.text.toString()
        if (sideAText.isEmpty() && sideBText.isEmpty()) {
            resetCardAndFinish()
        } else {
            val builder =
                AlertDialog.Builder(context)
            builder.setMessage(R.string.finish_add_card_message)
                .setPositiveButton(R.string.yes) { dialog, which -> resetCardAndFinish() }
                .setNeutralButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
            builder.create().show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_card, menu)
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        checkBox = menu.findItem(R.id.mKeepOpen)
        checkBox.setChecked(
            pref.getBoolean(
                Constants.KEEP_OPEN_KEY,
                true
            )
        )
        return true
    }

    override fun okClicked(view: View?) {
        val sideAText = sideAEditText!!.text.toString()
        val sideBText = sideBEditText!!.text.toString()
        if (!sideAText.isEmpty() && !sideBText.isEmpty()) {
            ModelManager.Companion.instance()!!.addCard(flashCard!!, sideAText, sideBText)
            PaukerManager.Companion.showToast(
                context as Activity,
                R.string.card_added,
                Toast.LENGTH_SHORT
            )
            sideAEditText!!.setText("")
            sideBEditText!!.setText("")
            PaukerManager.Companion.instance().setSaveRequired(true)
            sideAEditText!!.requestFocus()
            sideAEditText!!.setSelection(0, 0)
            if (checkBox != null && !checkBox!!.isChecked) finish()
        } else {
            PaukerManager.Companion.showToast(
                context as Activity,
                R.string.add_card_sides_empty_error,
                Toast.LENGTH_SHORT
            )
        }
    }

    override fun resetCardSides(view: View?) {
        flashCard = FlashCard()
        super.resetCardSides(view)
    }

    fun mKeepOpenClicked(item: MenuItem?) {
        val isChecked = checkBox!!.isChecked
        checkBox!!.isChecked = !isChecked
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(Constants.KEEP_OPEN_KEY, !isChecked).apply()
    }
}