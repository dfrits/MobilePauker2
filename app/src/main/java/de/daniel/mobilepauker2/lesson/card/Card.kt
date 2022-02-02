package de.daniel.mobilepauker2.lesson.card

import java.util.*

open class Card(var frontSide: CardSide, var reverseSide: CardSide) : Comparable<Card> {

    var id: String? = null
        get() {
            if (field == null) {
                field = UID().toString()
            }
            return field
        }
        private set
    val learnedTimestamp: Long
        get() = frontSide.learnedTimestamp
    var isLearned: Boolean
        get() = frontSide.isLearned
        set(learned) {
            frontSide.isLearned = learned
            if (learned) {
                frontSide.setLearnedTimeStamp(System.currentTimeMillis())
            }
        }
    var longTermBatchNumber: Int
        get() = frontSide.longTermBatchNumber
        set(batchNumber) {
            if (batchNumber > -1) {
                frontSide.longTermBatchNumber = batchNumber
            }
        }
    val isRepeatedByTyping: Boolean
        get() = frontSide.isRepeatedByTyping
    private var expirationTime: Long = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val otherCard: Card = other as Card
        return (frontSide == otherCard.frontSide
                && reverseSide == otherCard.reverseSide
                && expirationTime == otherCard.expirationTime)
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 47 * hash + frontSide.hashCode()
        hash = 47 * hash + reverseSide.hashCode()
        hash = 47 * hash + (expirationTime xor (expirationTime ushr 32)).toInt()
        return hash
    }

    override fun compareTo(other: Card): Int {
        val frontSideResult: Int = frontSide.compareTo(other.frontSide)
        if (frontSideResult != 0) {
            return frontSideResult
        }
        val reverseSideResult: Int = reverseSide.compareTo(other.reverseSide)
        if (reverseSideResult != 0) {
            return reverseSideResult
        }
        // both sides are equal, base decision on expiration time
        val otherExpirationTime: Long = other.getExpirationTime()
        if (expirationTime < otherExpirationTime) {
            return -1
        } else if (expirationTime > otherExpirationTime) {
            return 1
        }
        // cards are equal
        return 0
    }

    fun setLearnedTimeStamp(learnedTimeStamp: Long) {
        frontSide.setLearnedTimeStamp(learnedTimeStamp)
    }

    fun updateLearnedTimeStamp() {
        frontSide.setLearnedTimeStamp(System.currentTimeMillis())
    }

    fun getExpirationTime(): Long {
        return if (frontSide.isLearned) {
            frontSide.learnedTimestamp + expirationTime
        } else -1
    }

    fun setExpirationTime(expirationTime: Long) {
        this.expirationTime = expirationTime
    }

    fun expireCard() {
        val expiredTime = System.currentTimeMillis() - expirationTime - 60000
        frontSide.isLearned = true
        frontSide.setLearnedTimeStamp(expiredTime)
    }

    fun setRepeatByTyping(repeatByTyping: Boolean) {
        frontSide.setRepeatByTyping(repeatByTyping)
    }

    open fun flipSides() {
        val learnedTimestamp: Long = frontSide.learnedTimestamp
        val longTermBatchNumber: Int = frontSide.longTermBatchNumber
        val orientation: ComponentOrientation = frontSide.orientation
        val learned: Boolean = frontSide.isLearned
        val repeatedByTyping: Boolean = frontSide.isRepeatedByTyping
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
     * Sets a search pattern onto this card.
     * @param pattern   the search pattern
     * @param matchCase if the case of the pattern must be matched
     * @param cardSide  the card side where to look for the pattern can be one of: Pauker.FRONT_SIDE
     * Pauker.REVERSE_SIDE Pauker.BOTH_SIDES
     * @return a list with search match indices
     */
    /*fun search(cardSide: Element, pattern: String?, matchCase: Boolean): List<SearchHit?> {
        val searchHits: LinkedList<SearchHit?> = LinkedList<SearchHit?>()
        if (cardSide == Element.FRONT_SIDE) {
            searchHits.addAll(
                frontSide.search(
                    this, Element.FRONT_SIDE, pattern, matchCase
                )
            )
            reverseSide.cancelSearch()
        } else if (cardSide == Element.REVERSE_SIDE) {
            frontSide.cancelSearch()
            searchHits.addAll(
                reverseSide.search(
                    this, Element.REVERSE_SIDE, pattern, matchCase
                )
            )
        } else { // BOTH_SIDES
            searchHits.addAll(
                frontSide.search(
                    this, Element.FRONT_SIDE, pattern, matchCase
                )
            )
            searchHits.addAll(
                reverseSide.search(
                    this, Element.REVERSE_SIDE, pattern, matchCase
                )
            )
        }
        return searchHits
    }*/

    fun resetCard() {
        frontSide.reset()
        reverseSide.reset()
    }

    open fun expire() {
        val expiredTime = System.currentTimeMillis() - expirationTime - 60000
        frontSide.isLearned = true
        frontSide.setLearnedTimeStamp(expiredTime)
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

    private inner class UID
}