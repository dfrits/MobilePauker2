package de.daniel.mobilepauker2.lesson.batch

import de.daniel.mobilepauker2.lesson.card.Card
import de.daniel.mobilepauker2.utils.Log
import java.util.*
import kotlin.math.pow

class LongTermBatch(private val batchNumber: Int) : Batch(listOf()) {
    companion object {
        private const val ONE_SECOND: Long = 1000
        private const val ONE_MINUTE = ONE_SECOND * 60
        private const val ONE_HOUR = ONE_MINUTE * 60
        private const val ONE_DAY = ONE_HOUR * 24
        const val EXPIRATION_UNIT = ONE_DAY
    }

    private val expiredCards = mutableListOf<Card>()
    private val expirationTime: Double = EXPIRATION_UNIT * Math.E.pow(batchNumber)

    override fun addCard(card: Card) {
        card.longTermBatchNumber = batchNumber
        card.setExpirationTime(expirationTime.toLong())
        cards.add(card)
    }

    fun getNumberOfExpiredCards(): Int {
        refreshExpiredCards()
        return expiredCards.size
    }

    fun getExpiredCards(): Collection<Card> {
        refreshExpiredCards()
        return expiredCards
    }

    fun getLearnedCards(): Collection<Card> {
        val learnedCards: MutableCollection<Card> = ArrayList()
        val currentTime = System.currentTimeMillis()
        for (card in cards) {
            val learnedTime: Long = card.learnedTimestamp
            val diff = currentTime - learnedTime
            if (diff < expirationTime) {
                learnedCards.add(card)
            }
        }
        return learnedCards
    }

    fun refreshExpiredCards() {
        val currentTime = System.currentTimeMillis()

        expiredCards.clear()

        cards.forEach { card ->
            val learnedTime = card.learnedTimestamp
            val diff = currentTime - learnedTime
            if (diff > expirationTime) {
                expiredCards.add(card)
            }
        }
    }

    fun refreshExpiration() {
        expiredCards.clear()
        val currentTime = System.currentTimeMillis()
        for (card in cards) {
            val learnedTime: Long = card.learnedTimestamp
            val diff = currentTime - learnedTime
            val frontSide: String = card.frontSide.text
            Log.d(
                "LongTermBatch::refreshExpiration",
                "currentTime = $currentTime,cardTime=$learnedTime,diff=$diff," +
                    "expirationTime$expirationTime,$frontSide"
            )
            if (diff > expirationTime) {
                expiredCards.add(card)
            }
        }
    }
}