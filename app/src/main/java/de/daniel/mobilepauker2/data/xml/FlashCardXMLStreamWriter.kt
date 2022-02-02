package de.daniel.mobilepauker2.data.xml

import android.util.Xml
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.data.SaveResult
import de.daniel.mobilepauker2.lesson.Lesson
import de.daniel.mobilepauker2.lesson.batch.Batch
import de.daniel.mobilepauker2.lesson.card.Card
import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.utils.Log
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

class FlashCardXMLStreamWriter(
    private val lessonFile: File,
    private val isNewFile: Boolean,
    val lesson: Lesson?
) {

    fun writeLesson(): SaveResult {
        var tmpFile: File? = null
        val fileOutputStream = if (isNewFile) {
            FileOutputStream(lessonFile)
        } else {
            tmpFile = getTempFile("new")
            FileOutputStream(tmpFile)
        }
        val gzipOutputStream = GZIPOutputStream(fileOutputStream)

        return try {
            lessonToXML(gzipOutputStream)
            if (!isNewFile) {
                moveFile(tmpFile)
            } else SaveResult(true)
        } catch (e: OutOfMemoryError) {
            SaveResult(false, e.message)
        }
    }

    @Throws(OutOfMemoryError::class)
    private fun moveFile(tmpFile: File?): SaveResult {
        val tmpOldFile = getTempFile("old")
        if (lessonFile.renameTo(tmpOldFile)) {
            if (tmpFile!!.renameTo(lessonFile)) {
                if (!tmpOldFile.delete()) {
                    return SaveResult(false, null, R.string.error_removing_file)
                }
            } else {
                return SaveResult(false, null, R.string.error_moving_file)
            }
        } else {
            return SaveResult(false, null, R.string.error_moving_file)
        }
        return SaveResult(true)
    }

    private fun getTempFile(suffix: String): File {
        var tempFile = File("${lessonFile.path}$suffix")
        var i = 1
        while (tempFile.exists()) {
            tempFile = File("${lessonFile.path}$suffix $i")
            i++
        }
        return tempFile
    }

    private fun lessonToXML(outputStream: OutputStream) {
        val serializer = Xml.newSerializer()

        if (lesson == null) return

        try {
            serializer.setOutput(outputStream, "UTF-8")
            serializer.startDocument("UTF-8", true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            serializer.startTag("", "Lesson")
            serializer.attribute("", "LessonFormat", "1.7")
            serializer.startTag("", "Description")
            serializer.text(lesson.description)
            serializer.endTag("", "Description")
            serializer.startTag("", "Batch")
            val unlearnedBatch: Batch = lesson.unlearnedBatch
            for (card in unlearnedBatch.cards) {
                if (!serializeCard(card, serializer)) {
                    Log.w("FlashCardXMLStreamWriter::writeXML", "Failed to serialise card")
                }
            }
            serializer.endTag("", "Batch")

            // USTM
            serializer.startTag("", "Batch")
            serializer.endTag("", "Batch")

            //STM
            serializer.startTag("", "Batch")
            serializer.endTag("", "Batch")

            // Add all long term batches
            for (longTermBatch in lesson.longTermBatches) {
                serializer.startTag("", "Batch")
                for (card in longTermBatch.cards) {
                    if (!serializeCard(card, serializer)) {
                        Log.w("FlashCardXMLStreamWriter::writeXML", "Failed to serialise card")
                    }
                }
                serializer.endTag("", "Batch")
            }
            serializer.endTag("", "Lesson")
            serializer.endDocument()
            serializer.flush()
            outputStream.close()
        } catch (e: Exception) {
            Log.e("FlashCardXMLStreamWriter::writeXML", "Exception caught")
        }
    }

    private fun serializeCard(card: Card, serializer: XmlSerializer): Boolean {
        try {
            serializer.startTag("", "Card")

            // Frontside
            serializer.startTag("", "FrontSide")
            if (card.isLearned) {
                val timeStamp: Long = card.learnedTimestamp
                serializer.attribute("", "LearnedTimestamp", "$timeStamp")
            }
            try {
                serializer.attribute(
                    "",
                    "Orientation",
                    card.frontSide.orientation.orientation
                )
                serializer.attribute(
                    "",
                    "RepeatByTyping",
                    "${card.isRepeatedByTyping}"
                )
            } catch (e: IOException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Frontsideoptions!"
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Frontsideoptions!"
                )
            } catch (e: IllegalStateException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Frontsideoptions!"
                )
            } catch (e: NullPointerException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Frontsideoptions!"
                )
            }
            serializer.startTag("", "Text")
            serializer.text(card.frontSide.text)
            serializer.endTag("", "Text")
            var font: Font?
            try {
                font = card.frontSide.font
                if (font != null) {
                    serializer.startTag("", "Font")
                    serializer.attribute(
                        "",
                        "Background",
                        "${font.backgroundColor}"
                    )
                    serializer.attribute("", "Bold", "${font.isBold}")
                    serializer.attribute("", "Family", font.family)
                    serializer.attribute(
                        "",
                        "Foreground",
                        java.lang.String.valueOf(font.textColor)
                    )
                    serializer.attribute("", "Italic", "${font.isItalic}")
                    serializer.attribute("", "Size", "${font.textSize}")
                    serializer.endTag("", "Font")
                }
            } catch (e: IOException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Font of Frontside!"
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Font of Frontside!"
                )
            } catch (e: IllegalStateException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Font of Frontside!"
                )
            } catch (e: NullPointerException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Font of Frontside!"
                )
            }
            serializer.endTag("", "FrontSide")

            // ReverseSide
            serializer.startTag("", "ReverseSide")
            try {
                serializer.attribute(
                    "",
                    "Orientation",
                    card.reverseSide.orientation.orientation
                )
                serializer.attribute(
                    "",
                    "RepeatByTyping",
                    "${card.isRepeatedByTyping}"
                )
            } catch (e: IOException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Backsideoptions!"
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Backsideoptions!"
                )
            } catch (e: IllegalStateException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Backsideoptions!"
                )
            } catch (e: NullPointerException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Backsideoptions!"
                )
            }

            // BugFix*******************
            // 11/12/2011 - bf
            // include the reverse timestamp in the xml
            val reverseTimeStamp: Long = card.reverseSide.learnedTimestamp
            if (reverseTimeStamp != 0L) {
                serializer.attribute(
                    "",
                    "LearnedTimestamp",
                    "$reverseTimeStamp"
                )
            }

            //EndofBugFix*****************
            serializer.startTag("", "Text")
            serializer.text(card.reverseSide.text)
            serializer.endTag("", "Text")
            try {
                font = card.reverseSide.font
                if (font != null) {
                    serializer.startTag("", "Font")
                    serializer.attribute(
                        "",
                        "Background",
                        "${font.backgroundColor}"
                    )
                    serializer.attribute("", "Bold", "${font.isBold}")
                    serializer.attribute("", "Family", font.family)
                    serializer.attribute(
                        "",
                        "Foreground",
                        "${font.textColor}"
                    )
                    serializer.attribute("", "Italic", "${font.isItalic}")
                    serializer.attribute("", "Size", "${font.textSize}")
                    serializer.endTag("", "Font")
                }
            } catch (e: IOException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Font of Backside!"
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Font of Backside!"
                )
            } catch (e: IllegalStateException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Font of Backside!"
                )
            } catch (e: NullPointerException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "exception while serialising Font of Backside!"
                )
            }
            serializer.endTag("", "ReverseSide")
        } catch (e: Exception) {
            Log.e("FlashCardXMLStreamWriter::serialiseCard", "exception while serialising card!")
            return false
        } finally {
            try {
                serializer.endTag("", "Card")
            } catch (e: IOException) {
                Log.e(
                    "FlashCardXMLStreamWriter::serialiseCard",
                    "Error of serializer. Can't end card"
                )
            }
        }
        return true
    }
}