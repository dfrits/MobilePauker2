package com.daniel.mobilepauker2.model.pauker_native

import java.util.*

/**
 * A card is part of a batch. Besides having a front side and a reverse side
 * it can contain information about the date the card was learned and if
 * the card should be repeated by typing or not.
 * @author Ronny.Standtke@gmx.net
 */
open class Card
/**
 * creates a new card
 * @param frontSide   the front side of the card
 * @param reverseSide the reverse side of the card
 */(
    /**
     * returns the front side of the card
     * @return the front side of the card
     */
    var frontSide: CardSide,
    /**
     * returns the reverse side of the card
     * @return the reverse side of the card
     */
    var reverseSide: CardSide
) :
    Comparable<Card> {
    /**
     * returns the unique object identifier<br></br>
     * used for indexing!
     * @return the unique object id
     */
    // for indexing; unique id will be used to store card in the object-store
    var id: String? = null
        get() {
            if (field == null) {
                field = UID().toString()
            }
            return field
        }
        private set
    private var expirationTime: Long = 0
    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other =
            obj as Card
        return (frontSide == other.frontSide
                && reverseSide == other.reverseSide
                && expirationTime == other.expirationTime)
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 47 * hash + if (frontSide != null) frontSide.hashCode() else 0
        hash = 47 * hash + if (reverseSide != null) reverseSide.hashCode() else 0
        hash = 47 * hash + (expirationTime xor (expirationTime ushr 32)).toInt()
        return hash
    }

    override fun compareTo(otherCard: Card): Int {
        val frontSideResult = frontSide.compareTo(otherCard.frontSide)
        if (frontSideResult != 0) {
            return frontSideResult
        }
        val reverseSideResult = reverseSide.compareTo(otherCard.reverseSide)
        if (reverseSideResult != 0) {
            return reverseSideResult
        }
        // both sides are equal, base decision on expiration time
        val otherExpirationTime = otherCard.getExpirationTime()
        if (expirationTime < otherExpirationTime) {
            return -1
        } else if (expirationTime > otherExpirationTime) {
            return 1
        }
        // cards are equal
        return 0
    }

    /**
     * returns the timestamp when the card was learned
     * @return the timestamp when the card was learned
     */
    val learnedTimestamp: Long
        get() = frontSide.learnedTimestamp

    fun setLearnedTimeStamp(learnedTimeStamp: Long) {
        frontSide.setLearnedTimeStamp(learnedTimeStamp)
    }

    /**
     * updates the "learned" timestamp of this card (e.g. when moving this card
     * from one long term batch to the next one)
     */
    fun updateLearnedTimeStamp() {
        frontSide.setLearnedTimeStamp(System.currentTimeMillis())
    }

    /**
     * determines if the card is learned or not
     * @return if true the card is learned
     */
    /**
     * sets if the card is learned or not
     * @param learned if true the cards state is set to learned and the current date is used as
     * learnedDate
     */
    var isLearned: Boolean
        get() = frontSide.isLearned
        set(learned) {
            frontSide.isLearned = learned
            if (learned) {
                frontSide.setLearnedTimeStamp(System.currentTimeMillis())
            }
        }

    /**
     * determines the number of the batch this card is part of
     * @return the number of the batch this card is part of
     */
    /**
     * sets the number of the batch this card is part of
     * @param batchNumber the number of the batch this card is part of
     */
    var longTermBatchNumber: Int
        get() = frontSide.longTermBatchNumber
        set(batchNumber) {
            if (batchNumber > -1) {
                frontSide.longTermBatchNumber = batchNumber
            }
        }

    /**
     * returns the expiration time of this card
     * @return the expiration time of this card
     */
    fun getExpirationTime(): Long {
        return if (frontSide.isLearned) {
            frontSide.learnedTimestamp + expirationTime
        } else -1
    }

    /**
     * sets the expiration time of this card
     * @param expirationTime the expiration time of this card
     */
    fun setExpirationTime(expirationTime: Long) {
        this.expirationTime = expirationTime
    }

    /**
     * expires this card
     */
    fun expire() {
        val expiredTime = System.currentTimeMillis() - expirationTime - 60000
        frontSide.isLearned = true
        frontSide.setLearnedTimeStamp(expiredTime)
    }

    /**
     * determines if this card should be repeated by typing or not
     * @return if true this card should be repeated by typing
     */
    val isRepeatedByTyping: Boolean
        get() = frontSide.isRepeatedByTyping

    /**
     * sets if this card should be repeated by typing or not
     * @param repeatByTyping if true this card should be repeated by typing
     */
    fun setRepeatByTyping(repeatByTyping: Boolean) {
        frontSide.setRepeatByTyping(repeatByTyping)
    }

    /**
     * Sets a search pattern onto this card.
     * @param pattern   the search pattern
     * @param matchCase if the case of the pattern must be matched
     * @param cardSide  the card side where to look for the pattern can be one of: Pauker.FRONT_SIDE
     * Pauker.REVERSE_SIDE Pauker.BOTH_SIDES
     * @return a list with search match indices
     */
    fun search(
        cardSide: Element?,
        pattern: String?,
        matchCase: Boolean
    ): List<SearchHit> {
        val searchHits = LinkedList<SearchHit>()
        if (cardSide == Element.FRONT_SIDE) {
            searchHits.addAll(
                frontSide.search(
                    this,
                    Element.FRONT_SIDE,
                    pattern,
                    matchCase
                )
            )
            reverseSide.cancelSearch()
        } else if (cardSide == Element.REVERSE_SIDE) {
            frontSide.cancelSearch()
            searchHits.addAll(
                reverseSide.search(
                    this,
                    Element.REVERSE_SIDE,
                    pattern,
                    matchCase
                )
            )
        } else { // BOTH_SIDES
            searchHits.addAll(
                frontSide.search(
                    this,
                    Element.FRONT_SIDE,
                    pattern,
                    matchCase
                )
            )
            searchHits.addAll(
                reverseSide.search(
                    this,
                    Element.REVERSE_SIDE,
                    pattern,
                    matchCase
                )
            )
        }
        return searchHits
    }

    /**
     * flips the card sides
     */
    fun flip() {
        val learnedTimestamp = frontSide.learnedTimestamp
        val longTermBatchNumber = frontSide.longTermBatchNumber
        val orientation =
            frontSide.orientation
        val learned = frontSide.isLearned
        val repeatedByTyping = frontSide.isRepeatedByTyping
        val tmpCardSide = frontSide
        frontSide = reverseSide
        reverseSide = tmpCardSide
        frontSide.setLearnedTimeStamp(learnedTimestamp)
        frontSide.longTermBatchNumber = longTermBatchNumber
        frontSide.orientation = orientation
        frontSide.isLearned = learned
        frontSide.setRepeatByTyping(repeatedByTyping)
    }

    /**
     * Setzt die Karte zurück.
     */
    fun reset() {
        frontSide.reset()
        reverseSide.reset()
    }

    /**
     * the elements of a card
     */
    enum class Element {
        /**
         * the front side
         */
        FRONT_SIDE,
        /**
         * the reverse side
         */
        REVERSE_SIDE,
        /**
         * the batchnumber
         */
        BATCH_NUMBER,
        /**
         * the date when the card was learned
         */
        LEARNED_DATE,
        /**
         * the date when the card expires
         */
        EXPIRED_DATE,
        /**
         * the way a card has to be repeated
         */
        REPEATING_MODE
    }

    private inner class UID  //    /**
//     * returns the average font size of this card
//     * (averages front and reverse side)
//     * @return the average font size of this card
//     * (averages front and reverse side)
//     */
//    public float getAverageFontSize() {
//        return ((float) (frontSide.getFontSize() +
//                reverseSide.getFontSize()) / 2);
//    }
}