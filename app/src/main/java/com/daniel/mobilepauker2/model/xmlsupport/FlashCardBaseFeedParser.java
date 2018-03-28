package com.daniel.mobilepauker2.model.xmlsupport;

import com.daniel.mobilepauker2.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;


abstract class FlashCardBaseFeedParser implements IFlashCardFeedParser {

    static final String LESSON = "lesson";
    static final String DESCRIPTION = "description";
    static final String BATCH = "batch";
    static final String CARD = "card";
    static final String FRONTSIDE = "frontside";
    static final String REVERSESIDE = "reverseside";
    static final String TEXT = "text";
    static final String FONT = "font";

    private final URL feedUrl;

    FlashCardBaseFeedParser(URL feedUrl) {
        this.feedUrl = feedUrl;
    }

    InputStream getInputStream() {

        String feedURLString = feedUrl.toExternalForm().toLowerCase();
        Log.d("FlashCardBaseFeedParser::getInputStream", "feedURLString =" + feedURLString);
        try {
            if (feedURLString.endsWith(".gz")) {
                Log.d("FlashCardBaseFeedParser::getInputStream", "found a .gz file");
                return (new GZIPInputStream(feedUrl.openConnection().getInputStream()));
            } else if (feedUrl.toExternalForm().endsWith(".pau") || feedUrl.toExternalForm().endsWith(".xml")) {
                Log.d("FlashCardBaseFeedParser::getInputStream", "found a .pau file");
                return feedUrl.openConnection().getInputStream();
            } else if (feedUrl.toExternalForm().endsWith(".csv")) {
                Log.d("FlashCardBaseFeedParser::getInputStream", "found a .csv file");
                return feedUrl.openConnection().getInputStream();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;

    }
}
