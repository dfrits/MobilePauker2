/*
 * Copyright 2011 Brian Ford
 *
 * This file is part of Pocket Pauker.
 *
 * Pocket Pauker is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Pocket Pauker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * See http://www.gnu.org/licenses/.

 */
package com.daniel.mobilepauker2.model

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Environment
import android.text.format.DateFormat
import android.widget.TextView
import android.widget.Toast
import com.daniel.mobilepauker2.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.model.SettingsManager.Keys
import com.daniel.mobilepauker2.model.pauker_native.*
import com.daniel.mobilepauker2.model.xmlsupport.FlashCardXMLPullFeedParser
import com.daniel.mobilepauker2.statistics.BatchStatistics
import com.daniel.mobilepauker2.utils.Constants
import com.daniel.mobilepauker2.utils.Log
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.util.*

/**
 * Manages access to a lesson
 *
 *
 *
 *
 * Provides a single facade (API) to the pauker model
 *
 *
 * Controls:
 * * setting up a lesson (static)
 * * moving through learning phases
 * * moving cards between lesson batches
 */
class ModelManager private constructor() {
    val currentPack: MutableList<FlashCard?> =
        ArrayList()
    private val settingsManager: SettingsManager? = SettingsManager.Companion.instance()
    var lesson: Lesson? = null
    private var mCurrentCard: FlashCard? = FlashCard()
    var learningPhase: LearningPhase? = LearningPhase.NOTHING
        private set

    private fun setupCurrentPack(context: Context) {
        var cardIterator: Iterator<Card?>? =
            null
        if (lesson != null) {
            when (learningPhase) {
                LearningPhase.NOTHING -> {
                    currentPack.clear()
                    cardIterator = lesson.getCards().iterator()
                }
                LearningPhase.BROWSE_NEW -> {
                    currentPack.clear()
                    cardIterator = lesson.getUnlearnedBatch().cards.iterator()
                }
                LearningPhase.SIMPLE_LEARNING -> {
                    currentPack.clear()
                    cardIterator = lesson.getUnlearnedBatch().cards.iterator()
                }
                LearningPhase.FILLING_USTM -> {
                    Log.d(
                        "AndyPaukerApplication::setupCurrentPack",
                        "Setting batch to UnlearnedBatch"
                    )
                    currentPack.clear()
                    cardIterator = lesson.getUnlearnedBatch().cards.iterator()
                }
                LearningPhase.WAITING_FOR_USTM -> {
                    Log.d(
                        "AndyPaukerApplication::setupCurrentPack",
                        "Waiting for ustm"
                    )
                    return
                }
                LearningPhase.REPEATING_USTM -> {
                    Log.d(
                        "AndyPaukerApplication::setupCurrentPack",
                        "Setting pack as ultra short term memory"
                    )
                    currentPack.clear()
                    cardIterator = lesson.getUltraShortTermList().listIterator()
                }
                LearningPhase.WAITING_FOR_STM -> {
                    cardIterator = lesson.getShortTermList()
                        .listIterator() // Need to put something in the iterator for requery
                }
                LearningPhase.REPEATING_STM -> {
                    Log.d(
                        "AndyPaukerApplication::setupCurrentPack",
                        "Setting pack as short term memory"
                    )
                    currentPack.clear()
                    cardIterator = lesson.getShortTermList().listIterator()
                }
                LearningPhase.REPEATING_LTM -> {
                    Log.d(
                        "AndyPaukerApplication::setupCurrentPack",
                        "Setting pack as expired cards"
                    )
                    lesson!!.refreshExpiration()
                    currentPack.clear()
                    cardIterator = lesson.getExpiredCards().iterator()
                }
            }
            // Fill the current pack
            fillCurrentPack(context, cardIterator)
        }
    }

    fun setCurrentPack(context: Context, stackIndex: Int) {
        val cardIterator: Iterator<Card?>
        if (lessonSize > 0) {
            currentPack.clear()
            cardIterator = when (stackIndex) {
                0 -> lesson.getSummaryBatch().cards.iterator()
                1 -> lesson.getUnlearnedBatch().cards.iterator()
                else -> lesson!!.getLongTermBatch(stackIndex - 2).cards!!.iterator()
            }
            // Fill the current pack
            fillCurrentPack(context, cardIterator)
        }
    }

