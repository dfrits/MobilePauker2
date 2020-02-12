package com.daniel.mobilepauker2.core

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
object Constants {

    //APP-Konstansten
    const val DEFAULT_APP_FILE_DIRECTORY = "/Mobile Pauker++/"
    const val TIMER_BAR_CHANNEL_ID = "Timerbar"
    const val NOTIFICATION_CHANNEL_ID = "NOTIFICATION_CHANNEL_ID"
    const val TIME_BAR_ID = 1
    const val NOTIFICATION_ID = 2

    // Standard-XML-Werte
    const val STANDARD_ORIENTATION = "LTR"
    const val STANDARD_REPEAT = false

    // Dropboxkonstanten
    const val DROPBOX_APP_KEY = "9rqxyyq8cty3cf1"
    const val DROPBOX_ACCESS_TOKEN = "DROPBOX_ACCESS_TOKEN"
    const val DROPBOX_USER_ID = "DROPBOX_USER_ID"
    const val DROPBOX_PATH = ""

    // Datei, in der die Namen der lokal gelöschten Lektionen stehen
    const val DELETED_FILES_NAMES_FILE_NAME = "trash"

    // Datei, in der die Namen der lokal neuhinzugefügten Lektionen stehen
    const val ADDED_FILES_NAMES_FILE_NAME = "news"

    // Name einer neuen, nicht gespeicherten Lektion
    const val DEFAULT_FILE_NAME = "NONE"

    // Konstanten für den Swipegestenerkenner
    const val STACK_INDEX = "STACK_INDEX"

    // Activityrequestcodes
    const val REQUEST_CODE_SAVE_DIALOG_NORMAL = 1
    const val REQUEST_CODE_SYNC_DIALOG = 2
    const val REQUEST_CODE_EDIT_CARD = 3
    const val REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN = 4
    const val REQUEST_CODE_SAVE_DIALOG_NEW_LESSON = 5
    const val REQUEST_CODE_SAVE_DIALOG_OPEN = 6
    const val REQUEST_CODE_DB_ACC_DIALOG = 7

    // Messagekey für den Handler
    const val MESSAGE_BOOL_KEY = "RESULT"
    const val MESSAGE_MSG_KEY = "MSG"

    // Keys
    const val CURSOR_POSITION = "CURSOR_POSITION"
    const val LAST_TEXT_COLOR_CHOICE = "LAST_TEXT_COLOR_CHOICE"
    const val LAST_BACK_COLOR_CHOICE = "LAST_BACK_COLOR_CHOICE"
    const val KEEP_OPEN_KEY = "KEEP_OPEN_KEY"
    val PAUKER_FILE_ENDING = arrayOf(".pau.gz", ".xml.gz")

    // Shortcut
    const val SHORTCUT_ACTION = "com.daniel.mobilepauker2.main.ShortcutReceiver"
    const val SHORTCUT_EXTRA = "Filename"
}