package com.daniel.mobilepauker2.di

import com.daniel.mobilepauker2.main.MainMenuViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel { MainMenuViewModel() }
}