    private fun fillCurrentPack(
        context: Context,
        cardIterator: Iterator<Card?>?
    ) {
        while (cardIterator!!.hasNext()) {
            currentPack.add(cardIterator.next() as FlashCard?)
        }
        if (isShuffle(context)) {
            shuffleCurrentPack()
        }
    }

    fun addCard(
        sideA: String?,
        sideB: String?,
        index: String?,
        learnStatus: String?
    ) {
        val newCard = FlashCard(sideA, sideB, index, learnStatus)
        lesson.getUnlearnedBatch().addCard(newCard)
    }

    fun addCard(flashCard: FlashCard, sideA: String?, sideB: String?) {
        flashCard.sideAText = sideA
        flashCard.sideBText = sideB
        lesson!!.addCard(flashCard)
    }

    val batchStatistics: List<BatchStatistics?>
        get() {
            val batchSizes =
                ArrayList<BatchStatistics?>()
            val longTermBatches = lesson!!.longTermBatches
            var i = 0
            val size = longTermBatches!!.size
            while (i < size) {
                val longTermBatch = longTermBatches[i]
                val numberOfCards = longTermBatch.numberOfCards
                val expiredCards = longTermBatch.numberOfExpiredCards
                if (numberOfCards == 0) {
                    batchSizes.add(BatchStatistics(0, 0))
                } else {
                    batchSizes.add(BatchStatistics(numberOfCards, expiredCards))
                }
                i++
            }
            return batchSizes
        }

    fun editCard(position: Int, sideAText: String?, sideBText: String?) {
        if (position < 0 || position >= currentPack.size) {
            Log.e(
                "AndyPaukerApplication::learnedCard",
                "request to update a card with position outside the pack"
            )
            return
        }
        currentPack[position].setSideAText(sideAText)
        currentPack[position].setSideBText(sideBText)
    }

    /**
     * While repeating cards the user acknowledged that the card was remembered
     * correctly, we have to move the current card one batch further
     * @param position .
     */
    fun setCardLearned(position: Int) {
        if (position < 0 || position >= currentPack.size) {
            Log.e(
                "AndyPaukerApplication::learnedCard",
                "request to update a card with position outside the pack"
            )
            return
        }
        mCurrentCard = currentPack[position]
        pushCurrentCard()
    }

