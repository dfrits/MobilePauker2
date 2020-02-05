package com.daniel.mobilepauker2.model.xmlsupport

import com.daniel.mobilepauker2.utils.Log
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.zip.GZIPInputStream

abstract class FlashCardBaseFeedParser(private val feedUrl: URL) :
    IFlashCardFeedParser {
    val inputStream: InputStream?
        get() {
            val feedURLString = feedUrl.toExternalForm().toLowerCase()
            Log.d(
                "FlashCardBaseFeedParser::getInputStream",
                "feedURLString =$feedURLString"
            )
            try {
                if (feedURLString.endsWith(".gz")) {
                    Log.d(
                        "FlashCardBaseFeedParser::getInputStream",
                        "found a .gz file"
                    )
                    return GZIPInputStream(feedUrl.openConnection().getInputStream())
                } else if (feedUrl.toExternalForm().endsWith(".pau") || feedUrl.toExternalForm().endsWith(
                        ".xml"
                    )
                ) {
                    Log.d(
                        "FlashCardBaseFeedParser::getInputStream",
                        "found a .pau file"
                    )
                    return feedUrl.openConnection().getInputStream()
                } else if (feedUrl.toExternalForm().endsWith(".csv")) {
                    Log.d(
                        "FlashCardBaseFeedParser::getInputStream",
                        "found a .csv file"
                    )
                    return feedUrl.openConnection().getInputStream()
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            return null
        }

    companion object {
        const val LESSON = "lesson"
        const val DESCRIPTION = "description"
        const val BATCH = "batch"
        const val CARD = "card"
        const val FRONTSIDE = "frontside"
        const val REVERSESIDE = "reverseside"
        const val TEXT = "text"
        const val FONT = "font"
    }

}