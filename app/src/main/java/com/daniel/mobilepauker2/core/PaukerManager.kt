/*
 * Copyright 2011 Brian Ford
 *
 * This file is part of Pocket Pauker.
 *
 * Pocket Pauker is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Pocket Pauker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * See http://www.gnu.org/licenses/.

 */
package com.daniel.mobilepauker2.core

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.core.model.xmlsupport.FlashCardXMLPullFeedParser
import com.daniel.mobilepauker2.pauker_native.Log
import java.io.File
import java.io.IOException

class PaukerManager(val modelManager: ModelManager) {

    var currentFileName: String? =
            Constants.DEFAULT_FILE_NAME
        private set
    private var mFileAbsolutePath: String? = null
    var isSaveRequired = false

    /**
     * Get the default application data directory.
     *
     *
     * Note that this is not necessarily where the current file has been loaded from.
     * @return Default application data directory
     */
    val applicationDataDirectory: String?
        get() =// The application data directory can change when a file is loaded
            Constants.DEFAULT_APP_FILE_DIRECTORY

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
        currentFileName = Constants.DEFAULT_FILE_NAME
        modelManager.createNewLesson()
    }

    // Todo replace this with the File Class
    var fileAbsolutePath: String?
        get() = mFileAbsolutePath
        private set(fileAbsolutePath) { // Validate the filename
            if (fileAbsolutePath == null) {
                return
            }
            mFileAbsolutePath = fileAbsolutePath
        }

    fun setCurrentFileName(filename: String): Boolean {
        var newFilename = filename
        if (!newFilename.endsWith(".pau.gz")) newFilename += ".pau.gz"
        // Validate the filename
        if (!isNameValid(newFilename)) return false
        currentFileName = newFilename
        return true
    }

    fun isNameValid(filename: String?): Boolean {
        return if (filename == null || filename.isEmpty() || isNameEmpty(filename)) {
            false
        } else validateFileEnding(filename)
    }

    fun isNameEmpty(fileName: String): Boolean {
        for (ending in Constants.PAUKER_FILE_ENDING) {
            if (fileName == ending) return true
        }
        return false
    }

    /**
     * Gibt den Namen der aktuellen Lektion ohne Endungen zurück.
     * @return Lektionsname ohne Endungen
     */
    val readableFileName: String?
        get() {
            val filename = currentFileName
            return getReadableFileName(filename)
        }

    /**
     * Loads a lesson from a file
     * @param filename Name der Datei, die importiert werden soll
     * @return **True** if lesson loaded ok
     */
    @Throws(IOException::class)
    fun getFilePath(
            context: Context,
            filename: String?
    ): File { // Validate the filename
        if (!validateFilename(filename)) {
            showToast(
                    context as Activity,
                    R.string.error_filename_invalid,
                    Toast.LENGTH_LONG
            )
            throw IOException("Filename invalid")
        }
        val filePath =
                Environment.getExternalStorageDirectory().toString() + applicationDataDirectory + filename
        return File(filePath)
    }

    /**
     * Gibt den Namen der Lektion ohne Endungen zurück.
     * @param filename Lektionsname
     * @return Lektionsname ohne Endungen
     */
    fun getReadableFileName(filename: String?): String? {
        return if (validateFileEnding(filename)) {
            filename!!.substring(0, filename.length - 7)
        } else if (filename!!.endsWith(".pau") || filename.endsWith(".xml")) {
            filename.substring(0, filename.length - 4)
        } else {
            filename
        }
    }

    fun validateFilename(filename: String?): Boolean {
        if (filename == null) {
            Log.d("Validate Filename", "File name is invalid")
            return false
        }
        if (!validateFileEnding(filename)) {
            Log.d(
                    "Validate Filename",
                    "File not ending with .pau.gz"
            )
            return false
        }
        return true
    }

    private fun validateFileEnding(fileName: String?): Boolean {
        for (ending in Constants.PAUKER_FILE_ENDING) {
            if (fileName!!.endsWith(ending)) {
                return true
            }
        }
        return false
    }

    @Throws(SecurityException::class)
    fun listFiles(context: Context): Array<File>? {
        val appDirectory =
                File(Environment.getExternalStorageDirectory().toString() + applicationDataDirectory)
        val files: Array<File>
        if (!appDirectory.exists() && !appDirectory.mkdir()) {
            throw SecurityException()
        }
        files = if (appDirectory.exists() && appDirectory.isDirectory) {
            appDirectory.listFiles { file -> validateFilename(file.name) }
        } else {
            showToast(
                    context as Activity,
                    R.string.error_importflashcardfile_directory,
                    Toast.LENGTH_LONG
            )
            return null
        }
        return files
    }

    fun isFileExisting(context: Context, fileName: String): Boolean {
        try {
            val files = listFiles(context)
            for (file in files!!) {
                if (file.name == fileName) {
                    return true
                }
            }
        } catch (ignored: SecurityException) {
        }
        return false
    }

    @Throws(IOException::class)
    fun loadLessonFromFile(file: File?) {
        val uri = file!!.toURI()
        val xmlFlashCardFeedParser = FlashCardXMLPullFeedParser(uri.toURL())
        val lesson = xmlFlashCardFeedParser.parse()
        setCurrentFileName(file.name)
        fileAbsolutePath = file.absolutePath
        modelManager.lesson = lesson
    }

    companion object {
        lateinit var manager: PaukerManager
        fun instance(): PaukerManager {
            return manager
        }

        fun showToast(context: Activity, text: String?, duration: Int) {
            context.runOnUiThread {
                if (text != null && text.isNotEmpty()) {
                    Toast.makeText(context, text, duration).show()
                }
            }
        }

        fun showToast(context: Activity, textResource: Int, duration: Int) {
            showToast(context, context.getString(textResource), duration)
        }
    }
}