    /**
     * Removes the card from its current batch and moves it to the
     * un-learned batch. Use this to forget cards.
     * @param position .
     * @param context  .
     */
    fun pullCurrentCard(position: Int, context: Context) {
        if (position < 0 || position >= currentPack.size) {
            Log.e(
                "AndyPaukerApplication::setCardUnLearned",
                "request to update a card with position outside the pack"
            )
            return
        }
        mCurrentCard = currentPack[position]
        when (learningPhase) {
            LearningPhase.SIMPLE_LEARNING -> {
            }
            LearningPhase.REPEATING_USTM -> {
                mCurrentCard.setLearned(false)
                if (lesson.getUltraShortTermList().remove(mCurrentCard)) {
                    Log.d(
                        "AndyPaukerApplication::setCurretnCardUnlearned",
                        "Moved card from USTM to unlearned batch"
                    )
                } else {
                    Log.e(
                        "AndyPaukerApplication::setCurretnCardUnlearned",
                        "Unable to delete card from USTM"
                    )
                }
                when (settingsManager!!.getStringPreference(context, Keys.RETURN_FORGOTTEN_CARDS)) {
                    "1" -> lesson.getUnlearnedBatch().addCard(mCurrentCard)
                    "2" -> {
                        val numberOfCards = lesson.getUnlearnedBatch().numberOfCards
                        if (numberOfCards > 0) {
                            val random = Random()
                            val index = random.nextInt(numberOfCards)
                            lesson.getUnlearnedBatch().addCard(index, mCurrentCard)
                        } else {
                            lesson.getUnlearnedBatch().addCard(0, mCurrentCard)
                        }
                    }
                    else -> lesson.getUnlearnedBatch().addCard(0, mCurrentCard)
                }
            }
            LearningPhase.REPEATING_STM -> {
                mCurrentCard.setLearned(false)
                lesson.getShortTermList().remove(mCurrentCard)
                when (settingsManager!!.getStringPreference(context, Keys.RETURN_FORGOTTEN_CARDS)) {
                    "1" -> lesson.getUnlearnedBatch().addCard(mCurrentCard)
                    "2" -> {
                        val numberOfCards = lesson.getUnlearnedBatch().numberOfCards
                        if (numberOfCards > 0) {
                            val random = Random()
                            val index = random.nextInt(numberOfCards)
                            lesson.getUnlearnedBatch().addCard(index, mCurrentCard)
                        } else {
                            lesson.getUnlearnedBatch().addCard(0, mCurrentCard)
                        }
                    }
                    else -> lesson.getUnlearnedBatch().addCard(0, mCurrentCard)
                }
            }
            LearningPhase.REPEATING_LTM -> {
                mCurrentCard.setLearned(false)
                // remove card from current long term memory batch
                val longTermBatchNumber = mCurrentCard.getLongTermBatchNumber()
                val longTermBatch = lesson!!.getLongTermBatch(longTermBatchNumber)
                longTermBatch!!.removeCard(mCurrentCard)
                when (settingsManager!!.getStringPreference(context, Keys.RETURN_FORGOTTEN_CARDS)) {
                    "1" -> lesson.getUnlearnedBatch().addCard(mCurrentCard)
                    "2" -> {
                        val numberOfCards = lesson.getUnlearnedBatch().numberOfCards
                        if (numberOfCards > 0) {
                            val random = Random()
                            val index = random.nextInt(numberOfCards)
                            lesson.getUnlearnedBatch().addCard(index, mCurrentCard)
                        } else {
                            lesson.getUnlearnedBatch().addCard(0, mCurrentCard)
                        }
                    }
                    else -> lesson.getUnlearnedBatch().addCard(0, mCurrentCard)
                }
            }
            else -> Log.e(
                "AndyPaukerApplication::setCurretnCardUnlearned",
                "Learning phase not supported"
            )
        }
    }

    /**
     * While repeating cards the user acknowledged that the card was remembered
     * correctly, we have to move the current card one batch further
     */
    private fun pushCurrentCard() { //Log.d("AndyPaukerApplication::pushCurrentCard","Entry : Learning phase = ");
        val longTermBatch: LongTermBatch?
        when (learningPhase) {
            LearningPhase.SIMPLE_LEARNING -> {
                lesson.getUnlearnedBatch().removeCard(mCurrentCard)
                if (lesson.getNumberOfLongTermBatches() == 0) {
                    lesson!!.addLongTermBatch()
                }
                longTermBatch = lesson!!.getLongTermBatch(0)
                longTermBatch.addCard(mCurrentCard)
                mCurrentCard.setLearned(true)
            }
            LearningPhase.FILLING_USTM -> {
                lesson.getUltraShortTermList().add(mCurrentCard)
                lesson.getUnlearnedBatch().removeCard(mCurrentCard)
            }
            LearningPhase.REPEATING_USTM -> {
                lesson.getUltraShortTermList().remove(mCurrentCard)
                lesson.getShortTermList().add(mCurrentCard)
            }
            LearningPhase.REPEATING_STM -> {
                lesson.getShortTermList().remove(mCurrentCard)
                if (lesson.getNumberOfLongTermBatches() == 0) {
                    lesson!!.addLongTermBatch()
                }
                longTermBatch = lesson!!.getLongTermBatch(0)
                longTermBatch.addCard(mCurrentCard)
                mCurrentCard.setLearned(true)
            }
            LearningPhase.REPEATING_LTM -> {
                // remove card from current long term memory batch
                val longTermBatchNumber = mCurrentCard.getLongTermBatchNumber()
                val nextLongTermBatchNumber = longTermBatchNumber + 1
                longTermBatch = lesson!!.getLongTermBatch(longTermBatchNumber)
                longTermBatch.removeCard(mCurrentCard)
                // add card to next long term batch
                if (lesson.getNumberOfLongTermBatches()
                    == nextLongTermBatchNumber
                ) {
                    lesson!!.addLongTermBatch()
                }
                val nextLongTermBatch =
                    lesson!!.getLongTermBatch(nextLongTermBatchNumber)
                nextLongTermBatch!!.addCard(mCurrentCard)
                mCurrentCard!!.updateLearnedTimeStamp()
            }
            else -> throw RuntimeException(
                "unsupported learning phase \""
                        + learningPhase
                        + "\" -> can't find batch of card!"
            )
        }
    }

