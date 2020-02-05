package com.daniel.mobilepauker2.model.xmlsupport

import com.daniel.mobilepauker2.model.pauker_native.Lesson
import java.io.EOFException

interface IFlashCardFeedParser {
    @Throws(EOFException::class)
    fun parse(): Lesson
}