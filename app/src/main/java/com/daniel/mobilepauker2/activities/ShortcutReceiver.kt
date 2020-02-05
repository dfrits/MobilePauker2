package com.daniel.mobilepauker2.activities

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
import com.daniel.mobilepauker2.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.dropbox.SyncDialog
import com.daniel.mobilepauker2.model.SettingsManager
import com.daniel.mobilepauker2.model.SettingsManager.Keys
import com.daniel.mobilepauker2.model.TextDrawable
import com.daniel.mobilepauker2.utils.Constants
import com.daniel.mobilepauker2.utils.ErrorReporter
import com.daniel.mobilepauker2.utils.Log
import java.io.IOException

class ShortcutReceiver : Activity() {
    private val paukerManager: PaukerManager = PaukerManager.instance()
    private val context: Context = this
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
                    paukerManager!!.getFilePath(context, filename)
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
                paukerManager!!.loadLessonFromFile(paukerManager.getFilePath(context, filename))
                paukerManager.isSaveRequired = false
            }
            val intent = Intent(context, MainMenu::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } catch (e: IOException) {
            PaukerManager.Companion.showToast(
                context as Activity,
                getString(R.string.error_reading_from_xml),
                Toast.LENGTH_SHORT
            )
            ErrorReporter.Companion.instance()
                .AddCustomData("ImportThread", "IOException?")
        }
    }

    fun cancelClicked(view: View?) {}

    companion object {
        /**
         * Erstellt einen Shortcut und fügt diesen hinzu.
         * @param filename Name der Lektion, von der ein Shortcut erstellt werden soll
         */
        fun createShortcut(context: Context, filename: String) {
            val shortcutManager = context.getSystemService(
                ShortcutManager::class.java
            )
            if (shortcutManager != null) {
                if (shortcutManager.dynamicShortcuts.size == 5) {
                    Log.d(
                        "LessonImportActivity::createShortcut",
                        "already 5 shortcuts created"
                    )
                    PaukerManager.Companion.showToast(
                        context as Activity,
                        R.string.shortcut_create_error,
                        Toast.LENGTH_LONG
                    )
                } else {
                    val intent = Intent(context, ShortcutReceiver::class.java)
                    intent.action = Constants.SHORTCUT_ACTION
                    intent.putExtra(
                        Constants.SHORTCUT_EXTRA,
                        filename
                    )
                    val icon = TextDrawable(filename[0].toString())
                    icon.setBold(true)
                    val shortcut = ShortcutInfo.Builder(context, filename)
                        .setShortLabel(
                            PaukerManager.Companion.instance()!!.getReadableFileName(
                                filename
                            )
                        )
                        .setIcon(
                            Icon.createWithBitmap(
                                drawableToBitmap(
                                    icon
                                )
                            )
                        )
                        .setIntent(intent)
                        .build()
                    shortcutManager.addDynamicShortcuts(listOf(shortcut))
                    PaukerManager.Companion.showToast(
                        context as Activity,
                        R.string.shortcut_added,
                        Toast.LENGTH_SHORT
                    )
                    Log.d(
                        "LessonImportActivity::createShortcut",
                        "Shortcut created"
                    )
                }
            }
        }

        fun deleteShortcut(context: Context, ID: String) {
            val shortcutManager = context.getSystemService(
                ShortcutManager::class.java
            )
            if (shortcutManager != null) {
                shortcutManager.removeDynamicShortcuts(listOf(ID))
                PaukerManager.Companion.showToast(
                    context as Activity,
                    R.string.shortcut_removed,
                    Toast.LENGTH_SHORT
                )
                Log.d(
                    "LessonImportActivity::deleteShortcut",
                    "Shortcut deleted"
                )
            }
        }

        /**
         * Wandelt das Drawable in ein Bitmap um.
         * @param drawable TextDrawable
         * @return Bitmap
         */
        private fun drawableToBitmap(drawable: Drawable): Bitmap {
            val bitmap: Bitmap
            bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            } else {
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
            }
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        fun hasShortcut(context: Context, ID: String): Boolean {
            val shortcutManager = context.getSystemService(
                ShortcutManager::class.java
            )
            if (shortcutManager != null) {
                val shortcuts =
                    shortcutManager.dynamicShortcuts
                for (info in shortcuts) {
                    if (info.id == ID) {
                        return true
                    }
                }
            }
            return false
        }
    }
}