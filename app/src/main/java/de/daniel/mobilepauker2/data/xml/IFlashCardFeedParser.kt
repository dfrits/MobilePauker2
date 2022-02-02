package de.daniel.mobilepauker2.data.xml

import de.daniel.mobilepauker2.lesson.Lesson
import java.io.EOFException

interface IFlashCardFeedParser {

    @Throws(EOFException::class)
    fun parse(): Lesson
}