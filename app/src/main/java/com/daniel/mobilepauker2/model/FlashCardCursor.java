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

import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;

import com.daniel.mobilepauker2.utils.Log;

public class FlashCardCursor extends AbstractCursor {
    private final ModelManager modelManager = ModelManager.instance();

    static final String[] columnNames = new String[]{
            CardPackAdapter.KEY_ROWID,
            CardPackAdapter.KEY_SIDEA,
            CardPackAdapter.KEY_SIDEB,
            CardPackAdapter.KEY_INDEX,
            CardPackAdapter.KEY_LEARN_STATUS
    };

    private final int columnCount;

    public FlashCardCursor() {
        this.columnCount = columnNames.length;
    }

    /**
     * Adds a new row to the end of the FlashCard array.
     * @param columnValues in the same order as the the column names specified at cursor
     *                     construction time
     * @throws IllegalArgumentException if {@code columnValues.length != columnNames.length}
     */
    public void addRow(String[] columnValues) {

        if (columnValues.length != columnCount) {
            throw new IllegalArgumentException("columnNames.length = "
                    + columnCount + ", columnValues.length = "
                    + columnValues.length);
        }

        modelManager.addCard(
                columnValues[CardPackAdapter.KEY_SIDEA_ID],
                columnValues[CardPackAdapter.KEY_SIDEB_ID],
                columnValues[CardPackAdapter.KEY_ROWID_ID],
                columnValues[CardPackAdapter.KEY_INDEX_ID],
                columnValues[CardPackAdapter.KEY_LEARN_STATUS_ID]);
    }


    /**
     * Gets value at the given column for the current row.
     */
    private String get(int column) {
        if (column < 0 || column >= columnCount) {
            throw new CursorIndexOutOfBoundsException("Requested column: "
                    + column + ", # of columns: " + columnCount);
        }
        if (getPosition() < 0) {
            throw new CursorIndexOutOfBoundsException("Before first row.");
        }
        if (getPosition() >= modelManager.getCurrentBatchSize()) {
            throw new CursorIndexOutOfBoundsException("After last row.");
        }

        FlashCard flashCard = modelManager.getCard(getPosition());

        switch (column) {
            case (CardPackAdapter.KEY_SIDEA_ID): {
                return flashCard == null ? "" : flashCard.getSideAText();
            }
            case (CardPackAdapter.KEY_SIDEB_ID): {
                return flashCard == null ? "" : flashCard.getSideBText();
            }
            case (CardPackAdapter.KEY_ROWID_ID): {
                //return flashCard.getId();

                //********************* -- Is this ok - using the position of the cursor to id the flash card!
                return Integer.toString(getPosition());
            }
            case (CardPackAdapter.KEY_LEARN_STATUS_ID): {
                if (flashCard != null && flashCard.isLearned()) {
                    return "true";
                } else {
                    return "false";
                }
            }
            case (CardPackAdapter.KEY_INDEX_ID): {
                return flashCard == null ? null : flashCard.getIndex();
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return modelManager.getCurrentBatchSize();
    }

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }

    @Override
    public String getString(int column) {
        Object value = get(column);
        if (value == null) return null;
        return value.toString();
    }

    @Override
    public short getShort(int column) {
        Object value = get(column);
        if (value == null) return 0;
        return Short.parseShort(value.toString());
    }

    @Override
    public int getInt(int column) {
        Object value = get(column);
        if (value == null) return 0;
        return Integer.parseInt(value.toString());
    }

    @Override
    public long getLong(int column) {
        Object value = get(column);
        if (value == null) return 0;
        return Long.parseLong(value.toString());
    }

    @Override
    public float getFloat(int column) {
        Object value = get(column);
        if (value == null) return 0.0f;
        return Float.parseFloat(value.toString());
    }

    @Override
    public double getDouble(int column) {
        Object value = get(column);
        if (value == null) return 0.0d;
        return Double.parseDouble(value.toString());
    }

    @Override
    public boolean isNull(int column) {
        return get(column) == null;
    }

    public boolean requery() {
        if (modelManager.getCurrentBatchSize() == 0) {
            Log.d("FlashCArdCursor::requery", "Warning - cursor requery on empty card pack");
        } else {
            super.moveToFirst();
        }
        return super.requery();
    }
}
