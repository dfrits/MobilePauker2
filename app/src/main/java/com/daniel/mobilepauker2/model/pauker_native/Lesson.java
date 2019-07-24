package com.daniel.mobilepauker2.model.pauker_native;

/*
 * Lesson.java
 *
 * Created on 5. Juni 2001, 22:14
 */

import com.daniel.mobilepauker2.utils.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Lesson consists of cards and the status information for each card.
 * @author Ronny.Standtke@gmx.net
 */
public class Lesson {

    private static final Logger LOGGER =
            Logger.getLogger(Lesson.class.getName());
    private String description;
    // all card batches
    private final SummaryBatch summaryBatch;
    private final Batch unlearnedBatch;
    private final List<Card> ultraShortTermList;
    private final List<Card> shortTermList;
    private final List<LongTermBatch> longTermBatches;
    private Random random;

    /**
     * Creates new Lesson
     */
    public Lesson() {
        description = "";
        unlearnedBatch = new Batch(null);
        ultraShortTermList = new ArrayList<>();
        shortTermList = new ArrayList<>();
        longTermBatches = new ArrayList<>();
        // !!! create summaryBatch at the end, because it uses the reference to
        // this lesson and expects it to be completely initialized !!!
        summaryBatch = new SummaryBatch(this);
    }

    /**
     * sets the description of this lesson
     * @param description Describes the lesson. The author should include what
     *                    the lesson is all about, when it was created and information abou how to
     *                    reach him/her.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * returns the lesson description
     * @return the lesson description
     */
    public String getDescription() {
        return description;
    }

    /**
     * adds a new card to this lesson
     * @param card the new card
     */
    public void addCard(Card card) {
        summaryBatch.addCard(card);
        unlearnedBatch.addCard(card);
    }

    /**
     * returns the summary batch of this lesson
     * @return the summary batch of this lesson
     */
    public SummaryBatch getSummaryBatch() {
        return summaryBatch;
    }

    /**
     * returns the batch with the unlearned cards
     * @return the batch with the unlearned cards
     */
    public Batch getUnlearnedBatch() {
        return unlearnedBatch;
    }

    /**
     * returns the list of cards that are in the ultra-short-term memory
     * @return the list of cards that are in the ultra-short-term memory
     */
    public List<Card> getUltraShortTermList() {
        return ultraShortTermList;
    }

    /**
     * returns the list of cards that are in the short-term memory
     * @return the list of cards that are in the short-term memory
     */
    public List<Card> getShortTermList() {
        return shortTermList;
    }

    /**
     * adds a new batch to this lesson
     * @return the new batch
     */
    public LongTermBatch addLongTermBatch() {
        LongTermBatch newLongTermBatch =
                new LongTermBatch(longTermBatches.size());
        longTermBatches.add(newLongTermBatch);
        return newLongTermBatch;
    }

    /**
     * returns a longterm batch at a given index
     * @param index the index
     * @return the batch
     */
    public LongTermBatch getLongTermBatch(int index) {
        return longTermBatches.get(index);
    }

    /**
     * returns the number of longterm batches within this lesson
     * @return the number of longterm batches within this lesson
     */
    public int getNumberOfLongTermBatches() {
        return longTermBatches.size();
    }

    /**
     * returns the list of all long-term batches of this lesson
     * @return the list of all long-term batches of this lesson
     */
    public List<LongTermBatch> getLongTermBatches() {
        return longTermBatches;
    }

    /**
     * returns the total number of cards of this lesson
     * @return the total number of cards of this lesson
     */
    public int getNumberOfCards() {
        int numberOfCards = unlearnedBatch.getNumberOfCards();
        numberOfCards += ultraShortTermList.size();
        numberOfCards += shortTermList.size();
        for (LongTermBatch longTermBatch : longTermBatches) {
            numberOfCards += longTermBatch.getNumberOfCards();
        }
        return numberOfCards;
    }

    /**
     * returns a <CODE>List</CODE> of all cards of this lesson
     * @return a <CODE>List</CODE> of all cards of this lesson
     */
    public List<Card> getCards() {
        List<Card> cards = new ArrayList<>();
        List<Card> unlearnedCards = unlearnedBatch.getCards();
        if (unlearnedCards != null) {
            cards.addAll(unlearnedCards);
        }
        for (LongTermBatch longTermBatch : longTermBatches) {
            cards.addAll(longTermBatch.getCards());
        }
        return cards;
    }

