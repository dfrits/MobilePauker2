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

import android.database.Cursor

abstract class CardPackAdapter {
    abstract fun open(): CardPackAdapter
    abstract fun close()
    abstract fun createFlashCard(
        sideA: String?,
        sideB: String?,
        index: Int,
        learnStatus: Boolean
    ): Long

    abstract fun deleteFlashCard(cardId: Long): Boolean
    abstract fun fetchAllFlashCards(): Cursor
    abstract fun updateFlashCard(
        cardId: Long,
        sideA: String?,
        sideB: String?,
        index: Int,
        learnStatus: Boolean
    ): Boolean

    abstract fun countCardsInTable(): Int

    companion object {
        const val KEY_ROWID = "_id"
        const val KEY_SIDEA = "sidea"
        const val KEY_SIDEB = "sideb"
        const val KEY_INDEX = "indext"
        const val KEY_LEARN_STATUS = "learnStatus"
        const val KEY_ROWID_ID = 0
        const val KEY_SIDEA_ID = 1
        const val KEY_SIDEB_ID = 2
        const val KEY_INDEX_ID = 3
        const val KEY_LEARN_STATUS_ID = 4
    }
}