package com.daniel.mobilepauker2.model.pauker_native;

import android.support.annotation.NonNull;

import com.daniel.mobilepauker2.utils.Constants;

import java.util.LinkedList;
import java.util.List;


public class CardSide implements Comparable<CardSide> {

    public class Color {
        String Color = " ";

        public String getColor() {
            return Color;
        }

        public void setColor(String color) {
            Color = color;
        }
    }


    // content
    private String text;
    // style
    private Font font;
    private ComponentOrientation orientation;
    // learning
    private boolean repeatByTyping;
    private boolean learned;
    private int longTermBatchNumber;
    private long learnedTimestamp;
    // support for search result caching (speeds up batch rendering)
    private final List<SearchHit> searchHits;

    /**
     * creates a new CardSide
     */
    public CardSide() {
        this("");
    }

    /**
     * creates a new CardSide
     * @param text the card side text
     */
    public CardSide(String text) {
        this.text = text;
        searchHits = new LinkedList<>();
    }

    public int compareTo(@NonNull CardSide otherCardSide) {
        int textResult = text.compareTo(otherCardSide.getText());
        if (textResult != 0) {
            return textResult;
        }
        boolean otherCardByTyping = otherCardSide.isRepeatedByTyping();
        if (repeatByTyping && !otherCardByTyping) {
            return -1;
        } else if (!repeatByTyping && otherCardByTyping) {
            return 1;
        }
        boolean otherCardLearned = otherCardSide.isLearned();
        if (learned && !otherCardLearned) {
            return 1;
        } else if (!learned && otherCardLearned) {
            return -1;
        }
        int otherLongTermBatchNumber = otherCardSide.getLongTermBatchNumber();
        if (longTermBatchNumber < otherLongTermBatchNumber) {
            return -1;
        } else if (longTermBatchNumber > otherLongTermBatchNumber) {
            return 1;
        }
        long otherLearnedTimestamp = otherCardSide.getLearnedTimestamp();
        if (learnedTimestamp < otherLearnedTimestamp) {
            return -1;
        } else if (learnedTimestamp > otherLearnedTimestamp) {
            return 1;
        }
        // no difference...
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CardSide other = (CardSide) obj;
        return ((text == null) ? other.text == null : text.equals(other.text))
                && repeatByTyping == other.repeatByTyping
                && learned == other.learned
                && longTermBatchNumber == other.longTermBatchNumber
                && learnedTimestamp == other.learnedTimestamp;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (text != null ? text.hashCode() : 0);
        hash = 41 * hash + (repeatByTyping ? 1 : 0);
        hash = 41 * hash + longTermBatchNumber;
        hash = 41 * hash + (int) (learnedTimestamp ^ (learnedTimestamp >>> 32));
        return hash;
    }

    /**
     * returns the cardside text
     * @return the cardside text
     */
    public String getText() {
        return text;
    }

    /**
     * sets the cardside text
     * @param text the cardside text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * returns the explicitly set cardside font or <CODE>null</CODE>, if no font
     * was explicitly set for this card side
     * @return the explicitly set cardside font or <CODE>null</CODE>, if no font was explicitly set
     * for this card side
     */
    public Font getFont() {
        return font;
    }

    /**
     * sets the cardside font
     * @param font the cardside font
     */
    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * returns the cardside orientation
     * @return the cardside orientation
     */
    public ComponentOrientation getOrientation() {
        return orientation==null? new ComponentOrientation(Constants.STANDARD_ORIENTATION):orientation;
    }

    /**
     * sets the cardside orientation
     * @param orientation the cardside orientation
     */
    public void setOrientation(ComponentOrientation orientation) {
        this.orientation = orientation;
    }

    /**
     * returns the batch number this card belongs to if this card side would be
     * the frontside
     * @return the batch number this card belongs to if this card side would be the frontside
     */
    public int getLongTermBatchNumber() {
        return longTermBatchNumber;
    }

    /**
     * sets the long term batch number this card belongs to if this card side
     * would be the frontside
     * @param longTermBatchNumber the long term batch number
     */
    void setLongTermBatchNumber(int longTermBatchNumber) {
        this.longTermBatchNumber = longTermBatchNumber;
    }

    /**
     * returns the timestamp when the cardside was learned
     * @return the timestamp when the cardside was learned
     */
    public long getLearnedTimestamp() {
        return learnedTimestamp;
    }

    /**
     * sets the timestamp when the cardside was learned
     * @param learnedTimestamp the timestamp when the cardside was learned
     */
    public void setLearnedTimestamp(long learnedTimestamp) {
        this.learnedTimestamp = learnedTimestamp;
    }

    /**
     * returns if the cardside should be repeated by typing instead of
     * memorizing
     * @return <CODE>true</CODE>, if the cardside should be repeated by typing instead of
     * memorizing, <CODE>false</CODE> otherwise
     */
    public boolean isRepeatedByTyping() {
        return repeatByTyping;
    }

    /**
     * sets if the cardside should be repeated by typing instead of memorizing
     * @param repeatByTyping <CODE>true</CODE>, if the cardside should be repeated by typing instead
     *                       of memorizing, <CODE>false</CODE> otherwise
     */
    void setRepeatByTyping(boolean repeatByTyping) {
        this.repeatByTyping = repeatByTyping;
    }

    /**
     * searches for a string pattern at the card side
     * @param card      the card of this card side
     * @param cardSide  the side of this card side
     * @param pattern   the search pattern
     * @param matchCase if we must match the case
     * @return a list with search match indices
     */
    public List<SearchHit> search(Card card, Card.Element cardSide,
                                  String pattern, boolean matchCase) {
        searchHits.clear();
        if (pattern == null) {
            return searchHits;
        }
        String searchText = text;
        String searchPattern = pattern;
        if (!matchCase) {
            searchText = text.toLowerCase();
            searchPattern = pattern.toLowerCase();
        }
        for (int index = searchText.indexOf(searchPattern); index != -1; ) {
            searchHits.add(new SearchHit(card, cardSide, index));
            index = searchText.indexOf(searchPattern, index + 1);
        }
        return searchHits;
    }

    /**
     * returns a List of search match indices
     * @return a List of search match indices
     */
    public List<SearchHit> getSearchHits() {
        return searchHits;
    }

    /**
     * cancels the search process
     */
    public void cancelSearch() {
        searchHits.clear();
    }

    //    /**
    //     * returns the size of the font that is used for this card side
    //     * @return the size of the font that is used for this card side
    //     */
    //    public int getFontSize() {
    ////        if (font == null) {
    ////            return PaukerFrame.DEFAULT_FONT.getSize();
    ////        }
    ////        return font.getSize();
    //    	return 10;
    //    }
    //
    //    /**
    //     * returns the stlye of the font that is used for this card side
    //     * @return the stlye of the font that is used for this card side
    //     */
    //    public int getFontStyle() {
    ////        if (font == null) {
    ////            return PaukerFrame.DEFAULT_FONT.getStyle();
    ////        }
    ////        return font.getStyle();
    //
    ////      if (font == null) {
    ////      return PaukerFrame.DEFAULT_FONT.getStyle();
    ////  }
    //
    //    	return null;
    //    }

    /**
     * indicates if this card side is learned
     * @return <CODE>true</CODE>, if the card side is learned, <CODE>false</CODE> otherwise
     */
    public boolean isLearned() {
        return learned;
    }

    /**
     * sets if the card is learned
     * @param learned <CODE>true</CODE>, if the card is learned, <CODE>false</CODE> otherwise
     */
    public void setLearned(boolean learned) {
        this.learned = learned;
    }
}
