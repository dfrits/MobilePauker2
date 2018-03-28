package com.daniel.mobilepauker2.model.xmlsupport;

import android.util.SparseLongArray;
import android.util.Xml;

import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.pauker_native.Batch;
import com.daniel.mobilepauker2.model.pauker_native.Card;
import com.daniel.mobilepauker2.model.pauker_native.ComponentOrientation;
import com.daniel.mobilepauker2.model.pauker_native.Font;
import com.daniel.mobilepauker2.model.pauker_native.Lesson;
import com.daniel.mobilepauker2.model.pauker_native.LongTermBatch;
import com.daniel.mobilepauker2.utils.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.EOFException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlashCardXMLPullFeedParser extends FlashCardBaseFeedParser {

    public FlashCardXMLPullFeedParser(URL feedUrl) {
        super(feedUrl);
    }

    public Lesson parse() throws EOFException {

        List<FlashCard> flashCards = null;
        XmlPullParser parser = Xml.newPullParser();
        int batchCount = 0;
        String description = "No Description";

        try {
            // auto-detect the encoding from the stream
            parser.setInput(this.getInputStream(), null);
            int eventType = parser.getEventType();
            FlashCard currentFlashCard = null;

            boolean SIDEA = false;
            boolean SIDEB = false;

            boolean done = false;
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {

                String name;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        flashCards = new ArrayList<>();
                        break;

                    case XmlPullParser.START_TAG:
                        name = parser.getName();

                        if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.LESSON)) {
                            String lessonFormatString = parser.getAttributeValue(null, "LessonFormat");
                            if (lessonFormatString != null) {
                                float lessonFormat = Float.parseFloat(lessonFormatString);
                                Log.d("FlashCardXMLPullFeedParser::parse", "Lesson format is " + lessonFormat);
                            }
                        } else if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.DESCRIPTION)) {
                            description = parser.nextText();
                            if (description == null) {
                                description = "No description";
                            }
                        } else if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.CARD)) {
                            currentFlashCard = new FlashCard();

                        } else if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.BATCH)) {
                            batchCount++;

                        } else if (currentFlashCard != null) {
                            currentFlashCard.setInitialBatch(batchCount - 1); // This line is repeated many times!
                            if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.FRONTSIDE) || name.equalsIgnoreCase(FlashCardXMLPullFeedParser.REVERSESIDE)) {
                                String orientation = parser.getAttributeValue(null, "Orientation");
                                String repeatByTyping = parser.getAttributeValue(null, "RepeatByTyping");
                                String learnedTimestamp = parser.getAttributeValue(null, "LearnedTimestamp");

                                currentFlashCard.setRepeatByTyping(repeatByTyping.equals("true"));

                                if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.FRONTSIDE)) {
                                    SIDEA = true;
                                    SIDEB = false;

                                    if (orientation != null) {
                                        currentFlashCard.getFrontSide().setOrientation(new ComponentOrientation("LTR"));
                                    }

                                    if (learnedTimestamp != null) {
                                        long l = Long.parseLong(learnedTimestamp.trim());
                                        currentFlashCard.getFrontSide().setLearnedTimestamp(l);
                                    }

                                } else if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.REVERSESIDE)) {
                                    SIDEA = false;
                                    SIDEB = true;

                                    if (orientation != null) {
                                        currentFlashCard.getReverseSide().setOrientation(new ComponentOrientation("LTR"));
                                    }

                                    if (learnedTimestamp != null) {
                                        long l = Long.parseLong(learnedTimestamp.trim());
                                        currentFlashCard.getReverseSide().setLearnedTimestamp(l);
                                    }
                                }

                                //Log.d("FlashCardXMLPullFeedParser::parse", "orientation=" + orientation);

                            } else if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.TEXT)) {
                                if (SIDEA) {
                                    currentFlashCard.setSideAText(parser.nextText());
                                    //Log.d("FlashCardXMLPullFeedParser::parse","sideA=" + currentFlashCard.getSideAText());

                                } else if (SIDEB) {
                                    currentFlashCard.setSideBText(parser.nextText());

                                } else {
                                    currentFlashCard.setSideAText("Empty");
                                    currentFlashCard.setSideBText("Empty");
                                }


                                //Log.d("FlashCardXMLPullFeedParser::parse","sideB=" + currentFlashCard.getSideBText());
                            } else if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.FONT)) {
                                String background = parser.getAttributeValue(null, "Background");
                                String bold = parser.getAttributeValue(null, "Bold");
                                String family = parser.getAttributeValue(null, "Family");
                                String foreground = parser.getAttributeValue(null, "Foreground");
                                String italic = parser.getAttributeValue(null, "Italic");
                                String size = parser.getAttributeValue(null, "Size");

                                // Set to defaults if null
                                if (background == null) {
                                    background = "-1";
                                }

                                if (bold == null) {
                                    bold = "false";
                                }

                                if (family == null) {
                                    family = "Dialog";
                                }

                                if (foreground == null) {
                                    foreground = "-16777216";
                                }

                                if (italic == null) {
                                    italic = "false";
                                }

                                if (size == null) {
                                    size = "12";
                                }

                                if (SIDEA) {
                                    currentFlashCard.getFrontSide().setFont(new Font(background,
                                            bold,
                                            family,
                                            foreground,
                                            italic,
                                            size));
                                } else if (SIDEB) {
                                    currentFlashCard.getReverseSide().setFont(new Font(background,
                                            bold,
                                            family,
                                            foreground,
                                            italic,
                                            size));
                                }
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase(CARD) && currentFlashCard != null) {
                            flashCards.add(currentFlashCard);
                        } else if (name.equalsIgnoreCase(LESSON)) {
                            done = true;
                        }
                        break;
                }
                eventType = parser.next();
            }

            return setupLesson(flashCards, description);

        } catch (Exception e) {
            Log.e("FlashCardXMLPullFeedParser:parse()", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Lesson setupLesson(List<FlashCard> flashCardList, String description) {
        Lesson newLesson = new Lesson();
        Batch summaryBatch = newLesson.getSummaryBatch();

        newLesson.setDescription(description);

        for (int i = 0; i < flashCardList.size(); i++) {
            FlashCard flashCard = flashCardList.get(i);

            if (flashCard.getInitialBatch() < 3) {
                flashCard.setLearned(false);
            } else {
                flashCard.getFrontSide().setLearned(true); // Warning using flash card set learned here sets the learned timestamp!
            }

            if (newLesson.getNumberOfLongTermBatches() < (flashCard.getInitialBatch() - 2)) {
                Log.d("FC~XMLPullFeedParser::setupLesson", "num of long term batches=" + newLesson.getNumberOfLongTermBatches());
                Log.d("FC~XMLPullFeedParser::setupLesson", "card initla batch=" + flashCard.getInitialBatch());

                int batchesToAdd = (flashCard.getInitialBatch() - 2) - newLesson.getNumberOfLongTermBatches();
                Log.d("FC~XMLPullFeedParser::setupLesson", "batchsToAdd" + batchesToAdd);

                for (int j = 0; j < batchesToAdd; j++) {
                    newLesson.addLongTermBatch();
                }
            }

            Batch batch;
            if (flashCard.isLearned()) {
                // must put the card into the corresponding long
                // term batch
                batch = newLesson.getLongTermBatch(
                        flashCard.getInitialBatch() - 3);
            } else {
                // must put the card into the unlearned batch
                batch = newLesson.getUnlearnedBatch();
            }
            batch.addCard(flashCard);
            summaryBatch.addCard(flashCard);

            //Log.d("FlashCardXMLPullFeedParser::setupLesson" , "Added card to batch " + flashCard.getInitialBatch());
        }

        //newLesson.trim();
        newLesson.refreshExpiration();
        printLessonToDebug(newLesson);

        return newLesson;
    }

    /**
     * Findet das nächste Ablaufdatum. Falls keines gefunden wird, wird {@link Long#MIN_VALUE}
     * zurückgegeben.
     * @return Eine Map mit dem frühesten Ablaufdatum <b>(index = 0)</b> und die Anzahl abgelaufener
     * Karten (<b>index = 2)</b>
     * @throws EOFException .
     */
    public SparseLongArray getNextExpireDate() throws EOFException {

        final XmlPullParser parser = Xml.newPullParser();
        final SparseLongArray map = new SparseLongArray(2);
        final long currentTimestamp = System.currentTimeMillis();

        try {
            // auto-detect the encoding from the stream
            parser.setInput(this.getInputStream(), null);
            int eventType = parser.getEventType();
            long nextExpireTimeStamp = Long.MIN_VALUE;
            int batchCount = 0;
            long expiredCards = 0;

            boolean done = false;
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {

                String name;
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();

                        if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.LESSON)) {
                            String lessonFormatString = parser.getAttributeValue(null, "LessonFormat");
                            if (lessonFormatString != null) {
                                float lessonFormat = Float.parseFloat(lessonFormatString);
                                Log.d("FlashCardXMLPullFeedParser::parse", "Lesson format is " + lessonFormat);
                            }
                        } else if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.BATCH)) {
                            batchCount++;
                        } else if (name.equalsIgnoreCase(FlashCardXMLPullFeedParser.FRONTSIDE)
                                || name.equalsIgnoreCase(FlashCardXMLPullFeedParser.REVERSESIDE)) {
                            String learnedTimestamp = parser.getAttributeValue(null, "LearnedTimestamp");

                            if (learnedTimestamp != null) {
                                double factor = Math.pow(Math.E, batchCount - 4);
                                long expirationTime = (long) (LongTermBatch.getExpirationUnit() * factor);
                                try {
                                    long expireTimeStamp = Long.parseLong(learnedTimestamp) + expirationTime;
                                    if (nextExpireTimeStamp == Long.MIN_VALUE
                                            || expireTimeStamp < nextExpireTimeStamp) {
                                        nextExpireTimeStamp = expireTimeStamp;
                                    }

                                    long diff = currentTimestamp - Long.parseLong(learnedTimestamp);
                                    if (diff > expirationTime) {
                                        expiredCards++;
                                    }

                                } catch (NumberFormatException ignored) {}
                            }

                        }
                        break;

                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase(LESSON)) {
                            done = true;
                        }
                        break;
                }
                eventType = parser.next();
            }

            map.put(0, nextExpireTimeStamp);
            map.put(1, expiredCards);
            return map;

        } catch (Exception e) {
            Log.e("FlashCardXMLPullFeedParser:parse()", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void printLessonToDebug(Lesson lesson) {
        Collection<Card> cards = lesson.getLearnedCards();
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of learned cards is " + cards.size());

        cards = lesson.getExpiredCards();
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of expired cards is " + cards.size());

        cards = lesson.getShortTermList();
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of shortTerm cards is " + cards.size());

        cards = lesson.getCards();
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of all cards is " + cards.size());

        cards = lesson.getUltraShortTermList();
        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Size of ultraShortTerm cards is " + cards.size());

        Log.d("FlashCardXMLPullFeedParser::setupLesson", "Number of longterm batches is" + lesson.getLongTermBatches().size());

    }
}
