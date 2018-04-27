/* 
 * Copyright 2011 Brian Ford
 * 
 * This file is part of Pocket Pauker.
 * 
 * Pocket Pauker is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or (at your option) any later version.
 * 
 * Pocket Pauker is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 * 
 * See http://www.gnu.org/licenses/.

*/

package com.daniel.mobilepauker2.activities;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.daniel.mobilepauker2.model.CardPackRamAdapter;
import com.daniel.mobilepauker2.model.CursorLoader;
import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.FlashCardCursor;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.utils.Log;

import static com.daniel.mobilepauker2.utils.Constants.SWIPE_MAX_OFF_PATH;
import static com.daniel.mobilepauker2.utils.Constants.SWIPE_MIN_DISTANCE;
import static com.daniel.mobilepauker2.utils.Constants.SWIPE_THRESHOLD_VELOCITY;

public abstract class FlashCardSwipeScreenActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    protected final SettingsManager settingsManager = SettingsManager.instance();
    protected final ModelManager modelManager = ModelManager.instance();
    protected final String INSTANCESTATE_START_TIME = "INSTANCESTATE_START_TIME";
    protected final String INSTANCESTATE_STM_START_TIME = "INSTANCESTATE_STM_START_TIME";
    protected final String INSTANCESTATE_USTM_START_TIME = "INSTANCESTATE_USTM_START_TIME";
    protected final String INSTANCESTATE_CURSOR_POSITION = "INSTANCESTATE_CURSOR_POSITION";
    private final Context context = this;
    protected Cursor mCardCursor = null;
    protected FlashCard currentCard = new FlashCard();
    protected GestureDetector gestureDetector;
    protected CardPackRamAdapter mCardPackAdapter;
    protected boolean mActivitySetupOk = false;
    protected int mSavedCursorPosition = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCardPackAdapter = new CardPackRamAdapter(this);

        // Setup the cursor
        Log.d("FlashCardSwipeScreenActivity::onCreate", "Seting up cursor");

        getLoaderManager().initLoader(1, null, this);

        // Setup Gesture detection
        Log.d("FlashCardSwipeScreenActivity::onCreate", "Setting up gesture detection");
        gestureDetector = new GestureDetector(context, new MyGestureDetector());

        mActivitySetupOk = true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(INSTANCESTATE_CURSOR_POSITION, mCardCursor.getPosition());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d("FlashCardSwipeScreenActivity::onRestoreInstanceState", "Entry");
        mSavedCursorPosition = savedInstanceState.getInt(INSTANCESTATE_CURSOR_POSITION);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        return gestureDetector.onTouchEvent(ev);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(context, mCardPackAdapter);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCardCursor = mCardPackAdapter.fetchAllFlashCards();
        if (mCardCursor == null || mCardCursor.getCount() <= 0) // no cards available
        {
            Log.d("FlashCardSwipeScreenActivity::onCreate", "No cards available so stopping");
            mActivitySetupOk = false;
            this.finish();
        } else {
            cursorLoaded();
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mCardCursor.close();
    }

    abstract void onLeftSwipe();

    abstract void onRightSwipe();

    abstract void updateCurrentCard();

    public boolean isCardCursorAvailable() {
        return !(mCardCursor == null || mCardCursor.isClosed() || mCardCursor.getCount() <= 0);
    }

    public void refreshCursor() {
//        getLoaderManager().initLoader(LOADER_ID, null, this);
        if (mCardCursor instanceof FlashCardCursor) {
            FlashCardCursor cursor = (FlashCardCursor) mCardCursor;
            if (modelManager.getCurrentBatchSize() == 0) {
                Log.d("FlashCArdCursor::requery", "Warning - cursor requery on empty card pack");
            } else {
                cursor.moveToFirst();
            }
        }
    }

    public abstract void screenTouched();

    public abstract void moveCursorForwardToNextCard();

    public abstract void moveCursorBackToNextCard();

    protected abstract void fillData();

    protected abstract void cursorLoaded();

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1,
                               MotionEvent e2,
                               float velocityX,
                               float velocityY) {

            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    onLeftSwipe();
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    onRightSwipe();
                }
            } catch (Exception e) {
                Log.w("FlashCardSwipeScreenActivity::MyGestureDetector", "Caught exception while performing swipe action, ignored");
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            screenTouched();
            return false;
        }
    }
}
