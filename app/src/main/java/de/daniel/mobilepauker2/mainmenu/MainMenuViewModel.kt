package de.daniel.mobilepauker2.mainmenu

import androidx.lifecycle.ViewModel
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import javax.inject.Inject

class MainMenuViewModel @Inject constructor(
    val lessonManager: LessonManager,
    val dataManager: DataManager
) : ViewModel() {

    fun createNewLesson() {
        lessonManager.setupNewLesson()
        dataManager.saveRequired = false
    }

    fun checkLessonIsSetup() {
        if (!lessonManager.isLessonSetup()) lessonManager.createNewLesson()
    }

    fun getBatchSize(batchType: BatchType) = lessonManager.getBatchSize(batchType)

    fun resetShortTerms() {
        lessonManager.resetShortTermBatches()
    }

    fun getDescription(): String = lessonManager.lessonDescription
}