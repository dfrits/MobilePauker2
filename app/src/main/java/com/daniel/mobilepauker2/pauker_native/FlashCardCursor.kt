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
package com.daniel.mobilepauker2.pauker_native

import android.database.AbstractCursor
import android.database.CursorIndexOutOfBoundsException

class FlashCardCursor : AbstractCursor() {
    private val modelManager: ModelManager = ModelManager.instance()
    private val flashColumnCount: Int = columnNames.size

    /**
     * Adds a new row to the end of the FlashCard array.
     * @param columnValues in the same order as the the column names specified at cursor
     * construction time
     * @throws IllegalArgumentException if `columnValues.length != columnNames.length`
     */
    fun addRow(columnValues: Array<String?>) {
        require(columnValues.size == flashColumnCount) {
            ("columnNames.length = "
                    + flashColumnCount + ", columnValues.length = "
                    + columnValues.size)
        }
        modelManager.addCard(
            columnValues[CardPackAdapter.KEY_SIDEA_ID],
            columnValues[CardPackAdapter.KEY_SIDEB_ID],
            columnValues[CardPackAdapter.KEY_INDEX_ID],
            columnValues[CardPackAdapter.KEY_LEARN_STATUS_ID]
        )
    }

    /**
     * Gets value at the given column for the current row.
     */
    private operator fun get(column: Int): String? {
        if (column < 0 || column >= flashColumnCount) {
            throw CursorIndexOutOfBoundsException(
                "Requested column: "
                        + column + ", # of columns: " + flashColumnCount
            )
        }
        if (position < 0) {
            throw CursorIndexOutOfBoundsException("Before first row.")
        }
        if (position >= modelManager.currentBatchSize) {
            throw CursorIndexOutOfBoundsException("After last row.")
        }
        val flashCard = modelManager.getCard(position)
        when (column) {
            CardPackAdapter.KEY_SIDEA_ID -> {
                return flashCard?.sideAText ?: ""
            }
            CardPackAdapter.KEY_SIDEB_ID -> {
                return flashCard?.sideBText ?: ""
            }
            CardPackAdapter.KEY_ROWID_ID -> {
                return position.toString()
            }
            CardPackAdapter.KEY_LEARN_STATUS_ID -> {
                return if (flashCard != null && flashCard.isLearned) {
                    "true"
                } else {
                    "false"
                }
            }
            CardPackAdapter.KEY_INDEX_ID -> {
                return flashCard?.index
            }
        }
        return null
    }

    override fun getCount(): Int {
        return modelManager.currentBatchSize
    }

    override fun getColumnNames(): Array<String> {
        return flashColumnNames
    }

    override fun getString(column: Int): String {
        return get(column) ?: return ""
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

    companion object {
        val flashColumnNames = arrayOf(
            CardPackAdapter.KEY_ROWID,
            CardPackAdapter.KEY_SIDEA,
            CardPackAdapter.KEY_SIDEB,
            CardPackAdapter.KEY_INDEX,
            CardPackAdapter.KEY_LEARN_STATUS
        )
    }
}