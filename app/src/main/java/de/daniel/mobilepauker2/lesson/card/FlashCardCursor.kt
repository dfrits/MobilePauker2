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
package de.daniel.mobilepauker2.lesson.card

import android.database.AbstractCursor
import android.database.CursorIndexOutOfBoundsException
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType


class FlashCardCursor(val lessonManager: LessonManager) : AbstractCursor() {

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
        if (position >= lessonManager.getBatchSize(BatchType.CURRENT)) {
            throw CursorIndexOutOfBoundsException("After last row.")
        }
        val flashCard: FlashCard? = lessonManager.getCardFromCurrentPack(position)
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

    override fun getColumnCount(): Int = columnNames.size

    override fun getCount(): Int =
        lessonManager.getBatchSize(BatchType.CURRENT)

    override fun getColumnNames(): Array<String> = arrayOf(
        CardPackAdapter.KEY_ROWID,
        CardPackAdapter.KEY_SIDEA,
        CardPackAdapter.KEY_SIDEB,
        CardPackAdapter.KEY_INDEX,
        CardPackAdapter.KEY_LEARN_STATUS
    )

    override fun getString(column: Int): String = get(column) ?: ""

    override fun getShort(column: Int): Short = get(column)?.toShort() ?: 0

    override fun getInt(column: Int): Int = get(column)?.toInt() ?: 0

    override fun getLong(column: Int): Long = get(column)?.toLong() ?: 0

    override fun getFloat(column: Int): Float = get(column)?.toFloat() ?: 0.0f

    override fun getDouble(column: Int): Double = get(column)?.toDouble() ?: 0.0

    override fun isNull(column: Int): Boolean = get(column) == null
}