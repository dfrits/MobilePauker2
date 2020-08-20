package com.daniel.mobilepauker2.core

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    private val mutableMessage = MutableLiveData<Event<Int>>()
    val message: LiveData<Event<Int>> = mutableMessage

    fun postMessage(@StringRes message: Int, duration: Int) {
        mutableMessage.postValue(Event(message, duration))
    }
}