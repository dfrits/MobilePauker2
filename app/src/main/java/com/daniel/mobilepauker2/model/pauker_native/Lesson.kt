package com.daniel.mobilepauker2.model.pauker_native

import com.daniel.mobilepauker2.utils.Log
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/*
 * Lesson.java
 *
 * Created on 5. Juni 2001, 22:14
 */ /**
 * A Lesson consists of cards and the status information for each card.
 * @author Ronny.Standtke@gmx.net
 */
class Lesson {
    /**
     * returns the lesson description
     * @return the lesson description
     */
    /**
     * sets the description of this lesson
     * @param description Describes the lesson. The author should include what
     * the lesson is all about, when it was created and information abou how to
     * reach him/her.
     */
    var description: String? = ""
    /**
     * returns the summary batch of this lesson
     * @return the summary batch of this lesson
     */
    // all card batches
    val summaryBatch: SummaryBatch
    /**
     * returns the batch with the unlearned cards
     * @return the batch with the unlearned cards
     */
    val unlearnedBatch: Batch
    /**
     * returns the list of cards that are in the ultra-short-term memory
     * @return the list of cards that are in the ultra-short-term memory
     */
    val ultraShortTermList: MutableList<Card>
    /**
     * returns the list of cards that are in the short-term memory
     * @return the list of cards that are in the short-term memory
     */
    val shortTermList: MutableList<Card>
    internal val longTermBatches: MutableList<LongTermBatch>
    private val random: Random? = null

    /**
     * adds a new card to this lesson
     * @param card the new card
     */
    fun addCard(card: Card) {
        summaryBatch.addCard(card)
        unlearnedBatch.addCard(card)
    }

    /**
     * adds a new batch to this lesson
     * @return the new batch
     */
    fun addLongTermBatch(): LongTermBatch {
        val newLongTermBatch = LongTermBatch(longTermBatches.size)
        longTermBatches.add(newLongTermBatch)
        return newLongTermBatch
    }

    /**
     * returns a longterm batch at a given index
     * @param index the index
     * @return the batch
     */
    fun getLongTermBatch(index: Int): LongTermBatch {
        return longTermBatches[index]
    }

    /**
     * returns the number of longterm batches within this lesson
     * @return the number of longterm batches within this lesson
     */
    val numberOfLongTermBatches: Int
        get() = longTermBatches.size

    /**
     * returns the list of all long-term batches of this lesson
     * @return the list of all long-term batches of this lesson
     */
    fun getLongTermBatches(): List<LongTermBatch> {
        return longTermBatches
    }

    /**
     * returns the total number of cards of this lesson
     * @return the total number of cards of this lesson
     */
    val numberOfCards: Int
        get() {
            var numberOfCards = unlearnedBatch.numberOfCards
            numberOfCards += ultraShortTermList.size
            numberOfCards += shortTermList.size
            for (longTermBatch in longTermBatches) {
                numberOfCards += longTermBatch.numberOfCards
            }
            return numberOfCards
        }

    /**
     * returns a <CODE>List</CODE> of all cards of this lesson
     * @return a <CODE>List</CODE> of all cards of this lesson
     */
    val cards: MutableList<Card>
        get() {
            val cards: MutableList<Card> = ArrayList()
            val unlearnedCards = unlearnedBatch.cards
            if (unlearnedCards != null) {
                cards.addAll(unlearnedCards)
            }
            for (longTermBatch in longTermBatches) {
                val longTermBatchCards = longTermBatch.cards?.toList()
                longTermBatchCards?.let { cards.addAll(longTermBatchCards) }
            }
            return cards
        }

    /**
     * returns the total number of expired cards of this lesson
     * @return the total number of expired cards of this lesson
     */
    val numberOfExpiredCards: Int
        get() {
            var numberOfExpiredCards = 0
            for (longTermBatch in longTermBatches) {
                numberOfExpiredCards += longTermBatch.numberOfExpiredCards
            }
            return numberOfExpiredCards
        }

    /**
     * returns a collection of all expired cards
     * @return a collection of all expired cards
     */
    val expiredCards: Collection<Card>
        get() {
            val expiredCards: MutableCollection<Card> = ArrayList()
            for (longTermBatch in longTermBatches) {
                expiredCards.addAll(longTermBatch.expiredCards)
            }
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
            for (longTermBatch in longTermBatches) {
                learnedCards.addAll(longTermBatch.learnedCards)
            }
            return learnedCards
        }

