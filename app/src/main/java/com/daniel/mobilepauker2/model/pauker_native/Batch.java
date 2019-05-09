package com.daniel.mobilepauker2.model.pauker_native;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * a batch is part of a lesson
 * @author Ronny.Standtke@gmx.net
 */
public class Batch {

    /**
     * the list of all cards in this batch
     */
    protected final List<Card> cards;
    private static final Logger LOGGER =
            Logger.getLogger(Batch.class.getName());
    private static final Collator collator = Collator.getInstance();

    private static abstract class AbstractCardComparator<Card>
            implements Comparator<Card> {

        private boolean ascending = true;

        void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(Card card1, Card card2) {
            int result = compareCards(card1, card2);
            return ascending ? result : -result;
        }

        protected abstract int compareCards(Card card1, Card card2);
    }

    private static final AbstractCardComparator<Card> frontSideComparator =
            new AbstractCardComparator<Card>() {

                public int compareCards(Card card1, Card card2) {
                    String frontSideText1 = card1.getFrontSide().getText();
                    String frontSideText2 = card2.getFrontSide().getText();
                    return collator.compare(frontSideText1, frontSideText2);
                }
            };
    private static final AbstractCardComparator<Card> reverseSideComparator =
            new AbstractCardComparator<Card>() {

                public int compareCards(Card card1, Card card2) {
                    String reverseSideText1 = card1.getReverseSide().getText();
                    String reverseSideText2 = card2.getReverseSide().getText();
                    return collator.compare(reverseSideText1, reverseSideText2);
                }
            };
    private static final AbstractCardComparator<Card> batchNumberComparator =
            new AbstractCardComparator<Card>() {

                public int compareCards(Card card1, Card card2) {
                    int batchNumber1 = card1.getLongTermBatchNumber();
                    int batchNumber2 = card2.getLongTermBatchNumber();
                    if (batchNumber1 < batchNumber2) {
                        return -1;
                    } else if (batchNumber1 > batchNumber2) {
                        return 1;
                    }
                    return 0;
                }
            };
    private static final AbstractCardComparator<Card> learnedDateComparator =
            new AbstractCardComparator<Card>() {

                public int compareCards(Card card1, Card card2) {
                    long learnedTime1 = card1.getLearnedTimestamp();
                    long learnedTime2 = card2.getLearnedTimestamp();
                    if (learnedTime1 < learnedTime2) {
                        return -1;
                    } else if (learnedTime1 > learnedTime2) {
                        return 1;
                    }
                    return 0;
                }
            };
    private static final AbstractCardComparator<Card> expiredDateComparator =
            new AbstractCardComparator<Card>() {

                public int compareCards(Card card1, Card card2) {
                    long expirationTime1 = card1.getExpirationTime();
                    long expirationTime2 = card2.getExpirationTime();
                    if (expirationTime1 < expirationTime2) {
                        return -1;
                    } else if (expirationTime1 > expirationTime2) {
                        return 1;
                    }
                    return 0;
                }
            };
    private static final AbstractCardComparator<Card> repeatingModeComparator =
            new AbstractCardComparator<Card>() {

                public int compareCards(Card card1, Card card2) {
                    boolean typing1 = card1.isRepeatedByTyping();
                    boolean typing2 = card2.isRepeatedByTyping();
                    if (!typing1 && typing2) {
                        return -1;
                    } else if (typing1 && !typing2) {
                        return 1;
                    }
                    return 0;
                }
            };
    // support for search result caching
    private String searchPattern;
    private Card.Element searchSide;
    private boolean matchCase;
    private final List<SearchHit> searchHits;
    private int currentSearchHit;
    private SearchHit savedSearchHit;

    /**
     * constructs a new Batch with all the cards in <CODE>cards</CODE>
     * @param cards initial batch cards
     */
    Batch(List<Card> cards) {
        if (cards == null) {
            this.cards = new ArrayList<>();
        } else {
            this.cards = cards;
        }

        searchHits = new LinkedList<>();
    }

    /**
     * returns the number of cards in this batch
     * @return the number of cards in this batch
     */
    public int getNumberOfCards() {
        return cards.size();
    }

