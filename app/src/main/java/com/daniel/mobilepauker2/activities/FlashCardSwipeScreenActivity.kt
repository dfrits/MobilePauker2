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
package com.daniel.mobilepauker2.activities

import android.app.LoaderManager
import android.content.Context
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.model.*
import com.daniel.mobilepauker2.utils.Log

abstract class FlashCardSwipeScreenActivity : AppCompatActivity(),
    LoaderManager.LoaderCallbacks<Cursor> {
    protected val settingsManager: SettingsManager? = SettingsManager.Companion.instance()
    protected val modelManager: ModelManager? = ModelManager.Companion.instance()
    protected val INSTANCESTATE_CURSOR_POSITION = "INSTANCESTATE_CURSOR_POSITION"
    private val context: Context = this
    protected var mCardCursor: Cursor? = null
    protected var currentCard = FlashCard()
    protected var gestureDetector: GestureDetector? = null
    protected var mCardPackAdapter: CardPackRamAdapter? = null
    protected var mActivitySetupOk = false
    protected var mSavedCursorPosition = -1
    private var LOADER_ID = -1
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCardPackAdapter = CardPackRamAdapter(this)
        // Setup the cursor
        Log.d(
            "FlashCardSwipeScreenActivity::onCreate",
            "Seting up cursor"
        )
        LOADER_ID = 1
        loaderManager.initLoader(LOADER_ID, null, this)
        // Setup Gesture detection
        Log.d(
            "FlashCardSwipeScreenActivity::onCreate",
            "Setting up gesture detection"
        )
        gestureDetector = GestureDetector(context, MyGestureDetector())
        mActivitySetupOk = true
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putInt(INSTANCESTATE_CURSOR_POSITION, mCardCursor!!.position)
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Log.d(
            "FlashCardSwipeScreenActivity::onRestoreInstanceState",
            "Entry"
        )
        mSavedCursorPosition = savedInstanceState.getInt(INSTANCESTATE_CURSOR_POSITION)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        super.onTouchEvent(ev)
        return gestureDetector!!.onTouchEvent(ev)
    }

    override fun onCreateLoader(
        id: Int,
        args: Bundle
    ): Loader<Cursor> {
        return CursorLoader(context, mCardPackAdapter)
    }

    override fun onLoadFinished(
        loader: Loader<Cursor>,
        cursor: Cursor
    ) {
        mCardCursor = mCardPackAdapter!!.fetchAllFlashCards()
        //mCardCursor = cursor;
        if (mCardCursor == null || mCardCursor!!.count <= 0) // no cards available
        {
            Log.d(
                "FlashCardSwipeScreenActivity::onCreate",
                "No cards available so stopping"
            )
            mActivitySetupOk = false
            finish()
        } else {
            cursorLoaded()
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mCardCursor!!.close()
    }

    abstract fun updateCurrentCard()
    val isCardCursorAvailable: Boolean
        get() = !(mCardCursor == null || mCardCursor!!.isClosed || mCardCursor!!.count <= 0)

    /**
     * Lädt des Stack neu.
     */
    protected fun reloadStack() {
        mCardPackAdapter = CardPackRamAdapter(context)
        mSavedCursorPosition = -1
        loaderManager.destroyLoader(LOADER_ID)
        refreshCursor()
    }

    fun refreshCursor() {
        if (LOADER_ID > -1) {
            loaderManager.initLoader(LOADER_ID, null, this)
        }
    }

    fun setCursorToFirst() {
        if (mCardCursor is FlashCardCursor) {
            val cursor = mCardCursor as FlashCardCursor
            if (modelManager.getCurrentBatchSize() == 0) {
                Log.d(
                    "FlashCArdCursor::requery",
                    "Warning - cursor requery on empty card pack"
                )
            } else {
                cursor.moveToFirst()
            }
        }
    }

    abstract fun screenTouched()
    protected abstract fun fillData()
    protected abstract fun cursorLoaded()
    internal inner class MyGestureDetector : SimpleOnGestureListener() {
        override fun onDown(motionEvent: MotionEvent): Boolean {
            screenTouched()
            return false
        }
    }
}