    val filePath: File
        get() {
            val filePath = (Environment.getExternalStorageDirectory()
                .toString() + paukerManager.getApplicationDataDirectory()
                    + paukerManager.getCurrentFileName())
            return File(filePath)
        }

    /**
     * Zeigt einen Toast mit dem nächsten Ablaufdatum an, wenn es in den Einstellungen aktiviert ist.
     * @param context Kontext der aufrufenden Activity
     */
    fun showExpireToast(context: Context) {
        if (!settingsManager!!.getBoolPreference(context, Keys.ENABLE_EXPIRE_TOAST)) return
        val filePath = filePath
        val uri = filePath.toURI()
        val parser: FlashCardXMLPullFeedParser
        try {
            parser = FlashCardXMLPullFeedParser(uri.toURL())
            val map = parser.nextExpireDate
            if (map!![0] > Long.MIN_VALUE) {
                val dateL = map[0]
                val cal =
                    Calendar.getInstance(Locale.getDefault())
                cal.timeInMillis = dateL
                val date =
                    DateFormat.format("dd.MM.yyyy HH:mm", cal).toString()
                var text = context.getString(R.string.next_expire_date)
                text = "$text $date"
                PaukerManager.Companion.showToast(context as Activity, text, Toast.LENGTH_LONG * 2)
            }
        } catch (ignored: MalformedURLException) {
        }
    }

