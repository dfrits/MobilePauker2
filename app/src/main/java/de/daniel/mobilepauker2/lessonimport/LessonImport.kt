package de.daniel.mobilepauker2.lessonimport

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.dropbox.core.android.Auth
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.dropbox.DropboxAccDialog
import de.daniel.mobilepauker2.dropbox.SyncDialog
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.settings.SettingsManager
import de.daniel.mobilepauker2.shortcut.ShortcutsManager
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Constants.ACCESS_TOKEN
import de.daniel.mobilepauker2.utils.Constants.FILES
import de.daniel.mobilepauker2.utils.Constants.SYNC_FILE_ACTION
import de.daniel.mobilepauker2.utils.ErrorReporter
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

class LessonImport : AppCompatActivity(R.layout.open_lesson) {
    private val CONTEXT_DELETE = 0
    private val CONTEXT_OPEN = 1
    private val CONTEXT_CREATE_SHORTCUT = 2
    private val CONTEXT_DELETE_SHORTCUT = 3
    private val context = this
    private val fileNames = mutableListOf<String>()
    private lateinit var preferences: SharedPreferences

    @Inject
    lateinit var viewModel: LessonImportViewModel

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var shortcutsManager: ShortcutsManager

    @Inject
    lateinit var errorReporter: ErrorReporter

    private var accessToken: String? = null
    private var listView: ListView? = null
    private var files = emptyArray<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        preferences = PreferenceManager.getDefaultSharedPreferences(context)

