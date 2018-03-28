package com.daniel.mobilepauker2.model.xmlsupport;

import com.daniel.mobilepauker2.model.pauker_native.Lesson;

import java.io.EOFException;

public interface IFlashCardFeedParser {
    Lesson parse() throws EOFException;
}

