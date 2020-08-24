package com.daniel.mobilepauker2.core

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {

    private val mutableMessage = MutableLiveData<Event<Int>>()
    val message: LiveData<Event<Int>> = mutableMessage

    fun postMessage(@StringRes message: Int, duration: Int = Toast.LENGTH_SHORT) {
        mutableMessage.postValue(Event(message, duration))
    }
}