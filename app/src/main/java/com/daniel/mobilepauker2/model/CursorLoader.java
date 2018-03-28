package com.daniel.mobilepauker2.model;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;

/**
 * Created by Daniel on 18.03.2018.
 * Masterarbeit:
 * MobilePauker++ - innovativ, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class CursorLoader extends AsyncTaskLoader<Cursor> {
    private Cursor cursor;
    private final CardPackRamAdapter adapter;

    public CursorLoader(Context context, CardPackRamAdapter cardPackRamAdapter) {
        super(context);
        adapter = cardPackRamAdapter;
    }

    @Override
    public Cursor loadInBackground() {
        if(cursor == null)
            cursor =  adapter.fetchAllFlashCards();
        return cursor;
    }

    @Override
    public void deliverResult(Cursor result) {
        if (isReset() && result != null) {
            result.close();
            return;
        }

        Cursor oldCursor = cursor;
        cursor = result;

        if (isStarted()) {
            super.deliverResult(result);
        }

        if (oldCursor != null && oldCursor != result) {
            oldCursor.close();
        }
    }

    @Override
    protected void onStartLoading() {
        if(cursor != null) deliverResult(cursor);
        else forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();

        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    @Override
    public void onCanceled(Cursor data) {
        super.onCanceled(data);

        cursor.close();
    }
}
