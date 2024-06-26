package de.daniel.mobilepauker2.editcard

import android.app.Activity
import android.app.AlertDialog.Builder
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.preference.PreferenceManager
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.lesson.card.FlashCard
import de.daniel.mobilepauker2.search.Search
import de.daniel.mobilepauker2.utils.Constants

class AddCard : AbstractEditCard() {
    private lateinit var checkBox: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flashCard = FlashCard()

        findViewById<ImageButton>(R.id.sSideA).visibility = View.VISIBLE
        findViewById<ImageButton>(R.id.sSideB).visibility = View.VISIBLE
        onBackPressedDispatcher.addCallback { backPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_card, menu)
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        checkBox = menu.findItem(R.id.mKeepOpen)
        checkBox.isChecked = pref.getBoolean(Constants.KEEP_OPEN_KEY, true)
        return true
    }

    override fun searchCardA(view: View?) {
        val query = sideAEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            val browseIntent = Intent(Intent.ACTION_SEARCH)
            browseIntent.setClass(context, Search::class.java)
            browseIntent.putExtra(SearchManager.QUERY, query)
            startActivity(browseIntent)
        }
    }

    override fun searchCardB(view: View?) {
        val query = sideBEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            val browseIntent = Intent(Intent.ACTION_SEARCH)
            browseIntent.setClass(context, Search::class.java)
            browseIntent.putExtra(SearchManager.QUERY, query)
            startActivity(browseIntent)
        }
    }

    override fun okClicked(view: View?) {
        val sideAText = sideAEditText.text.toString()
        val sideBText = sideBEditText.text.toString()
        if (sideAText.isNotEmpty() && sideBText.isNotEmpty()) {
            lessonManager.addCard(flashCard, sideAText, sideBText)
            toaster.showToast(context as Activity, R.string.card_added, Toast.LENGTH_SHORT)
            resetCardSides()
            dataManager.saveRequired = true
            sideAEditText.requestFocus()
            sideAEditText.setSelection(0, 0)
            if (!checkBox.isChecked) finish()
        } else {
            toaster.showToast(
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
        val isChecked = checkBox.isChecked
        checkBox.isChecked = !isChecked
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(Constants.KEEP_OPEN_KEY, !isChecked).apply()
    }

    private fun backPressed() {
        val sideAText = sideAEditText.text.toString()
        val sideBText = sideBEditText.text.toString()
        if (sideAText.isEmpty() && sideBText.isEmpty()) {
            resetCardAndFinish()
        } else {
            val builder = Builder(context)
            builder.setMessage(R.string.finish_add_card_message)
                .setPositiveButton(R.string.yes) { _, _ -> resetCardAndFinish() }
                .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            builder.create().show()
        }
    }
}