package de.daniel.mobilepauker2.learning

import android.database.Cursor
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.lesson.card.CardPackRamAdapter
import de.daniel.mobilepauker2.lesson.card.FlashCard
import de.daniel.mobilepauker2.lesson.card.FlashCardCursor
import de.daniel.mobilepauker2.utils.Log
import javax.inject.Inject

abstract class FlashCardSwipeScreen : AppCompatActivity(R.layout.learn_cards),
    LoaderManager.LoaderCallbacks<Cursor> {

    private val INSTANCESTATE_CURSOR_POSITION = "INSTANCESTATE_CURSOR_POSITION"
    protected lateinit var mCardCursor: Cursor
    protected var currentCard: FlashCard = FlashCard()
    private var gestureDetector: GestureDetector? = null
    protected var mCardPackAdapter: CardPackRamAdapter? = null
    protected var mActivitySetupOk = false
    protected var mSavedCursorPosition = -1
    private var LOADER_ID = -1
    private lateinit var loader: LoaderManager

    @Inject
    lateinit var lessonManager: LessonManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        loader = LoaderManager.getInstance(this)

        mCardPackAdapter = CardPackRamAdapter(lessonManager)

        // Setup the cursor
        Log.d("FlashCardSwipeScreenActivity::onCreate", "Seting up cursor")
        LOADER_ID = 1
        //val loader = LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
        loader.initLoader(LOADER_ID, null, this)

        // Setup Gesture detection
        Log.d("FlashCardSwipeScreenActivity::onCreate", "Setting up gesture detection")
        gestureDetector = GestureDetector(this, MyGestureDetector())
        mActivitySetupOk = true
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putInt(INSTANCESTATE_CURSOR_POSITION, mCardCursor.position)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Log.d("FlashCardSwipeScreenActivity::onRestoreInstanceState", "Entry")
        mSavedCursorPosition = savedInstanceState.getInt(INSTANCESTATE_CURSOR_POSITION)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        super.onTouchEvent(ev)
        return gestureDetector!!.onTouchEvent(ev)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        mCardCursor = mCardPackAdapter!!.fetchAllFlashCards()
        if (mCardCursor.count <= 0) // no cards available
        {
            Log.d("FlashCardSwipeScreenActivity::onCreate", "No cards available so stopping")
            mActivitySetupOk = false
            finish()
        } else {
            cursorLoaded()
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mCardCursor.close()
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> =
        CursorLoader(this, mCardPackAdapter!!)

    abstract fun updateCurrentCard()

    open fun isCardCursorAvailable(): Boolean =
        !(mCardCursor.isClosed || mCardCursor.count <= 0)

    protected open fun reloadStack() {
        mCardPackAdapter = CardPackRamAdapter(lessonManager)
        mSavedCursorPosition = -1
        loader.destroyLoader(LOADER_ID)
        refreshCursor()
    }

    open fun refreshCursor() {
        if (LOADER_ID > -1) {
            loader.initLoader(LOADER_ID, null, this)
        }
    }

    open fun setCursorToFirst() {
        if (mCardCursor is FlashCardCursor) {
            val cursor: FlashCardCursor = mCardCursor as FlashCardCursor
            if (lessonManager.getBatchSize(BatchType.CURRENT) == 0) {
                Log.d("FlashCArdCursor::requery", "Warning - cursor requery on empty card pack")
            } else {
                cursor.moveToFirst()
            }
        }
    }

    abstract fun screenTouched()

    protected abstract fun fillData()

    protected abstract fun cursorLoaded()

    inner class MyGestureDetector : SimpleOnGestureListener() {
        override fun onDown(motionEvent: MotionEvent): Boolean {
            screenTouched()
            return false
        }
    }
}