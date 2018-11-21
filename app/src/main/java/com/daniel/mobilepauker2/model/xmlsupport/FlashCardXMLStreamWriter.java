package com.daniel.mobilepauker2.model.xmlsupport;

import android.util.Xml;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.pauker_native.Batch;
import com.daniel.mobilepauker2.model.pauker_native.Card;
import com.daniel.mobilepauker2.model.pauker_native.Font;
import com.daniel.mobilepauker2.model.pauker_native.Lesson;
import com.daniel.mobilepauker2.model.pauker_native.LongTermBatch;
import com.daniel.mobilepauker2.utils.Log;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class FlashCardXMLStreamWriter {

    //TODO This sould de in a class of its own or in the Flash cardd xml stream writer...
    public static void saveLesson() {
        final ModelManager modelManager = ModelManager.instance();
        final PaukerManager paukerManager = PaukerManager.instance();
        if (modelManager.isLessonNotNew()) {
            // Neuen temporären Pfad, damit alte erstmal bestehen bleibt
            String name = "neu_" + modelManager.getFilePath().getName();
            String path = modelManager.getFilePath().getParent();
            File newxmlfile = new File(path, name);

            Log.d("ModelManager::saveLesson", "Filename = " + paukerManager.getCurrentFileName());
            Log.d("ModelManager::saveLesson", "Directory= " + paukerManager.getFileAbsolutePath());
            Log.d("ModelManager::saveLesson", "Directory= " + newxmlfile.getAbsolutePath());

            GZIPOutputStream gzipOutputStream;
            try {
                if (!new File(path).exists() && !new File(path).mkdirs()) {
                    return;
                }
                if (!newxmlfile.exists() && !newxmlfile.createNewFile()) {
                    return;
                }

                FileOutputStream fos = new FileOutputStream(newxmlfile);
                gzipOutputStream = new GZIPOutputStream(fos);
                boolean isRenamed = false;
                if (FlashCardXMLStreamWriter.writeXML(modelManager.getLesson(), gzipOutputStream)) {
                    isRenamed = newxmlfile.renameTo(modelManager.getFilePath());
                }
                if (isRenamed) {
                    //noinspection ResultOfMethodCallIgnored
                    newxmlfile.delete();
                }

                gzipOutputStream.close();
            } catch (FileNotFoundException e) {
                Log.e("ModelManager::saveLesson", "can't create FileOutputStream");
                throw new RuntimeException(e);
            } catch (IOException e) {
                Log.e("ModelManager::saveLesson", "exception in saveLesson() method");
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean writeXML(Lesson lesson, OutputStream outputStream) {

        XmlSerializer serializer = Xml.newSerializer();

        if (serializer == null) {
            Log.e("FlashCardXMLStreamWriter::writeXML", "Serializer is null");
            return false; // TODO error code
        }

        try {
            serializer.setOutput(outputStream, "UTF-8");
            serializer.startDocument("UTF-8", true);

            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startTag("", "Lesson");
            serializer.attribute("", "LessonFormat", "1.7");

            serializer.startTag("", "Description");

            if (lesson.getDescription() != null) {
                serializer.text(lesson.getDescription());
            }

            serializer.endTag("", "Description");

            serializer.startTag("", "Batch");

            Batch unlearnedBatch = lesson.getUnlearnedBatch();

            for (Card card : unlearnedBatch.getCards()) {
                if (!serializeCard(card, serializer)) {
                    Log.w("FlashCardXMLStreamWriter::writeXML", "Failed to serialise card");
                }
            }
            serializer.endTag("", "Batch");

            // USTM
            serializer.startTag("", "Batch");
            serializer.endTag("", "Batch");

            //STM
            serializer.startTag("", "Batch");
            serializer.endTag("", "Batch");

            // Add all long term batches

            for (LongTermBatch longTermBatch : lesson.getLongTermBatches()) {
                serializer.startTag("", "Batch");
                for (Card card : longTermBatch.getCards()) {
                    serializeCard(card, serializer);
                }
                serializer.endTag("", "Batch");
            }

            serializer.endTag("", "Lesson");
            serializer.endDocument();

            serializer.flush();
            outputStream.close();

            return true;
        } catch (Exception e) {
            Log.e("FlashCardXMLStreamWriter::writeXML", "Exception caught");
            return false;
        }
    }

    private static boolean serializeCard(Card card, XmlSerializer serializer) {
        if (serializer == null) {
            return false;
        }

        if (!isCardValid(card)) {
            // Be ultra safe as if anything goes wrong here we can lose the file on the sdcard
            // as the xml may not be well formed
            Log.e("FlashCardXMLStreamWriter::serializeCard", "Card not valid");
            return false;
        }

        try {
            serializer.startTag("", "Card");

            // Frontside
            serializer.startTag("", "FrontSide");

            if (card.isLearned()) {
                long timeStamp = card.getLearnedTimestamp();

                serializer.attribute("", "LearnedTimestamp", Long.toString(timeStamp));
            }

            serializer.attribute("", "Orientation", String.valueOf(card.getFrontSide().getOrientation().getOrientation()));
            serializer.attribute("", "RepeatByTyping", String.valueOf(card.getFrontSide().isRepeatedByTyping()));

            serializer.startTag("", "Text");

            if (card.getFrontSide().getText() != null) {
                serializer.text(card.getFrontSide().getText());
            }

            serializer.endTag("", "Text");

            Font font = card.getFrontSide().getFont();
            font = font == null ? new Font() : font;
            serializer.startTag("", "Font");
            serializer.attribute("", "Background", String.valueOf(font.getBackgroundColor()));
            serializer.attribute("", "Bold", String.valueOf(font.isBold()));
            serializer.attribute("", "Family", font.getFamily());
            serializer.attribute("", "Foreground", String.valueOf(font.getTextColor()));
            serializer.attribute("", "Italic", String.valueOf(font.isItalic()));
            serializer.attribute("", "Size", String.valueOf(font.getTextSize()));
            serializer.endTag("", "Font");

            serializer.endTag("", "FrontSide");

            // ReverseSide
            serializer.startTag("", "ReverseSide");
            serializer.attribute("", "Orientation", String.valueOf(card.getFrontSide().getOrientation().getOrientation()));
            serializer.attribute("", "RepeatByTyping", String.valueOf(card.getFrontSide().isRepeatedByTyping()));

            // BugFix*******************
            // 11/12/2011 - bf
            // include the reverse timestamp in the xml

            long reverseTimeStamp = card.getReverseSide().getLearnedTimestamp();
            if (reverseTimeStamp != 0) {
                serializer.attribute("", "LearnedTimestamp", Long.toString(reverseTimeStamp));
            }

            //EndofBugFix*****************

            serializer.startTag("", "Text");
            if (card.getReverseSide().getText() != null) {
                serializer.text(card.getReverseSide().getText());
            }
            serializer.endTag("", "Text");

            font = card.getReverseSide().getFont();
            font = font == null ? new Font() : font;
            serializer.startTag("", "Font");
            serializer.attribute("", "Background", String.valueOf(font.getBackgroundColor()));
            serializer.attribute("", "Bold", String.valueOf(font.isBold()));
            serializer.attribute("", "Family", font.getFamily());
            serializer.attribute("", "Foreground", String.valueOf(font.getTextColor()));
            serializer.attribute("", "Italic", String.valueOf(font.isItalic()));
            serializer.attribute("", "Size", String.valueOf(font.getTextSize()));
            serializer.endTag("", "Font");

            serializer.endTag("", "ReverseSide");

            serializer.endTag("", "Card");
        } catch (Exception e) {
            Log.e("FlashCardXMLStreamWriter::serialiseCard", "exception while serialising card!");
            return false;
            //throw new RuntimeException(e);
            // TODO carry on at all costs in case we lose the pack
            // TODO each serialiser call should have its own try catch
        }

        return true;
    }

    private static boolean isCardValid(Card card) {
        if (card == null) {
            return false;
        } else return card.getFrontSide() != null &&
                card.getReverseSide() != null;
    }

}
