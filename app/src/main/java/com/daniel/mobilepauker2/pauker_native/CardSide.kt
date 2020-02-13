package com.daniel.mobilepauker2.pauker_native

import com.daniel.mobilepauker2.core.Constants
import java.util.*

class CardSide private constructor(var text: String) : Comparable<CardSide> {
    // style
    var font: Font? = null
    // learning
    var isRepeatedByTyping = false
    var isLearned = false
    var longTermBatchNumber = 0
    var learnedTimestamp: Long = 0
    var orientation: ComponentOrientation? =
        null
        get() = if (field == null) ComponentOrientation(
            Constants.STANDARD_ORIENTATION
        ) else field
    private val searchHits: MutableList<SearchHit>

    constructor() : this("")

    override fun compareTo(other: CardSide): Int {
        val textResult = text.compareTo(other.text)
        if (textResult != 0) {
            return textResult
        }
        val otherCardByTyping = other.isRepeatedByTyping
        if (isRepeatedByTyping && !otherCardByTyping) {
            return -1
        } else if (!isRepeatedByTyping && otherCardByTyping) {
            return 1
        }
        val otherCardLearned = other.isLearned
        if (isLearned && !otherCardLearned) {
            return 1
        } else if (!isLearned && otherCardLearned) {
            return -1
        }
        val otherLongTermBatchNumber = other.longTermBatchNumber
        if (longTermBatchNumber < otherLongTermBatchNumber) {
            return -1
        } else if (longTermBatchNumber > otherLongTermBatchNumber) {
            return 1
        }
        val otherLearnedTimestamp = other.learnedTimestamp
        if (learnedTimestamp < otherLearnedTimestamp) {
            return -1
        } else if (learnedTimestamp > otherLearnedTimestamp) {
            return 1
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val otherSide = other as CardSide
        return ((text == otherSide.text)
                && isRepeatedByTyping == otherSide.isRepeatedByTyping
                && isLearned == otherSide.isLearned
                && longTermBatchNumber == otherSide.longTermBatchNumber
                && learnedTimestamp == otherSide.learnedTimestamp)
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + (text.hashCode())
        hash = 41 * hash + if (isRepeatedByTyping) 1 else 0
        hash = 41 * hash + longTermBatchNumber
        hash = 41 * hash + (learnedTimestamp xor (learnedTimestamp ushr 32)).toInt()
        return hash
    }

    /**
     * sets the timestamp when the cardside was learned
     * @param learnedTimestamp the timestamp when the cardside was learned
     */
    fun setLearnedTimeStamp(learnedTimestamp: Long) {
        this.learnedTimestamp = learnedTimestamp
    }

    /**
     * sets if the cardside should be repeated by typing instead of memorizing
     * @param repeatByTyping <CODE>true</CODE>, if the cardside should be repeated by typing instead
     * of memorizing, <CODE>false</CODE> otherwise
     */
    fun setRepeatByTyping(repeatByTyping: Boolean) {
        isRepeatedByTyping = repeatByTyping
    }

    /**
     * searches for a string pattern at the card side
     * @param card      the card of this card side
     * @param cardSide  the side of this card side
     * @param pattern   the search pattern
     * @param matchCase if we must match the case
     * @return a list with search match indices
     */
    fun search(
        card: Card,
        cardSide: Card.Element,
        pattern: String?,
        matchCase: Boolean
    ): List<SearchHit> {
        searchHits.clear()
        if (pattern == null) {
            return searchHits
        }
        var searchText = text
        var searchPattern = pattern
        if (!matchCase) {
            searchText = text.toLowerCase(Locale.getDefault())
            searchPattern = pattern.toLowerCase(Locale.getDefault())
        }
        var index = searchText.indexOf(searchPattern)
        while (index != -1) {
            searchHits.add(SearchHit(card, cardSide, index))
            index = searchText.indexOf(searchPattern, index + 1)
        }
        return searchHits
    }

    @Suppress("unused")
    fun getSearchHits(): List<SearchHit> {
        return searchHits
    }

    fun cancelSearch() {
        searchHits.clear()
    }

    fun reset() {
        isLearned = false
        learnedTimestamp = 0
        longTermBatchNumber = 0
    }

    /**
     * creates a new CardSide
     * @param text the card side text
     */
    init {
        searchHits = LinkedList()
    }
}