package de.daniel.mobilepauker2.data.xml

import de.daniel.mobilepauker2.utils.Log
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.zip.GZIPInputStream

abstract class FlashCardBasedFeedParser(val feedUrl: URL) : IFlashCardFeedParser {
    val LESSON = "lesson"
    val DESCRIPTION = "description"
    val BATCH = "batch"
    val CARD = "card"
    val FRONTSIDE = "frontside"
    val REVERSESIDE = "reverseside"
    val TEXT = "text"
    val FONT = "font"

    open fun getInputStream(): InputStream? {
        val feedURLString = feedUrl.toExternalForm().toLowerCase(Locale.ROOT)
        Log.d("FlashCardBaseFeedParser::getInputStream", "feedURLString =$feedURLString")
        try {
            if (feedURLString.endsWith(".gz")) {
                Log.d("FlashCardBaseFeedParser::getInputStream", "found a .gz file")
                return GZIPInputStream(feedUrl.openConnection().getInputStream())
            } else if (feedUrl.toExternalForm().endsWith(".pau") || feedUrl.toExternalForm()
                    .endsWith(".xml")
            ) {
                Log.d("FlashCardBaseFeedParser::getInputStream", "found a .pau file")
                return feedUrl.openConnection().getInputStream()
            } else if (feedUrl.toExternalForm().endsWith(".csv")) {
                Log.d("FlashCardBaseFeedParser::getInputStream", "found a .csv file")
                return feedUrl.openConnection().getInputStream()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return null
    }
}