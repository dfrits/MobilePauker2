package com.daniel.mobilepauker2.model.pauker_native

import java.text.Collator
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * a batch is part of a lesson
 * @author Ronny.Standtke@gmx.net
 */
open class Batch internal constructor(cards: MutableList<Card>?) {
    /**
     * the list of all cards in this batch
     */
    var cards: MutableList<Card>? = null

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

    // support for search result caching
    private var searchPattern: String? = null
    private var searchSide: Card.Element? = null
    private var matchCase = false
    private val searchHits: MutableList<SearchHit>
    private var currentSearchHit = 0
    private var savedSearchHit: SearchHit? = null
    /**
     * returns the number of cards in this batch
     * @return the number of cards in this batch
     */
    val numberOfCards: Int = cards?.size ?: 0

    /**
     * returns the card at the index <CODE>i</CODE>
     * @param index the index of the returned card
     * @return the card at index <CODE>i</CODE>
     */
    fun getCard(index: Int): Card? {
        return cards?.let { it[index] }
    }

    /**
     * adds a card to this batch
     * @param card the new card
     */
    open fun addCard(card: Card) {
        cards?.add(card)
        card.isLearned = false
    }

    /**
     * adds a new card at a certain index
     * @param index the index of the new card
     * @param card  the new card
     */
    fun addCard(index: Int, card: Card) {
        cards?.add(index, card)
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
    fun removeCard(card: Card?): Boolean { // !!! IMPORTANT !!!
        // Do not refresh search stuff here or removing from summary batch
        // will break!
        // ("real" batches dont have the current search info)
        // remove card
        return cards?.remove(card) ?: false
    }

    /**
     * removes a card from the batch
     * @param index the index where the card should be removed
     * @return the removed card
     */
    open fun removeCard(index: Int): Card? { // save current search hit
        savedSearchHit = getCurrentSearchHit()
        // remove card
        val card = cards?.removeAt(index)
        // re-fill searchHits list (all indices are potentially wrong now)
        search(searchPattern, matchCase, searchSide)
        // restore current search hit
        restoreSearchHit()
        return card
    }

    /**
     * determines the index of a special card
     * @param card the card
     * @return the index of the card
     */
    fun indexOf(card: Card?): Int {
        return cards?.indexOf(card) ?: -1
    }

    /**
     * sorts the cards in the batch according to an <CODE>sortIndex</CODE>
     * @param cardElement the card element that must be used for sorting the cards
     * @param ascending   if true the cards are sorted ascending otherwise descending
     */
    fun sortCards(
        cardElement: Card.Element?,
        ascending: Boolean
    ) {
        var comparator: AbstractCardComparator<Card>? = null
        when (cardElement) {
            Card.Element.FRONT_SIDE -> comparator =
                frontSideComparator
            Card.Element.REVERSE_SIDE -> comparator =
                reverseSideComparator
            Card.Element.BATCH_NUMBER -> comparator =
                batchNumberComparator
            Card.Element.LEARNED_DATE -> comparator =
                learnedDateComparator
            Card.Element.EXPIRED_DATE -> comparator =
                expiredDateComparator
            Card.Element.REPEATING_MODE -> comparator =
                repeatingModeComparator
            else -> LOGGER.log(
                Level.WARNING,
                "unknown cardElement {0}", cardElement
            )
        }
        comparator?.setAscending(ascending)
        Collections.sort(cards, comparator)
    }

    /**
     * searches for a given string
     * @param searchPattern the pattern to search for
     * @param matchCase     if true the search is case sensitive
     * @param cardSide      the side to search at
     * @return if true a pattern match was found
     */
    fun search(
        searchPattern: String?,
        matchCase: Boolean,
        cardSide: Card.Element?
    ): Boolean { // store search parameters
        this.searchPattern = searchPattern
        this.matchCase = matchCase
        searchSide = cardSide
        if (refreshSearchHits()) {
            currentSearchHit = 0
            return true
        }
        return false
    }

    /**
     * returns the current search hit
     * @return the current search hit
     */
    private fun getCurrentSearchHit(): SearchHit? {
        return if (currentSearchHit > -1 && currentSearchHit < searchHits.size) {
            searchHits[currentSearchHit]
        } else null
    }

    private fun refreshSearchHits(): Boolean {
        searchHits.clear()
        if (searchPattern == null || searchPattern!!.length == 0) {
            return false
        }
        cards?.let {
            for (card in it) {
                searchHits.addAll(card.search(searchSide, searchPattern, matchCase))
            }
        }
        return searchHits.isNotEmpty()
    }

    private fun restoreSearchHit() { // restore current search hit
        if (savedSearchHit != null) {
            currentSearchHit = searchHits.indexOf(savedSearchHit!!)
        }
    }

    companion object {
        private val LOGGER =
            Logger.getLogger(Batch::class.java.name)
        private val collator = Collator.getInstance()

        private val frontSideComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(
                    card1: Card,
                    card2: Card
                ): Int {
                    val frontSideText1 = card1.frontSide.text
                    val frontSideText2 = card2.frontSide.text
                    return collator.compare(
                        frontSideText1,
                        frontSideText2
                    )
                }
            }

        private val reverseSideComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(
                    card1: Card,
                    card2: Card
                ): Int {
                    val reverseSideText1 = card1.reverseSide.text
                    val reverseSideText2 = card2.reverseSide.text
                    return collator.compare(
                        reverseSideText1,
                        reverseSideText2
                    )
                }
            }

        private val batchNumberComparator: AbstractCardComparator<Card> =
            object : AbstractCardComparator<Card>() {
                public override fun compareCards(
                    card1: Card,
                    card2: Card
                ): Int {
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
                public override fun compareCards(
                    card1: Card,
                    card2: Card
                ): Int {
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
                public override fun compareCards(
                    card1: Card,
                    card2: Card
                ): Int {
                    val expirationTime1 = card1.expirationTime
                    val expirationTime2 = card2.expirationTime
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
                public override fun compareCards(
                    card1: Card,
                    card2: Card
                ): Int {
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

    /**
     * constructs a new Batch with all the cards in <CODE>cards</CODE>
     * @param cards initial batch cards
     */
    init {
        if (cards == null) {
            this.cards = ArrayList()
        } else {
            this.cards = cards
        }
        searchHits = LinkedList()
    }
}