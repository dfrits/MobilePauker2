package de.daniel.mobilepauker2.lesson.card

import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.utils.Log

class CardPackRamAdapter(val lessonManager: LessonManager) : CardPackAdapter() {
    val isLastCard: Boolean
        get() = cardCursor.isLast

    private val cardCursor: FlashCardCursor = FlashCardCursor(lessonManager)

    override fun open(): CardPackAdapter {
        return this
    }

    override fun close() {
        cardCursor.close()
    }

    @Throws(CursorIndexOutOfBoundsException::class)
    override fun deleteFlashCard(cardId: Long): Boolean {
        val position: Int = cardCursor.position
        var requestFirst = false
        if (position < 0) {
            throw CursorIndexOutOfBoundsException("Before first row.")
        }
        if (position >= lessonManager.getBatchSize(BatchType.CURRENT)) {
            throw CursorIndexOutOfBoundsException("After last row.")
        }

        Log.d("CardPackRamAdapter::deleteFlashCard", "CardCount - " + cardCursor.count)
        if (cardCursor.isFirst) {
            requestFirst = true
        } else {
            cardCursor.moveToPrevious()
        }
        val returnVal = lessonManager.deleteCard(position)

        // Point to the first card if
        // * We deleted second last card (size now is 1)
        // * We have just deleted the first card
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
        lessonManager.putCardToNextBatch(cardCursor.position)
    }

    fun setCardUnLearned() {
        lessonManager.moveCardToUnlearndBatch(cardCursor.position)
    }
}