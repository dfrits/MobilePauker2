package de.daniel.mobilepauker2.lessonimport

import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.data.xml.FlashCardXMLPullFeedParser
import de.daniel.mobilepauker2.models.NextExpireDateResult
import java.io.IOException
import javax.inject.Inject

class LessonImportViewModel @Inject constructor(private val dataManager: DataManager) {
    var lastSelection: Int = -1
        private set

    fun resetSelection() {
        lastSelection = -1
    }

    fun itemClicked(position: Int) {
        if (lastSelection != position) {
            lastSelection = position
        } else {
            resetSelection()
        }
    }

    fun getNextExpireDate(filename: String): NextExpireDateResult =
        dataManager.getNextExpireDate(dataManager.getFilePathForName(filename))

    @Throws(IOException::class)
    fun openLesson(filename: String) {
        dataManager.loadLessonFromFile(dataManager.getFilePathForName(filename))
        dataManager.saveRequired = false
    }
}