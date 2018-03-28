package com.daniel.mobilepauker2.model.pauker_native;

/*
 * LongTermBatch.java
 *
 * Created on 12. Juli 2007, 14:05
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * a long term batch
 * @author Ronny.Standtke@gmx.net
 */
public class LongTermBatch extends Batch {

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = ONE_SECOND * 60;
    private static final long ONE_HOUR = ONE_MINUTE * 60;
    private static final long ONE_DAY = ONE_HOUR * 24;
    private static final long EXPIRATION_UNIT = ONE_DAY;
    private final int batchNumber;
    private final Collection<Card> expiredCards;
    private final long expirationTime;

    /**
     * Creates a new instance of LongTermBatch
     * @param batchNumber the number of this long term batch
     */
    public LongTermBatch(int batchNumber) {
        super(null);
        this.batchNumber = batchNumber;
        double factor = Math.pow(Math.E, batchNumber);
        expirationTime = (long) (EXPIRATION_UNIT * factor);
        expiredCards = new ArrayList<>();
    }

    /**
     * returns the batch number
     * @return the batch number
     */
    public int getBatchNumber() {
        return batchNumber;
    }

    /**
     * returns the expiration time of this batch
     * @return the expiration time of this batch
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * adds a card to this batch
     * @param card the new card
     */
    @Override
    public void addCard(Card card) {
        card.setLongTermBatchNumber(batchNumber);
        card.setExpirationTime(expirationTime);
        cards.add(card);
    }

    /**
     * returns a collection of all expired cards of this batch
     * @return a collection of all expired cards of this batch
     */
    public Collection<Card> getExpiredCards() {
        refreshExpiration();
        return expiredCards;
    }

    /**
     * returns a collection of all learned (not new or expired) cards
     * @return a collection of all learned (not new or expired) cards
     */
    public Collection<Card> getLearnedCards() {
        Collection<Card> learnedCards = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        for (Card card : cards) {
            long learnedTime = card.getLearnedTimestamp();
            long diff = currentTime - learnedTime;
            if (diff < expirationTime) {
                learnedCards.add(card);
            }
        }
        return learnedCards;
    }

    /**
     * returns the number of expired cards
     * @return the number of expired cards
     */
    public int getNumberOfExpiredCards() {
        refreshExpiration();
        return expiredCards.size();
    }

    /**
     * gets the oldest expired card
     * @return the expired card
     */
    public Card getOldestExpiredCard() {
        refreshExpiration();
        // return the card with the oldest expiration date
        Iterator iterator = expiredCards.iterator();
        Card oldestCard = (Card) iterator.next();
        while (iterator.hasNext()) {
            Card tmpCard = (Card) iterator.next();
            if (tmpCard.getExpirationTime() < oldestCard.getExpirationTime()) {
                oldestCard = tmpCard;
            }
        }
        return oldestCard;
    }

    /**
     * recalculates the batch of expired cards
     */
    public void refreshExpiration() {
        expiredCards.clear();
        long currentTime = System.currentTimeMillis();
        for (Card card : cards) {

            long learnedTime = card.getLearnedTimestamp();
            long diff = currentTime - learnedTime;
            String frontSide = card.getFrontSide().getText();
            //Log.d("LongTermBatch::refreshExpiration","currentTime = "+currentTime + ",cardTime=" 
            //		+ learnedTime + ",diff=" + diff + ",expirationTime" + expirationTime + "," + frontSide);

            if (diff > expirationTime) {
                expiredCards.add(card);
            }
        }
    }

    public static long getExpirationUnit() {
        return EXPIRATION_UNIT;
    }
}
