package de.daniel.mobilepauker2.search

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView.MultiChoiceModeListener
import de.daniel.mobilepauker2.R

class SearchResultMultiChoiceModeListener(
    private val listenerCallback: SRMCMListenerCallback,
    private val showResetCards: Boolean
) : MultiChoiceModeListener {

    private var activeState: ActionMode? = null
    private var choosenItems = 0

    override fun onItemCheckedStateChanged(
        mode: ActionMode,
        position: Int,
        id: Long,
        checked: Boolean
    ) {
        listenerCallback.itemCheckedStateChanged(position, checked)
        if (checked) choosenItems++ else if (choosenItems > 0) choosenItems--
        mode.title = choosenItems.toString()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.search_result_context, menu)
        mode.title = choosenItems.toString()
        activeState = mode
        menu.findItem(R.id.mResetCard).isVisible = showResetCards
        menu.findItem(R.id.mResetCard).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.findItem(R.id.mRepeatNow).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.findItem(R.id.mFlipSides).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mResetCard -> {
                listenerCallback.resetCards()
                true
            }
            R.id.mRepeatNow -> {
                listenerCallback.repeatCardsNow()
                true
            }
            R.id.mFlipSides -> {
                listenerCallback.flipSides()
                true
            }
            R.id.mDeleteCard -> {
                listenerCallback.deleteCards()
                true
            }
            R.id.mSelectAll -> {
                listenerCallback.selectAll()
                true
            }
            R.id.mRepeatTypeInput -> {
                listenerCallback.setRepeatingType(true)
                true
            }
            R.id.mRepeatTypeThinking -> {
                listenerCallback.setRepeatingType(false)
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        activeState = null
    }

    fun getActiveState(): ActionMode? {
        return activeState
    }

    interface SRMCMListenerCallback {
        fun itemCheckedStateChanged(position: Int, checked: Boolean)
        fun resetCards()
        fun repeatCardsNow()
        fun flipSides()
        fun selectAll()
        fun deleteCards()
        fun setRepeatingType(isRepeatByTyping: Boolean)
    }
}