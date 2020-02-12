package com.daniel.mobilepauker2.core.model.xmlsupport

import android.util.Xml
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.pauker_native.Card
import com.daniel.mobilepauker2.pauker_native.Font
import com.daniel.mobilepauker2.pauker_native.Lesson
import com.daniel.mobilepauker2.pauker_native.Log
import org.xmlpull.v1.XmlSerializer
import java.io.*
import java.util.zip.GZIPOutputStream

object FlashCardXMLStreamWriter {

    //TODO This sould de in a class of its own or in the Flash card xml stream writer...
    @Throws(SecurityException::class)
    fun saveLesson() {
        val modelManager: ModelManager = ModelManager.Companion.instance()
        val paukerManager: PaukerManager = PaukerManager.Companion.instance()
        if (modelManager.isLessonNotNew) { // Neuen temporären Pfad, damit alte erstmal bestehen bleibt
            val name = "neu_" + modelManager.filePath.name
            val path = modelManager.filePath.parent
            val newxmlfile = File(path, name)
            Log.d(
                "ModelManager::saveLesson",
                "Filename = " + paukerManager.currentFileName
            )
            Log.d(
                "ModelManager::saveLesson",
                "Directory= " + paukerManager.fileAbsolutePath
            )
            Log.d(
                "ModelManager::saveLesson",
                "Directory= " + newxmlfile.absolutePath
            )
            val gzipOutputStream: GZIPOutputStream
            try {
                if (!File(path).exists() && !File(path).mkdirs()) {
                    return
                }
                if (!newxmlfile.exists() && !newxmlfile.createNewFile()) {
                    return
                }
                val fos = FileOutputStream(newxmlfile)
                gzipOutputStream = GZIPOutputStream(fos)
                var isRenamed = false
                if (writeXML(modelManager.lesson, gzipOutputStream)) {
                    isRenamed = newxmlfile.renameTo(modelManager.filePath)
                }
                newxmlfile.delete()
                if (!isRenamed) {
                    throw SecurityException("Saving not possible. Unkown error.")
                }
                gzipOutputStream.close()
            } catch (e: FileNotFoundException) {
                Log.e(
                    "ModelManager::saveLesson",
                    "can't create FileOutputStream"
                )
                throw RuntimeException(e)
            } catch (e: IOException) {
                Log.e(
                    "ModelManager::saveLesson",
                    "exception in saveLesson() method"
                )
                throw RuntimeException(e)
            }
        }
    }

    private fun writeXML(lesson: Lesson?, outputStream: OutputStream): Boolean {
        val serializer = Xml.newSerializer()
        if (serializer == null) {
            Log.e(
                "FlashCardXMLStreamWriter::writeXML",
                "Serializer is null"
            )
            return false // TODO error code
        }
        return try {
            serializer.setOutput(outputStream, "UTF-8")
            serializer.startDocument("UTF-8", true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            serializer.startTag("", "Lesson")
            serializer.attribute("", "LessonFormat", "1.7")
            serializer.startTag("", "Description")

            lesson?.description?.let { serializer.text(lesson.description) }

            serializer.endTag("", "Description")
            serializer.startTag("", "Batch")
            val unlearnedBatch = lesson?.unlearnedBatch
            unlearnedBatch?.cards?.let {
                for (card in it) {
                    if (!serializeCard(card, serializer)) {
                        Log.w(
                            "FlashCardXMLStreamWriter::writeXML",
                            "Failed to serialise card"
                        )
                    }
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

            lesson?.longTermBatches?.let {
                for (longTermBatch in it) {
                    serializer.startTag("", "Batch")
                    for (card in longTermBatch.cards!!) {
                        if (!serializeCard(card, serializer)) {
                            Log.w(
                                "FlashCardXMLStreamWriter::writeXML",
                                "Failed to serialise card"
                            )
                        }
                    }
                    serializer.endTag("", "Batch")
                }
            }

            serializer.endTag("", "Lesson")
            serializer.endDocument()
            serializer.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            Log.e(
                "FlashCardXMLStreamWriter::writeXML",
                "Exception caught"
            )
            false
        }
    }

    private fun serializeCard(card: Card?, serializer: XmlSerializer?): Boolean {
        if (serializer == null) {
            return false
        }
        if (!isCardValid(card)) {
            // Be ultra safe as if anything goes wrong here we can lose the file on the sdcard
            // as the xml may not be well formed
            Log.e(
                "FlashCardXMLStreamWriter::serializeCard",
                "Card not valid"
            )
            return false
        }
        try {
            serializer.startTag("", "Card")
            // Frontside
            serializer.startTag("", "FrontSide")
            if (card!!.isLearned) {
                val timeStamp = card.learnedTimestamp
                serializer.attribute("", "LearnedTimestamp", java.lang.Long.toString(timeStamp))
            }
            try {
                serializer.attribute(
                    "",
                    "Orientation",
                    card.frontSide.orientation?.orientation.toString()
                )
                serializer.attribute("", "RepeatByTyping", card.isRepeatedByTyping.toString())
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
            if (card.frontSide.text != null) {
                serializer.text(card.frontSide.text)
            }
            serializer.endTag("", "Text")
            var font: Font?
            try {
                font = card.frontSide.font
                if (font != null) {
                    serializer.startTag("", "Font")
                    serializer.attribute("", "Background", font.backgroundColor.toString())
                    serializer.attribute("", "Bold", font.isBold.toString())
                    serializer.attribute("", "Family", font.family)
                    serializer.attribute("", "Foreground", font.textColor.toString())
                    serializer.attribute("", "Italic", font.isItalic.toString())
                    serializer.attribute("", "Size", font.textSize.toString())
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
                    card.reverseSide.orientation?.orientation.toString()
                )
                serializer.attribute("", "RepeatByTyping", card.isRepeatedByTyping.toString())
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
            val reverseTimeStamp = card.reverseSide.learnedTimestamp
            if (reverseTimeStamp != 0L) {
                serializer.attribute(
                    "",
                    "LearnedTimestamp",
                    java.lang.Long.toString(reverseTimeStamp)
                )
            }
            //EndofBugFix*****************
            serializer.startTag("", "Text")
            if (card.reverseSide.text != null) {
                serializer.text(card.reverseSide.text)
            }
            serializer.endTag("", "Text")
            try {
                font = card.reverseSide.font
                if (font != null) {
                    serializer.startTag("", "Font")
                    serializer.attribute("", "Background", font.backgroundColor.toString())
                    serializer.attribute("", "Bold", font.isBold.toString())
                    serializer.attribute("", "Family", font.family)
                    serializer.attribute("", "Foreground", font.textColor.toString())
                    serializer.attribute("", "Italic", font.isItalic.toString())
                    serializer.attribute("", "Size", font.textSize.toString())
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
            Log.e(
                "FlashCardXMLStreamWriter::serialiseCard",
                "exception while serialising card!"
            )
            return false
            //throw new RuntimeException(e);
// TODO carry on at all costs in case we lose the pack
// TODO each serialiser call should have its own try catch
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

    private fun isCardValid(card: Card?): Boolean {
        return if (card == null) {
            false
        } else card.frontSide != null &&
                card.reverseSide != null
    }
}