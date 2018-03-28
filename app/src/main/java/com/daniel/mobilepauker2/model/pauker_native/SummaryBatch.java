package com.daniel.mobilepauker2.model.pauker_native;

/*
 * SummaryBatch.java
 *
 * Created on 12. Juli 2007, 14:08
 */

/**
 * a temporary batch that contains all cards of a lesson
 */
public class SummaryBatch extends Batch {

    private final Lesson lesson;

    /**
     * creates a new SummaryBatch
     * @param lesson the lesson for this SummaryBatch
     */
    public SummaryBatch(Lesson lesson) {
        super(lesson.getCards());
        this.lesson = lesson;
    }

    /**
     * adds a card to this batch
     * @param card the new card
     */
    @Override
    public void addCard(Card card) {
        cards.add(card);
    }

    /**
     * removes a card from the batch
     * @param index the index where the card should be removed
     * @return the removed card
     */
    @Override
    public Card removeCard(int index) {
        Card card = super.removeCard(index);

        // also remove the card from the "real" batch
        if (card.isLearned()) {
            int batchNumber = card.getLongTermBatchNumber();
            LongTermBatch longTermBatch = lesson.getLongTermBatch(batchNumber);
            longTermBatch.removeCard(card);
        } else {
            Batch unlearnedBatch = lesson.getUnlearnedBatch();
            unlearnedBatch.removeCard(card);
        }
        return card;
    }
}
