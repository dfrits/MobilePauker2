package com.daniel.mobilepauker2.lessonimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.text.Html
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.core.model.xmlsupport.FlashCardXMLPullFeedParser
import com.daniel.mobilepauker2.dropbox.DropboxAccDialog
import com.daniel.mobilepauker2.dropbox.SyncDialog
import com.daniel.mobilepauker2.main.ShortcutReceiver
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.Log
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.pauker_native.PaukerAndModelManager
import com.daniel.mobilepauker2.settings.SettingsManager
import com.daniel.mobilepauker2.settings.SettingsManager.Keys
import com.dropbox.core.android.Auth
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Created by Daniel on 04.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
@Suppress("UNUSED_PARAMETER")
class LessonImportActivity : AppCompatActivity() {
    private val modelManager: ModelManager? = ModelManager.instance()
    private val paukerManager: PaukerManager? = PaukerManager.instance()
    private val context: Context = this
    private var accessToken: String? = null
    private val fileNames = ArrayList<String>()
    private var listView: ListView? = null
    private var preferences: SharedPreferences? = null
    private var files: Array<File>? = null
    /**
     * Speichert die letzte Selektion in der Liste.
     */
    private var lastSelection = -1
    lateinit var paukerAndModelManager: PaukerAndModelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        setContentView(R.layout.open_lesson)
        init()
    }

    private fun init() {
        if (readFlashCardFiles()) {
            findViewById<View>(R.id.tNothingFound).visibility = View.GONE
            errorMessage = null
        } else {
            findViewById<View>(R.id.tNothingFound).visibility = View.VISIBLE
        }
        initListView()
    }

    private fun initListView() {
        listView = findViewById(R.id.lvLessons)
        listView?.let {
            it.adapter = LessonImportAdapter(
                context,
                fileNames
            )
            it.choiceMode = AbsListView.CHOICE_MODE_SINGLE
            it.setOnItemClickListener { _, _, position, _ -> itemClicked(position) }
            registerForContextMenu(it)
            it.setOnCreateContextMenuListener{ menu, _, menuInfo ->
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
    }

    private fun itemClicked(position: Int) {
        val infoText = findViewById<TextView>(R.id.infoText)
        val item = listView!!.getChildAt(position) ?: return
        if (lastSelection != position) {
            item.isSelected = true
            lastSelection = position
            var text = getString(R.string.next_expire_date)
            try {
                val uri = paukerManager!!.getFilePath(
                    context,
                    listView!!.getItemAtPosition(position) as String
                ).toURI()
                val parser = FlashCardXMLPullFeedParser(uri.toURL())
                val map = parser.nextExpireDate
                if (map[0] > Long.MIN_VALUE) {
                    if (map[1, 0] > 0) {
                        val numberOfCards = map[1]
                        text = getString(R.string.expired_cards) + " " + numberOfCards.toString()
                    } else {
                        val dateL = map[0]
                        val cal =
                            Calendar.getInstance(Locale.getDefault())
                        cal.timeInMillis = dateL
                        val date =
                            DateFormat.format("dd.MM.yyyy HH:mm", cal)
                                .toString()
                        text = "$text $date"
                    }
                } else {
                    text = text + " " + getString(R.string.nothing_learned_yet)
                }
            } catch (ignored: IOException) {
                PaukerManager.showToast(
                    context as Activity,
                    R.string.error_reading_from_xml,
                    Toast.LENGTH_SHORT
                )
                resetSelection(null)
                init()
                text = ""
            } catch (ignored: RuntimeException) {
                PaukerManager.showToast(
                    context as Activity,
                    R.string.error_reading_from_xml,
                    Toast.LENGTH_SHORT
                )
                resetSelection(null)
                init()
                text = ""
            }
            if (text.isNotEmpty()) {
                infoText.text = text
                infoText.visibility = View.VISIBLE
            }
        } else {
            item.isSelected = false
            lastSelection = -1
            infoText.visibility = View.GONE
        }
        (listView!!.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        invalidateOptionsMenu()
    }

    /**
     * Setzt die Auswahl zurück. Ausgelöst, wenn außerhalb der Liste gelickt wird.
     * @param view Wird nicht benötigt
     */
    fun resetSelection(view: View?) {
        if (lastSelection != -1) {
            listView!!.clearChoices()
            lastSelection = -1
            findViewById<View>(R.id.infoText).visibility = View.GONE
            (listView!!.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            invalidateOptionsMenu()
        }
    }

    /**
     * Liest die Lektionen aus dem Ordner aus und zeigt sie in einer Liste an.
     * @return **True**, wenn Lektionen vorhanden sind und erfolgreich ausgelesen werden konnten.
     * Sonst **false**
     */
    private fun readFlashCardFiles(): Boolean {
        return try { // Dateien auslesen
            files = paukerManager?.listFiles(context)
            // Sortieren
            Arrays.sort(files) { o1, o2 -> o1.name.compareTo(o2.name) }
            // Liste füllen und Endungen abschneiden
            fileNames.clear()
            if (files?.size == 0) {
                return false
            }
            for (aFile in files!!) {
                fileNames.add(aFile.name)
            }
            true
        } catch (e: Exception) {
            Log.d(
                "ImportFlashCardFile::onCreate",
                "Unable to read directory from flash card $e"
            )
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.open_lesson, menu)
        // Wurde auf ein Item geklickt Buttons ändern auf Lesson öffnen
        // Wurde Selektion aufgehoben, dann wieder zruckändern
        menu.findItem(R.id.mSyncFilesWithDropbox).isVisible = lastSelection == -1
        menu.findItem(R.id.mOpenLesson).isVisible = lastSelection != -1
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
            } else {
                Log.d("OpenLesson", "Synchro nicht erfolgreich")
                PaukerManager.showToast(
                    context as Activity,
                    R.string.error_synchronizing,
                    Toast.LENGTH_SHORT
                )
            }
            init()
            if (paukerManager!!.isLessonNotNew) if (fileNames.contains(paukerManager.currentFileName)) {
                try {
                    openLesson(paukerManager.currentFileName)
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
                paukerAndModelManager.setupNewApplicationLesson()
                paukerManager.isSaveRequired = false
            }
        }
        if (requestCode == Constants.REQUEST_CODE_DB_ACC_DIALOG && resultCode == Activity.RESULT_OK) {
            accessToken = preferences!!.getString(
                Constants.DROPBOX_ACCESS_TOKEN,
                null
            )
            if (accessToken != null) {
                startSync()
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
                openLesson(fileNames[lastSelection])
                finish()
            } catch (e: IOException) {
                PaukerManager.showToast(
                    context as Activity,
                    R.string.error_reading_from_xml,
                    Toast.LENGTH_LONG
                )
                ErrorReporter.instance()
                    .addCustomData("ImportThread", "IOException?")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (lastSelection > -1) {
            resetSelection(null)
        } else {
            super.onBackPressed()
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
        accessToken = preferences!!.getString(
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
            startSync()
        }
    }

    private fun startSync() {
        val syncIntent = Intent(context, SyncDialog::class.java)
        syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken)
        syncIntent.putExtra(SyncDialog.FILES, files)
        syncIntent.action = SyncDialog.SYNC_ALL_ACTION
        startActivityForResult(
            syncIntent,
            Constants.REQUEST_CODE_SYNC_DIALOG
        )
    }

    /**
     * Öffnet eine Lektion und beendet bei Erfolg die Activity.
     * @param ignored Wird nicht benötigt
     */
    fun mOpenLessonClicked(ignored: MenuItem?) {
        openLesson(lastSelection)
    }

    private fun deleteLesson(position: Int) {
        val builder =
            AlertDialog.Builder(context)
        builder.setMessage(R.string.delete_lesson_message)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                dialog.dismiss()
                val filename = listView!!.getItemAtPosition(position).toString()
                val filePath =
                    Environment.getExternalStorageDirectory().toString() +
                            paukerManager?.applicationDataDirectory + filename
                val file = File(filePath)
                if (file.isFile) {
                    if (modelManager!!.deleteLesson(context, file)) {
                        init()
                        resetSelection(null)
                        ShortcutReceiver.deleteShortcut(context, filename)
                        if (!fileNames.contains(paukerManager?.currentFileName)) {
                            paukerAndModelManager.setupNewApplicationLesson()
                            paukerManager!!.isSaveRequired = false
                        }
                    } else {
                        PaukerManager.showToast(
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
            if (SettingsManager.instance().getBoolPreference(
                    context,
                    Keys.AUTO_DOWNLOAD
                )
            ) {
                val accessToken =
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(
                            Constants.DROPBOX_ACCESS_TOKEN,
                            null
                        )
                val syncIntent = Intent(context, SyncDialog::class.java)
                syncIntent.putExtra(
                    SyncDialog.FILES,
                    paukerManager!!.getFilePath(context, filename)
                )
                syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken)
                syncIntent.action = SyncDialog.SYNC_FILE_ACTION
                startActivityForResult(
                    syncIntent,
                    Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN
                )
                Log.d(
                    "LessonImportActivity:openLesson",
                    "Check for newer version on DB"
                )
            } else {
                PaukerManager.showToast(
                    context as Activity,
                    R.string.open_lesson_hint,
                    Toast.LENGTH_SHORT
                )
                openLesson(filename)
                finish()
            }
        } catch (e: IOException) {
            resetSelection(null)
            PaukerManager.showToast(
                context as Activity,
                getString(R.string.error_reading_from_xml),
                Toast.LENGTH_SHORT
            )
            ErrorReporter.instance()
                .addCustomData("ImportThread", "IOException?")
        }
    }

    /**
     * Öffnet die Lektion mit dem übergebenen Namen.
     * @param filename Lektionsname
     * @throws IOException .
     */
    @Throws(IOException::class)
    private fun openLesson(filename: String?) {
        paukerAndModelManager.loadLessonFromFile(paukerManager!!.getFilePath(context, filename))
        paukerManager.isSaveRequired = false
    }

    /**
     * Erstellt einen Shortcut und fügt diesen hinzu.
     * @param position Position der Lektion von der ein Shortcut erstellt werden soll
     */
    private fun createShortCut(position: Int) {
        ShortcutReceiver.createShortcut(
            this,
            listView!!.getItemAtPosition(position) as String
        )
        init()
        resetSelection(null)
    }

    /**
     * Entfernt den Shortcut.
     * @param position Position in der Liste
     */
    private fun deleteShortCut(position: Int) {
        ShortcutReceiver.deleteShortcut(
            this,
            listView!!.getItemAtPosition(position) as String
        )
        init()
        resetSelection(null)
    }

    fun downloadNewLesson(view: View?) {
        val builder =
            AlertDialog.Builder(context)
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

    private fun openBrowserForDownload() {
        val url = "http://pauker.sourceforge.net/pauker.php?page=lessons"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    companion object {
        private const val CONTEXT_DELETE = 0
        private const val CONTEXT_OPEN = 1
        private const val CONTEXT_CREATE_SHORTCUT = 2
        private const val CONTEXT_DELETE_SHORTCUT = 3
        private var errorMessage: String? = null
    }
}