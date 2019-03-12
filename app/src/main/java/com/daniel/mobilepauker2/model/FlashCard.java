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

package com.daniel.mobilepauker2.model;

import com.daniel.mobilepauker2.model.pauker_native.Card;
import com.daniel.mobilepauker2.model.pauker_native.CardSide;
import com.daniel.mobilepauker2.model.pauker_native.Font;

public class FlashCard extends Card {

    private String mIndex;
    private int mInitialBatch = 0;

    public int getInitialBatch() {
        return mInitialBatch;
    }

    public void setInitialBatch(int initialBatch) {
        this.mInitialBatch = initialBatch;
    }

    public FlashCard(String frontSideText, String reverseSideText, String index, String learnStatus) {
        super(new CardSide(), new CardSide());

        this.frontSide.setText(frontSideText);
        this.reverseSide.setText(reverseSideText);
        this.setIndex(index);
        this.frontSide.setFont(new Font());
        this.reverseSide.setFont(new Font());

        if (learnStatus.contentEquals("true")) {
            this.setLearned(true);
        } else {
            this.setLearned(false);
        }
    }

    public FlashCard() {
        super(new CardSide(), new CardSide());
    }

    public String getIndex() {
        return mIndex;
    }

    public void setIndex(String index) {
        mIndex = index;
    }

    public enum SideShowing {SIDE_A, SIDE_B}

    private SideShowing side = SideShowing.SIDE_A;

    public String getSideAText() {
        return this.frontSide.getText();
    }

    public void setSideAText(String sideAText) {
        this.frontSide.setText(sideAText);
    }

    public String getSideBText() {
        return this.reverseSide.getText();
    }

    public void setSideBText(String sideBText) {
        this.reverseSide.setText(sideBText);
    }

    public SideShowing getSide() {
        return side;
    }

    public void setSide(SideShowing side) {
        this.side = side;
    }
}
