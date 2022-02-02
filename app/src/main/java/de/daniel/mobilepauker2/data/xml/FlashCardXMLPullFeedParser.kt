package de.daniel.mobilepauker2.data.xml

import android.util.Xml
import de.daniel.mobilepauker2.lesson.Lesson
import de.daniel.mobilepauker2.lesson.batch.Batch
import de.daniel.mobilepauker2.lesson.batch.LongTermBatch
import de.daniel.mobilepauker2.lesson.batch.SummaryBatch
import de.daniel.mobilepauker2.lesson.card.Card
import de.daniel.mobilepauker2.lesson.card.ComponentOrientation
import de.daniel.mobilepauker2.lesson.card.FlashCard
import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.models.NextExpireDateResult
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.util.*
import kotlin.math.pow

class FlashCardXMLPullFeedParser(feedUrl: URL) : FlashCardBasedFeedParser(feedUrl) {

    override fun parse(): Lesson {
        var flashCards: MutableList<FlashCard>? = null
        val parser = Xml.newPullParser()
        var batchCount = 0
        var description = "No Description"
        return try {
            parser.setInput(getInputStream(), null)
            var eventType = parser.eventType
            var currentFlashCard: FlashCard? = null
            var SIDEA = false
            var SIDEB = false
            var done = false
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                var name: String
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> flashCards = ArrayList<FlashCard>()
                    XmlPullParser.START_TAG -> {
                        name = parser.name
                        if (name.equals(LESSON, ignoreCase = true)) {
                            val lessonFormatString = parser.getAttributeValue(null, "LessonFormat")
                            if (lessonFormatString != null) {
                                val lessonFormat = lessonFormatString.toFloat()
                                Log.d(
                                    "FlashCardXMLPullFeedParser::parse",
                                    "Lesson format is $lessonFormat"
                                )
                            }
                        } else if (name.equals(
                                DESCRIPTION,
                                ignoreCase = true
                            )
                        ) {
                            description = parser.nextText()
                            if (description == null) {
                                description = "No description"
                            }
                        } else if (name.equals(
                                CARD,
                                ignoreCase = true
                            )
                        ) {
                            currentFlashCard = FlashCard()
                        } else if (name.equals(
                                BATCH,
                                ignoreCase = true
                            )
                        ) {
                            batchCount++
                        } else if (currentFlashCard != null) {
                            currentFlashCard.initialBatch = batchCount - 1
                            if (name.equals(
                                    FRONTSIDE,
                                    ignoreCase = true
                                ) || name.equals(
                                    REVERSESIDE,
                                    ignoreCase = true
                                )
                            ) {
                                var orientation = parser.getAttributeValue(null, "Orientation")
                                orientation = orientation ?: Constants.STANDARD_ORIENTATION
                                val repeatByTyping =
                                    parser.getAttributeValue(null, "RepeatByTyping")
                                val bRepeatByTyping =
                                    if (repeatByTyping == null) Constants.STANDARD_REPEAT else repeatByTyping == "true"
                                val learnedTimestamp =
                                    parser.getAttributeValue(null, "LearnedTimestamp")
                                if (name.equals(
                                        FRONTSIDE,
                                        ignoreCase = true
                                    )
                                ) {
                                    SIDEA = true
                                    SIDEB = false
                                    currentFlashCard.frontSide.orientation =
                                        ComponentOrientation(orientation)
                                    currentFlashCard.setRepeatByTyping(bRepeatByTyping)

                                    if (learnedTimestamp != null) {
                                        val l = learnedTimestamp.trim { it <= ' ' }.toLong()
                                        currentFlashCard.setLearnedTimeStamp(l)
                                    }
                                } else if (name.equals(
                                        REVERSESIDE,
                                        ignoreCase = true
                                    )
                                ) {
                                    SIDEA = false
                                    SIDEB = true
                                    currentFlashCard.reverseSide.orientation =
                                        ComponentOrientation(orientation)
                                    currentFlashCard.reverseSide.setRepeatByTyping(bRepeatByTyping)

                                    if (learnedTimestamp != null) {
                                        val l = learnedTimestamp.trim { it <= ' ' }.toLong()
                                        currentFlashCard.reverseSide.setLearnedTimeStamp(l)
                                    }
                                }

                                //Log.d("FlashCardXMLPullFeedParser::parse", "orientation=" + orientation);
                            } else if (name.equals(
                                    TEXT,
                                    ignoreCase = true
                                )
                            ) {
                                when {
                                    SIDEA -> {
                                        currentFlashCard.sideAText = parser.nextText()
                                        //Log.d("FlashCardXMLPullFeedParser::parse","sideA=" + currentFlashCard.getSideAText());
                                    }
                                    SIDEB -> {
                                        currentFlashCard.sideBText = parser.nextText()
                                    }
                                    else -> {
                                        currentFlashCard.sideAText = "Empty"
                                        currentFlashCard.sideBText = "Empty"
                                    }
                                }
                            } else if (name.equals(
                                    FONT,
                                    ignoreCase = true
                                )
                            ) {
                                var background = parser.getAttributeValue(null, "Background")
                                var bold = parser.getAttributeValue(null, "Bold")
                                var family = parser.getAttributeValue(null, "Family")
                                var foreground = parser.getAttributeValue(null, "Foreground")
                                var italic = parser.getAttributeValue(null, "Italic")
                                var size = parser.getAttributeValue(null, "Size")

                                // Set to defaults if null
                                if (background == null) {
                                    background = "-1"
                                }
                                if (bold == null) {
                                    bold = "false"
                                }
                                if (family == null) {
                                    family = "Dialog"
                                }
                                if (foreground == null) {
                                    foreground = "-16777216"
                                }
                                if (italic == null) {
                                    italic = "false"
                                }
                                if (size == null) {
                                    size = "12"
                                }
                                if (SIDEA) {
                                    currentFlashCard.frontSide.font =
                                        Font(
                                            background,
                                            bold,
                                            family,
                                            foreground,
                                            italic,
                                            size
                                        )
                                } else if (SIDEB) {
                                    currentFlashCard.reverseSide.font =
                                        Font(
                                            background,
                                            bold,
                                            family,
                                            foreground,
                                            italic,
                                            size
                                        )
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        name = parser.name
                        if (name.equals(CARD, ignoreCase = true) && currentFlashCard != null) {
                            flashCards?.add(currentFlashCard)
                        } else if (name.equals(LESSON, ignoreCase = true)) {
                            done = true
                        }
                    }
                }
                eventType = parser.next()
            }

            if (flashCards != null) {
                setupLesson(flashCards, description)
            } else {
                Lesson()
            }
        } catch (e: Exception) {
            Log.e("FlashCardXMLPullFeedParser:parse()", e.message, e)
            throw RuntimeException(e)
        }
    }

    private fun setupLesson(flashCardList: List<FlashCard>, description: String): Lesson {
        val newLesson = Lesson()
        val summaryBatch: SummaryBatch = newLesson.summaryBatch
        newLesson.description = description
        for (i in flashCardList.indices) {
            val flashCard: FlashCard = flashCardList[i]
            if (flashCard.initialBatch < 3) {
                flashCard.isLearned = false
            } else {
                // Warning using flash card set learned here sets the learned timestamp!
                flashCard.frontSide.isLearned = true
            }
            if (newLesson.getLongTermBatchesSize() < flashCard.initialBatch - 2) {
                Log.d(
                    "FC~XMLPullFeedParser::setupLesson",
                    "num of long term batches=" + newLesson.getLongTermBatchesSize()
                )
                Log.d(
                    "FC~XMLPullFeedParser::setupLesson",
                    "card initla batch=" + flashCard.initialBatch
                )
                val batchesToAdd: Int =
                    flashCard.initialBatch - 2 - newLesson.getLongTermBatchesSize()
                Log.d("FC~XMLPullFeedParser::setupLesson", "batchsToAdd$batchesToAdd")
                for (j in 0 until batchesToAdd) {
                    newLesson.addLongTermBatch()
                }
            }

            val batch: Batch = if (flashCard.isLearned) {
                newLesson.getLongTermBatchFromIndex(flashCard.initialBatch - 3) as LongTermBatch
            } else {
                newLesson.unlearnedBatch
            }
            batch.addCard(flashCard)
            summaryBatch.addCard(flashCard)
        }

        newLesson.refreshExpiredCards()
        printLessonToDebug(newLesson)

        return newLesson
    }

    /**
     * Findet das nächste Ablaufdatum. Falls keines gefunden wird, wird [Long.MIN_VALUE]
     * zurückgegeben.
     */
    fun getNextExpireDate(): NextExpireDateResult {
        val parser = Xml.newPullParser()
        val currentTimestamp = System.currentTimeMillis()
        return try {
            parser.setInput(getInputStream(), null)
            var eventType = parser.eventType
            var nextExpireTimeStamp = Long.MIN_VALUE
            var batchCount = 0
            var expiredCards: Long = 0
            var done = false

            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                var name: String
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        name = parser.name
                        if (name.equals(LESSON, ignoreCase = true)) {
                            val lessonFormatString = parser.getAttributeValue(null, "LessonFormat")
                            if (lessonFormatString != null) {
                                val lessonFormat = lessonFormatString.toFloat()
                                Log.d(
                                    "FlashCardXMLPullFeedParser::parse",
                                    "Lesson format is $lessonFormat"
                                )
                            }
                        } else if (name.equals(BATCH, ignoreCase = true)) {
                            batchCount++
                        } else if (name.equals(FRONTSIDE, ignoreCase = true)
                            || name.equals(REVERSESIDE, ignoreCase = true)
                        ) {
                            val learnedTimestamp =
                                parser.getAttributeValue(null, "LearnedTimestamp")
                            if (learnedTimestamp != null) {
                                val factor = Math.E.pow((batchCount - 4).toDouble())
                                val expirationTime =
                                    (LongTermBatch.EXPIRATION_UNIT * factor).toLong()
                                try {
                                    val expireTimeStamp = learnedTimestamp.toLong() + expirationTime
                                    if (nextExpireTimeStamp == Long.MIN_VALUE || expireTimeStamp < nextExpireTimeStamp) {
                                        nextExpireTimeStamp = expireTimeStamp
                                    }
                                    val diff = currentTimestamp - learnedTimestamp.toLong()
                                    if (diff > expirationTime) {
                                        expiredCards++
                                    }
                                } catch (ignored: NumberFormatException) {
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        name = parser.name
                        if (name.equals(LESSON, ignoreCase = true)) {
                            done = true
                        }
                    }
                }
                eventType = parser.next()
            }
            NextExpireDateResult(nextExpireTimeStamp, expiredCards, feedUrl.file)
        } catch (e: Exception) {
            Log.e("FlashCardXMLPullFeedParser:parse()", e.message, e)
            throw RuntimeException(e)
        }
    }

    private fun printLessonToDebug(lesson: Lesson) {
        var cards: Collection<Card?>? = lesson.getLearnedCards()
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of learned cards is " + cards!!.size)
        cards = lesson.getExpiredCards()
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of expired cards is " + cards.size)
        cards = lesson.shortTermList
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of shortTerm cards is " + cards.size)
        cards = lesson.getCards()
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of all cards is " + cards.size)
        cards = lesson.ultraShortTermList
        Log.d(
            "FlashCardXMLPullFeedParser::setupLesson",
            "Size of ultraShortTerm cards is " + cards.size
        )
        Log.d(
            "FlashCardXMLPullFeedParser::setupLesson",
            "Number of longterm batches is" + lesson.getLongTermBatchesSize()
        )
    }
}