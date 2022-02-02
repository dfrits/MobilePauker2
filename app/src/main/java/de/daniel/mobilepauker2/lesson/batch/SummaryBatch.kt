package de.daniel.mobilepauker2.lesson.batch

import de.daniel.mobilepauker2.lesson.Lesson
import de.daniel.mobilepauker2.lesson.card.Card

class SummaryBatch(val lesson: Lesson) : Batch(lesson.getCards()) {

    override fun addCard(card: Card) {
        cards.add(card)
    }

    override fun removeCard(index: Int): Card {
        val card = super.removeCard(index)

        // also remove the card from the "real" batch
        if (card.isLearned) {
            val batchNumber: Int = card.longTermBatchNumber
            val longTermBatch: LongTermBatch =
                lesson.getLongTermBatchFromIndex(batchNumber) as LongTermBatch
            longTermBatch.removeCard(card)
        } else {
            val unlearnedBatch: Batch = lesson.unlearnedBatch
            unlearnedBatch.removeCard(card)
        }
        return card
    }
}