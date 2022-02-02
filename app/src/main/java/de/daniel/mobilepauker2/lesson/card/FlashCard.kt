package de.daniel.mobilepauker2.lesson.card

import de.daniel.mobilepauker2.models.Font

class FlashCard : Card {
    var index: String? = null
    var initialBatch = 0
    var side = SideShowing.SIDE_A
    var sideAText: String
        get() = frontSide.text
        set(sideAText) {
            frontSide.text = sideAText
        }
    var sideBText: String
        get() = reverseSide.text
        set(sideBText) {
            reverseSide.text = sideBText
        }

    constructor(
        frontSideText: String,
        reverseSideText: String,
        index: String,
        learnStatus: String
    ) : super(CardSide(), CardSide()) {
        frontSide.text = frontSideText
        reverseSide.text = reverseSideText
        this.index = index
        frontSide.font = Font()
        reverseSide.font = Font()
        this.isLearned = learnStatus.contentEquals("true")
    }

    constructor() : super(CardSide(), CardSide())

    enum class SideShowing {
        SIDE_A, SIDE_B
    }
}