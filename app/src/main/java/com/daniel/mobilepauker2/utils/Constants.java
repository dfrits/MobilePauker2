package com.daniel.mobilepauker2.utils;

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class Constants {
    //APP-Konstansten
    public static final String DEFAULT_APP_FILE_DIRECTORY = "/Mobile Pauker++/";
    public static final String TIMER_NOTIFY_CHANNEL_ID = "Timers";
    public static final String TIMER_BAR_CHANNEL_ID = "Timerbar";
    public static final int TIME_NOTIFY_ID = 0;
    public static final int TIME_BAR_ID = 1;

    // Standard-XML-Werte
    public static final String STANDARD_ORIENTATION = "LTR";
    public static final boolean STANDARD_REPEAT = false;

    // Dropboxkonstanten
    public final static String DROPBOX_APP_KEY = "9rqxyyq8cty3cf1";
    public final static String DROPBOX_ACCESS_TOKEN = "DROPBOX_ACCESS_TOKEN";
    public final static String DROPBOX_USER_ID = "DROPBOX_USER_ID";
    public static final String DROPBOX_PATH = "";

    // Datei, in der die Namen der lokal gelöschten Lektionen stehen
    public static final String DELETED_FILES_NAMES_FILE_NAME = "trash";

    // Datei, in der die Namen der lokal neuhinzugefügten Lektionen stehen
    public static final String ADDED_FILES_NAMES_FILE_NAME = "news";

    // Name einer neuen, nicht gespeicherten Lektion
    public static final String DEFAULT_FILE_NAME = "NONE";

    // Konstanten für den Swipegestenerkenner
    public static final String STACK_INDEX = "STACK_INDEX";

    // Activityrequestcodes
    public static final int REQUEST_CODE_SAVE_DIALOG_NORMAL = 1;
    public static final int REQUEST_CODE_SYNC_DIALOG = 2;
    public static final int REQUEST_CODE_EDIT_CARD = 3;
    //public static final int REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN = 4;
    public static final int REQUEST_CODE_SAVE_DIALOG_NEW_LESSON = 5;
    public static final int REQUEST_CODE_SAVE_DIALOG_OPEN = 6;
    public static final int REQUEST_CODE_DB_ACC_DIALOG = 7;

    // Messagekey für den Handler
    public static final String MESSAGE_BOOL_KEY = "RESULT";
    public static final String MESSAGE_MSG_KEY = "MSG";

    // Keys
    public static final String CURSOR_POSITION = "CURSOR_POSITION";
    public static final String LAST_TEXT_COLOR_CHOICE = "LAST_TEXT_COLOR_CHOICE";
    public static final String LAST_BACK_COLOR_CHOICE = "LAST_BACK_COLOR_CHOICE";
    public static final String KEEP_OPEN_KEY = "KEEP_OPEN_KEY";
    public static final String[] PAUKER_FILE_ENDING = {".pau.gz", ".xml.gz", ".pau"};
}
