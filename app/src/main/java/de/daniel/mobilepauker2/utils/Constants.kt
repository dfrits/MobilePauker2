package de.daniel.mobilepauker2.utils

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
    const val DROPBOX_AUTH_ACTION = "de.daniel.mobilepauker2.dropbox.auth"
    const val DROPBOX_UNLINK_ACTION = "de.daniel.mobilepauker2.dropbox.unlink"

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
    const val HIDE_TIMER_KEY = "HIDE_TIMER_KEY"
    val PAUKER_FILE_ENDING = arrayOf(".pau.gz", ".xml.gz")

    // Shortcut
    const val SHORTCUT_ACTION = "com.daniel.mobilepauker2.activities.ShortcutReceiver"
    const val SHORTCUT_EXTRA = "Filename"

    // Sync
    const val ACCESS_TOKEN = "ACCESS_TOKEN"
    const val FILES = "FILES"
    const val SYNC_ALL_ACTION = "SYNC_ALL_ACTION"
    const val UPLOAD_FILE_ACTION = "UPLOAD_FILE_ACTION"
    const val SYNC_FILE_ACTION = "SYNC_FILE_ACTION"

    // Cache
    const val CACHED_FILES = "CACHED_FILES"
    const val CACHED_CURSOR = "CACHED_CURSOR"
}