    /**
     * returns the total number of expired cards of this lesson
     * @return the total number of expired cards of this lesson
     */
    public int getNumberOfExpiredCards() {
        int numberOfExpiredCards = 0;
        for (LongTermBatch longTermBatch : longTermBatches) {
            numberOfExpiredCards += longTermBatch.getNumberOfExpiredCards();
        }
        return numberOfExpiredCards;
    }

//    /**
//     * returns a random expired card
//     * @return a random expired card
//     */
//    public Card getRandomExpiredCard() {
//        // collect all expired cards
//        List<Card> expiredCards = new ArrayList<Card>();
//        for (LongTermBatch longTermBatch : longTermBatches) {
//            for (Card expiredCard : longTermBatch.getExpiredCards()) {
//                expiredCards.add(expiredCard);
//            }
//        }
//
//        // choose one of them randomly
//        int expiredCardCount = expiredCards.size();
//        if (expiredCardCount > 0) {
//            // lazy creation of random source
//            if (random == null) {
//                random = new Random();
//            }
//            int randomIndex = random.nextInt(expiredCardCount);
//            return expiredCards.get(randomIndex);
//        }
//
//        return null;
//    }

    /**
     * returns a collection of all expired cards
     * @return a collection of all expired cards
     */
    public Collection<Card> getExpiredCards() {
        Collection<Card> expiredCards = new ArrayList<>();
        for (LongTermBatch longTermBatch : longTermBatches) {
            expiredCards.addAll(longTermBatch.getExpiredCards());
        }
        return expiredCards;
    }

    /**
     * returns a collection of all learned (not new or expired) cards
     * @return a collection of all learned (not new or expired) cards
     */
    public Collection<Card> getLearnedCards() {
        Collection<Card> learnedCards = new ArrayList<>();
        for (LongTermBatch longTermBatch : longTermBatches) {
            learnedCards.addAll(longTermBatch.getLearnedCards());
        }
        return learnedCards;
    }

    /**
     * runs through the whole lesson end collects all expired cards in an
     * internal batch
     */
    public void refreshExpiration() {
        Log.d("Lesson::refresExpiration", "entry");
        for (LongTermBatch longTermBatch : longTermBatches) {
            Log.d("Lesson::refresExpiration", "loop");
            longTermBatch.refreshExpiration();
        }
    }

    /**
     * removes empty longterm batches at the end of this lesson
     */
    private void trim() {
        for (int i = longTermBatches.size() - 1; i >= 0; i--) {
            LongTermBatch longTermBatch = longTermBatches.get(i);
            if (longTermBatch.getNumberOfCards() == 0) {
                longTermBatches.remove(i);
            } else {
                return;
            }
        }
    }

    /**
     * moves all cards of longterm batches back to the unlearned batch
     */
    public void reset() {
        for (LongTermBatch longTermBatch : longTermBatches) {
            for (Card card : longTermBatch.getCards()) {
                card.setLearned(false);
                unlearnedBatch.addCard(card);
            }
        }
        longTermBatches.clear();
    }

