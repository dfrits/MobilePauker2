package com.daniel.mobilepauker2.lessonimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.dropbox.DropboxAccDialog
import com.daniel.mobilepauker2.main.ShortcutReceiver
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.Log
import com.daniel.mobilepauker2.pauker_native.PaukerAndModelManager
import com.dropbox.core.android.Auth
import kotlinx.android.synthetic.main.open_lesson.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.IOException

/**
 * Created by Daniel on 04.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
@Suppress("UNUSED_PARAMETER")
class LessonImportActivity : AppCompatActivity() {
    private val context: Context = this
    private var preferences: SharedPreferences? = null

    lateinit var paukerAndModelManager: PaukerAndModelManager

    private val viewModel: LessonImportViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        setContentView(R.layout.open_lesson)
        initListView()
        viewModel.readFlashCardFiles()
    }

    private fun initNothingFoundView(isEmpty: Boolean) {
        if (isEmpty) {
            tNothingFound.visibility = View.VISIBLE
        } else {
            tNothingFound.visibility = View.GONE
            errorMessage = null
        }
    }

    private fun initListView() {
        viewModel.fileNamesLiveData.observe(this, Observer { fileNames ->
            initNothingFoundView(fileNames.isEmpty())

            lvLessons.let {
                it.adapter = LessonImportAdapter(
                        context,
                        fileNames
                )
                it.choiceMode = AbsListView.CHOICE_MODE_SINGLE
                it.setOnItemClickListener { _, _, position, _ -> itemClicked(position) }
                registerForContextMenu(it)
                it.setOnCreateContextMenuListener { menu, _, menuInfo ->
                    menu.add(
                            0,
                            CONTEXT_DELETE, 0, R.string.delete
                    )
                    menu.add(
                            0,
                            CONTEXT_OPEN, 0, R.string.open_lesson
                    )
                    val pos = (menuInfo as AdapterContextMenuInfo).position
                    if (ShortcutReceiver.hasShortcut(
                                    context,
                                    it.getItemAtPosition(pos) as String
                            )
                    ) {
                        menu.add(
                                0,
                                CONTEXT_DELETE_SHORTCUT,
                                0,
                                R.string.shortcut_remove
                        )
                    } else {
                        menu.add(
                                0,
                                CONTEXT_CREATE_SHORTCUT,
                                0,
                                R.string.shortcut_add
                        )
                    }
                }
            }
        })
    }

    private fun itemClicked(position: Int) {
        val item = lvLessons.getChildAt(position) ?: return
        if (!viewModel.isPositionSelected(position)) {
            viewModel.lessonSelected(position)
            item.isSelected = true
            val text = try {
                viewModel.getNextExpireDate(lvLessons.getItemAtPosition(position) as String)
            } catch (ignored: IOException) {
                showErrorToast()
                resetSelection(null)
                viewModel.readFlashCardFiles()
                ""
            } catch (ignored: RuntimeException) {
                showErrorToast()
                resetSelection(null)
                viewModel.readFlashCardFiles()
                ""
            }

            if (text.isNotEmpty()) {
                infoText.text = text
                infoText.visibility = View.VISIBLE
            }
        } else {
            item.isSelected = false
            resetSelection(null)
            infoText.visibility = View.GONE
        }
        (lvLessons.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        invalidateOptionsMenu()
    }

    private fun showErrorToast() {
        PaukerManager.showToast(
                context as Activity,
                R.string.error_reading_from_xml,
                Toast.LENGTH_SHORT
        )
    }

    /**
     * Setzt die Auswahl zurück. Ausgelöst, wenn außerhalb der Liste gelickt wird.
     * @param view Wird nicht benötigt (RootView)
     */
    fun resetSelection(view: View?) {
        if (viewModel.resetSelection()) {
            lvLessons.clearChoices()
            infoText.visibility = View.GONE
            (lvLessons.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.open_lesson, menu)
        // Wurde auf ein Item geklickt Buttons ändern auf Lesson öffnen
        // Wurde Selektion aufgehoben, dann wieder zruckändern
        menu.findItem(R.id.mSyncFilesWithDropbox).isVisible = viewModel.isPositionSelected(-1)
        menu.findItem(R.id.mOpenLesson).isVisible = !viewModel.isPositionSelected(-1)
        return true
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as AdapterContextMenuInfo
        val position = menuInfo.position
        when (item.itemId) {
            CONTEXT_DELETE -> {
                Log.d(
                        "LessonImportActivity::deleteLesson",
                        "list pos:" + position + " id:" + menuInfo.id
                )
                deleteLesson(position)
            }
            CONTEXT_OPEN -> viewModel.openLesson(position)
            CONTEXT_CREATE_SHORTCUT -> {
                Log.d(
                        "LessonImportActivity::createShortcut", "create new dynamic " +
                        "shortcut for list pos:" + position + " id:" + menuInfo.id
                )
                viewModel.createShortCut(position)
                resetSelection(null)
            }
            CONTEXT_DELETE_SHORTCUT -> {
                Log.d(
                        "LessonImportActivity::deleteShortcut", "delete dynamic " +
                        "shortcut for list pos:" + position + " id:" + menuInfo.id
                )
                viewModel.deleteShortCut(position)
                resetSelection(null)
            }
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val uid = Auth.getUid()
        val storedUid =
                preferences!!.getString(Constants.DROPBOX_USER_ID, null)
        if (uid != null && uid != storedUid) {
            preferences!!.edit()
                    .putString(Constants.DROPBOX_USER_ID, uid).apply()
        }
    }

    override fun finish() {
        if (errorMessage != null) {
            PaukerManager.showToast(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
            )
        }
        super.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("OpenLesson", "Synchro erfolgreich")

                if (viewModel.isLessonNotNew()) {
                    val currentFileName = viewModel.currentFileNameExist()
                    if (currentFileName.isNotEmpty()) {
                        try {
                            viewModel.openLesson(currentFileName)
                        } catch (ignored: IOException) {
                            PaukerManager.showToast(
                                    context as Activity,
                                    R.string.reopen_lesson_error,
                                    Toast.LENGTH_LONG
                            )
                            ErrorReporter.instance()
                                    .addCustomData("ImportThread", "IOException?")
                        }
                    } else {
                        viewModel.setupNewLesson()
                    }
                }
            } else {
                Log.d("OpenLesson", "Synchro nicht erfolgreich")
                PaukerManager.showToast(
                        context as Activity,
                        R.string.error_synchronizing,
                        Toast.LENGTH_SHORT
                )
            }

            viewModel.readFlashCardFiles()
        }
        if (requestCode == Constants.REQUEST_CODE_DB_ACC_DIALOG && resultCode == Activity.RESULT_OK) {
            val accessToken = preferences!!.getString(
                    Constants.DROPBOX_ACCESS_TOKEN,
                    null
            )
            if (accessToken != null) {
                viewModel.startSync(accessToken)
            }
        }
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN) {
            try {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(
                            "LessonImportActivity::onActivityResult",
                            "File wurde aktualisiert"
                    )
                } else {
                    Log.d(
                            "LessonImportActivity::onActivityResult",
                            "File wurde nicht aktualisiert"
                    )
                }
                viewModel.openSelectedLessonAfterSync()
                finish()
            } catch (e: IOException) {
                showErrorToast()
                ErrorReporter.instance()
                        .addCustomData("ImportThread", "IOException?")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (viewModel.isPositionSelected(-1)) {
            super.onBackPressed()
        } else {
            resetSelection(null)
        }
    }

    /**
     * Syncronisiert die Dateien. Wird vom Nutzer ausgelöst.
     *
     *
     * Bei Erststart wird Authentifizierung gestartet. Vorher findet keine automatische
     * Syncronisation statt.
     */
    fun syncManuallyClicked(item: MenuItem?) {
        val accessToken = preferences!!.getString(
                Constants.DROPBOX_ACCESS_TOKEN,
                null
        )
        if (accessToken == null) {
            val assIntent = Intent(context, DropboxAccDialog::class.java)
            assIntent.putExtra(DropboxAccDialog.AUTH_MODE, true)
            startActivityForResult(
                    assIntent,
                    Constants.REQUEST_CODE_DB_ACC_DIALOG
            )
        } else {
            viewModel.startSync(accessToken)
        }
    }

    /**
     * Öffnet eine Lektion und beendet bei Erfolg die Activity.
     * @param ignored Wird nicht benötigt
     */
    fun mOpenLessonClicked(ignored: MenuItem?) {
        try {
            if (viewModel.openSelectedLesson()) {
                finish()
            }
        } catch (e: IOException) {
            resetSelection(null)
            PaukerManager.showToast(
                    context as Activity,
                    context.getString(R.string.error_reading_from_xml),
                    Toast.LENGTH_SHORT
            )
            ErrorReporter.instance().addCustomData("ImportThread", "IOException?")
        }
    }

    private fun deleteLesson(position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.delete_lesson_message)
                .setPositiveButton(R.string.delete) { dialog, _ ->
                    dialog.dismiss()
                    val filename = lvLessons.getItemAtPosition(position).toString()
                    viewModel.deleteLesson(filename)
                    resetSelection(null)
                }
                .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    fun downloadNewLesson(view: View?) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(
                Html.fromHtml(
                        getString(R.string.download_file_dialog_message),
                        Html.FROM_HTML_MODE_LEGACY
                )
        )
                .setPositiveButton(R.string.next) { _, _ -> viewModel.openBrowserForDownload() }
                .setNeutralButton(R.string.cancel, null)
        builder.create().show()
    }

    companion object {
        private const val CONTEXT_DELETE = 0
        private const val CONTEXT_OPEN = 1
        private const val CONTEXT_CREATE_SHORTCUT = 2
        private const val CONTEXT_DELETE_SHORTCUT = 3
        private var errorMessage: String? = null
    }
}