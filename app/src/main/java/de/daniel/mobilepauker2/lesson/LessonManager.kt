package de.daniel.mobilepauker2.lesson

import android.content.Context
import dagger.Lazy
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.batch.Batch
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.lesson.batch.LongTermBatch
import de.daniel.mobilepauker2.lesson.card.Card
import de.daniel.mobilepauker2.lesson.card.CardPackAdapter
import de.daniel.mobilepauker2.lesson.card.FlashCard
import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.models.LearningPhase.*
import de.daniel.mobilepauker2.models.LearningPhase.Companion.currentPhase
import de.daniel.mobilepauker2.settings.SettingsManager
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.LEARN_NEW_CARDS_RANDOMLY
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.RETURN_FORGOTTEN_CARDS
import de.daniel.mobilepauker2.statistics.BatchStatistics
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonManager @Inject constructor(val context: @JvmSuppressWildcards Context) {
    var lesson: Lesson? = null
        private set
    var currentPack = mutableListOf<FlashCard>()
    var lessonDescription = lesson?.description ?: ""

    @Inject
    lateinit var dataManager: Lazy<DataManager>

    @Inject
    lateinit var settingsManager: SettingsManager

    init {
        (context as PaukerApplication).applicationSingletonComponent.inject(this)
    }

    fun setupNewLesson() {
        dataManager.get().setNewFileName(Constants.DEFAULT_FILE_NAME)
        lesson = Lesson()
    }

    fun isLessonNotNew() =
        dataManager.get().getReadableCurrentFileName() != Constants.DEFAULT_FILE_NAME

    fun addCard(flashCard: FlashCard, sideA: String, sideB: String) {
        flashCard.sideAText = sideA
        flashCard.sideBText = sideB
        lesson?.addNewCard(flashCard)
    }

    fun addCard(sideA: String, sideB: String, index: String, learnStatus: String) {
        val newCard = FlashCard(sideA, sideB, index, learnStatus)
        lesson?.unlearnedBatch?.addCard(newCard)
    }

    fun getBatchStatistics(): List<BatchStatistics> {
        val batchSizes: ArrayList<BatchStatistics> = ArrayList<BatchStatistics>()
        lesson?.let { lesson ->
            val longTermBatches: List<LongTermBatch> = lesson.longTermBatches
            var i = 0
            val size = longTermBatches.size
            while (i < size) {
                val longTermBatch = longTermBatches[i]
                val numberOfCards = longTermBatch.getNumberOfCards()
                val expiredCards = longTermBatch.getNumberOfExpiredCards()
                if (numberOfCards == 0) {
                    batchSizes.add(BatchStatistics(0, 0))
                } else {
                    batchSizes.add(BatchStatistics(numberOfCards, expiredCards))
                }
                i++
            }
        }
        return batchSizes
    }

    fun editCard(position: Int, sideAText: String, sideBText: String) {
        if (position < 0 || position >= currentPack.size) return

        currentPack[position].sideAText = sideAText
        currentPack[position].sideBText = sideBText
    }

    fun putCardToNextBatch(position: Int) {
        if (position < 0 || position >= currentPack.size) {
            Log.e(
                "MobilePauker++::learnedCard",
                "request to update a card with position outside the pack"
            )
            return
        }
        val currentCard = currentPack[position]
        pushCurrentCard(currentCard)
    }

    fun moveCardToUnlearndBatch(position: Int) {
        if (position < 0 || position >= currentPack.size) {
            Log.e(
                "MobilePauker++::setCardUnLearned",
                "request to update a card with position outside the pack"
            )
            return
        }
        val currentCard = currentPack[position]
        lesson ?. let { lesson ->
            when (currentPhase) {
                SIMPLE_LEARNING -> {
                }
                REPEATING_USTM -> {
                    currentCard.isLearned = false
                    if (lesson.ultraShortTermList.remove(currentCard)) {
                        Log.d(
                            "MobilePauker++::setCurretnCardUnlearned",
                            "Moved card from USTM to unlearned batch"
                        )
                    } else {
                        Log.e(
                            "MobilePauker++::setCurretnCardUnlearned",
                            "Unable to delete card from USTM"
                        )
                    }
                    when (settingsManager.getStringPreference(RETURN_FORGOTTEN_CARDS)) {
                        "1" -> lesson.unlearnedBatch.addCard(currentCard)
                        "2" -> {
                            val numberOfCards: Int = lesson.unlearnedBatch.getNumberOfCards()
                            if (numberOfCards > 0) {
                                val random = Random()
                                val index = random.nextInt(numberOfCards)
                                lesson.unlearnedBatch.addCard(index, currentCard)
                            } else {
                                lesson.unlearnedBatch.addCard(0, currentCard)
                            }
                        }
                        else -> lesson.unlearnedBatch.addCard(0, currentCard)
                    }
                }
                REPEATING_STM -> {
                    currentCard.isLearned = false
                    lesson.shortTermList.remove(currentCard)
                    when (settingsManager.getStringPreference(RETURN_FORGOTTEN_CARDS)) {
                        "1" -> lesson.unlearnedBatch.addCard(currentCard)
                        "2" -> {
                            val numberOfCards: Int = lesson.unlearnedBatch.getNumberOfCards()
                            if (numberOfCards > 0) {
                                val random = Random()
                                val index = random.nextInt(numberOfCards)
                                lesson.unlearnedBatch.addCard(index, currentCard)
                            } else {
                                lesson.unlearnedBatch.addCard(0, currentCard)
                            }
                        }
                        else -> lesson.unlearnedBatch.addCard(0, currentCard)
                    }
                }
                REPEATING_LTM -> {
                    currentCard.isLearned = false
                    // remove card from current long term memory batch
                    val longTermBatchNumber: Int = currentCard.longTermBatchNumber
                    val longTermBatch: LongTermBatch =
                        lesson.getLongTermBatchFromIndex(longTermBatchNumber) as LongTermBatch
                    longTermBatch.removeCard(currentCard)
                    when (settingsManager.getStringPreference(RETURN_FORGOTTEN_CARDS)) {
                        "1" -> lesson.unlearnedBatch.addCard(currentCard)
                        "2" -> {
                            val numberOfCards: Int = lesson.unlearnedBatch.getNumberOfCards()
                            if (numberOfCards > 0) {
                                val random = Random()
                                val index = random.nextInt(numberOfCards)
                                lesson.unlearnedBatch.addCard(index, currentCard)
                            } else {
                                lesson.unlearnedBatch.addCard(0, currentCard)
                            }
                        }
                        else -> lesson.unlearnedBatch.addCard(0, currentCard)
                    }
                }
                else -> Log.e(
                    "MobilePauker++::setCurrentCardUnlearned",
                    "Learning phase not supported"
                )
            }
        }
    }

    fun resetShortTermLists() {
        lesson?.let { lesson ->
            val ustmList: List<Card> = lesson.ultraShortTermList
            val stmList: List<Card> = lesson.shortTermList
            for (i in ustmList.indices) {
                lesson.unlearnedBatch.addCard(ustmList[i])
            }
            for (i in stmList.indices) {
                lesson.unlearnedBatch.addCard(stmList[i])
            }
            lesson.ultraShortTermList.clear()
            lesson.shortTermList.clear()
        }
    }

    fun resetLongTermBatches() {
        lesson?.resetLongTermBatches()
    }

    fun resetShortTermBatches() {
        lesson?.resetShortTermBatches()
    }

    fun flipAllCards() {
        lesson?.flipAllCardSides()
    }

    fun isLessonSetup(): Boolean = lesson != null

    fun isLessonEmpty() =
        lesson?.getCards()?.isEmpty() == true && lesson?.description?.isEmpty() == true

    fun createNewLesson() {
        lesson = Lesson()
    }

    fun sortBatch(stackIndex: Int, sortByElement: Card.Element, asc_direction: Boolean): Boolean {
        val batch: Batch? = when (stackIndex) {
            0 -> lesson?.summaryBatch
            1 -> lesson?.unlearnedBatch
            else -> lesson?.getLongTermBatchFromIndex(stackIndex - 2)
        }

        return batch?.sortCards(sortByElement, asc_direction) ?: false
    }

    fun deleteCard(position: Int): Boolean {
        val card: Card = currentPack.get(position)
        if (deleteCard(card)) return false
        currentPack.removeAt(position)
        return true
    }

    fun deleteCard(card: Card): Boolean {
        if (card.isLearned) {
            val batchNumber: Int = card.longTermBatchNumber
            val longTermBatch: LongTermBatch =
                lesson?.getLongTermBatchFromIndex(batchNumber) as LongTermBatch
            if (longTermBatch.removeCard(card)) {
                Log.d(
                    "MobilePauker++::deleteCard",
                    "Deleted from long term batch$batchNumber"
                )
            } else {
                Log.e(
                    "MobilePauker++::deleteCard",
                    "Card not in long term batch$batchNumber"
                )
            }
        } else {
            if (lesson?.unlearnedBatch?.removeCard(card) == true) {
                Log.d("MobilePauker++::deleteCard", "Deleted from unlearned batch")
            } else if (lesson?.ultraShortTermList?.remove(card) == true) {
                Log.d("MobilePauker++::deleteCard", "Deleted from ultra short term batch")
            } else if (lesson?.shortTermList?.remove(card) == true) {
                Log.d("MobilePauker++::deleteCard", "Deleted from short term batch")
            } else {
                Log.e(
                    "MobilePauker++::deleteCard",
                    "Could not delete card from unlearned batch  "
                )
                return false
            }
        }
        if (lesson?.summaryBatch?.removeCard(card) == true) {
            Log.d("MobilePauker++::deleteCard", "Deleted from summary batch")
        } else {
            Log.e("MobilePauker++::deleteCard", "Could not delete card from summary batch  ")
            return false
        }
        return true
    }

    fun getCardFromCurrentPack(position: Int): FlashCard? {
        return if (position < 0 || position >= currentPack.size) {
            null
        } else {
            currentPack.get(position)
        }
    }

    fun getCardFont(side_ID: Int, position: Int): Font {
        val flashCard: FlashCard = getCardFromCurrentPack(position) ?: return Font()

        return (if (side_ID == CardPackAdapter.KEY_SIDEA_ID) flashCard.frontSide.font
        else flashCard.reverseSide.font) ?: Font()
    }

    fun forgetCard(card: Card) {
        if (!card.isLearned) return
        val batch: Batch? = getBatchOfCard(card)
        val index = batch?.indexOf(card) ?: -1
        if (index != -1 && batch != null) {
            lesson?.forgetCards(batch, intArrayOf(index))
        }
    }

    fun instantRepeatCard(card: Card) {
        val batch = getBatchOfCard(card)
        val index = batch?.indexOf(card) ?: -1
        if (index != -1 && batch != null) {
            lesson?.instantRepeatCards(batch, intArrayOf(index))
        }
    }

    fun getBatchSize(batchType: BatchType): Int = when (batchType) {
        BatchType.CURRENT -> currentPack.size
        BatchType.EXPIRED -> lesson?.getNumberOfExpiredCards() ?: 0
        BatchType.LESSON -> lesson?.getCards()?.size ?: 0
        BatchType.UNLEARNED -> lesson?.unlearnedBatch?.getNumberOfCards() ?: 0
        BatchType.ULTRA_SHORT_TERM -> lesson?.ultraShortTermList?.size ?: 0
        BatchType.SHORT_TERM -> lesson?.shortTermList?.size ?: 0
    }

    fun setupLesson(setupLesson: Lesson) {
        lesson = setupLesson
    }

    private fun getBatchOfCard(card: Card): Batch? {
        val batchNumber: Int = card.longTermBatchNumber
        return if (batchNumber == 0) lesson?.summaryBatch
        else if (batchNumber == 1) lesson?.unlearnedBatch
        else lesson?.getLongTermBatchFromIndex(batchNumber)
    }

    private fun shuffleCurrentPack() {
        currentPack.shuffle()
    }

    private fun doPackNeedShuffle(): Boolean {
        if (currentPhase == SIMPLE_LEARNING
            || currentPhase == REPEATING_STM
            || currentPhase == REPEATING_USTM
            || currentPhase == FILLING_USTM
        ) {
            if (settingsManager.getBoolPreference(LEARN_NEW_CARDS_RANDOMLY)) {
                return true
            }
        }

        return (currentPhase == REPEATING_LTM
            && settingsManager.getBoolPreference(LEARN_NEW_CARDS_RANDOMLY))
    }

    private fun pushCurrentCard(currentCard: FlashCard) {
        lesson?.let { lesson ->
            val longTermBatch: LongTermBatch
            when (currentPhase) {
                SIMPLE_LEARNING -> {
                    lesson.unlearnedBatch.removeCard(currentCard)
                    if (lesson.getLongTermBatchesSize() == 0) {
                        lesson.addLongTermBatch()
                    }
                    longTermBatch = lesson.getLongTermBatchFromIndex(0) as LongTermBatch
                    longTermBatch.addCard(currentCard)
                    currentCard.isLearned = true
                }
                FILLING_USTM -> {
                    lesson.ultraShortTermList.add(currentCard)
                    lesson.unlearnedBatch.removeCard(currentCard)
                }
                REPEATING_USTM -> {
                    lesson.ultraShortTermList.remove(currentCard)
                    lesson.shortTermList.add(currentCard)
                }
                REPEATING_STM -> {
                    lesson.shortTermList.remove(currentCard)
                    if (lesson.getLongTermBatchesSize() == 0) {
                        lesson.addLongTermBatch()
                    }
                    longTermBatch = lesson.getLongTermBatchFromIndex(0) as LongTermBatch
                    longTermBatch.addCard(currentCard)
                    currentCard.isLearned = true
                }
                REPEATING_LTM -> {
                    // remove card from current long term memory batch
                    val longTermBatchNumber: Int = currentCard.longTermBatchNumber
                    val nextLongTermBatchNumber = longTermBatchNumber + 1
                    longTermBatch =
                        lesson.getLongTermBatchFromIndex(longTermBatchNumber) as LongTermBatch
                    longTermBatch.removeCard(currentCard)
                    // add card to next long term batch
                    if (lesson.getLongTermBatchesSize() == nextLongTermBatchNumber) {
                        lesson.addLongTermBatch()
                    }
                    val nextLongTermBatch: LongTermBatch =
                        lesson.getLongTermBatchFromIndex(nextLongTermBatchNumber) as LongTermBatch
                    nextLongTermBatch.addCard(currentCard)
                    currentCard.updateLearnedTimeStamp()
                }
                else -> throw RuntimeException(
                    "unsupported learning phase \""
                        + currentPhase
                        + "\" -> can't find batch of card!"
                )
            }
        }
    }

    fun setupCurrentPack() {
        var cardIterator: Iterator<Card>
        lesson?.let { lesson ->
            when (currentPhase) {
                NOTHING -> {
                    currentPack.clear()
                    cardIterator = lesson.getCards().iterator()
                }
                BROWSE_NEW -> {
                    currentPack.clear()
                    cardIterator = lesson.unlearnedBatch.cards.iterator()
                }
                SIMPLE_LEARNING -> {
                    currentPack.clear()
                    cardIterator = lesson.unlearnedBatch.cards.iterator()
                }
                FILLING_USTM -> {
                    Log.d(
                        "MobilePauker++::setupCurrentPack",
                        "Setting batch to UnlearnedBatch"
                    )
                    currentPack.clear()
                    cardIterator = lesson.unlearnedBatch.cards.iterator()
                }
                WAITING_FOR_USTM -> {
                    Log.d("MobilePauker++::setupCurrentPack", "Waiting for ustm")
                    return
                }
                REPEATING_USTM -> {
                    Log.d(
                        "MobilePauker++::setupCurrentPack",
                        "Setting pack as ultra short term memory"
                    )
                    currentPack.clear()
                    cardIterator = lesson.ultraShortTermList.listIterator()
                }
                WAITING_FOR_STM -> {
                    cardIterator = lesson.shortTermList.listIterator()
                }
                REPEATING_STM -> {
                    Log.d(
                        "MobilePauker++::setupCurrentPack",
                        "Setting pack as short term memory"
                    )
                    currentPack.clear()
                    cardIterator = lesson.shortTermList.listIterator()
                }
                REPEATING_LTM -> {
                    Log.d(
                        "MobilePauker++::setupCurrentPack",
                        "Setting pack as expired cards"
                    )
                    lesson.refreshExpiration()
                    currentPack.clear()
                    cardIterator = lesson.getExpiredCards().iterator()
                }
            }

            // Fill the current pack
            fillCurrentPack(cardIterator)
        }
    }

    fun setCurrentPack(stackIndex: Int): MutableList<FlashCard> {
        val cardIterator: Iterator<Card>
        lesson?.let { lesson ->
            if (getBatchSize(BatchType.LESSON) > 0) {
                currentPack.clear()
                cardIterator = when (stackIndex) {
                    0 -> lesson.summaryBatch.cards.iterator()
                    1 -> lesson.unlearnedBatch.cards.iterator()
                    else -> lesson.getLongTermBatchFromIndex(stackIndex - 2).cards.iterator()
                }

                // Fill the current pack
                fillCurrentPack(cardIterator)
            }
        }
        return currentPack
    }

    private fun fillCurrentPack(cardIterator: Iterator<Card>) {
        while (cardIterator.hasNext()) {
            currentPack.add(cardIterator.next() as FlashCard)
        }
        if (doPackNeedShuffle()) {
            shuffleCurrentPack()
        }
    }
}