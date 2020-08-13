package com.daniel.mobilepauker2.di

import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.lessonimport.LessonImportViewModel
import com.daniel.mobilepauker2.main.MainMenuViewModel
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.pauker_native.PaukerAndModelManager
import com.daniel.mobilepauker2.settings.SettingsManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { PaukerAndModelManager(get(), get()) }
    single { ModelManager(get()) }
    single { SettingsManager() }
    single { PaukerManager() }
    single { ErrorReporter(androidContext()) }

    viewModel { MainMenuViewModel(get(), get(), androidContext()) }
    viewModel { LessonImportViewModel(get(), androidContext()) }
}