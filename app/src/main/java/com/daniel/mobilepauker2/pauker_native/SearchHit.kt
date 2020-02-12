package com.daniel.mobilepauker2.pauker_native

/**
 * A search hit, when using the search function in Pauker.
 * @author Ronny.Standtke@gmx.net
 */
class SearchHit
/**
 * Creates a new instance of SearchHit
 * @param card          the card
 * @param cardSide      the card side
 * @param cardSideIndex the index of the card side
 */(
    val card: Card,
    val cardSide: Card.Element,
    val cardSideIndex: Int
) {
    /**
     * returns the card
     * @return the card
     */
    /**
     * returns the card side
     * @return the card side
     */
    /**
     * returns the index of the card side
     * @return the index of the card side
     */

    override fun equals(`object`: Any?): Boolean {
        if (`object` is SearchHit) { // class cast
            val otherSearchHit = `object`
            // compare cards
            if (card != otherSearchHit.card) {
                return false
            }
            when (cardSide) {
                Card.Element.FRONT_SIDE -> if (otherSearchHit.cardSide ==
                    Card.Element.REVERSE_SIDE
                ) {
                    return false
                }
                Card.Element.REVERSE_SIDE -> if (otherSearchHit.cardSide ==
                    Card.Element.FRONT_SIDE
                ) {
                    return false
                }
                else -> {
                }
            }
            // compare "yellow" mark index
            return cardSideIndex == otherSearchHit.cardSideIndex
        }
        return false
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 31 * hash + card.id.hashCode()
        hash = 31 * hash + cardSide.ordinal
        hash = 31 * hash + cardSideIndex
        return hash
    }

}