package com.daniel.mobilepauker2.lessonimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.BaseActivity
import com.daniel.mobilepauker2.core.BaseViewModel
import com.daniel.mobilepauker2.dropbox.SyncDialog
import com.daniel.mobilepauker2.main.MainMenu
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.settings.SettingsManager
import com.daniel.mobilepauker2.settings.SettingsManager.Keys
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.pauker_native.Log
import com.daniel.mobilepauker2.pauker_native.PaukerAndModelManager
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.KoinComponent
import java.io.*

@Suppress("UNUSED_PARAMETER")
class LessonReceiver : BaseActivity() {
    private val context: Activity = this
    override val baseViewModel: BaseViewModel by viewModel()
    lateinit var paukerAndModelManager: PaukerAndModelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_dialog)
        (findViewById<View>(R.id.pTitle) as TextView).setText(R.string.import_title)
        val intent = intent
        Log.d("LessonReceiver::importLesson", "ENTRY")
        if (intent == null || intent.data == null) {
            Log.d(
                    "LessonReceiver::importLesson filename: ",
                    "intent is null"
            )
            baseViewModel.postMessage(R.string.error_no_pauker_file)
            finish()
        } else {
            val fileUri = intent.data
            val filePath = fileUri?.encodedPath
            Log.d(
                    "LessonReceiver::importLesson filePath: ",
                    filePath
            )
            if (filePath == null || PaukerManager.instance().isNameEmpty(filePath)) {
                baseViewModel.postMessage(R.string.error_no_file_found)
                finish()
            } else {
                val localFile = File(
                        Environment.getExternalStorageDirectory()
                                .toString() + PaukerManager.instance().applicationDataDirectory,
                        File(filePath).name
                )
                if (localFile.exists()) {
                    Log.d(
                            "LessonReceiver::importLesson localFile: ",
                            "File existiert bereits"
                    )
                    baseViewModel.postMessage(R.string.error_file_already_exists, Toast.LENGTH_LONG)
                    finish()
                } else {
                    try {
                        val inputStream =
                                contentResolver.openInputStream(fileUri)
                        val outputStream =
                                FileOutputStream(localFile)
                        if (inputStream != null) {
                            copyFile(inputStream, outputStream)
                            Log.d(
                                    "LessonReceiver::importLesson",
                                    "import success"
                            )
                            ModelManager.instance().addLesson(context, localFile)
                            Log.d(
                                    "LessonReceiver::importLesson",
                                    "lesson added"
                            )
                            paukerAndModelManager.loadLessonFromFile(localFile)
                            Log.d(
                                    "LessonReceiver::importLesson",
                                    "lesson opend"
                            )
                            Log.d(
                                    "LessonReceiver::importLesson",
                                    "start MainMenu"
                            )
                            baseViewModel.postMessage(R.string.lesson_import_success, Toast.LENGTH_LONG)
                            if (SettingsManager.instance().getBoolPreference(
                                            context,
                                            Keys.AUTO_UPLOAD
                                    )
                            ) {
                                uploadFile(localFile)
                                Log.d(
                                        "LessonReceiver::importLesson",
                                        "Lesson uploaded"
                                )
                            } else {
                                restartApp()
                            }
                        }
                    } catch (e: IOException) {
                        baseViewModel.postMessage(R.string.error_while_reading)
                        finish()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG) {
            restartApp()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
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

    private fun uploadFile(localFile: File) {
        val accessToken = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.DROPBOX_ACCESS_TOKEN, null)
        val intent = Intent(context, SyncDialog::class.java)
        intent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken)
        intent.putExtra(SyncDialog.FILES, localFile)
        intent.action = SyncDialog.UPLOAD_FILE_ACTION
        startActivityForResult(
                intent,
                Constants.REQUEST_CODE_SYNC_DIALOG
        )
    }

    fun cancelClicked(view: View?) {}
}