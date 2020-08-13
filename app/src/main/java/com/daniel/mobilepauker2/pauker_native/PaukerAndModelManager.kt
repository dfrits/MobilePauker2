package com.daniel.mobilepauker2.pauker_native

import android.app.Activity
import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.core.model.xmlsupport.FlashCardXMLPullFeedParser
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.util.*

class PaukerAndModelManager(
        val paukerManager: PaukerManager,
        val modelManager: ModelManager
) {

    @Throws(IOException::class)
    fun loadLessonFromFile(file: File?) {
        val uri = file!!.toURI()
        val xmlFlashCardFeedParser = FlashCardXMLPullFeedParser(uri.toURL())
        val lesson = xmlFlashCardFeedParser.parse()
        paukerManager.setCurrentFileName(file.name)
        paukerManager.fileAbsolutePath = file.absolutePath
        modelManager.lesson = lesson
    }

    /**
     * Setup a new lesson in the default application directory
     *
     *
     * Note the lesson is not created until it is saved
     * Note this method appends .pau.gz to the application filename.
     */
    fun setupNewApplicationLesson() { /*String _filename = filename + ".pau.gz";
        if (setCurrentFileName(_filename)) {
            mApplicationDataDirectory = Constants.DEFAULT_APP_FILE_DIRECTORY;
            String filePath = Environment.getExternalStorageDirectory() + getApplicationDataDirectory() + _filename;
            File file = new File(filePath);
            setCurrentFileName(file.getName());
            setFileAbsolutePath(file.getAbsolutePath());
            ModelManager.instance().createNewLesson(filename);
            return true;
        } else {
            return false;
        }*/
        paukerManager.currentFileName = Constants.DEFAULT_FILE_NAME
        modelManager.createNewLesson()
    }

    fun addLesson(context: Context) {
        val filename = paukerManager.currentFileName
        modelManager.addLesson(context, filename)
    }

    /**
     * Zeigt einen Toast mit dem nächsten Ablaufdatum an, wenn es in den Einstellungen aktiviert ist.
     * @param context Kontext der aufrufenden Activity
     */
    fun showExpireToast(context: Context) {
        //if (!settingsManager.getBoolPreference(context, SettingsManager.Keys.ENABLE_EXPIRE_TOAST)) return
        val filePath = paukerManager.filePath
        val uri = filePath.toURI()
        val parser: FlashCardXMLPullFeedParser
        try {
            parser = FlashCardXMLPullFeedParser(uri.toURL())
            val map = parser.nextExpireDate
            if (map[0] > Long.MIN_VALUE) {
                val dateL = map[0]
                val cal =
                        Calendar.getInstance(Locale.getDefault())
                cal.timeInMillis = dateL
                val date =
                        DateFormat.format("dd.MM.yyyy HH:mm", cal).toString()
                var text = context.getString(R.string.next_expire_date)
                text = "$text $date"
                PaukerManager.showToast(context as Activity, text, Toast.LENGTH_LONG * 2)
            }
        } catch (ignored: MalformedURLException) {
        }
    }
}