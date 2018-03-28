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

package com.daniel.mobilepauker2.model;

import android.content.Context;
import android.database.Cursor;


public abstract class CardPackAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_SIDEA = "sidea";
    public static final String KEY_SIDEB = "sideb";
    public static final String KEY_INDEX = "indext";
    public static final String KEY_LEARN_STATUS = "learnStatus";


    public static final int KEY_ROWID_ID = 0;
    public static final int KEY_SIDEA_ID = 1;
    public static final int KEY_SIDEB_ID = 2;
    public static final int KEY_INDEX_ID = 3;
    public static final int KEY_LEARN_STATUS_ID = 4;

    protected final Context mContext;

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * @param context the Context within which to work
     */
    public CardPackAdapter(Context context) {
        this.mContext = context;
    }

    public abstract CardPackAdapter open();

    public abstract void close();

    public abstract long createFlashCard(String sideA, String sideB, int index, boolean learnStatus);

    public abstract boolean deleteFlashCard(long cardId);

    public abstract Cursor fetchAllFlashCards();

    public abstract boolean updateFlashCard(long cardId, String sideA, String sideB, int index, boolean learnStatus);

    public abstract int countCardsInTable();
}
