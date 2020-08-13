package com.daniel.mobilepauker2.main

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.browse.SearchActivity
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.editor.AddCardActivity
import com.daniel.mobilepauker2.learning.LearnCardsActivity
import com.daniel.mobilepauker2.lessonexport.SaveDialog
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.pauker_native.PaukerAndModelManager
import com.daniel.mobilepauker2.settings.SettingsManager

class MainMenuViewModel(
        val paukerAndModelManager: PaukerAndModelManager,
        val settingsManager: SettingsManager,
        val context: Context
) : ViewModel() {
    private val paukerManager = paukerAndModelManager.paukerManager
    private val modelManager = paukerAndModelManager.modelManager

    fun isLessonSetup(): Boolean = modelManager.isLessonSetup

    fun createNewLesson() {
        modelManager.createNewLesson()
    }

    fun hasCardsToLearn(): Boolean = modelManager.unlearnedBatchSize != 0

    fun hasExpiredCards(): Boolean = modelManager.expiredCardsSize != 0

    fun getTitle(): String = if (isLessonNotNew()) {
        paukerManager.readableFileName ?: context.getString(R.string.app_name)
    } else {
        context.getString(R.string.app_name)
    }

    fun getDescription(): String = modelManager.description

    fun canShowBatchDetails(index: Int): Boolean =
            (index > 1 && modelManager.batchStatistics[index - 2].batchSize == 0
                    || index == 1 && modelManager.unlearnedBatchSize == 0
                    || modelManager.lessonSize == 0)

    fun isLessonNotNew(): Boolean = paukerManager.isLessonNotNew

    fun isLessonAndDescriptionEmpty(): Boolean = modelManager.isLessonEmpty

    fun isLessonEmpty(): Boolean = modelManager.lessonSize > 0

    fun isSaveRequired(): Boolean = paukerManager.isSaveRequired

    fun resetLesson() {
        modelManager.resetLesson()
    }

    fun updateSaveRequired(required: Boolean) {
        paukerManager.isSaveRequired = required
    }

    fun saveLesson() {
        PaukerManager.showToast(
                context as Activity,
                R.string.saving_success,
                Toast.LENGTH_SHORT
        )
        paukerManager.isSaveRequired = false
        if (!settingsManager.getBoolPreference(context, SettingsManager.Keys.ENABLE_EXPIRE_TOAST))
            paukerAndModelManager.showExpireToast(context)
    }

    fun showBatchDetails(index: Int) {
        if (canShowBatchDetails(index)) {
            val browseIntent = Intent(Intent.ACTION_SEARCH)
            browseIntent.setClass(context, SearchActivity::class.java)
            browseIntent.putExtra(SearchManager.QUERY, "")
            browseIntent.putExtra(Constants.STACK_INDEX, index)
            context.startActivity(browseIntent)
        }
    }

    fun learnNewCardClicked() {
        if (settingsManager.getBoolPreference(context, SettingsManager.Keys.HIDE_TIMES)) {
            modelManager.setLearningPhase(context, ModelManager.LearningPhase.SIMPLE_LEARNING)
        } else {
            modelManager.setLearningPhase(context, ModelManager.LearningPhase.FILLING_USTM)
        }
        context.startActivity(Intent(context, LearnCardsActivity::class.java))
    }

    fun repeatCardsClicked() {
        modelManager.setLearningPhase(context, ModelManager.LearningPhase.REPEATING_LTM)
        val importActivity = Intent(context, LearnCardsActivity::class.java)
        context.startActivity(importActivity)
    }

    fun addNewCardClicked() {
        context.startActivity(Intent(context, AddCardActivity::class.java))
    }

    fun saveLessonForResult(requestCode: Int) {
        (context as Activity).startActivityForResult(Intent(context, SaveDialog::class.java), requestCode)
    }

    fun setupNewLesson() {
        paukerAndModelManager.setupNewApplicationLesson()
        paukerManager.isSaveRequired = false
    }

    fun forgetCards() {
        modelManager.forgetAllCards()
        paukerManager.isSaveRequired = true
    }

    fun flipCards() {
        modelManager.flipAllCards()
        paukerManager.isSaveRequired = true
    }
}