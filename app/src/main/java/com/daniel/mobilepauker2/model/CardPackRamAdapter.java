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
import android.database.CursorIndexOutOfBoundsException;

import com.daniel.mobilepauker2.utils.Log;

public class CardPackRamAdapter extends CardPackAdapter {
    private final ModelManager modelManager = ModelManager.instance();
    private final FlashCardCursor cardCursor;

    public CardPackRamAdapter(Context context) {
        super(context);
        cardCursor = new FlashCardCursor(mContext);
    }

    @Override
    public CardPackAdapter open() {
        return this;
    }

    @Override
    public void close() {
        cardCursor.close();
    }

    @Override
    public long createFlashCard(String sideA, String sideB, int index,
                                boolean learnStatus) {

        String[] columnValues = new String[5];

        columnValues[0] = "1";
        columnValues[1] = sideA;
        columnValues[2] = sideB;
        columnValues[3] = Integer.toString(index);

        if (learnStatus) {
            columnValues[4] = "true";
        } else {
            columnValues[4] = "false";
        }

        cardCursor.addRow(columnValues);

        return 0;
    }

    @Override
    public boolean deleteFlashCard(long cardId) throws CursorIndexOutOfBoundsException {

        int position = cardCursor.getPosition();
        boolean returnVal;
        boolean requestFirst = false;

        if (position < 0) {
            throw new CursorIndexOutOfBoundsException("Before first row.");
        }
        if (position >= modelManager.getCurrentBatchSize()) {
            throw new CursorIndexOutOfBoundsException("After last row.");
        }

        //******************************************************************
        //BUGFIX: Deleting cards caused a crash due to cursor moving out of bounds
        //Date: 29 October 2011
        //******************************************************************
        Log.d("CardPackRamAdapter::deleteFlashCard", "CardCount - " + cardCursor.getCount());

        if (cardCursor.isFirst()) {
            requestFirst = true;
        } else {
            cardCursor.moveToPrevious();
        }

        returnVal = modelManager.deleteCard(position);

        // Point to the first card if
        // * We deleted second last card (size now is 1)
        // * We have just deleted the first card
        if (cardCursor.getCount() == 1 || requestFirst) {
            cardCursor.moveToFirst();
        }

        return returnVal;

        //******************************************************************
        //BUGFIX: End of bugfix
        //******************************************************************
    }

    @Override
    public Cursor fetchAllFlashCards() {
        return cardCursor;

    }

    @Override
    public int countCardsInTable() {
        return cardCursor.getCount();
    }

    public void setCardLearned(long rowId) {
        // Ignore rowId here as not using database
        // Just set what ever card the cursor is pointing to as learned
        modelManager.setCardLearned(cardCursor.getPosition());
    }

    public void setCardUnLearned(Context context, long rowId) {
        // Ignore rowId here as not using database
        // Just set what ever card the cursor is pointing to as learned
        modelManager.pullCurrentCard(cardCursor.getPosition(), context);
    }

    public boolean isLastCard() {
        return cardCursor.isLast();
    }

    @Override
    public boolean updateFlashCard(long cardId, String sideA, String sideB,
                                   int index, boolean learnStatus) {
        return false;
    }
}
