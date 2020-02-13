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

import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException

class CardPackRamAdapter : CardPackAdapter() {
    private val modelManager: ModelManager =
        ModelManager.instance()
    private val cardCursor: FlashCardCursor = FlashCardCursor()
    override fun open(): CardPackAdapter {
        return this
    }

    override fun close() {
        cardCursor.close()
    }

    override fun createFlashCard(
        sideA: String?,
        sideB: String?,
        index: Int,
        learnStatus: Boolean
    ): Long {
        val columnValues = arrayOfNulls<String>(5)
        columnValues[0] = "1"
        columnValues[1] = sideA
        columnValues[2] = sideB
        columnValues[3] = index.toString()
        if (learnStatus) {
            columnValues[4] = "true"
        } else {
            columnValues[4] = "false"
        }
        cardCursor.addRow(columnValues)
        return 0
    }

    @Throws(CursorIndexOutOfBoundsException::class)
    override fun deleteFlashCard(cardId: Long): Boolean {
        val position = cardCursor.position
        val returnVal: Boolean
        var requestFirst = false
        if (position < 0) {
            throw CursorIndexOutOfBoundsException("Before first row.")
        }
        if (position >= modelManager.currentBatchSize) {
            throw CursorIndexOutOfBoundsException("After last row.")
        }
        Log.d(
            "CardPackRamAdapter::deleteFlashCard",
            "CardCount - " + cardCursor.count
        )
        if (cardCursor.isFirst) {
            requestFirst = true
        } else {
            cardCursor.moveToPrevious()
        }
        returnVal = modelManager.deleteCard(position)

        if (cardCursor.count == 1 || requestFirst) {
            cardCursor.moveToFirst()
        }
        return returnVal
    }

    override fun fetchAllFlashCards(): Cursor {
        return cardCursor
    }

    override fun countCardsInTable(): Int {
        return cardCursor.count
    }

    fun setCardLearned() {
        modelManager.setCardLearned(cardCursor.position)
    }

    fun setCardUnLearned(context: Context) {
        modelManager.pullCurrentCard(cardCursor.position, context)
    }

    val isLastCard: Boolean
        get() = cardCursor.isLast

    override fun updateFlashCard(
        cardId: Long,
        sideA: String?,
        sideB: String?,
        index: Int,
        learnStatus: Boolean
    ): Boolean {
        return false
    }
}