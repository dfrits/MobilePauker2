package de.daniel.mobilepauker2.learning

import android.content.Context
import android.database.Cursor
import androidx.loader.content.AsyncTaskLoader
import de.daniel.mobilepauker2.lesson.card.CardPackRamAdapter

class CursorLoader(context: Context, private val adapter: CardPackRamAdapter) :
    AsyncTaskLoader<Cursor>(context) {

    private var cursor: Cursor? = null

    override fun loadInBackground(): Cursor? {
        if (cursor == null)
            cursor = adapter.fetchAllFlashCards()

        return cursor
    }

    override fun deliverResult(result: Cursor?) {
        if (isReset && result != null) {
            result.close()
            return
        }
        val oldCursor = cursor
        cursor = result
        if (isStarted) {
            super.deliverResult(result)
        }
        if (oldCursor != null && oldCursor !== result) {
            oldCursor.close()
        }
    }

    override fun onStartLoading() {
        if (cursor != null) deliverResult(cursor) else forceLoad()
    }

    override fun onStopLoading() {
        cancelLoad()
    }

    override fun onReset() {
        onStopLoading()
        if (cursor != null) {
            cursor!!.close()
            cursor = null
        }
    }

    override fun onCanceled(data: Cursor?) {
        super.onCanceled(data)
        if (cursor != null) {
            cursor!!.close()
        }
    }
}