package com.daniel.mobilepauker2.main

import android.content.Context
import androidx.lifecycle.ViewModel
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.settings.SettingsManager

class MainMenuViewModel(
        val modelManager: ModelManager,
        val paukerManager: PaukerManager,
        val settingsManager: SettingsManager,
        val errorReporter: ErrorReporter,
        val context: Context
) : ViewModel() {


}