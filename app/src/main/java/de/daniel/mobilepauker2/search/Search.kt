package de.daniel.mobilepauker2.search

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.editcard.EditCard
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.card.Card.*
import de.daniel.mobilepauker2.lesson.card.Card.Element.*
import de.daniel.mobilepauker2.lesson.card.FlashCard
import de.daniel.mobilepauker2.search.SearchResultMultiChoiceModeListener.*
import de.daniel.mobilepauker2.utils.Constants
import java.util.*
import javax.inject.Inject

class Search : AppCompatActivity() {
    private val context: Context = this
    private var listView: ListView? = null
    private var modalListener: SearchResultMultiChoiceModeListener? = null

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var viewModel: SearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        intent = intent
        if (Intent.ACTION_SEARCH == intent.action) {
            setContentView(R.layout.search_cards)
            listView = findViewById(R.id.listView)
            viewModel.stackIndex = intent.getIntExtra(Constants.STACK_INDEX, 0)
            viewModel.pack = lessonManager.setCurrentPack(viewModel.stackIndex)
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.browse, menu)
        val search = menu.findItem(R.id.mSearch)
        val searchView = search.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                viewModel.itemPosition.clear()
                showResults(viewModel.queryString(query))
                return true
            }
        })

        search.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                (item?.actionView as SearchView?)?.setQuery("", true)
                return true
            }

        })

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) searchView.clearFocus()
        }

        val query = intent.getStringExtra(SearchManager.QUERY)
        query?.let {
            searchView.setQuery(it, true)
            searchView.isIconifiedByDefault = false
            if (it.isNotEmpty()) {
                search.expandActionView()
            }
            return true
        }

        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when {
        viewModel.sortTypeSelected(item) -> {
            invalidateOptionsMenu()
            true
        }
        else -> false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_EDIT_CARD && resultCode == RESULT_OK) {
            viewModel.pack = lessonManager.currentPack
        }
        listView?.isEnabled = true

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()

        invalidateOptionsMenu()
    }

    fun mOpenSearchClicked(searchMenuItem: MenuItem) {
        val searchView = searchMenuItem.actionView as SearchView
        searchView.isIconified = false
    }

    fun mStartChooseClicked(item: MenuItem?) {
        if (modalListener != null) {
            listView?.startActionMode(modalListener)
        }
    }

    private fun showResults(results: List<FlashCard>) {
        val adapter = CardAdapter(context, results)
        listView!!.adapter = adapter
        listView!!.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView!!.onItemClickListener =
            OnItemClickListener { _, _, position, _ ->
                if (modalListener != null && modalListener?.getActiveState() != null) {
                    listView?.setItemChecked(position, !listView!!.isItemChecked(position))
                    return@OnItemClickListener
                }
                if (viewModel.itemPosition.size > 0) {
                    editCard(viewModel.itemPosition[position])
                } else {
                    editCard(position)
                }
            }
        modalListener = SearchResultMultiChoiceModeListener(object : SRMCMListenerCallback {
            override fun itemCheckedStateChanged(position: Int, checked: Boolean) {
                val item = listView!!.getItemAtPosition(position) as FlashCard

                viewModel.itemCheckStateChanged(item, checked)
            }

            override fun resetCards() {
                val builder = AlertDialog.Builder(context)

                builder.setTitle(R.string.reset_cards)
                    .setMessage(R.string.reset_cards_message)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.resetCards()
                        invalidateOptionsMenu()
                    }
                    .setNeutralButton(R.string.cancel, null)
                builder.create().show()
            }

            override fun repeatCardsNow() {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(R.string.instant_repeat)
                    .setMessage(R.string.instant_repeat_message)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.repeatCardsNow()
                        invalidateOptionsMenu()
                    }
                    .setNeutralButton(R.string.cancel, null)
                builder.create().show()
            }

            override fun flipSides() {
                val builder = AlertDialog.Builder(context)

                builder.setTitle(R.string.flip_card_sides)
                    .setMessage(R.string.flip_cards_message)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.flipSides()
                        invalidateOptionsMenu()
                    }
                    .setNeutralButton(R.string.cancel, null)
                builder.create().show()
            }

            override fun selectAll() {
                listView?.let {
                    for (i in 0 until it.count) {
                        if (!it.isItemChecked(i)) {
                            it.setItemChecked(i, true)
                        }
                    }
                }
            }

            override fun deleteCards() {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(R.string.delete_card)
                    .setMessage(R.string.delete_card_message)
                    .setPositiveButton(
                        R.string.yes
                    ) { _, _ ->
                        viewModel.deleteCards()
                        invalidateOptionsMenu()
                    }
                    .setNeutralButton(R.string.cancel, null)
                builder.create().show()
            }

            override fun setRepeatingType(isRepeatByTyping: Boolean) {
                viewModel.setRepeatingType(isRepeatByTyping)
                invalidateOptionsMenu()
            }
        }, viewModel.stackIndex != 1)
        listView?.setMultiChoiceModeListener(modalListener)
    }

    private fun editCard(position: Int) {
        listView?.isEnabled = false
        val intent = Intent(context, EditCard::class.java)
        intent.putExtra(Constants.CURSOR_POSITION, position)
        startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_CARD)
    }
}