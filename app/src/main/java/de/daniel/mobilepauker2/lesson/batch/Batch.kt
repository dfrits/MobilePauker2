package de.daniel.mobilepauker2.lesson.batch

import de.daniel.mobilepauker2.lesson.card.Card
import java.text.Collator
import java.util.*

/**
 * a batch is part of a lesson
 * @author Ronny.Standtke@gmx.net
 */
open class Batch internal constructor(private val cardList: List<Card>) {
    val cards: MutableList<Card> = mutableListOf()

    // support for search result caching
    private var searchPattern: String? = null
    private var searchSide: Card.Element? = null
    private var matchCase = false

    //private val searchHits: MutableList<SearchHit>
    private var currentSearchHit = 0
    //private var savedSearchHit: SearchHit? = null

    /**
     * adds a card to this batch
     * @param card the new card
     */
    open fun addCard(card: Card) {
        cards.add(card)
        card.isLearned = false
    }

    /**
     * adds a new card at a certain index
     * @param index the index of the new card
     * @param card  the new card
     */
    fun addCard(index: Int, card: Card) {
        cards.add(index, card)
        card.isLearned = false
    }

    /**
     * adds cards to the batch
     * @param cards the new cards
     */
    fun addCards(cards: List<Card>) {
        for (card in cards) {
            addCard(card)
        }
    }

    /**
     * removes a card from the batch
     * @param card the card to be removed
     * @return <tt>true</tt>, if the card could be removed
     */
    fun removeCard(card: Card): Boolean {
        // !!! IMPORTANT !!!
        // Do not refresh search stuff here or removing from summary batch
        // will break!
        // ("real" batches dont have the current search info)

        // remove card
        return cards.remove(card)
    }

    /**
     * removes a card from the batch
     * @param index the index where the card should be removed
     * @return the removed card
     */
    open fun removeCard(index: Int): Card {
        // save current search hit
        //savedSearchHit = getCurrentSearchHit()

        // remove card
        val card = cards.removeAt(index)

        // re-fill searchHits list (all indices are potentially wrong now)
        //search(searchPattern, matchCase, searchSide)

        // restore current search hit
        //restoreSearchHit()
        return card
    }

    /**
     * determines the index of a special card
     * @param card the card
     * @return the index of the card
     */
    fun indexOf(card: Card): Int {
        return cards.indexOf(card)
    }

    /**
     * sorts the cards in the batch according to an <CODE>sortIndex</CODE>
     * @param cardElement the card element that must be used for sorting the cards
     * @param ascending   if true the cards are sorted ascending otherwise descending
     */
    fun sortCards(cardElement: Card.Element?, ascending: Boolean): Boolean {
        var comparator: AbstractCardComparator<Card>? = null
        comparator = when (cardElement) {
            Card.Element.FRONT_SIDE -> frontSideComparator
            Card.Element.REVERSE_SIDE -> reverseSideComparator
            Card.Element.BATCH_NUMBER -> batchNumberComparator
            Card.Element.LEARNED_DATE -> learnedDateComparator
            Card.Element.EXPIRED_DATE -> expiredDateComparator
            Card.Element.REPEATING_MODE -> repeatingModeComparator
            else -> null
        }

        if (comparator != null) {
            comparator.setAscending(ascending)
            Collections.sort(cards, comparator)
            return true
        }

        return false
    }

    fun getNumberOfCards(): Int = cards.size

    fun getCardFromIndex(index: Int): Card = cards[index]

    /**
     * searches for a given string
     * @param searchPattern the pattern to search for
     * @param matchCase     if true the search is case sensitive
     * @param cardSide      the side to search at
     * @return if true a pattern match was found
     */
    /*fun search(searchPattern: String?, matchCase: Boolean, cardSide: Card.Element?): Boolean {
        // store search parameters
        this.searchPattern = searchPattern
        this.matchCase = matchCase
        searchSide = cardSide
        if (refreshSearchHits()) {
            currentSearchHit = 0
            return true
        }
        return false
    }*/

    /**
     * returns the current search hit
     * @return the current search hit
     */
    /*private fun getCurrentSearchHit(): SearchHit? {
        return if (currentSearchHit > -1 && currentSearchHit < searchHits.size) {
            searchHits[currentSearchHit]
        } else null
    }

    private fun refreshSearchHits(): Boolean {
        searchHits.clear()
        if (searchPattern == null || searchPattern!!.length == 0) {
            return false
        }
        for (card in cards) {
            searchHits.addAll(card.search(searchSide, searchPattern, matchCase))
        }
        return !searchHits.isEmpty()
    }

    private fun restoreSearchHit() {
        // restore current search hit
        if (savedSearchHit != null) {
            currentSearchHit = searchHits.indexOf(savedSearchHit)
        }
    }*/

    private abstract class AbstractCardComparator<Card> : Comparator<Card> {
        private var ascending = true
        fun setAscending(ascending: Boolean) {
            this.ascending = ascending
        }

        override fun compare(card1: Card, card2: Card): Int {
            val result = compareCards(card1, card2)
            return if (ascending) result else -result
        }

        protected abstract fun compareCards(card1: Card, card2: Card): Int
    }

    companion object {
        private val collator = Collator.getInstance()
        private val frontSideComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(card1: Card, card2: Card): Int {
                    val frontSideText1 = card1.frontSide.text
                    val frontSideText2 = card2.frontSide.text
                    return collator.compare(frontSideText1, frontSideText2)
                }
            }
        private val reverseSideComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(card1: Card, card2: Card): Int {
                    val reverseSideText1 = card1.reverseSide.text
                    val reverseSideText2 = card2.reverseSide.text
                    return collator.compare(reverseSideText1, reverseSideText2)
                }
            }
        private val batchNumberComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(card1: Card, card2: Card): Int {
                    val batchNumber1 = card1.longTermBatchNumber
                    val batchNumber2 = card2.longTermBatchNumber
                    if (batchNumber1 < batchNumber2) {
                        return -1
                    } else if (batchNumber1 > batchNumber2) {
                        return 1
                    }
                    return 0
                }
            }
        private val learnedDateComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(card1: Card, card2: Card): Int {
                    val learnedTime1 = card1.learnedTimestamp
                    val learnedTime2 = card2.learnedTimestamp
                    if (learnedTime1 < learnedTime2) {
                        return -1
                    } else if (learnedTime1 > learnedTime2) {
                        return 1
                    }
                    return 0
                }
            }
        private val expiredDateComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(card1: Card, card2: Card): Int {
                    val expirationTime1 = card1.getExpirationTime()
                    val expirationTime2 = card2.getExpirationTime()
                    if (expirationTime1 < expirationTime2) {
                        return -1
                    } else if (expirationTime1 > expirationTime2) {
                        return 1
                    }
                    return 0
                }
            }
        private val repeatingModeComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(card1: Card, card2: Card): Int {
                    val typing1 = card1.isRepeatedByTyping
                    val typing2 = card2.isRepeatedByTyping
                    if (!typing1 && typing2) {
                        return -1
                    } else if (typing1 && !typing2) {
                        return 1
                    }
                    return 0
                }
            }
    }

    init {
        //searchHits = LinkedList<SearchHit>()
        cards.addAll(cardList)
    }
}