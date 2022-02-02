package de.daniel.mobilepauker2.lessonimport

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.mainmenu.MainMenu
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import java.io.*
import javax.inject.Inject


class LessonReceiver : AppCompatActivity() {
    private val context = this

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        setContentView(R.layout.progress_dialog)
        (findViewById<View>(R.id.pTitle) as TextView).setText(R.string.import_title)

        Log.d("LessonReceiver::importLesson", "ENTRY")

        if (intent == null || intent.data == null) {
            Log.d("LessonReceiver::importLesson filename: ", "intent is null")
            toaster.showToast(
                context as Activity,
                R.string.simple_error_message,
                Toast.LENGTH_LONG
            )
            finish()
        } else {
            val fileUri = intent.data
            if (fileUri == null) {
                toaster.showToast(context as Activity, R.string.no_lesson_found, Toast.LENGTH_SHORT)
                finish()
            } else {
                val fileName = when {
                    fileUri.scheme.equals(ContentResolver.SCHEME_CONTENT) -> {
                        getFileNameFromContentUri(fileUri)
                    }
                    fileUri.scheme.equals(ContentResolver.SCHEME_FILE) -> {
                        fileUri.lastPathSegment
                    }
                    else -> {
                        toaster.showToast(
                            context as Activity,
                            R.string.no_lesson_found,
                            Toast.LENGTH_SHORT
                        )
                        finish()
                        null
                    }
                }

                val localFile = getLocalFile(fileName)
                localFile?.let { importFileFromUri(fileUri, localFile) }
            }
        }
    }

    private fun getFileNameFromContentUri(fileUri: Uri): String? {
        val returnCursor: Cursor? = context.contentResolver.query(
            fileUri, null, null, null, null
        )
        try {
            if (returnCursor != null && returnCursor.moveToFirst()) {
                val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return returnCursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            return null
        } finally {
            returnCursor?.close()
        }
        return null
    }

    private fun getLocalFile(fileName: String?): File? {
        if (fileName == null || !dataManager.isNameValid(fileName)) {
            toaster.showToast(context as Activity, R.string.no_lesson_found, Toast.LENGTH_SHORT)
            finish()
            return null
        } else {
            val localFile = dataManager.getFilePathForName(fileName)

            if (localFile.exists()) {
                Log.d("LessonReceiver::importLesson localFile: ", "File existiert bereits")
                toaster.showToast(
                    context as Activity,
                    R.string.file_already_exists,
                    Toast.LENGTH_LONG
                )
                finish()
            }

            return localFile
        }
    }

    private fun importFileFromUri(fileUri: Uri, localFile: File) {
        try {
            val inputStream = contentResolver.openInputStream(fileUri)
            val outputStream = FileOutputStream(localFile)

            inputStream?.let {
                copyFile(inputStream, outputStream)
                Log.d("LessonReceiver::importLesson", "import success")

                dataManager.loadLessonFromFile(localFile)
                Log.d("LessonReceiver::importLesson", "lesson opend")

                Log.d("LessonReceiver::importLesson", "start MainMenu")
                toaster.showToast(
                    context as Activity,
                    R.string.lesson_import_success,
                    Toast.LENGTH_LONG
                )

                restartApp()
            }
        } catch (e: IOException) {
            toaster.showToast(context, "Fehler beim Einlesen", Toast.LENGTH_SHORT)
            finish()
        }
    }

    private fun restartApp() {
        Log.d("LessonReceiver::restartApp", "App restarted")
        val mainMenu = Intent(context, MainMenu::class.java)
        mainMenu.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(mainMenu)
        finish()
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    fun cancelClicked(view: View?) {}
}