    /**
     * returns the card at the index <CODE>i</CODE>
     * @param index the index of the returned card
     * @return the card at index <CODE>i</CODE>
     */
    Card getCard(int index) {
        return cards.get(index);
    }

    /**
     * returns all cards in the batch
     * @return all cards in the batch
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * adds a card to this batch
     * @param card the new card
     */
    public void addCard(Card card) {
        cards.add(card);
        card.setLearned(false);
    }

    /**
     * adds a new card at a certain index
     * @param index the index of the new card
     * @param card  the new card
     */
    public void addCard(int index, Card card) {
        cards.add(index, card);
        card.setLearned(false);
    }

    /**
     * adds cards to the batch
     * @param cards the new cards
     */
    void addCards(List<Card> cards) {
        for (Card card : cards) {
            addCard(card);
        }
    }

    /**
     * removes a card from the batch
     * @param card the card to be removed
     * @return <tt>true</tt>, if the card could be removed
     */
    public boolean removeCard(Card card) {
        // !!! IMPORTANT !!!
        // Do not refresh search stuff here or removing from summary batch
        // will break!
        // ("real" batches dont have the current search info)

        // remove card
        return cards.remove(card);
    }

    /**
     * removes a card from the batch
     * @param index the index where the card should be removed
     * @return the removed card
     */
    public Card removeCard(int index) {
        // save current search hit
        savedSearchHit = getCurrentSearchHit();

        // remove card
        Card card = cards.remove(index);

        // re-fill searchHits list (all indices are potentially wrong now)
        search(searchPattern, matchCase, searchSide);

        // restore current search hit
        restoreSearchHit();

        return card;
    }

    /**
     * determines the index of a special card
     * @param card the card
     * @return the index of the card
     */
    public int indexOf(Card card) {
        return cards.indexOf(card);
    }

    /**
     * sorts the cards in the batch according to an <CODE>sortIndex</CODE>
     * @param cardElement the card element that must be used for sorting the cards
     * @param ascending   if true the cards are sorted ascending otherwise descending
     */
    public void sortCards(Card.Element cardElement, boolean ascending) {

        AbstractCardComparator<Card> comparator = null;
        switch (cardElement) {
            case FRONT_SIDE:
                comparator = frontSideComparator;
                break;
            case REVERSE_SIDE:
                comparator = reverseSideComparator;
                break;
            case BATCH_NUMBER:
                comparator = batchNumberComparator;
                break;
            case LEARNED_DATE:
                comparator = learnedDateComparator;
                break;
            case EXPIRED_DATE:
                comparator = expiredDateComparator;
                break;
            case REPEATING_MODE:
                comparator = repeatingModeComparator;
                break;
            default:
                LOGGER.log(Level.WARNING,
                        "unknown cardElement {0}", cardElement);
        }

        comparator.setAscending(ascending);
        Collections.sort(cards, comparator);
    }

    /**
     * searches for a given string
     * @param searchPattern the pattern to search for
     * @param matchCase     if true the search is case sensitive
     * @param cardSide      the side to search at
     * @return if true a pattern match was found
     */
    public boolean search(String searchPattern, boolean matchCase, Card.Element cardSide) {
        // store search parameters
        this.searchPattern = searchPattern;
        this.matchCase = matchCase;
        searchSide = cardSide;

        if (refreshSearchHits()) {
            currentSearchHit = 0;
            return true;
        }
        return false;
    }

    /**
     * returns the current search hit
     * @return the current search hit
     */
    private SearchHit getCurrentSearchHit() {
        if ((currentSearchHit > -1) && (currentSearchHit < searchHits.size())) {
            return searchHits.get(currentSearchHit);
        }
        return null;
    }

    private boolean refreshSearchHits() {
        searchHits.clear();
        if ((searchPattern == null) || (searchPattern.length() == 0)) {
            return false;
        }
        for (Card card : cards) {
            searchHits.addAll(card.search(searchSide, searchPattern, matchCase));
        }
        return !searchHits.isEmpty();
    }

    private void restoreSearchHit() {
        // restore current search hit
        if (savedSearchHit != null) {
            currentSearchHit = searchHits.indexOf(savedSearchHit);
        }
    }
}