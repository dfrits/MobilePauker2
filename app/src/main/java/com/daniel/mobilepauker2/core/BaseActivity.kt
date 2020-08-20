package com.daniel.mobilepauker2.core

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.koin.android.viewmodel.ext.android.viewModel
import toast

abstract class BaseActivity:AppCompatActivity() {

    val baseViewModel: BaseViewModel by viewModel()

    protected open fun initObserver(){
        baseViewModel.message.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let {
                toast(it)
            }
        })
    }
}