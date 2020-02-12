package com.daniel.mobilepauker2.pauker_native

import java.util.*

/*
 * LongTermBatch.java
 *
 * Created on 12. Juli 2007, 14:05
 *
 */ /**
 * a long term batch
 * @author Ronny.Standtke@gmx.net
 */
class LongTermBatch(
    /**
     * returns the batch number
     * @return the batch number
     */
    val batchNumber: Int
) :
    Batch(null) {
    val expiredCards: MutableCollection<Card>
    /**
     * returns the expiration time of this batch
     * @return the expiration time of this batch
     */
    val expirationTime: Long

    /**
     * adds a card to this batch
     * @param card the new card
     */
    override fun addCard(card: Card) {
        card.longTermBatchNumber = batchNumber
        card.expirationTime = expirationTime
        cards?.add(card)
    }

    /**
     * returns a collection of all expired cards of this batch
     * @return a collection of all expired cards of this batch
     */
    fun refreshAndGetExpiredCards(): Collection<Card> {
        refreshExpiration()
        return expiredCards
    }

    /**
     * returns a collection of all learned (not new or expired) cards
     * @return a collection of all learned (not new or expired) cards
     */
    val learnedCards: Collection<Card>
        get() {
            val learnedCards: MutableCollection<Card> =
                ArrayList()
            val currentTime = System.currentTimeMillis()
            for (card in cards!!) {
                val learnedTime = card.learnedTimestamp
                val diff = currentTime - learnedTime
                if (diff < expirationTime) {
                    learnedCards.add(card)
                }
            }
            return learnedCards
        }

    /**
     * returns the number of expired cards
     * @return the number of expired cards
     */
    val numberOfExpiredCards: Int
        get() {
            refreshExpiration()
            return expiredCards.size
        }// return the card with the oldest expiration date

    /**
     * gets the oldest expired card
     * @return the expired card
     */
    val oldestExpiredCard: Card
        get() {
            refreshExpiration()
            // return the card with the oldest expiration date
            val iterator: Iterator<*> = expiredCards.iterator()
            var oldestCard =
                iterator.next() as Card
            while (iterator.hasNext()) {
                val tmpCard =
                    iterator.next() as Card
                if (tmpCard.expirationTime < oldestCard.expirationTime) {
                    oldestCard = tmpCard
                }
            }
            return oldestCard
        }

    /**
     * recalculates the batch of expired cards
     */
    fun refreshExpiration() {
        expiredCards.clear()
        val currentTime = System.currentTimeMillis()
        for (card in cards!!) {
            val learnedTime = card.learnedTimestamp
            val diff = currentTime - learnedTime
            if (diff > expirationTime) {
                expiredCards.add(card)
            }
        }
    }

    companion object {
        private const val ONE_SECOND: Long = 1000
        private const val ONE_MINUTE = ONE_SECOND * 60
        private const val ONE_HOUR = ONE_MINUTE * 60
        private const val ONE_DAY = ONE_HOUR * 24
        const val expirationUnit = ONE_DAY
    }

    /**
     * Creates a new instance of LongTermBatch
     * @param batchNumber the number of this long term batch
     */
    init {
        val factor = Math.pow(Math.E, batchNumber.toDouble())
        expirationTime = (expirationUnit * factor) as Long
        expiredCards = ArrayList()
    }
}