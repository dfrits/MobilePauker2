package de.daniel.mobilepauker2.lesson

import de.daniel.mobilepauker2.lesson.batch.Batch
import de.daniel.mobilepauker2.lesson.batch.LongTermBatch
import de.daniel.mobilepauker2.lesson.batch.SummaryBatch
import de.daniel.mobilepauker2.lesson.card.Card
import de.daniel.mobilepauker2.utils.Log
import java.util.*

class Lesson {
    var description: String = ""
    val longTermBatches = mutableListOf<LongTermBatch>()
    val unlearnedBatch = Batch(mutableListOf())
    val shortTermBatch = mutableListOf<Card>()
    val ultraShortTermBatch = mutableListOf<Card>()
    val summaryBatch = SummaryBatch(this)
    val ultraShortTermList = mutableListOf<Card>()
    val shortTermList = mutableListOf<Card>()

    fun getLongTermBatchesSize(): Int = longTermBatches.size

    fun addLongTermBatch(): LongTermBatch {
        val newBatch = LongTermBatch(longTermBatches.size)
        longTermBatches.add(newBatch)
        return newBatch
    }

    fun getLongTermBatchFromIndex(index: Int): Batch = longTermBatches[index]

    fun refreshExpiredCards() {
        longTermBatches.forEach { it.refreshExpiredCards() }
    }

    fun addNewCard(card: Card) {
        summaryBatch.addCard(card)
        unlearnedBatch.addCard(card)
    }

    fun getCards(): List<Card> {
        val cards: MutableList<Card> = ArrayList()
        val unlearnedCards: List<Card> = unlearnedBatch.cards

        cards.addAll(unlearnedCards)

        for (longTermBatch in longTermBatches) {
            cards.addAll(longTermBatch.cards)
        }

        return cards
    }

    fun getNumberOfExpiredCards(): Int {
        var numberOfExpiredCards = 0
        for (longTermBatch in longTermBatches) {
            numberOfExpiredCards += longTermBatch.getNumberOfExpiredCards()
        }
        return numberOfExpiredCards
    }

    fun getExpiredCards(): Collection<Card> {
        val expiredCards: MutableCollection<Card> = ArrayList()
        for (longTermBatch in longTermBatches) {
            expiredCards.addAll(longTermBatch.getExpiredCards())
        }
        return expiredCards
    }

    fun getLearnedCards(): Collection<Card> {
        val learnedCards: MutableCollection<Card> = ArrayList()
        for (longTermBatch in longTermBatches) {
            learnedCards.addAll(longTermBatch.getLearnedCards())
        }
        return learnedCards
    }

    fun refreshExpiration() {
        Log.d("Lesson::refresExpiration", "entry")
        for (longTermBatch in longTermBatches) {
            Log.d("Lesson::refresExpiration", "loop")
            longTermBatch.refreshExpiration()
        }
    }

    fun resetLongTermBatches() {
        for (longTermBatch in longTermBatches) {
            for (card in longTermBatch.cards) {
                card.isLearned = false
                unlearnedBatch.addCard(card)
            }
        }
        longTermBatches.clear()
    }

    fun resetShortTermBatches() {
        for (card in ultraShortTermList) {
            unlearnedBatch.addCard(card)
        }

        for (card in shortTermList) {
            unlearnedBatch.addCard(card)
        }

        ultraShortTermList.clear()
        shortTermList.clear()
    }

    fun merge(otherLesson: Lesson) {
        // merge unlearned cards
        val otherUnlearnedCards: List<Card> = otherLesson.unlearnedBatch.cards
        unlearnedBatch.addCards(otherUnlearnedCards)
        summaryBatch.addCards(otherUnlearnedCards)

        // merge learned cards in the long term batches
        val otherNumberOfLongTermBatches: Int = otherLesson.getLongTermBatchesSize()

        for (i in 0 until otherNumberOfLongTermBatches) {
            if (longTermBatches.size < i + 1) {
                addLongTermBatch()
            }
            val batch: Batch = getLongTermBatchFromIndex(i)
            val otherBatch: Batch = otherLesson.getLongTermBatchFromIndex(i)
            val cards: List<Card> = otherBatch.cards

            batch.addCards(cards)
            summaryBatch.addCards(otherUnlearnedCards)
        }
    }

