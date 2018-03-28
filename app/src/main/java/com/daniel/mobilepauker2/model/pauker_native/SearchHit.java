package com.daniel.mobilepauker2.model.pauker_native;


/**
 * A search hit, when using the search function in Pauker.
 * @author Ronny.Standtke@gmx.net
 */
public class SearchHit {

    private final Card card;
    private final Card.Element cardSide;
    private final int cardSideIndex;

    /**
     * Creates a new instance of SearchHit
     * @param card          the card
     * @param cardSide      the card side
     * @param cardSideIndex the index of the card side
     */
    public SearchHit(Card card, Card.Element cardSide, int cardSideIndex) {
        this.card = card;
        this.cardSide = cardSide;
        this.cardSideIndex = cardSideIndex;
    }

    /**
     * returns the card
     * @return the card
     */
    public Card getCard() {
        return card;
    }

    /**
     * returns the card side
     * @return the card side
     */
    public Card.Element getCardSide() {
        return cardSide;
    }

    /**
     * returns the index of the card side
     * @return the index of the card side
     */
    public int getCardSideIndex() {
        return cardSideIndex;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof SearchHit) {
            // class cast
            SearchHit otherSearchHit = (SearchHit) object;

            // compare cards
            if (!card.equals(otherSearchHit.getCard())) {
                return false;
            }

            // here cardSide should never be BOTH_SIDES!
            switch (cardSide) {
                case FRONT_SIDE:
                    if (otherSearchHit.getCardSide() ==
                            Card.Element.REVERSE_SIDE) {
                        return false;
                    }
                    break;
                case REVERSE_SIDE:
                    if (otherSearchHit.getCardSide() ==
                            Card.Element.FRONT_SIDE) {
                        return false;
                    }
                    break;
                default:

            }

            // compare "yellow" mark index
            return cardSideIndex == otherSearchHit.getCardSideIndex();
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + card.getId().hashCode();
        hash = 31 * hash + cardSide.ordinal();
        hash = 31 * hash + cardSideIndex;
        return hash;
    }
}
