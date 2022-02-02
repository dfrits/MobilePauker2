package de.daniel.mobilepauker2.search

import android.view.MenuItem
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.card.Card
import de.daniel.mobilepauker2.lesson.card.FlashCard
import de.daniel.mobilepauker2.utils.Log
import java.util.*
import javax.inject.Inject

class SearchViewModel @Inject constructor() {
    var stackIndex = 0
    var pack: List<FlashCard> = emptyList()
    var itemPosition: Vector<Int> = Vector()
    private var checkedCards: MutableList<FlashCard> = mutableListOf()

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var dataManager: DataManager

    fun sortTypeSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mSortFrontASC -> {
                sortCards(Card.Element.FRONT_SIDE, true)
            }
            R.id.mSortBackASC -> {
                sortCards(Card.Element.REVERSE_SIDE, true)
            }
            R.id.mSortBatchnumberASC -> {
                sortCards(Card.Element.BATCH_NUMBER, true)
            }
            R.id.mSortLearnedDateASC -> {
                sortCards(Card.Element.LEARNED_DATE, true)
            }
            R.id.mSortExpiredDateASC -> {
                sortCards(Card.Element.EXPIRED_DATE, true)
            }
            R.id.mSortRepetTypeASC -> {
                sortCards(Card.Element.REPEATING_MODE, true)
            }
            R.id.mSortFrontDSC -> {
                sortCards(Card.Element.FRONT_SIDE, false)
            }
            R.id.mSortBackDSC -> {
                sortCards(Card.Element.REVERSE_SIDE, false)
            }
            R.id.mSortBatchnumberDSC -> {
                sortCards(Card.Element.BATCH_NUMBER, false)
            }
            R.id.mSortLearnedDateDSC -> {
                sortCards(Card.Element.LEARNED_DATE, false)
            }
            R.id.mSortExpiredDateDSC -> {
                sortCards(Card.Element.EXPIRED_DATE, false)
            }
            R.id.mSortRepetTypeDSC -> {
                sortCards(Card.Element.REPEATING_MODE, false)
            }
            else -> false
        }
    }

    fun itemCheckStateChanged(item: FlashCard, checked: Boolean) {
        if (checked) {
            checkedCards.add(item)
        } else {
            checkedCards.remove(item)
        }
    }

    fun resetCards() {
        for (card in checkedCards) {
            lessonManager.forgetCard(card)
        }
        checkedCards = mutableListOf()
        dataManager.saveRequired = true
        pack = lessonManager.setCurrentPack(stackIndex)
    }

    fun repeatCardsNow() {
        for (card in checkedCards) {
            lessonManager.instantRepeatCard(card)
        }
        checkedCards = mutableListOf()
        dataManager.saveRequired = true
        pack = lessonManager.setCurrentPack(stackIndex)
    }

    fun flipSides() {
        for (card in checkedCards) {
            card.flipSides()
        }
        checkedCards = mutableListOf()
        dataManager.saveRequired = true
        pack = lessonManager.setCurrentPack(stackIndex)
    }

    fun deleteCards() {
        for (card in checkedCards) {
            val cardDeleted: Boolean = lessonManager.deleteCard(card)
            Log.d(
                "SearchActivity::MultiChoiceListener::deleteCards",
                "Card deleted: $cardDeleted"
            )
        }
        checkedCards = mutableListOf()
        dataManager.saveRequired = true
        pack = lessonManager.setCurrentPack(stackIndex)
    }

    fun setRepeatingType(isRepeatByTyping: Boolean) {
        for (card in checkedCards) {
            card.setRepeatByTyping(isRepeatByTyping)
        }
        checkedCards = mutableListOf()
        dataManager.saveRequired = true
        pack = lessonManager.setCurrentPack(stackIndex)
    }

    fun queryString(query: String): List<FlashCard> {
        val results: MutableList<FlashCard> = mutableListOf()
        if (query == "") {
            return pack
        } else {
            var card: FlashCard
            for (i in pack.indices) {
                card = pack[i]
                Log.d("SearchActivity::ShowResults", "Index - " + card.id)
                val frontSide: String = card.frontSide.text.lowercase()
                val backSide: String = card.reverseSide.text.lowercase()

                if (frontSide.contains(query.lowercase()) || backSide.contains(query.lowercase())) {
                    results.add(card)
                    itemPosition.add(i)
                }
            }
            return results
        }
    }

    private fun sortCards(sortByElement: Card.Element, asc_direction: Boolean): Boolean = when {
        lessonManager.sortBatch(stackIndex, sortByElement, asc_direction) -> {
            dataManager.saveRequired = true
            pack = lessonManager.setCurrentPack(stackIndex)
            true
        }
        else -> false
    }
}