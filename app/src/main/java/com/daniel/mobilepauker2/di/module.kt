package com.daniel.mobilepauker2.di

import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.main.MainMenuViewModel
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.settings.SettingsManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { ModelManager(get(), get()) }
    single { SettingsManager() }
    single { PaukerManager(get()) }
    single { ErrorReporter(get()) }

    viewModel { MainMenuViewModel(get(), get(), get(), get(), androidContext()) }
}