    fun deleteLesson(context: Context, file: File): Boolean {
        val filename = file.name
        try {
            if (file.delete()) {
                val fos = context.openFileOutput(
                    Constants.DELETED_FILES_NAMES_FILE_NAME,
                    Context.MODE_APPEND
                )
                val text =
                    "\n" + filename + ";*;" + System.currentTimeMillis()
                fos.write(text.toByteArray())
                fos.close()
            } else return false
        } catch (e: IOException) {
            return false
        }
        return try {
            val list = getLokalAddedFiles(context)
            if (list.contains(filename)) {
                resetAddedFilesData(context)
                val fos = context.openFileOutput(
                    Constants.ADDED_FILES_NAMES_FILE_NAME,
                    Context.MODE_APPEND
                )
                for (name in list) {
                    if (name != filename) {
                        val newText = "\n" + name
                        fos.write(newText.toByteArray())
                    }
                }
                fos.close()
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    fun addLesson(context: Context, file: File) {
        var filename = file.name
        val index =
            if (filename.endsWith(".xml")) filename.indexOf(".xml") else filename.indexOf(".pau")
        if (index != -1) {
            filename = filename.substring(0, index)
            addLesson(context, filename)
        }
    }

    fun addLesson(context: Context) {
        val filename = paukerManager.getCurrentFileName()
        addLesson(context, filename)
    }

    private fun addLesson(context: Context, fileName: String?) {
        try {
            val fos = context.openFileOutput(
                Constants.ADDED_FILES_NAMES_FILE_NAME,
                Context.MODE_APPEND
            )
            val text = "\n" + fileName
            fos.write(text.toByteArray())
            fos.close()
        } catch (ignored: IOException) {
        }
        try {
            val map =
                getLokalDeletedFiles(context)
            if (map.keys.contains(fileName)) {
                resetDeletedFilesData(context)
                val fos = context.openFileOutput(
                    Constants.DELETED_FILES_NAMES_FILE_NAME,
                    Context.MODE_APPEND
                )
                for ((key) in map) {
                    if (key != fileName) {
                        val newText =
                            "\n" + fileName + ";*;" + System.currentTimeMillis()
                        fos.write(newText.toByteArray())
                    }
                }
                fos.close()
            }
        } catch (ignored: IOException) {
        }
    }

    fun getLokalDeletedFiles(context: Context): Map<String?, String> {
        val filesToDelete: MutableMap<String?, String> =
            HashMap()
        try {
            val fis =
                context.openFileInput(Constants.DELETED_FILES_NAMES_FILE_NAME)
            val reader =
                BufferedReader(InputStreamReader(fis))
            var fileName = reader.readLine()
            while (fileName != null) {
                if (!fileName.trim { it <= ' ' }.isEmpty()) {
                    try {
                        val split: Array<String?> =
                            fileName.split(";*;").toTypedArray()
                        val name = if (split[0] == null) "" else split[0]!!
                        val time = if (split[1] == null) "-1" else split[1]!!
                        filesToDelete[name] = time
                    } catch (e: Exception) {
                        filesToDelete[fileName] = "-1"
                    }
                }
                fileName = reader.readLine()
            }
        } catch (ignored: IOException) {
        }
        return filesToDelete
    }

    fun getLokalAddedFiles(context: Context): List<String> {
        val filesToAdd: MutableList<String> =
            ArrayList()
        try {
            val fis =
                context.openFileInput(Constants.ADDED_FILES_NAMES_FILE_NAME)
            val reader =
                BufferedReader(InputStreamReader(fis))
            var fileName = reader.readLine()
            while (fileName != null) {
                if (!fileName.trim { it <= ' ' }.isEmpty()) {
                    filesToAdd.add(fileName)
                }
                fileName = reader.readLine()
            }
        } catch (ignored: IOException) {
        }
        return filesToAdd
    }

    fun resetDeletedFilesData(context: Context): Boolean {
        return try {
            val fos = context.openFileOutput(
                Constants.DELETED_FILES_NAMES_FILE_NAME,
                Context.MODE_PRIVATE
            )
            val text = "\n"
            fos.write(text.toByteArray())
            fos.close()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun resetAddedFilesData(context: Context): Boolean {
        return try {
            val fos = context.openFileOutput(
                Constants.ADDED_FILES_NAMES_FILE_NAME,
                Context.MODE_PRIVATE
            )
            val text = "\n"
            fos.write(text.toByteArray())
            fos.close()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun resetIndexFiles(context: Context): Boolean {
        return resetDeletedFilesData(context) && resetAddedFilesData(context)
    }

    /**
     * Move all cards in USTM and STM back to unlearned batch
     */
    fun resetLesson() {
        val ustmList =
            lesson.getUltraShortTermList()
        val stmList =
            lesson.getShortTermList()
        for (i in ustmList!!.indices) {
            lesson.getUnlearnedBatch().addCard(ustmList!![i])
        }
        for (i in stmList!!.indices) {
            lesson.getUnlearnedBatch().addCard(stmList!![i])
        }
        lesson.getUltraShortTermList().clear()
        lesson.getShortTermList().clear()
    }

    /**
     * Verschiebt alle Karten auf den Stapel der ungelernten Karten.
     */
    fun forgetAllCards() {
        lesson!!.reset()
    }

    /**
     * Dreht alle Karten um.
     */
    fun flipAllCards() {
        lesson!!.flip()
    }

    val isLessonNotNew: Boolean
        get() = paukerManager.getCurrentFileName() != Constants.DEFAULT_FILE_NAME

    val isLessonSetup: Boolean
        get() = lesson != null

    /**
     * Prüft ob die Lektion leer ist.
     * @return True, wenn keine Karten vorhanden sind und die Beschreibung leer ist
     */
    val isLessonEmpty: Boolean
        get() = isLessonSetup && lesson.getCards().isEmpty() && lesson.getDescription().isEmpty()

    fun createNewLesson() {
        Log.d("AndyPaukerApplication::setupNewLesson", "Entry")
        val newLesson = Lesson()
        lesson = newLesson
    }

    /**
     * Sortiert den Batch, aber aktuellisiert nicht den aktuellen.
     * @param stackIndex    Index des Batchs
     * @param sortByElement Nach diesem Element wird soriert
     * @param asc_direction In welche Richtung sortiert werden soll
     */
    fun sortBatch(
        stackIndex: Int,
        sortByElement: Card.Element?,
        asc_direction: Boolean
    ) {
        val batch: Batch?
        batch = when (stackIndex) {
            0 -> lesson.getSummaryBatch()
            1 -> lesson.getUnlearnedBatch()
            else -> lesson!!.getLongTermBatch(stackIndex - 2)
        }
        batch?.sortCards(sortByElement, asc_direction)
    }

    /**
     * Löscht die Karte an der übergebenen Position und entfernt diese aus dem aktuellen Stack.
     * @param position Position der Karte
     * @return True, wenn Karte erfolgreich gelöscht wurde
     */
    fun deleteCard(position: Int): Boolean {
        val card: Card? = currentPack[position]
        if (deleteCard(card)) return false
        currentPack.removeAt(position)
        return true
    }

    /**
     * Löscht die Karte, aber entfernt diese nicht aus dem aktuellen Stack.
     * @param card Zu löschende Karte
     * @return True, wenn Karte erfolgreich gelöscht wurde
     */
    fun deleteCard(card: Card?): Boolean {
        Log.d("AndyPaukerApplication::deleteCard", "entry")
        if (card!!.isLearned) {
            val batchNumber = card.longTermBatchNumber
            val longTermBatch = lesson!!.getLongTermBatch(batchNumber)
            if (longTermBatch!!.removeCard(card)) {
                Log.d(
                    "AndyPaukerApplication::deleteCard",
                    "Deleted from long term batch$batchNumber"
                )
            } else {
                Log.e(
                    "AndyPaukerApplication::deleteCard",
                    "Card not in long term batch$batchNumber"
                )
            }
        } else {
            if (lesson.getUnlearnedBatch().removeCard(card)) {
                Log.d(
                    "AndyPaukerApplication::deleteCard",
                    "Deleted from unlearned batch"
                )
            } else if (lesson.getUltraShortTermList().remove(card)) {
                Log.d(
                    "AndyPaukerApplication::deleteCard",
                    "Deleted from ultra short term batch"
                )
            } else if (lesson.getShortTermList().remove(card)) {
                Log.d(
                    "AndyPaukerApplication::deleteCard",
                    "Deleted from short term batch"
                )
            } else {
                Log.e(
                    "AndyPaukerApplication::deleteCard",
                    "Could not delete card from unlearned batch  "
                )
                return false
            }
        }
        if (lesson.getSummaryBatch().removeCard(card)) {
            Log.d(
                "AndyPaukerApplication::deleteCard",
                "Deleted from summary batch"
            )
        } else {
            Log.e(
                "AndyPaukerApplication::deleteCard",
                "Could not delete card from summary batch  "
            )
            return false
        }
        return true
    }

    fun setLearningPhase(
        context: Context,
        learningPhase: LearningPhase?
    ) {
        this.learningPhase = learningPhase
        setupCurrentPack(context)
    }

    var description: String?
        get() = lesson.getDescription()
        set(s) {
            lesson.setDescription(s)
        }

    fun getCard(position: Int): FlashCard? {
        return if (position < 0 || position >= currentPack.size) {
            null
        } else {
            currentPack[position]
        }
    }

    fun getCardFont(
        side_ID: Int,
        position: Int
    ): Font {
        val flashCard = getCard(position) ?: return Font()
        val font: Font?
        font =
            if (side_ID == CardPackAdapter.Companion.KEY_SIDEA_ID) flashCard.frontSide.font else flashCard.reverseSide.font
        return font ?: Font()
    }

    /**
     * Setzt die entsprechende Font-Werte bei der Karte, falls sie vorhanden sind. Sonst werden
     * Standartwerte gesetzt. Gesetzt werden Textgröße, Textfarbe, Fett, Kursiv, Font und
     * Hintergrundfarbe.
     * @param font     Seite, bei der die Werte gesetzt werden sollen
     * @param cardSide Hiervon werden die Werte ausgelesen
     */
    fun setFont(
        font: Font?,
        cardSide: TextView
    ) {
        var font = font
        font = font ?: Font()
        val textSize = font.textSize
        cardSide.textSize = if (textSize > 16) textSize.toFloat() else 16.toFloat()
        cardSide.setTextColor(font.textColor)
        val bold = font.isBold
        val italic = font.isItalic
        if (bold && italic) cardSide.typeface = Typeface.create(
            font.family,
            Typeface.BOLD_ITALIC
        ) else if (bold) cardSide.typeface = Typeface.create(
            font.family,
            Typeface.BOLD
        ) else if (italic) cardSide.typeface = Typeface.create(
            font.family,
            Typeface.ITALIC
        ) else cardSide.typeface = Typeface.create(font.family, Typeface.NORMAL)
        val backgroundColor = font.backgroundColor
        if (backgroundColor != -1) cardSide.background =
            createBoxBackground(backgroundColor) else cardSide.setBackgroundResource(
            R.drawable.box_background
        )
    }

    private fun createBoxBackground(backgroundColor: Int): GradientDrawable {
        val background = GradientDrawable()
        background.shape = GradientDrawable.RECTANGLE
        background.cornerRadius = 2f
        background.setStroke(3, Color.BLACK)
        background.setColor(backgroundColor)
        return background
    }

    val currentBatchSize: Int
        get() = currentPack.size

    val lessonSize: Int
        get() = lesson.getCards().size

    val expiredCardsSize: Int
        get() = lesson.getNumberOfExpiredCards()

    val unlearnedBatchSize: Int
        get() = lesson.getUnlearnedBatch().cards.size

    //TODO Have an enum BATCH_TYPE and pass that into a single getBatchSize(BatchType)
    val ultraShortTermMemorySize: Int
        get() = lesson.getUltraShortTermList().size

    val shortTermMemorySize: Int
        get() = lesson.getShortTermList().size

    /*
     * Shuffle the card pack
     */
    private fun shuffleCurrentPack() {
        Collections.shuffle(currentPack)
    }

    /*
     * Return true if the pack needs shuffled
     */
    private fun isShuffle(context: Context): Boolean { // Check if we need to add random learn
        if (learningPhase == LearningPhase.SIMPLE_LEARNING || learningPhase == LearningPhase.REPEATING_STM || learningPhase == LearningPhase.REPEATING_USTM || learningPhase == LearningPhase.FILLING_USTM
        ) {
            if (settingsManager!!.getBoolPreference(context, Keys.LEARN_NEW_CARDS_RANDOMLY)) {
                return true
            }
        }
        return (learningPhase == LearningPhase.REPEATING_LTM
                && settingsManager!!.getBoolPreference(context, Keys.LEARN_NEW_CARDS_RANDOMLY))
    }

    fun forgetCard(card: Card) {
        if (!card.isLearned) return
        val batch = getBatchOfCard(card)
        val index = batch!!.indexOf(card)
        if (index != -1) {
            lesson!!.forgetCards(batch, intArrayOf(index))
        }
    }

    fun instantRepeatCard(card: Card) {
        val batch = getBatchOfCard(card)
        val index = batch!!.indexOf(card)
        if (index != -1) {
            lesson!!.instantRepeatCards(batch, intArrayOf(index))
        }
    }

    private fun getBatchOfCard(card: Card): Batch? {
        val batchNumber = card.longTermBatchNumber
        return if (batchNumber == 0) lesson.getSummaryBatch() else if (batchNumber == 1) lesson.getUnlearnedBatch() else lesson!!.getLongTermBatch(
            batchNumber
        )
    }

    /**
     * the learning phases
     */
    enum class LearningPhase {
        /**
         * not learning
         */
        NOTHING,
        /**
         * Just browsing the new cards - not learning
         */
        BROWSE_NEW,
        /**
         * Not using USTM or STM memory and timers
         */
        SIMPLE_LEARNING,
        /**
         * the phase of filling the ultra short term memory
         */
        FILLING_USTM,
        /**
         * the phase of waiting for the ultra short term memory
         */
        WAITING_FOR_USTM,
        /**
         * the phase of repeating the ultra short term memory
         */
        REPEATING_USTM,
        /**
         * the phase of waiting for the short term memory
         */
        WAITING_FOR_STM,
        /**
         * the phase of repeating the short term memory
         */
        REPEATING_STM,
        /**
         * the phase of repeating the long term memory
         */
        REPEATING_LTM
    }

    companion object {
        private val paukerManager: PaukerManager? = PaukerManager.Companion.instance()
        private var instance: ModelManager? = null
        fun instance(): ModelManager? {
            if (instance == null) {
                instance = ModelManager()
            }
            return instance
        }
    }
}