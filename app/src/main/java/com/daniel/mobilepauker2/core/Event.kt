package com.daniel.mobilepauker2.core

open class Event<out T>(
        private val content: T,
        private val duration: Int
) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? = if (hasBeenHandled) {
        null
    } else {
        hasBeenHandled = true
        content
    }

    fun peekContent(): T = content
}