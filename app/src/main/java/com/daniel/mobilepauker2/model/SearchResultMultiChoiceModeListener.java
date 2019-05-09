package com.daniel.mobilepauker2.model;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.daniel.mobilepauker2.R;

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
public class SearchResultMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {
    private final boolean showResetCards;
    private ActionMode activeState;
    private SRMCMListenerCallback callback;
    private int choosenItems;

    public SearchResultMultiChoiceModeListener(SRMCMListenerCallback listenerCallback, boolean showResetCards) {
        callback = listenerCallback;
        this.showResetCards = showResetCards;
        choosenItems = 0;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        callback.itemCheckedStateChanged(position, checked);
        if (checked)
            choosenItems++;
        else if (choosenItems > 0)
            choosenItems--;
        mode.setTitle(String.valueOf(choosenItems));
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.search_result_context, menu);
        mode.setTitle(String.valueOf(choosenItems));
        activeState = mode;

        menu.findItem(R.id.mResetCard).setVisible(showResetCards);
        menu.findItem(R.id.mResetCard).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.mRepeatNow).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.mFlipSides).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mResetCard:
                callback.resetCards();
                return true;
            case R.id.mRepeatNow:
                callback.repeatCardsNow();
                return true;
            case R.id.mFlipSides:
                callback.flipSides();
                return true;
            case R.id.mDeleteCard:
                callback.deleteCards();
                return true;
            case R.id.mSelectAll:
                callback.selectAll();
                return true;
            case R.id.mRepeatTypeInput:
                callback.setRepeatingType(true);
                return true;
            case R.id.mRepeatTypeThinking:
                callback.setRepeatingType(false);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        activeState = null;
    }

    public ActionMode getActiveState() {
        return activeState;
    }

    public interface SRMCMListenerCallback {
        void itemCheckedStateChanged(int position, boolean checked);

        void resetCards();

        void repeatCardsNow();

        void flipSides();

        void selectAll();

        void deleteCards();

        void setRepeatingType(boolean isRepeatByTyping);
    }
}