    fun flipAllCardSides() {
        for (card in getCards()) {
            //Daten zwischenspeichern, da immer die Vorderseite abgefragt wird
            val isLearned: Boolean = card.isLearned
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
            card.flipSides()

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
        removeEmptyLongTermBatches()
        refreshExpiration()
    }

    fun getNextExpirationTime(): Long {
        var noExpiredCards = true
        for (longTermBatch in longTermBatches) {
            if (longTermBatch.getNumberOfExpiredCards() != 0) {
                noExpiredCards = false
                break
            }
        }
        var nextExpirationDate = Long.MAX_VALUE
        if (noExpiredCards) {
            // look at all learned cards and determine next expiation date
            val cards: Collection<Card> = getCards()
            for (card in cards) {
                if (card.isLearned) {
                    val cardExpirationTime = card.getExpirationTime()
                    if (cardExpirationTime < nextExpirationDate) {
                        nextExpirationDate = cardExpirationTime
                    }
                }
            }
        }
        return nextExpirationDate
    }

    fun forgetCards(batch: Batch, indices: IntArray) {
        if (batch === summaryBatch) {
            if (indices.size == batch.getNumberOfCards()) {
                // special handling when all cards are selected
                for (longTermBatch in longTermBatches) {
                    for (i in longTermBatch.getNumberOfCards() - 1 downTo 0) {
                        val card = longTermBatch.removeCard(i)
                        unlearnedBatch.addCard(card)
                        card.resetCard()
                    }
                }
            } else {
                // remove cards from the "real" batches
                for (i in indices.indices.reversed()) {
                    val card: Card = batch.getCardFromIndex(indices[i])
                    if (card.isLearned) {
                        val longTermBatchNumber: Int = card.longTermBatchNumber
                        // must move the card
                        val longTermBatch = longTermBatches[longTermBatchNumber]
                        longTermBatch.removeCard(card)
                        unlearnedBatch.addCard(card)
                        card.resetCard()
                    }
                }
            }
        } else {
            // long term batches
            for (i in indices.indices.reversed()) {
                val card = batch.removeCard(indices[i])
                unlearnedBatch.addCard(card)
                card.resetCard()
            }
        }

        // cleanup
        removeEmptyLongTermBatches()
    }

    fun instantRepeatCards(batch: Batch, indices: IntArray) {
        // ensure that the first long term batch exists
        if (longTermBatches.isEmpty()) {
            longTermBatches.add(LongTermBatch(0))
        }
        val firstLongTermBatch = longTermBatches[0]
        if (batch === summaryBatch) {
            if (indices.size == batch.getNumberOfCards()) {
                // special handling when all cards are selected

                // handle all unlearned cards
                for (i in unlearnedBatch.getNumberOfCards() - 1 downTo 0) {
                    val card = unlearnedBatch.removeCard(i)
                    firstLongTermBatch.addCard(card)
                    card.expire()
                }

                // handle all long term batches
                for (longTermBatch in longTermBatches) {
                    for (i in longTermBatch.getNumberOfCards() - 1 downTo 0) {
                        val card = longTermBatch.removeCard(i)
                        firstLongTermBatch.addCard(card)
                        card.expire()
                    }
                }
            } else {

                // search every card in the "real" batches
                for (i in indices.indices.reversed()) {
                    val card: Card = batch.getCardFromIndex(indices[i])
                    if (card.isLearned) {
                        val longTermBatchNumber: Int = card.longTermBatchNumber
                        if (longTermBatchNumber != 0) {
                            // must move the card
                            val longTermBatch = longTermBatches[longTermBatchNumber]
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
        } else if (batch === unlearnedBatch) {
            // just move all selected cards
            for (i in indices.indices.reversed()) {
                val card = batch.removeCard(indices[i])
                firstLongTermBatch.addCard(card)
                card.expire()
            }
        } else {
            // the batch is a long term batch
            // move cards only if we are not already in the first longterm batch
            if (batch === firstLongTermBatch) {
                for (i in indices.indices.reversed()) {
                    val card: Card = batch.getCardFromIndex(indices[i])
                    card.expire()
                }
            } else {
                for (i in indices.indices.reversed()) {
                    val card = batch.removeCard(indices[i])
                    firstLongTermBatch.addCard(card)
                    card.expire()
                }
            }
        }

        // cleanup
        removeEmptyLongTermBatches()
        refreshExpiration()
    }

    private fun removeEmptyLongTermBatches() {
        for (i in longTermBatches.indices.reversed()) {
            val longTermBatch = longTermBatches[i]
            if (longTermBatch.getNumberOfCards() == 0) {
                longTermBatches.removeAt(i)
            } else {
                return
            }
        }
    }
}