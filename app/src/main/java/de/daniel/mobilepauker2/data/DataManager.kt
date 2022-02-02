package de.daniel.mobilepauker2.data

import android.content.Context
import android.os.Environment
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.xml.FlashCardXMLPullFeedParser
import de.daniel.mobilepauker2.data.xml.FlashCardXMLStreamWriter
import de.daniel.mobilepauker2.lesson.Lesson
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.models.CacheFile
import de.daniel.mobilepauker2.models.NextExpireDateResult
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject constructor(val context: @JvmSuppressWildcards Context) {
    var saveRequired: Boolean = false
    var currentFileName = Constants.DEFAULT_FILE_NAME
        private set

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var toaster: Toaster

    init {
        (context as PaukerApplication).applicationSingletonComponent.inject(this)
    }

    fun setNewFileName(newName: String): Boolean {
        setCorrectFileEnding(newName).also {
            if (!isNameValid(it)) return false

            currentFileName = it
            return true
        }
    }

    fun getReadableCurrentFileName(): String = getReadableFileName(currentFileName)

    fun getReadableFileName(fileName: String) = if (validateFileEnding(fileName)) {
        fileName.substring(0, fileName.length - 7)
    } else if (fileName.endsWith(".pau") || fileName.endsWith(".xml")) {
        fileName.substring(0, fileName.length - 4)
    } else {
        fileName
    }

    fun getPathOfCurrentFile(): File = getFilePathForName(currentFileName)

    @Throws(IOException::class)
    fun getFilePathForName(filename: String): File {
        if (!validateFileEnding(filename)) {
            throw IOException("Filename invalid")
        }
        val filePath = "${Environment.getExternalStorageDirectory()}" +
            "${Constants.DEFAULT_APP_FILE_DIRECTORY}$filename"
        return File(filePath)
    }

    @Throws(SecurityException::class)
    fun listFiles(): Array<File> {
        val appDirectory = File(
            Environment.getExternalStorageDirectory()
                .toString() + Constants.DEFAULT_APP_FILE_DIRECTORY
        )

        if (!appDirectory.exists() && !appDirectory.mkdir()) return emptyArray()

        if (appDirectory.exists() && appDirectory.isDirectory) {
            val listFiles = appDirectory.listFiles { file ->
                isNameValid(file.name)
            }
            if (listFiles != null) return listFiles
        }

        return emptyArray()
    }

    fun isFileExisting(fileName: String): Boolean {
        val files = listFiles()
        for (file in files) {
            if (file.name == fileName) {
                return true
            }
        }
        return false
    }

    @Throws(IOException::class)
    fun loadLessonFromFile(file: File) {
        val uri = file.toURI()
        val xmlFlashCardFeedParser = FlashCardXMLPullFeedParser(uri.toURL())
        val lesson: Lesson = xmlFlashCardFeedParser.parse()
        currentFileName = file.name
        lessonManager.setupLesson(lesson)
    }

    fun getNextExpireDate(file: File): NextExpireDateResult {
        val uri = file.toURI()
        val xmlFlasCardFeedParser = FlashCardXMLPullFeedParser(uri.toURL())

        return xmlFlasCardFeedParser.getNextExpireDate()
    }

    fun writeLessonToFile(isNewFile: Boolean): SaveResult {
        if (currentFileName == Constants.DEFAULT_FILE_NAME) {
            return SaveResult(false, context.getString(R.string.error_filename_invalid))
        }

        val result = try {
            FlashCardXMLStreamWriter(
                getFilePathForName(currentFileName),
                isNewFile,
                lessonManager.lesson
            ).writeLesson()
        } catch (e: IOException) {
            SaveResult(false, context.getString(R.string.error_filename_invalid))
        }

        if (result.successful) {
            saveRequired = false
        } else {
            Log.e("Save Lesson", result.errorMessage)
        }

        return result
    }

    fun deleteLesson(file: File): Boolean = file.delete()

    fun isNameValid(filename: String): Boolean {
        return !isNameEmpty(filename) && validateFileEnding(filename)
    }

    fun setCorrectFileEnding(name: String): String {
        if (name.endsWith(".pau")) return "$name.gz"

        if (name.endsWith(".pau.gz") || name.endsWith(".xml.gz")) return name

        return "$name.pau.gz"
    }

    fun cacheFiles() {
        val currentFiles: MutableList<CacheFile> = mutableListOf()
        listFiles().forEach {
            currentFiles.add(CacheFile(it.path, it.lastModified()))
        }
        val json = Gson().toJson(currentFiles)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(Constants.CACHED_FILES, json)
            .apply()
    }

    fun getCachedFiles(): List<File>? {
        val json = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.CACHED_FILES, null) ?: return null

        val files = mutableListOf<File>()

        Gson().fromJson(json, Array<CacheFile>::class.java).forEach {
            val file = File(it.path)
            file.setLastModified(it.lastModified)
            files.add(file)
        }

        return files
    }

    fun cacheCursor(cursor: String?) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(Constants.CACHED_CURSOR, cursor)
            .apply()
    }

    fun getCachedCursor(): String? = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(Constants.CACHED_CURSOR, null)

    private fun isNameEmpty(fileName: String): Boolean {
        if (fileName.isEmpty()) return true

        for (ending in Constants.PAUKER_FILE_ENDING) {
            if (fileName == ending) return true
        }
        return false
    }

    private fun validateFileEnding(fileName: String): Boolean {
        for (ending in Constants.PAUKER_FILE_ENDING) {
            if (fileName.endsWith(ending)) {
                return true
            }
        }
        return false
    }
}