    /**
     * merges this lesson with the lesson <CODE>otherLesson</CODE>
     * @param otherLesson the lesson to be merged with this lesson
     */
    public void merge(Lesson otherLesson) {
        // merge unlearned cards
        List<Card> otherUnlearnedCards =
                otherLesson.getUnlearnedBatch().getCards();
        unlearnedBatch.addCards(otherUnlearnedCards);
        summaryBatch.addCards(otherUnlearnedCards);

        // merge learned cards in the long term batches
        int otherNumberOfLongTermBatches =
                otherLesson.getNumberOfLongTermBatches();
        LOGGER.log(Level.FINE,
                "the lesson to be merged contains {0} longterm batches",
                otherNumberOfLongTermBatches);
        for (int i = 0; i < otherNumberOfLongTermBatches; i++) {
            if (longTermBatches.size() < (i + 1)) {
                addLongTermBatch();
            }
            Batch batch = getLongTermBatch(i);
            Batch otherBatch = otherLesson.getLongTermBatch(i);
            List<Card> cards = otherBatch.getCards();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "batch {0} contains {1} cards",
                        new Object[]{i, cards.size()});
            }
            batch.addCards(cards);
            summaryBatch.addCards(otherUnlearnedCards);
        }
    }

    /**
     * flips all cards and moves them to their new batches
     */
    public void flip() {
        for (Card card : getCards()) {
            //Daten zwischenspeichern, da immer die Vorderseite abgefragt wird
            boolean isLearned = card.isLearned();
            int batchNumber = -1;
            LongTermBatch longTermBatch = null;

            // remove card
            if (isLearned) {
                batchNumber = card.getLongTermBatchNumber();
                longTermBatch = longTermBatches.get(batchNumber);
                longTermBatch.removeCard(card);
            } else {
                unlearnedBatch.removeCard(card);
            }

            // flip card
            card.flip();

            // re-add card
            if (isLearned) {
                for (int size = longTermBatches.size(); size < (batchNumber + 1); size++) {
                    longTermBatches.add(new LongTermBatch(size));
                }
                longTermBatch.addCard(card);
            } else {
                unlearnedBatch.addCard(card);
            }
        }

        // clean up
        trim();
        refreshExpiration();
    }

    /**
     * returns the next expiration time
     * @return the next expiration time
     */
    public long getNextExpirationTime() {
        boolean noExpiredCards = true;
        for (LongTermBatch longTermBatch : longTermBatches) {
            if (longTermBatch.getNumberOfExpiredCards() != 0) {
                noExpiredCards = false;
                break;
            }
        }

        long nextExpirationDate = Long.MAX_VALUE;
        if (noExpiredCards) {
            // look at all learned cards and determine next expiation date
            Collection<Card> cards = getCards();
            for (Card card : cards) {
                if (card.isLearned()) {
                    long cardExpirationTime = card.getExpirationTime();
                    if (cardExpirationTime < nextExpirationDate) {
                        nextExpirationDate = cardExpirationTime;
                    }
                }
            }
        }

        return nextExpirationDate;
    }

    /**
     * moves cards back to the unlearned batch
     * @param batch   the active/choosen batch
     * @param indices the indices of the choosen cards
     */
    public void forgetCards(Batch batch, int[] indices) {
        if (batch == summaryBatch) {

            if (indices.length == batch.getNumberOfCards()) {
                // special handling when all cards are selected
                for (LongTermBatch longTermBatch : longTermBatches) {
                    for (int i = longTermBatch.getNumberOfCards() - 1;
                         i >= 0; i--) {
                        Card card = longTermBatch.removeCard(i);
                        unlearnedBatch.addCard(card);
                        card.reset();
                    }
                }

            } else {
                // remove cards from the "real" batches
                for (int i = indices.length - 1; i >= 0; i--) {
                    Card card = batch.getCard(indices[i]);
                    if (card.isLearned()) {
                        int longTermBatchNumber = card.getLongTermBatchNumber();
                        // must move the card
                        LongTermBatch longTermBatch =
                                longTermBatches.get(longTermBatchNumber);
                        longTermBatch.removeCard(card);
                        unlearnedBatch.addCard(card);
                        card.reset();
                    }
                }
            }

        } else {
            // long term batches
            for (int i = indices.length - 1; i >= 0; i--) {
                Card card = batch.removeCard(indices[i]);
                unlearnedBatch.addCard(card);
                card.reset();
            }
        }

        // cleanup
        trim();
    }

    /**
     * moves cards back to the first long term batch and sets them expired
     * @param batch   the active/choosen batch
     * @param indices the indices of the choosen cards
     */
    public void instantRepeatCards(Batch batch, int[] indices) {
        // ensure that the first long term batch exists
        if (longTermBatches.isEmpty()) {
            longTermBatches.add(new LongTermBatch(0));
        }
        LongTermBatch firstLongTermBatch = longTermBatches.get(0);

        if (batch == summaryBatch) {

            if (indices.length == batch.getNumberOfCards()) {
                // special handling when all cards are selected

                // handle all unlearned cards
                for (int i = unlearnedBatch.getNumberOfCards() - 1;
                     i >= 0; i--) {
                    Card card = unlearnedBatch.removeCard(i);
                    firstLongTermBatch.addCard(card);
                    card.expire();
                }

                // handle all long term batches
                for (LongTermBatch longTermBatch : longTermBatches) {
                    for (int i = longTermBatch.getNumberOfCards() - 1;
                         i >= 0; i--) {
                        Card card = longTermBatch.removeCard(i);
                        firstLongTermBatch.addCard(card);
                        card.expire();
                    }
                }

            } else {

                // search every card in the "real" batches
                for (int i = indices.length - 1; i >= 0; i--) {
                    Card card = batch.getCard(indices[i]);
                    if (card.isLearned()) {
                        int longTermBatchNumber = card.getLongTermBatchNumber();
                        if (longTermBatchNumber != 0) {
                            // must move the card
                            LongTermBatch longTermBatch =
                                    longTermBatches.get(longTermBatchNumber);
                            longTermBatch.removeCard(card);
                            firstLongTermBatch.addCard(card);
                        }
                    } else {
                        unlearnedBatch.removeCard(card);
                        firstLongTermBatch.addCard(card);
                    }
                    card.expire();
                }
            }

        } else if (batch == unlearnedBatch) {
            // just move all selected cards
            for (int i = indices.length - 1; i >= 0; i--) {
                Card card = batch.removeCard(indices[i]);
                firstLongTermBatch.addCard(card);
                card.expire();
            }

        } else {
            // the batch is a long term batch
            // move cards only if we are not already in the first longterm batch
            if (batch == firstLongTermBatch) {
                for (int i = indices.length - 1; i >= 0; i--) {
                    Card card = batch.getCard(indices[i]);
                    card.expire();
                }
            } else {
                for (int i = indices.length - 1; i >= 0; i--) {
                    Card card = batch.removeCard(indices[i]);
                    firstLongTermBatch.addCard(card);
                    card.expire();
                }
            }
        }

        // cleanup
        trim();
        refreshExpiration();
    }
}