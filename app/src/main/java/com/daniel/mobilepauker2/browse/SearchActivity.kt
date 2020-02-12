package com.daniel.mobilepauker2.browse

import android.app.Activity
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
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.editor.EditCardActivity
import com.daniel.mobilepauker2.pauker_native.FlashCard
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.browse.SearchResultMultiChoiceModeListener.SRMCMListenerCallback
import com.daniel.mobilepauker2.pauker_native.Card
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.pauker_native.Log
import java.util.*

/**
 * Created by Daniel on 06.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class SearchActivity : AppCompatActivity() {
    private val context: Context = this
    private val modelManager: ModelManager = ModelManager.instance()
    private val paukerManager: PaukerManager = PaukerManager.instance()
    private var modalListener: SearchResultMultiChoiceModeListener? = null
    private var itemPosition: Vector<Int>? = null
    private var listView: ListView? = null
    private var stackIndex = 0
    private var pack: MutableList<FlashCard?>? = null
    private var checkedCards: MutableList<FlashCard>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent = getIntent()
        if (Intent.ACTION_SEARCH == intent.getAction()) {
            setContentView(R.layout.search_cards)
            listView = findViewById(R.id.listView)
            itemPosition = Vector()
            stackIndex = intent.getIntExtra(Constants.STACK_INDEX, 0)
            modelManager.setCurrentPack(context, stackIndex)
            pack = modelManager.currentPack
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.browse, menu)
        val search = menu.findItem(R.id.mSearch)
        val searchView =
            search.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                itemPosition!!.clear()
                showResults(queryString(query))
                return true
            }
        })
        searchView.setOnQueryTextFocusChangeListener { v, hasFocus -> if (!hasFocus) searchView.clearFocus() }
        val query = intent!!.getStringExtra(SearchManager.QUERY)
        searchView.setQuery(query, true)
        searchView.setIconifiedByDefault(false)
        if (!query.isEmpty()) {
            search.expandActionView()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mSortFrontASC -> {
                sortCards(
                    Card.Element.FRONT_SIDE,
                    true
                )
                true
            }
            R.id.mSortBackASC -> {
                sortCards(
                    Card.Element.REVERSE_SIDE,
                    true
                )
                true
            }
            R.id.mSortBatchnumberASC -> {
                sortCards(
                    Card.Element.BATCH_NUMBER,
                    true
                )
                true
            }
            R.id.mSortLearnedDateASC -> {
                sortCards(
                    Card.Element.LEARNED_DATE,
                    true
                )
                true
            }
            R.id.mSortExpiredDateASC -> {
                sortCards(
                    Card.Element.EXPIRED_DATE,
                    true
                )
                true
            }
            R.id.mSortRepetTypeASC -> {
                sortCards(
                    Card.Element.REPEATING_MODE,
                    true
                )
                true
            }
            R.id.mSortFrontDSC -> {
                sortCards(
                    Card.Element.FRONT_SIDE,
                    false
                )
                true
            }
            R.id.mSortBackDSC -> {
                sortCards(
                    Card.Element.REVERSE_SIDE,
                    false
                )
                true
            }
            R.id.mSortBatchnumberDSC -> {
                sortCards(
                    Card.Element.BATCH_NUMBER,
                    false
                )
                true
            }
            R.id.mSortLearnedDateDSC -> {
                sortCards(
                    Card.Element.LEARNED_DATE,
                    false
                )
                true
            }
            R.id.mSortExpiredDateDSC -> {
                sortCards(
                    Card.Element.EXPIRED_DATE,
                    false
                )
                true
            }
            R.id.mSortRepetTypeDSC -> {
                sortCards(
                    Card.Element.REPEATING_MODE,
                    false
                )
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_EDIT_CARD && resultCode == Activity.RESULT_OK) {
            pack = modelManager.currentPack
            invalidateOptionsMenu()
        }
        listView!!.isEnabled = true
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showResults(results: List<FlashCard?>) {
        val adapter =
            CardAdapter(context, results)
        listView?.adapter = adapter
        listView?.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView?.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            if (modalListener != null && modalListener?.activeState != null) {
                listView!!.setItemChecked(position, !listView!!.isItemChecked(position))
                return@OnItemClickListener
            }
            if (itemPosition!!.size > 0) {
                editCard(itemPosition!![position])
            } else {
                editCard(position)
            }
        }
        modalListener =
            SearchResultMultiChoiceModeListener(
                object : SRMCMListenerCallback {
                    override fun itemCheckedStateChanged(
                        position: Int,
                        checked: Boolean
                    ) {
                        if (checkedCards == null) checkedCards = ArrayList()
                        val item = listView!!.getItemAtPosition(position) as FlashCard
                        if (checked) {
                            checkedCards!!.add(item)
                        } else {
                            checkedCards!!.remove(item)
                        }
                    }

                    override fun resetCards() {
                        if (checkedCards != null) {
                            val builder =
                                AlertDialog.Builder(context)
                            builder.setTitle(R.string.reset_cards)
                                .setMessage(R.string.reset_cards_message)
                                .setPositiveButton(R.string.yes) { dialog, which ->
                                    for (card in checkedCards!!) {
                                        modelManager.forgetCard(card)
                                    }
                                    checkedCards = null
                                    paukerManager.isSaveRequired = true
                                    modelManager.setCurrentPack(context, stackIndex)
                                    pack = modelManager.currentPack
                                    invalidateOptionsMenu()
                                }
                                .setNeutralButton(R.string.cancel, null)
                            builder.create().show()
                        }
                    }

                    override fun repeatCardsNow() {
                        if (checkedCards != null) {
                            val builder =
                                AlertDialog.Builder(context)
                            builder.setTitle(R.string.instant_repeat)
                                .setMessage(R.string.instant_repeat_message)
                                .setPositiveButton(R.string.yes) { dialog, which ->
                                    for (card in checkedCards!!) {
                                        modelManager.instantRepeatCard(card)
                                    }
                                    checkedCards = null
                                    paukerManager.isSaveRequired = true
                                    modelManager.setCurrentPack(context, stackIndex)
                                    pack = modelManager.currentPack
                                    invalidateOptionsMenu()
                                }
                                .setNeutralButton(R.string.cancel, null)
                            builder.create().show()
                        }
                    }

                    override fun flipSides() {
                        if (checkedCards != null) {
                            val builder =
                                AlertDialog.Builder(context)
                            builder.setTitle(R.string.flip_card_sides)
                                .setMessage(R.string.flip_cards_message)
                                .setPositiveButton(R.string.yes) { dialog, which ->
                                    for (card in checkedCards!!) {
                                        card.flip()
                                    }
                                    checkedCards = null
                                    paukerManager.isSaveRequired = true
                                    modelManager.setCurrentPack(context, stackIndex)
                                    pack = modelManager.currentPack
                                    invalidateOptionsMenu()
                                }
                                .setNeutralButton(R.string.cancel, null)
                            builder.create().show()
                        }
                    }

                    override fun selectAll() {
                        for (i in 0 until listView!!.count) {
                            if (!listView!!.isItemChecked(i)) {
                                listView!!.setItemChecked(i, true)
                            }
                        }
                    }

                    override fun deleteCards() {
                        if (checkedCards != null) {
                            val builder =
                                AlertDialog.Builder(context)
                            builder.setTitle(R.string.delete_card)
                                .setMessage(R.string.delete_card_message)
                                .setPositiveButton(R.string.yes) { dialog, which ->
                                    for (card in checkedCards!!) {
                                        val cardDeleted = modelManager!!.deleteCard(card)
                                        Log.d(
                                            "SearchActivity::MultiChoiceListener::deleteCards",
                                            "Card deleted: $cardDeleted"
                                        )
                                    }
                                    checkedCards = null
                                    paukerManager.isSaveRequired = true
                                    modelManager!!.setCurrentPack(context, stackIndex)
                                    pack = modelManager.currentPack
                                    invalidateOptionsMenu()
                                }
                                .setNeutralButton(R.string.cancel, null)
                            builder.create().show()
                        }
                    }

                    override fun setRepeatingType(isRepeatByTyping: Boolean) {
                        if (checkedCards != null) {
                            for (card in checkedCards!!) {
                                card.setRepeatByTyping(isRepeatByTyping)
                            }
                            checkedCards = null
                            paukerManager.isSaveRequired = true
                            modelManager.setCurrentPack(context, stackIndex)
                            pack = modelManager.currentPack
                            invalidateOptionsMenu()
                        }
                    }
                },
                stackIndex != 1
            )
        listView!!.setMultiChoiceModeListener(modalListener)
    }

    private fun queryString(query: String): List<FlashCard?> {
        var results: MutableList<FlashCard?> = ArrayList()
        if (query == "") {
            pack?.let { results = it }
        } else {
            var card: FlashCard?
            for (i in pack!!.indices) {
                card = pack!![i]
                Log.d(
                    "SearchActivity::ShowResults",
                    "Index - " + card?.id
                )
                val frontSide = card?.frontSide?.text?.toLowerCase(Locale.getDefault()) ?: ""
                val backSide = card?.reverseSide?.text?.toLowerCase(Locale.getDefault()) ?: ""
                //frontSide = Normalizer.normalize(frontSide, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");;
//backSide = Normalizer.normalize(backSide, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");;
//query = Normalizer.normalize(query, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");;
                if (frontSide.contains(query.toLowerCase(Locale.getDefault()))
                    || backSide.contains(query.toLowerCase(Locale.getDefault()))
                ) {
                    results.add(card)
                    itemPosition!!.add(i)
                }
            }
        }
        return results
    }

    /**
     * Sortiert die Liste nach dem angebenen Element aufsteigend oder absteigend.
     * @param sortByElement Element nach dem sortiert werden soll
     * @param asc_direction Gibt an ob aufsteigend oder absteigen. True ist aufsteigend und
     * false absteigend
     */
    private fun sortCards(
        sortByElement: Card.Element,
        asc_direction: Boolean
    ) {
        modelManager.sortBatch(stackIndex, sortByElement, asc_direction)
        paukerManager.isSaveRequired = true
        modelManager.setCurrentPack(context, stackIndex)
        pack = modelManager.currentPack
        invalidateOptionsMenu()
    }

    private fun editCard(position: Int) {
        listView!!.isEnabled = false
        val intent = Intent(context, EditCardActivity::class.java)
        intent.putExtra(Constants.CURSOR_POSITION, position)
        startActivityForResult(
            intent,
            Constants.REQUEST_CODE_EDIT_CARD
        )
    }

    fun mOpenSearchClicked(searchMenuItem: MenuItem) {
        val searchView =
            searchMenuItem.actionView as SearchView
        searchView.isIconified = false
    }

    /**
     * Startet die Multiauswahl manuell.
     * @param item Nicht notwendig
     */
    fun mStartChooseClicked(item: MenuItem?) {
        if (modalListener != null) {
            listView!!.startActionMode(modalListener)
        }
    }
}