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
package com.daniel.mobilepauker2.utils

import com.daniel.mobilepauker2.model.ModelManager
import com.daniel.mobilepauker2.statistics.BatchStatistics

object DebugToolBox {
    fun printStatsToDebug(batchStatistics: List<BatchStatistics>) {
        val modelManager: ModelManager = ModelManager.Companion.instance()
        Log.d(
            "LessonStatsActivity::onCreate",
            "Lesson Size   = " + modelManager.lessonSize
        )
        Log.d(
            "LessonStatsActivity::onCreate",
            "Expired Cards = " + modelManager.expiredCardsSize
        )
        Log.d(
            "LessonStatsActivity::onCreate",
            "New Cards     = " + modelManager.unlearnedBatchSize
        )
        Log.d(
            "LessonStatsActivity::onCreate",
            "USTM          = " + modelManager.ultraShortTermMemorySize
        )
        for (i in batchStatistics.indices) {
            Log.d(
                "LessonStatsActivity::onCreate",
                "Batch " + i + "   = " + batchStatistics[i].batchSize
            )
        }
    }
}