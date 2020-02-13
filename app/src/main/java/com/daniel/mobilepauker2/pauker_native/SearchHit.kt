package com.daniel.mobilepauker2.pauker_native

/**
 * A search hit, when using the search function in Pauker.
 * @author Ronny.Standtke@gmx.net
 */
class SearchHit(val card: Card, val cardSide: Card.Element, val cardSideIndex: Int) {

    override fun equals(other: Any?): Boolean {
        if (other is SearchHit) {
            if (card != other.card) {
                return false
            }
            when (cardSide) {
                Card.Element.FRONT_SIDE -> if (other.cardSide ==
                    Card.Element.REVERSE_SIDE
                ) {
                    return false
                }
                Card.Element.REVERSE_SIDE -> if (other.cardSide ==
                    Card.Element.FRONT_SIDE
                ) {
                    return false
                }
                else -> {
                }
            }
            // compare "yellow" mark index
            return cardSideIndex == other.cardSideIndex
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