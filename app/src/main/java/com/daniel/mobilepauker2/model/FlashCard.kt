/* 
 * Copyright 2011 Brian Ford
 * 
 * This file is part of Pocket Pauker.
 * 
 * Pocket Pauker is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or (at your option) any later version.
 * 
 * Pocket Pauker is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 * 
 * See http://www.gnu.org/licenses/.

*/
package com.daniel.mobilepauker2.model

import com.daniel.mobilepauker2.model.pauker_native.Card
import com.daniel.mobilepauker2.model.pauker_native.CardSide
import com.daniel.mobilepauker2.model.pauker_native.Font

class FlashCard : Card {
    var index: String? = null
    var initialBatch = 0

    constructor(
        frontSideText: String?,
        reverseSideText: String?,
        index: String?,
        learnStatus: String?
    ) : super(CardSide(), CardSide()) {
        frontSide.text = frontSideText
        reverseSide.text = reverseSideText
        this.index = index
        frontSide.font = Font()
        reverseSide.font = Font()
        if (learnStatus!!.contentEquals("true")) {
            this.isLearned = true
        } else {
            this.isLearned = false
        }
    }

    constructor() : super(CardSide(), CardSide()) {}

    enum class SideShowing {
        SIDE_A, SIDE_B
    }

    var side = SideShowing.SIDE_A
    var sideAText: String?
        get() = frontSide.text
        set(sideAText) {
            frontSide.text = sideAText
        }

    var sideBText: String?
        get() = reverseSide.text
        set(sideBText) {
            reverseSide.text = sideBText
        }

}