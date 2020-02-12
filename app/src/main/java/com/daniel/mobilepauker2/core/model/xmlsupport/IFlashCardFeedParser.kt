package com.daniel.mobilepauker2.core.model.xmlsupport

import com.daniel.mobilepauker2.pauker_native.Lesson
import java.io.EOFException

interface IFlashCardFeedParser {
    @Throws(EOFException::class)
    fun parse(): Lesson
}