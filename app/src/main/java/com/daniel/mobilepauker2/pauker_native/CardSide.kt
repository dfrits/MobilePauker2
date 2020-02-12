package com.daniel.mobilepauker2.pauker_native

import com.daniel.mobilepauker2.core.Constants
import java.util.*

class CardSide private constructor(text: String) : Comparable<CardSide> {
    /**
     * returns the cardside text
     * @return the cardside text
     */
    /**
     * sets the cardside text
     * @param text the cardside text
     */
    // content
    var text: String?
    /**
     * returns the explicitly set cardside font or <CODE>null</CODE>, if no font
     * was explicitly set for this card side
     * @return the explicitly set cardside font or <CODE>null</CODE>, if no font was explicitly set
     * for this card side
     */
    /**
     * sets the cardside font
     * @param font the cardside font
     */
    // style
    var font: Font? = null
    /**
     * returns the cardside orientation
     * @return the cardside orientation
     */
    /**
     * sets the cardside orientation
     * @param orientation the cardside orientation
     */
    var orientation: ComponentOrientation? =
        null
        get() = if (field == null) ComponentOrientation(
            Constants.STANDARD_ORIENTATION
        ) else field
    /**
     * returns if the cardside should be repeated by typing instead of
     * memorizing
     * @return <CODE>true</CODE>, if the cardside should be repeated by typing instead of
     * memorizing, <CODE>false</CODE> otherwise
     */
    // learning
    var isRepeatedByTyping = false
        private set
    /**
     * indicates if this card side is learned
     * @return <CODE>true</CODE>, if the card side is learned, <CODE>false</CODE> otherwise
     */
    /**
     * sets if the card is learned
     * @param learned <CODE>true</CODE>, if the card is learned, <CODE>false</CODE> otherwise
     */
    var isLearned = false
    /**
     * returns the batch number this card belongs to if this card side would be
     * the frontside
     * @return the batch number this card belongs to if this card side would be the frontside
     */
    /**
     * sets the long term batch number this card belongs to if this card side
     * would be the frontside
     * @param longTermBatchNumber the long term batch number
     */
    var longTermBatchNumber = 0
    /**
     * returns the timestamp when the cardside was learned
     * @return the timestamp when the cardside was learned
     */
    var learnedTimestamp: Long = 0
        private set
    // support for search result caching (speeds up batch rendering)
    private val searchHits: MutableList<SearchHit>

    /**
     * creates a new CardSide
     */
    constructor() : this("") {}

    override fun compareTo(otherCardSide: CardSide): Int {
        val textResult = text!!.compareTo(otherCardSide.text!!)
        if (textResult != 0) {
            return textResult
        }
        val otherCardByTyping = otherCardSide.isRepeatedByTyping
        if (isRepeatedByTyping && !otherCardByTyping) {
            return -1
        } else if (!isRepeatedByTyping && otherCardByTyping) {
            return 1
        }
        val otherCardLearned = otherCardSide.isLearned
        if (isLearned && !otherCardLearned) {
            return 1
        } else if (!isLearned && otherCardLearned) {
            return -1
        }
        val otherLongTermBatchNumber = otherCardSide.longTermBatchNumber
        if (longTermBatchNumber < otherLongTermBatchNumber) {
            return -1
        } else if (longTermBatchNumber > otherLongTermBatchNumber) {
            return 1
        }
        val otherLearnedTimestamp = otherCardSide.learnedTimestamp
        if (learnedTimestamp < otherLearnedTimestamp) {
            return -1
        } else if (learnedTimestamp > otherLearnedTimestamp) {
            return 1
        }
        // no difference...
        return 0
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as CardSide
        return ((if (text == null) other.text == null else text == other.text)
                && isRepeatedByTyping == other.isRepeatedByTyping && isLearned == other.isLearned && longTermBatchNumber == other.longTermBatchNumber && learnedTimestamp == other.learnedTimestamp)
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + if (text != null) text.hashCode() else 0
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
            searchText = text!!.toLowerCase()
            searchPattern = pattern.toLowerCase()
        }
        var index = searchText!!.indexOf(searchPattern)
        while (index != -1) {
            searchHits.add(SearchHit(card, cardSide, index))
            index = searchText.indexOf(searchPattern, index + 1)
        }
        return searchHits
    }

    /**
     * returns a List of search match indices
     * @return a List of search match indices
     */
    fun getSearchHits(): List<SearchHit> {
        return searchHits
    }

    /**
     * cancels the search process
     */
    fun cancelSearch() {
        searchHits.clear()
    }
    //    /**
//     * returns the size of the font that is used for this card side
//     * @return the size of the font that is used for this card side
//     */
//    public int getFontSize() {
////        if (font == null) {
////            return PaukerFrame.DEFAULT_FONT.getSize();
////        }
////        return font.getSize();
//    	return 10;
//    }
//
//    /**
//     * returns the stlye of the font that is used for this card side
//     * @return the stlye of the font that is used for this card side
//     */
//    public int getFontStyle() {
////        if (font == null) {
////            return PaukerFrame.DEFAULT_FONT.getStyle();
////        }
////        return font.getStyle();
//
////      if (font == null) {
////      return PaukerFrame.DEFAULT_FONT.getStyle();
////  }
//
//    	return null;
//    }

    /**
     * Setzt die Kartenseite zurück.
     */
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
        this.text = text
        searchHits = LinkedList()
    }
}