        init()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.open_lesson, menu)

        menu.findItem(R.id.mSyncFilesWithDropbox).isVisible = viewModel.lastSelection == -1
        menu.findItem(R.id.mOpenLesson).isVisible = viewModel.lastSelection != -1
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
            CONTEXT_OPEN -> openLesson(position)
            CONTEXT_CREATE_SHORTCUT -> {
                Log.d(
                    "LessonImportActivity::createShortcut", "create new dynamic " +
                        "shortcut for list pos:" + position + " id:" + menuInfo.id
                )
                createShortCut(position)
            }
            CONTEXT_DELETE_SHORTCUT -> {
                Log.d(
                    "LessonImportActivity::deleteShortcut", "delete dynamic " +
                        "shortcut for list pos:" + position + " id:" + menuInfo.id
                )
                deleteShortCut(position)
            }
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val uid = Auth.getUid()
        val storedUid = preferences.getString(Constants.DROPBOX_USER_ID, null)
        if (uid != storedUid) {
            preferences.edit().putString(Constants.DROPBOX_USER_ID, uid).apply()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG) {
            if (resultCode == RESULT_OK) {
                Log.d("OpenLesson", "Synchro erfolgreich")
            } else {
                Log.d("OpenLesson", "Synchro nicht erfolgreich")
                toaster.showToast(
                    context as Activity,
                    R.string.error_synchronizing,
                    Toast.LENGTH_SHORT
                )
            }
            init()
            if (lessonManager.isLessonNotNew())
                if (fileNames.contains(dataManager.currentFileName)) {
                    try {
                        viewModel.openLesson(dataManager.getReadableCurrentFileName())
                    } catch (ignored: IOException) {
                        toaster.showToast(
                            context as Activity,
                            R.string.reopen_lesson_error,
                            Toast.LENGTH_LONG
                        )
                        errorReporter.addCustomData("ImportThread", "IOException?")
                    }
                } else {
                    lessonManager.setupNewLesson()
                    dataManager.saveRequired = false
                }
        }
        if (requestCode == Constants.REQUEST_CODE_DB_ACC_DIALOG && resultCode == RESULT_OK) {
            accessToken = preferences.getString(Constants.DROPBOX_ACCESS_TOKEN, null)
            if (accessToken != null) {
                startSync()
            }
        }
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN) {
            try {
                if (resultCode == RESULT_OK) {
                    Log.d("LessonImportActivity::onActivityResult", "File wurde aktualisiert")
                } else {
                    Log.d("LessonImportActivity::onActivityResult", "File wurde nicht aktualisiert")
                }
                viewModel.openLesson(fileNames[viewModel.lastSelection])
                finish()
            } catch (e: IOException) {
                toaster.showToast(
                    context as Activity,
                    R.string.error_reading_from_xml,
                    Toast.LENGTH_LONG
                )
                errorReporter.addCustomData("ImportThread", "IOException?")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (viewModel.lastSelection > -1) {
            resetSelection()
        } else {
            super.onBackPressed()
        }
    }

    private fun init() {
        if (readFlashCardFiles()) {
            findViewById<TextView>(R.id.tNothingFound).visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.tNothingFound).visibility = View.VISIBLE
        }
        initListView()
    }

    private fun initListView() {
        listView = findViewById(R.id.lvLessons)
        listView?.let { listView ->
            listView.adapter = LessonImportAdapter(context, fileNames.toList())
            listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE
            listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
                itemClicked(position)
            }
            registerForContextMenu(listView)
            listView.setOnCreateContextMenuListener { menu, _, menuInfo ->
                menu.add(0, CONTEXT_DELETE, 0, R.string.delete)
                menu.add(0, CONTEXT_OPEN, 0, R.string.open_lesson)

                val pos = (menuInfo as AdapterContextMenuInfo).position
                if (shortcutsManager.hasShortcut(listView.getItemAtPosition(pos) as String)
                ) {
                    menu.add(0, CONTEXT_DELETE_SHORTCUT, 0, R.string.shortcut_remove)
                } else {
                    menu.add(0, CONTEXT_CREATE_SHORTCUT, 0, R.string.shortcut_add)
                }
            }
        }
    }

    private fun itemClicked(position: Int) {
        val infoText: TextView = findViewById(R.id.infoText)
        if (viewModel.lastSelection != position) {
            listView!!.getChildAt(position).isSelected = true
            viewModel.itemClicked(position)
            var text: String? = getString(R.string.next_expire_date)
            try {
                val result =
                    viewModel.getNextExpireDate(listView!!.getItemAtPosition(position) as String)
                if (result.timeStamp > Long.MIN_VALUE) {
                    if (result.expiredCards > 0) {
                        text = "${getString(R.string.expired_cards)} ${result.expiredCards}"
                    } else {
                        val dateL = result.timeStamp
                        val cal = Calendar.getInstance(Locale.getDefault())
                        cal.timeInMillis = dateL
                        val date = DateFormat.format("dd.MM.yyyy HH:mm", cal).toString()
                        text = "$text $date"
                    }
                } else {
                    text = text + " " + getString(R.string.nothing_learned_yet)
                }
            } catch (e: Exception) {
                when (e) {
                    is IOException, is java.lang.RuntimeException -> {
                        toaster.showToast(
                            context as Activity,
                            R.string.error_reading_from_xml,
                            Toast.LENGTH_SHORT
                        )
                        resetSelection()
                        init()
                        text = null
                    }
                    else -> throw e
                }
            }
            if (text != null) {
                infoText.text = text
                infoText.visibility = View.VISIBLE
            }
        } else {
            listView!!.getChildAt(position).isSelected = false
            infoText.visibility = View.GONE
        }
        (listView!!.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        invalidateOptionsMenu()
    }

    private fun readFlashCardFiles(): Boolean {
        return try {
            // Dateien auslesen
            files = dataManager.listFiles()

            // Sortieren
            Arrays.sort(files) { o1, o2 -> o1.name.compareTo(o2.name) }

            // Liste füllen und Endungen abschneiden
            fileNames.clear()
            if (files.isEmpty()) {
                return false
            }
            for (aFile in files) {
                fileNames.add(aFile.name)
            }
            true
        } catch (e: Exception) {
            Log.d("ImportFlashCardFile::onCreate", "Unable to read directory from flash card $e")
            false
        }
    }

    private fun startSync() {
        val syncIntent = Intent(context, SyncDialog::class.java)
        syncIntent.putExtra(Constants.ACCESS_TOKEN, accessToken)
        syncIntent.putExtra(Constants.FILES, files)
        syncIntent.action = Constants.SYNC_ALL_ACTION
        startActivityForResult(syncIntent, Constants.REQUEST_CODE_SYNC_DIALOG)
    }

    private fun deleteLesson(position: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.delete_lesson_message)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                dialog.dismiss()
                val filename = listView!!.getItemAtPosition(position).toString()
                val file = dataManager.getFilePathForName(filename)
                if (file.isFile) {
                    if (dataManager.deleteLesson(file)) {
                        init()
                        resetSelection()
                        shortcutsManager.deleteShortcut(context as Activity, filename)
                        if (!fileNames.contains(dataManager.currentFileName)) {
                            lessonManager.setupNewLesson()
                            dataManager.saveRequired = false
                        }
                    } else {
                        toaster.showToast(
                            context as Activity,
                            R.string.delete_lesson_error,
                            Toast.LENGTH_SHORT
                        )
                    }
                }
            }
            .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun openLesson(position: Int) {
        val filename = listView!!.getItemAtPosition(position) as String
        try {
            toaster.showToast(
                context as Activity,
                R.string.open_lesson_hint,
                Toast.LENGTH_SHORT
            )
            viewModel.openLesson(filename)
            finish()
        } catch (e: IOException) {
            resetSelection(null)
            toaster.showToast(
                context as Activity,
                getString(R.string.error_reading_from_xml),
                Toast.LENGTH_SHORT
            )
            errorReporter.addCustomData("ImportThread", "IOException?")
        }
    }

    private fun createShortCut(position: Int) {
        shortcutsManager.createShortcut(
            context as Activity,
            listView!!.getItemAtPosition(position) as String
        )
        init()
        resetSelection(null)
    }

    private fun deleteShortCut(position: Int) {
        shortcutsManager.deleteShortcut(
            context as Activity,
            listView!!.getItemAtPosition(position) as String
        )
        init()
        resetSelection(null)
    }

    private fun openBrowserForDownload() {
        val url = "http://pauker.sourceforge.net/pauker.php?page=lessons"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    // View Clicks

    fun resetSelection(view: View? = null) {
        if (viewModel.lastSelection != -1) {
            listView?.clearChoices()
            viewModel.resetSelection()
            findViewById<TextView>(R.id.infoText).visibility = View.GONE
            (listView?.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            invalidateOptionsMenu()
        }
    }

    fun downloadNewLesson(view: View) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage(
            Html.fromHtml(
                getString(R.string.download_file_dialog_message),
                Html.FROM_HTML_MODE_LEGACY
            )
        )
            .setPositiveButton(R.string.next) { _, _ -> openBrowserForDownload() }
            .setNeutralButton(R.string.cancel, null)
        builder.create().show()
    }

    // Menu Clicks

    fun syncManuallyClicked(item: MenuItem?) {
        accessToken = preferences.getString(Constants.DROPBOX_ACCESS_TOKEN, null)
        if (accessToken == null) {
            val assIntent = Intent(context, DropboxAccDialog::class.java)
            assIntent.action = Constants.DROPBOX_AUTH_ACTION
            startActivityForResult(assIntent, Constants.REQUEST_CODE_DB_ACC_DIALOG)
        } else {
            startSync()
        }
    }

    fun mOpenLessonClicked(item: MenuItem?) {
        openLesson(viewModel.lastSelection)
    }
}