    /**
     * runs through the whole lesson end collects all expired cards in an
     * internal batch
     */
    fun refreshExpiration() {
        Log.d("Lesson::refresExpiration", "entry")
        for (longTermBatch in longTermBatches) {
            Log.d("Lesson::refresExpiration", "loop")
            longTermBatch.refreshExpiration()
        }
    }

    /**
     * removes empty longterm batches at the end of this lesson
     */
    private fun trim() {
        for (i in longTermBatches.indices.reversed()) {
            val longTermBatch = longTermBatches[i]
            if (longTermBatch.numberOfCards == 0) {
                longTermBatches.removeAt(i)
            } else {
                return
            }
        }
    }

    /**
     * moves all cards of longterm batches back to the unlearned batch
     */
    fun reset() {
        for (longTermBatch in longTermBatches) {
            longTermBatch.cards?.let {
                for (card in it) {
                    card.isLearned = false
                    unlearnedBatch.addCard(card)
                }
            }
        }
        longTermBatches.clear()
    }

    /**
     * merges this lesson with the lesson <CODE>otherLesson</CODE>
     * @param otherLesson the lesson to be merged with this lesson
     */
    fun merge(otherLesson: Lesson) { // merge unlearned cards
        otherLesson.unlearnedBatch.cards?.let { otherUnlearnedCards ->
            unlearnedBatch.addCards(otherUnlearnedCards)
            summaryBatch.addCards(otherUnlearnedCards)
            // merge learned cards in the long term batches
            val otherNumberOfLongTermBatches = otherLesson.numberOfLongTermBatches
            LOGGER.log(
                Level.FINE,
                "the lesson to be merged contains {0} longterm batches",
                otherNumberOfLongTermBatches
            )
            for (i in 0 until otherNumberOfLongTermBatches) {
                if (longTermBatches.size < i + 1) {
                    addLongTermBatch()
                }
                val batch: Batch = getLongTermBatch(i)
                val otherBatch: Batch =
                    otherLesson.getLongTermBatch(i)
                otherBatch.cards?.let { cards ->
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "batch {0} contains {1} cards",
                            arrayOf<Any>(i, cards.size)
                        )
                    }
                    batch.addCards(cards.toList())
                }
                summaryBatch.addCards(otherUnlearnedCards)
            }
        }
    }

    /**
     * flips all cards and moves them to their new batches
     */
    fun flip() {
        for (card in cards) { //Daten zwischenspeichern, da immer die Vorderseite abgefragt wird
            val isLearned = card.isLearned
            var batchNumber = -1
            var longTermBatch: LongTermBatch? = null
            // remove card
            if (isLearned) {
                batchNumber = card.longTermBatchNumber
                longTermBatch = longTermBatches[batchNumber]
                longTermBatch.removeCard(card)
            } else {
                unlearnedBatch.removeCard(card)
            }
            // flip card
            card.flip()
            // re-add card
            if (isLearned) {
                for (size in longTermBatches.size until batchNumber + 1) {
                    longTermBatches.add(LongTermBatch(size))
                }
                longTermBatch!!.addCard(card)
            } else {
                unlearnedBatch.addCard(card)
            }
        }
        // clean up
        trim()
        refreshExpiration()
    }// look at all learned cards and determine next expiation date

    /**
     * returns the next expiration time
     * @return the next expiration time
     */
    val nextExpirationTime: Long
        get() {
            var noExpiredCards = true
            for (longTermBatch in longTermBatches) {
                if (longTermBatch.numberOfExpiredCards != 0) {
                    noExpiredCards = false
                    break
                }
            }
            var nextExpirationDate = Long.MAX_VALUE
            if (noExpiredCards) { // look at all learned cards and determine next expiation date
                val cards: Collection<Card> =
                    cards
                for (card in cards) {
                    if (card.isLearned) {
                        val cardExpirationTime = card.expirationTime
                        if (cardExpirationTime < nextExpirationDate) {
                            nextExpirationDate = cardExpirationTime
                        }
                    }
                }
            }
            return nextExpirationDate
        }

    /**
     * moves cards back to the unlearned batch
     * @param batch   the active/choosen batch
     * @param indices the indices of the choosen cards
     */
    fun forgetCards(batch: Batch, indices: IntArray) {
        if (batch === summaryBatch) {
            if (indices.size == batch.numberOfCards) { // special handling when all cards are selected
                for (longTermBatch in longTermBatches) {
                    for (i in longTermBatch.numberOfCards - 1 downTo 0) {
                        longTermBatch.removeCard(i)?.let { card ->
                            unlearnedBatch.addCard(card)
                            card.reset()
                        }
                    }
                }
            } else { // remove cards from the "real" batches
                for (i in indices.indices.reversed()) {
                    val card =
                        batch.getCard(indices[i])
                    if (card!!.isLearned) {
                        val longTermBatchNumber = card.longTermBatchNumber
                        // must move the card
                        val longTermBatch = longTermBatches[longTermBatchNumber]
                        longTermBatch.removeCard(card)
                        unlearnedBatch.addCard(card)
                        card.reset()
                    }
                }
            }
        } else { // long term batches
            for (i in indices.indices.reversed()) {
                batch.removeCard(indices[i])?.let { card ->
                    unlearnedBatch.addCard(card)
                    card.reset()
                }
            }
        }
        // cleanup
        trim()
    }

    /**
     * moves cards back to the first long term batch and sets them expired
     * @param batch   the active/choosen batch
     * @param indices the indices of the choosen cards
     */
    fun instantRepeatCards(
        batch: Batch?,
        indices: IntArray
    ) { // ensure that the first long term batch exists
        if (longTermBatches.isEmpty()) {
            longTermBatches.add(LongTermBatch(0))
        }
        val firstLongTermBatch = longTermBatches[0]
        if (batch === summaryBatch) {
            if (indices.size == batch.numberOfCards) {
                // special handling when all cards are selected
                // handle all unlearned cards
                for (i in unlearnedBatch.numberOfCards - 1 downTo 0) {
                    unlearnedBatch.removeCard(i)?.let { card ->
                        firstLongTermBatch.addCard(card)
                        card.expire()
                    }
                }
                // handle all long term batches
                for (longTermBatch in longTermBatches) {
                    for (i in longTermBatch.numberOfCards - 1 downTo 0) {
                        longTermBatch.removeCard(i)?.let { card ->
                            firstLongTermBatch.addCard(card)
                            card.expire()
                        }
                    }
                }
            } else { // search every card in the "real" batches
                for (i in indices.indices.reversed()) {
                    val card =
                        batch.getCard(indices[i])
                    if (card!!.isLearned) {
                        val longTermBatchNumber = card.longTermBatchNumber
                        if (longTermBatchNumber != 0) { // must move the card
                            val longTermBatch =
                                longTermBatches[longTermBatchNumber]
                            longTermBatch.removeCard(card)
                            firstLongTermBatch.addCard(card)
                        }
                    } else {
                        unlearnedBatch.removeCard(card)
                        firstLongTermBatch.addCard(card)
                    }
                    card.expire()
                }
            }
        } else if (batch === unlearnedBatch) { // just move all selected cards
            for (i in indices.indices.reversed()) {
                batch.removeCard(indices[i])?.let { card ->
                    firstLongTermBatch.addCard(card)
                    card.expire()
                }
            }
        } else { // the batch is a long term batch
// move cards only if we are not already in the first longterm batch
            if (batch === firstLongTermBatch) {
                for (i in indices.indices.reversed()) {
                    val card = batch.getCard(indices[i])
                    card!!.expire()
                }
            } else {
                for (i in indices.indices.reversed()) {
                    batch?.removeCard(indices[i])?.let { card ->
                        firstLongTermBatch.addCard(card)
                        card.expire()
                    }
                }
            }
        }
        // cleanup
        trim()
        refreshExpiration()
    }

    companion object {
        private val LOGGER =
            Logger.getLogger(Lesson::class.java.name)
    }

    /**
     * Creates new Lesson
     */
    init {
        unlearnedBatch = Batch(null)
        ultraShortTermList =
            ArrayList()
        shortTermList = ArrayList()
        longTermBatches = ArrayList()
        // !!! create summaryBatch at the end, because it uses the reference to
// this lesson and expects it to be completely initialized !!!
        summaryBatch = SummaryBatch(this)
    }
}