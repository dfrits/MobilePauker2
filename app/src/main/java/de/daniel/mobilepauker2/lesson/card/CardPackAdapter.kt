package de.daniel.mobilepauker2.lesson.card

import android.content.Context
import android.database.Cursor

abstract class CardPackAdapter {

    abstract fun open(): CardPackAdapter

    abstract fun close()

    abstract fun deleteFlashCard(cardId: Long): Boolean

    abstract fun fetchAllFlashCards(): Cursor

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