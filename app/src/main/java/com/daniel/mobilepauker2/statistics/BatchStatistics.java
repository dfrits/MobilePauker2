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

package com.daniel.mobilepauker2.statistics;

public class BatchStatistics {

    private int batchSize;
    private int expiredCardsSize;

    public BatchStatistics(int batchSize, int expiredCardsSize) {
        this.batchSize = batchSize;
        this.expiredCardsSize = expiredCardsSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getExpiredCardsSize() {
        return expiredCardsSize;
    }

    public void setExpiredCardsSize(int expiredCardsSize) {
        this.expiredCardsSize = expiredCardsSize;
    }

}
