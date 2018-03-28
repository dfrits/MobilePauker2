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

        public void setAscending(boolean ascending) {
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
    public Batch(List<Card> cards) {
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
    public Card getCard(int index) {
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
    public void addCards(List<Card> cards) {
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
     * empties the batch
     */
    public void clear() {
        cards.clear();
    }

    /**
     * moves certain cards a certain offset
     * @param rows   the rows to be moved
     * @param offset the offset of the movement
     */
    public void moveCards(int[] rows, int offset) {
        // save current search hit
        savedSearchHit = getCurrentSearchHit();

        // move
        if (offset > 0) {
            // move down (bottom up)
            for (int i = rows.length - 1; i >= 0; i--) {
                cards.add(rows[i] + offset, cards.remove(rows[i]));
            }
        } else {
            // move up (top down)
            for (int row : rows) {
                cards.add(row + offset, cards.remove(row));
            }
        }

        // reload search hits (without searching)
        reloadSearchHits();

        // restore current search hit
        restoreSearchHit();
    }

    /**
     * shuffles the cards in the batch
     * @param selectedCards the selected cards
     * @return an array which contains the new index for every card after mixing
     * them up
     */
    public int[] shuffle(int[] selectedCards) {
        // save current search hit
        savedSearchHit = getCurrentSearchHit();

        // remember original sorting
        Object[] originalSorting = cards.toArray();

        // actual shuffling
        Collections.shuffle(cards);

        // determine new sorting
        // (needed for scrolling to the previously selected cards)
        int indices = selectedCards.length;
        int[] newIndices = new int[indices];
        for (int i = 0; i < indices; i++) {
            int selectedIndex = selectedCards[i];
            Card selectedCard = (Card) originalSorting[selectedIndex];
            newIndices[i] = cards.indexOf(selectedCard);
        }

        // reload search hits (without searching)
        reloadSearchHits();

        // restore current search hit
        restoreSearchHit();

        return newIndices;
    }

    /**
     * sorts the cards in the batch according to an <CODE>sortIndex</CODE>
     * @param cardElement the card element that must be used for sorting the cards
     * @param ascending   if true the cards are sorted ascending otherwise descending
     * @return an array which contains the new index for every card after mixing them up
     */
    public int[] sortCards(Card.Element cardElement, boolean ascending) {

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

        if (comparator != null) {

            comparator.setAscending(ascending);

            Card[] originalSorting = (Card[]) cards.toArray();
            Collections.sort(cards, comparator);

            int numberOfCards = cards.size();
            int[] newIndices = new int[numberOfCards];
            for (int i = 0; i < numberOfCards; i++) {
                newIndices[i] = cards.indexOf(originalSorting[i]);
            }

            return newIndices;
        }

        return null;
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
     * returns the search pattern
     * @return the search pattern
     */
    public String getSearchPattern() {
        return searchPattern;
    }

    /**
     * indicates, if searching is case sensitive or not
     * @return returns <CODE>true</CODE>, if searching is case sensitive,
     * otherwise <CODE>false</CODE>
     */
    public boolean isMatchCase() {
        return matchCase;
    }

    /**
     * returns the card side for searching
     * @return the card side for searching
     */
    public Card.Element getSearchSide() {
        return searchSide;
    }

    /**
     * returns the current search hit
     * @return the current search hit
     */
    public SearchHit getCurrentSearchHit() {
        if ((currentSearchHit > -1) && (currentSearchHit < searchHits.size())) {
            return searchHits.get(currentSearchHit);
        }
        return null;
    }

    /**
     * continues a search
     * @param forward if <CODE>true</CODE>, the search is continued in forward direction,
     *                otherwise in backwards direction
     * @return <CODE>true</CODE>, if the search was successful,
     * <CODE>false</CODE> otherwise
     */
    public boolean continueSearch(boolean forward) {
        int numberOfSearchHits = searchHits.size();
        if (numberOfSearchHits == 0) {
            // we have no search hits, so we can not continue the search
            return false;
        } else {
            if (forward) {
                // we move the reference in the searchHits list
                currentSearchHit++;
                if (currentSearchHit == numberOfSearchHits) {
                    // we reached the end of the list and must loop
                    currentSearchHit = 0;
                }
            } else {
                // we move the reference in the searchHits list
                currentSearchHit--;
                if (currentSearchHit == -1) {
                    // we reached the top of the list and must loop
                    currentSearchHit = numberOfSearchHits - 1;
                }
            }
            return true;
        }
    }

    /**
     * sets the card side for searching
     * @param cardSide the card side for searching
     * @return <CODE>true</CODE>, if there is still a search hit,
     * <CODE>false</CODE> otherwise
     */
    public boolean setSearchCardSide(Card.Element cardSide) {
        // early return
        if (searchPattern == null) {
            return false;
        }
        searchSide = cardSide;
        return repeatSearch();
    }

    /**
     * determines if searching is case-sensitive
     * @param matchCase if <CODE>true</CODE>, searching is case sensitive, otherwise searching is
     *                  not case sensitive
     * @return <CODE>true</CODE>, if there is still a search hit,
     * <CODE>false</CODE> otherwise
     */
    public boolean setMatchCase(boolean matchCase) {
        // early return
        if (searchPattern == null) {
            return false;
        }
        this.matchCase = matchCase;
        return repeatSearch();
    }

    /**
     * stops any search process
     */
    public void stopSearching() {
        for (Card card : cards) {
            card.stopSearching();
        }
        searchPattern = null;
        searchHits.clear();
        currentSearchHit = -1;
    }

    /**
     * repeats the current search (e.g. when a card or search parameter changed)
     * @return <CODE>true</CODE>, if there is still a search hit,
     * <CODE>false</CODE> otherwise
     */
    public boolean repeatSearch() {
        // remember old searchHit
        SearchHit oldSearchHit = null;
        if ((currentSearchHit > -1) && (currentSearchHit < searchHits.size())) {
            Object tmpObject = searchHits.get(currentSearchHit);
            if (tmpObject != null) {
                oldSearchHit = (SearchHit) tmpObject;
            }
        }

        // re-fill the searchHits list
        if (refreshSearchHits()) {
            if (oldSearchHit != null) {
                // is oldSearchHit still in there?
                int tmpIndex = searchHits.indexOf(oldSearchHit);
                if (tmpIndex == -1) {
                    currentSearchHit = 0;
                } else {
                    currentSearchHit = tmpIndex;
                }
            }
            return true;
        }
        return false;
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

    private boolean reloadSearchHits() {
        searchHits.clear();
        for (Card card : cards) {
            searchHits.addAll(card.getSearchHits());
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