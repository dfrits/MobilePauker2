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
package com.daniel.mobilepauker2.model

import android.database.AbstractCursor
import android.database.CursorIndexOutOfBoundsException
import com.daniel.mobilepauker2.utils.Log

class FlashCardCursor : AbstractCursor() {
    private val modelManager: ModelManager? = ModelManager.Companion.instance()
    private val columnCount: Int
    /**
     * Adds a new row to the end of the FlashCard array.
     * @param columnValues in the same order as the the column names specified at cursor
     * construction time
     * @throws IllegalArgumentException if `columnValues.length != columnNames.length`
     */
    fun addRow(columnValues: Array<String?>) {
        require(columnValues.size == columnCount) {
            ("columnNames.length = "
                    + columnCount + ", columnValues.length = "
                    + columnValues.size)
        }
        modelManager!!.addCard(
            columnValues[CardPackAdapter.Companion.KEY_SIDEA_ID],
            columnValues[CardPackAdapter.Companion.KEY_SIDEB_ID],
            columnValues[CardPackAdapter.Companion.KEY_INDEX_ID],
            columnValues[CardPackAdapter.Companion.KEY_LEARN_STATUS_ID]
        )
    }

    /**
     * Gets value at the given column for the current row.
     */
    private operator fun get(column: Int): String? {
        if (column < 0 || column >= columnCount) {
            throw CursorIndexOutOfBoundsException(
                "Requested column: "
                        + column + ", # of columns: " + columnCount
            )
        }
        if (position < 0) {
            throw CursorIndexOutOfBoundsException("Before first row.")
        }
        if (position >= modelManager.getCurrentBatchSize()) {
            throw CursorIndexOutOfBoundsException("After last row.")
        }
        val flashCard = modelManager!!.getCard(position)
        when (column) {
            CardPackAdapter.Companion.KEY_SIDEA_ID -> {
                return if (flashCard == null) "" else flashCard.sideAText
            }
            CardPackAdapter.Companion.KEY_SIDEB_ID -> {
                return if (flashCard == null) "" else flashCard.sideBText
            }
            CardPackAdapter.Companion.KEY_ROWID_ID -> {
                //return flashCard.getId();
//********************* -- Is this ok - using the position of the cursor to id the flash card!
                return Integer.toString(position)
            }
            CardPackAdapter.Companion.KEY_LEARN_STATUS_ID -> {
                return if (flashCard != null && flashCard.isLearned) {
                    "true"
                } else {
                    "false"
                }
            }
            CardPackAdapter.Companion.KEY_INDEX_ID -> {
                return flashCard?.index
            }
        }
        return null
    }

    override fun getCount(): Int {
        return modelManager.getCurrentBatchSize()
    }

    override fun getColumnNames(): Array<String> {
        return Companion.columnNames
    }

    override fun getString(column: Int): String {
        return get(column) ?: return null
    }

    override fun getShort(column: Int): Short {
        val value = get(column) ?: return 0
        return value.toShort()
    }

    override fun getInt(column: Int): Int {
        val value = get(column) ?: return 0
        return value.toInt()
    }

    override fun getLong(column: Int): Long {
        val value = get(column) ?: return 0
        return value.toLong()
    }

    override fun getFloat(column: Int): Float {
        val value = get(column) ?: return 0.0f
        return value.toFloat()
    }

    override fun getDouble(column: Int): Double {
        val value = get(column) ?: return 0.0
        return value.toDouble()
    }

    override fun isNull(column: Int): Boolean {
        return get(column) == null
    }

    override fun requery(): Boolean {
        if (modelManager.getCurrentBatchSize() == 0) {
            Log.d(
                "FlashCArdCursor::requery",
                "Warning - cursor requery on empty card pack"
            )
        } else {
            super.moveToFirst()
        }
        return super.requery()
    }

    companion object {
        val columnNames = arrayOf<String>(
            CardPackAdapter.Companion.KEY_ROWID,
            CardPackAdapter.Companion.KEY_SIDEA,
            CardPackAdapter.Companion.KEY_SIDEB,
            CardPackAdapter.Companion.KEY_INDEX,
            CardPackAdapter.Companion.KEY_LEARN_STATUS
        )
    }

    init {
        columnCount = Companion.columnNames.size
    }
}