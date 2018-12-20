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

public abstract class FlashCardSwipeScreenActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    protected final SettingsManager settingsManager = SettingsManager.instance();
    protected final ModelManager modelManager = ModelManager.instance();
    protected final String INSTANCESTATE_CURSOR_POSITION = "INSTANCESTATE_CURSOR_POSITION";
    private final Context context = this;
    protected Cursor mCardCursor = null;
    protected FlashCard currentCard = new FlashCard();
    protected GestureDetector gestureDetector;
    protected CardPackRamAdapter mCardPackAdapter;
    protected boolean mActivitySetupOk = false;
    protected int mSavedCursorPosition = 0;
    private int LOADER_ID = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCardPackAdapter = new CardPackRamAdapter(this);

        // Setup the cursor
        Log.d("FlashCardSwipeScreenActivity::onCreate", "Seting up cursor");

        LOADER_ID = 1;
        getLoaderManager().initLoader(LOADER_ID, null, this);

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

    abstract void updateCurrentCard();

    public boolean isCardCursorAvailable() {
        return !(mCardCursor == null || mCardCursor.isClosed() || mCardCursor.getCount() <= 0);
    }

    public void refreshCursor() {
        if (LOADER_ID > -1) {
            getLoaderManager().initLoader(LOADER_ID, null, this);
        }
    }

    public void setCursorToFirst() {
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

    protected abstract void fillData();

    protected abstract void cursorLoaded();

    class MyGestureDetector extends SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            screenTouched();
            return false;
        }
    }
}
