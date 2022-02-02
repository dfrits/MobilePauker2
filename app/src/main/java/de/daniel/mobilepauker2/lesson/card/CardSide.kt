package de.daniel.mobilepauker2.lesson.card

import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.utils.Constants
import java.util.*

class CardSide private constructor(var text: String) : Comparable<CardSide> {
    constructor() : this("")

    var font: Font? = null
    var orientation: ComponentOrientation = ComponentOrientation(Constants.STANDARD_ORIENTATION)
    var isRepeatedByTyping = false
        private set
    var isLearned = false
    var longTermBatchNumber = 0
    var learnedTimestamp: Long = 0
        private set

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
        // no difference...
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val otherCardSide = other as CardSide
        return ((if (text.isEmpty()) otherCardSide.text.isEmpty() else text == otherCardSide.text)
                && isRepeatedByTyping == otherCardSide.isRepeatedByTyping
                && isLearned == otherCardSide.isLearned
                && longTermBatchNumber == otherCardSide.longTermBatchNumber
                && learnedTimestamp == otherCardSide.learnedTimestamp
                )
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + if (text.isNotEmpty()) text.hashCode() else 0
        hash = 41 * hash + if (isRepeatedByTyping) 1 else 0
        hash = 41 * hash + longTermBatchNumber
        hash = 41 * hash + (learnedTimestamp xor (learnedTimestamp ushr 32)).toInt()
        return hash
    }

    fun setLearnedTimeStamp(learnedTimestamp: Long) {
        this.learnedTimestamp = learnedTimestamp
    }

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
    /*fun search(
        card: Card?, cardSide: Card.Element?,
        pattern: String?, matchCase: Boolean
    ): List<SearchHit> {
        searchHits.clear()
        if (pattern == null) {
            return searchHits
        }
        var searchText = text
        var searchPattern = pattern
        if (!matchCase) {
            searchText = text?.toLowerCase(Locale.ROOT)
            searchPattern = pattern.toLowerCase(Locale.ROOT)
        }
        var index = searchText?.indexOf(searchPattern)
        while (index != -1) {
            searchHits.add(SearchHit(card, cardSide, index))
            index = searchText?.indexOf(searchPattern, index + 1)
        }
        return searchHits
    }*/

    /**
     * returns a List of search match indices
     * @return a List of search match indices
     */
    /*fun getSearchHits(): List<SearchHit> {
        return searchHits
    }*/

    /*fun cancelSearch() {
        searchHits.clear()
    }*/

    fun reset() {
        isLearned = false
        learnedTimestamp = 0
        longTermBatchNumber = 0
    }
}