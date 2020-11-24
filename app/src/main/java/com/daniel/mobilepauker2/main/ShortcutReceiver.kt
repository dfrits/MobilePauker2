package com.daniel.mobilepauker2.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.core.model.ui.TextDrawable
import com.daniel.mobilepauker2.dropbox.SyncDialog
import com.daniel.mobilepauker2.learning.LearnCardsActivity
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.Log
import com.daniel.mobilepauker2.pauker_native.PaukerAndModelManager
import com.daniel.mobilepauker2.settings.SettingsManager
import com.daniel.mobilepauker2.settings.SettingsManager.Keys
import org.koin.core.KoinComponent
import org.koin.core.get
import java.io.IOException

@Suppress("UNUSED_PARAMETER")
class ShortcutReceiver : Activity(), KoinComponent {
    private val paukerManager: PaukerManager = get()
    private val context: Context = this
    lateinit var paukerAndModelManager: PaukerAndModelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_dialog)
        val progressBar = findViewById<RelativeLayout>(R.id.pFrame)
        progressBar.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        title.setText(R.string.open_lesson_hint)
        val intent = intent
        if (Constants.SHORTCUT_ACTION == intent.action) {
            openLesson(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN) {
            if (resultCode == RESULT_OK) {
                Log.d(
                        "ShortcutReceiver::onActivityResult",
                        "File wurde aktualisiert"
                )
            } else {
                Log.d(
                        "ShortcutReceiver::onActivityResult",
                        "File wurde nicht aktualisiert"
                )
            }
            val filename =
                    intent.getStringExtra(Constants.SHORTCUT_EXTRA)
            if (filename != null) {
                openLesson(filename)
            } else {
                Log.d(
                        "ShortcutReceiver::onActivityResult",
                        "Filename is null"
                )
            }
        }
    }

    /**
     * Handelt den Shortcut und öffnet die entsprechende Lektion.
     * @param shortcutIntent Intent vom Shortcut.
     */
    private fun openLesson(shortcutIntent: Intent) {
        if (LearnCardsActivity.isLearningRunning) {
            PaukerManager.showToast(
                    context as Activity,
                    R.string.shortcut_open_error_learning_running,
                    Toast.LENGTH_SHORT
            )
            return
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            PaukerManager.showToast(
                    context as Activity,
                    R.string.shortcut_open_error_permission,
                    Toast.LENGTH_SHORT
            )
            return
        }
        val filename =
                shortcutIntent.getStringExtra(Constants.SHORTCUT_EXTRA) ?: return
        if (SettingsManager.instance().getBoolPreference(context, Keys.AUTO_DOWNLOAD)) {
            Log.d(
                    "ShortcutReceiver::openLesson",
                    "Check for newer version on DB"
            )
            val accessToken = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.DROPBOX_ACCESS_TOKEN, null)
            val syncIntent = Intent(context, SyncDialog::class.java)
            try {
                syncIntent.putExtra(
                        SyncDialog.FILES,
                        paukerManager.getFilePath(context, filename)
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken)
            syncIntent.action = SyncDialog.SYNC_FILE_ACTION
            startActivityForResult(
                    syncIntent,
                    Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN
            )
        } else {
            openLesson(filename)
        }
    }

    /**
     * Öffnet die Lektion und beendet anschließend die Activity.
     * @param filename Name der Lektion
     */
    private fun openLesson(filename: String) {
        try {
            if (paukerManager.currentFileName != filename) {
                paukerAndModelManager.loadLessonFromFile(paukerManager.getFilePath(context, filename))
                paukerManager.isSaveRequired = false
            }
            val intent = Intent(context, MainMenu::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } catch (e: IOException) {
            PaukerManager.showToast(
                    context as Activity,
                    getString(R.string.error_reading_from_xml),
                    Toast.LENGTH_SHORT
            )
            ErrorReporter.instance()
                    .addCustomData("ImportThread", "IOException?")
        }
    }

    fun cancelClicked(view: View?) {}
}