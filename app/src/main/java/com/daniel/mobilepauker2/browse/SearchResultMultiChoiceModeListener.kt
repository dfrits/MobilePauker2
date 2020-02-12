package com.daniel.mobilepauker2.browse

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView.MultiChoiceModeListener
import com.daniel.mobilepauker2.R

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class SearchResultMultiChoiceModeListener(
    private val callback: SRMCMListenerCallback,
    private val showResetCards: Boolean
) : MultiChoiceModeListener {
    var activeState: ActionMode? = null
        private set
    private var choosenItems = 0
    override fun onItemCheckedStateChanged(
        mode: ActionMode,
        position: Int,
        id: Long,
        checked: Boolean
    ) {
        callback.itemCheckedStateChanged(position, checked)
        if (checked) choosenItems++ else if (choosenItems > 0) choosenItems--
        mode.title = choosenItems.toString()
    }

    override fun onCreateActionMode(
        mode: ActionMode,
        menu: Menu
    ): Boolean {
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

    override fun onPrepareActionMode(
        mode: ActionMode,
        menu: Menu
    ): Boolean {
        return false
    }

    override fun onActionItemClicked(
        mode: ActionMode,
        item: MenuItem
    ): Boolean {
        return when (item.itemId) {
            R.id.mResetCard -> {
                callback.resetCards()
                true
            }
            R.id.mRepeatNow -> {
                callback.repeatCardsNow()
                true
            }
            R.id.mFlipSides -> {
                callback.flipSides()
                true
            }
            R.id.mDeleteCard -> {
                callback.deleteCards()
                true
            }
            R.id.mSelectAll -> {
                callback.selectAll()
                true
            }
            R.id.mRepeatTypeInput -> {
                callback.setRepeatingType(true)
                true
            }
            R.id.mRepeatTypeThinking -> {
                callback.setRepeatingType(false)
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        activeState = null
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