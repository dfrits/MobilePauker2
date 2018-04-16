package com.daniel.mobilepauker2.utils;

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class Constants {
    public static final String DEFAULT_APP_FILE_DIRECTORY = "/MobilePauker/";

    // Dropboxkonstanten
    public final static String DROPBOX_APP_KEY = "9rqxyyq8cty3cf1";
    public final static String DROPBOX_ACCESS_TOKEN = "DROPBOX_ACCESS_TOKEN";
    public final static String DROPBOX_USER_ID = "DROPBOX_USER_ID";
    public static final String DROPBOX_PATH = "";

    // Datei, in der die Namen der lokal gelöschten Lektionen stehen
    public static final String DELETED_FILES_NAMES_FILE_NAME = "trash";

    // Name einer neuen, nicht gespeicherten Lektion
    public static final String DEFAULT_FILE_NAME = "NONE";

    // Konstanten für den Swipegestenerkenner
    public static final String STACK_INDEX = "STACK_INDEX";
    public static final int SWIPE_MIN_DISTANCE = 60;
    public static final int SWIPE_MAX_OFF_PATH = 400;
    public static final int SWIPE_THRESHOLD_VELOCITY = 100;

    // Activityrequestcodes
    public static final int REQUEST_CODE_SAVE_DIALOG = 1;
    public static final int REQUEST_CODE_SYNC_DIALOG = 2;
    public static final int REQUEST_CODE_EDIT_CARD = 3;

    // Messagekey für den Handler
    public static final String MESSAGE_KEY = "RESULT";

    public static String CURSOR_POSITION = "CURSOR_POSITION";
}
