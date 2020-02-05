package com.daniel.mobilepauker2.model.pauker_native

/*
 * SummaryBatch.java
 *
 * Created on 12. Juli 2007, 14:08
 */ /**
 * a temporary batch that contains all cards of a lesson
 */
class SummaryBatch
/**
 * creates a new SummaryBatch
 * @param lesson the lesson for this SummaryBatch
 */(private val lesson: Lesson) :
    Batch(lesson.cards) {
    /**
     * adds a card to this batch
     * @param card the new card
     */
    override fun addCard(card: Card?) {
        cards!!.add(card)
    }

    /**
     * removes a card from the batch
     * @param index the index where the card should be removed
     * @return the removed card
     */
    override fun removeCard(index: Int): Card? {
        val card = super.removeCard(index)
        // also remove the card from the "real" batch
        if (card!!.isLearned) {
            val batchNumber = card.longTermBatchNumber
            val longTermBatch = lesson.getLongTermBatch(batchNumber)
            longTermBatch!!.removeCard(card)
        } else {
            val unlearnedBatch =
                lesson.unlearnedBatch
            unlearnedBatch!!.removeCard(card)
        }
        return card
    }

}