package com.daniel.mobilepauker2.model.xmlsupport;

import android.util.Xml;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.pauker_native.Batch;
import com.daniel.mobilepauker2.model.pauker_native.Card;
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
        if (modelManager.isLessonNew()) {
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
                if (FlashCardXMLStreamWriter.writeXML(modelManager.getLesson(), gzipOutputStream)) {
                    newxmlfile.renameTo(modelManager.getFilePath());
                }
                newxmlfile.delete();

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

    public static boolean writeXML(Lesson lesson, OutputStream outputStream) {

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

            serializer.attribute("", "Orientation", "LTR"); // TODO this should not default
            serializer.attribute("", "RepeatByTyping", "false"); // TODO this should not default

            serializer.startTag("", "Text");

            if (card.getFrontSide().getText() != null) {
                serializer.text(card.getFrontSide().getText());
            }

            serializer.endTag("", "Text");

            if (isFrontSideFontValid(card)) {
                serializer.startTag("", "Font");
                serializer.attribute("", "Background", card.getFrontSide().getFont().getBackgroundColor());
                serializer.attribute("", "Bold", card.getFrontSide().getFont().getBold());
                serializer.attribute("", "Family", card.getFrontSide().getFont().getFamily());
                serializer.attribute("", "Foreground", card.getFrontSide().getFont().getForeground());
                serializer.attribute("", "Italic", card.getFrontSide().getFont().getItalic());
                serializer.attribute("", "Size", card.getFrontSide().getFont().getSize());
                serializer.endTag("", "Font");
            } else {
                Log.w("FlashCardXMLStreamWriter::serialiseCard", "card front font null");
            }

            serializer.endTag("", "FrontSide");

            // ReverseSide
            serializer.startTag("", "ReverseSide");
            serializer.attribute("", "Orientation", "LTR"); // TODO defaults
            serializer.attribute("", "RepeatByTyping", "false"); // TODO defaults

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


            if (isReverseSideFontValid(card)) {
                serializer.startTag("", "Font");
                serializer.attribute("", "Background", card.getReverseSide().getFont().getBackgroundColor());
                serializer.attribute("", "Bold", card.getReverseSide().getFont().getBold());
                serializer.attribute("", "Family", card.getReverseSide().getFont().getFamily());
                serializer.attribute("", "Foreground", card.getReverseSide().getFont().getForeground());
                serializer.attribute("", "Italic", card.getReverseSide().getFont().getItalic());
                serializer.attribute("", "Size", card.getReverseSide().getFont().getSize());
                serializer.endTag("", "Font");
            } else {
                Log.w("FlashCArdXMLStreamWriter::serialiseCard", "card reverse font null");
            }

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

    private static boolean isFrontSideFontValid(Card card) {
        boolean fontBoolean = true;

        if (card.getFrontSide().getFont() == null) {
            fontBoolean = false;
        } else {

            if (card.getFrontSide().getFont().getBackgroundColor() == null ||
                    card.getFrontSide().getFont().getBackgroundColor() == null ||
                    card.getFrontSide().getFont().getBold() == null ||
                    card.getFrontSide().getFont().getFamily() == null ||
                    card.getFrontSide().getFont().getForeground() == null ||
                    card.getFrontSide().getFont().getItalic() == null ||
                    card.getFrontSide().getFont().getSize() == null
                    ) {
                fontBoolean = false;
            }
        }

        return fontBoolean;
    }

    private static boolean isReverseSideFontValid(Card card) {
        boolean fontBoolean = true;

        if (card.getReverseSide().getFont() == null) {
            fontBoolean = false;
        } else {

            if (card.getReverseSide().getFont().getBackgroundColor() == null ||
                    card.getReverseSide().getFont().getBackgroundColor() == null ||
                    card.getReverseSide().getFont().getBold() == null ||
                    card.getReverseSide().getFont().getFamily() == null ||
                    card.getReverseSide().getFont().getForeground() == null ||
                    card.getReverseSide().getFont().getItalic() == null ||
                    card.getReverseSide().getFont().getSize() == null
                    ) {
                fontBoolean = false;
            }
        }

        return fontBoolean;
    }

    private static boolean isCardValid(Card card) {
        if (card == null) {
            return false;
        } else if (card.getFrontSide() == null ||
                card.getReverseSide() == null) {
            return false;
        }
        return true;
    }

}
