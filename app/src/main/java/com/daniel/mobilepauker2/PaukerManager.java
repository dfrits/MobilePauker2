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

package com.daniel.mobilepauker2;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.pauker_native.Lesson;
import com.daniel.mobilepauker2.model.xmlsupport.FlashCardXMLPullFeedParser;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;

public class PaukerManager {

    private static PaukerManager instance = null;

    private String mCurrentFileName = Constants.DEFAULT_FILE_NAME;
    private String mFileAbsolutePath = null;
    private boolean mSaveRequired = false;

    private PaukerManager() {
    }

    public static PaukerManager instance() {
        if (instance == null) {
            instance = new PaukerManager();
        }
        return instance;
    }

    // TODO Toaster
    public static void showToast(final Activity context, final String text, final int duration) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (text != null && !text.isEmpty()) {
                    Toast.makeText(context, text, duration).show();
                }
            }
        });
    }

    // TODO Toaster
    public static void showToast(Activity context, int textResource, int duration) {
        showToast(context, context.getString(textResource), duration);
    }

    /**
     * Get the default application data directory.
     * <p>
     * Note that this is not necessarily where the current file has been loaded from.
     * @return Default application data directory
     */
    public String getApplicationDataDirectory() {
        // The application data directory can change when a file is loaded
        return Constants.DEFAULT_APP_FILE_DIRECTORY;
    }

    /**
     * Setup a new lesson in the default application directory
     * <p>
     * Note the lesson is not created until it is saved
     * Note this method appends .pau.gz to the application filename.
     */
    public void setupNewApplicationLesson() { // TODO LessonManager
        /*String _filename = filename + ".pau.gz";
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
        mCurrentFileName = Constants.DEFAULT_FILE_NAME;
        ModelManager.instance().createNewLesson();
    }

    public String getFileAbsolutePath() {
        return mFileAbsolutePath;
    }

    private void setFileAbsolutePath(String fileAbsolutePath) {
        // Validate the filename
        if (fileAbsolutePath == null) {
            return;
        }

        mFileAbsolutePath = fileAbsolutePath;
    }

    public boolean setCurrentFileName(String filename) {
        if (!filename.endsWith(".pau.gz"))
            filename += ".pau.gz";

        // Validate the filename
        if (!isNameValid(filename)) return false;

        mCurrentFileName = filename;
        return true;
    }

    public boolean isNameValid(String filename) {
        if (filename == null || filename.isEmpty() || isNameEmpty(filename)) {
            return false;
        }
        return validateFileEnding(filename);
    }

    public boolean isNameEmpty(String fileName) {
        for (String ending : Constants.PAUKER_FILE_ENDING) {
            if (fileName.equals(ending)) return true;
        }
        return false;
    }

    public String getCurrentFileName() {
        return mCurrentFileName;
    }

    public boolean isSaveRequired() {
        return mSaveRequired;
    }

    public void setSaveRequired(boolean saveRequired) {
        mSaveRequired = saveRequired;
    }

    /**
     * Gibt den Namen der aktuellen Lektion ohne Endungen zurück.
     * @return Lektionsname ohne Endungen
     */
    public String getReadableFileName() {
        String filename = mCurrentFileName;

        return getReadableFileName(filename);
    }

    /**
     * Loads a lesson from a file
     * @param filename Name der Datei, die importiert werden soll
     * @return <b>True</b> if lesson loaded ok
     */
    public File getFilePath(Context context, String filename) throws IOException {
        // Validate the filename
        if (!validateFilename(filename)) {
            PaukerManager.showToast((Activity) context, R.string.error_filename_invalid, Toast.LENGTH_LONG);
            throw new IOException("Filename invalid");
        }

        String filePath = Environment.getExternalStorageDirectory() + getApplicationDataDirectory() + filename;
        return new File(filePath);
    }

    /**
     * Gibt den Namen der Lektion ohne Endungen zurück.
     * @param filename Lektionsname
     * @return Lektionsname ohne Endungen
     */
    public String getReadableFileName(String filename) {
        if (validateFileEnding(filename)) {
            return filename.substring(0, filename.length() - 7);
        } else if (filename.endsWith(".pau") || filename.endsWith(".xml")) {
            return filename.substring(0, filename.length() - 4);
        } else {
            return filename;
        }
    }

    public boolean validateFilename(String filename) {
        if (filename == null) {
            Log.d("Validate Filename", "File name is invalid");
            return false;
        }

        if (!validateFileEnding(filename)) {
            Log.d("Validate Filename", "File not ending with .pau.gz");
            return false;
        }

        return true;
    }

    private boolean validateFileEnding(String fileName) {
        for (String ending : Constants.PAUKER_FILE_ENDING) {
            if (fileName.endsWith(ending)) {
                return true;
            }
        }
        return false;
    }

    public File[] listFiles(final Context context) throws SecurityException {
        File appDirectory = new File(Environment.getExternalStorageDirectory() + getApplicationDataDirectory());
        File[] files = new File[0];

        if (!appDirectory.exists() && !appDirectory.mkdir()) {
            return files;
        }

        if (appDirectory.exists() && appDirectory.isDirectory()) {
            files = appDirectory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return validateFilename(file.getName());
                }
            });
        } else {
            showToast((Activity) context, R.string.error_importflashcardfile_directory, Toast.LENGTH_LONG);
            return files;
        }
        return files;
    }

    public boolean isFileExisting(Context context, String fileName) {
        File[] files = listFiles(context);

        for (File file : files) {
            if (file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    public void loadLessonFromFile(File file) throws IOException {
        URI uri = file.toURI();
        FlashCardXMLPullFeedParser xmlFlashCardFeedParser = new FlashCardXMLPullFeedParser(uri.toURL());
        Lesson lesson = xmlFlashCardFeedParser.parse();
        setCurrentFileName(file.getName());
        setFileAbsolutePath(file.getAbsolutePath());
        ModelManager.instance().setLesson(lesson);
    }
}
