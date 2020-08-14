package com.daniel.mobilepauker2.lessonimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.text.format.DateFormat
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.core.model.xmlsupport.FlashCardXMLPullFeedParser
import com.daniel.mobilepauker2.dropbox.SyncDialog
import com.daniel.mobilepauker2.main.ShortcutReceiver
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.Log
import com.daniel.mobilepauker2.pauker_native.PaukerAndModelManager
import com.daniel.mobilepauker2.settings.SettingsManager
import kotlinx.android.synthetic.main.open_lesson.*
import java.io.File
import java.io.IOException
import java.util.*

class LessonImportViewModel(
        val paukerAndModelManager: PaukerAndModelManager,
        val context: Context
) : ViewModel() {
    private val paukerManager = paukerAndModelManager.paukerManager
    private val modelManager = paukerAndModelManager.modelManager
    private val files: MutableList<File> = mutableListOf()
    private var accessToken: String? = null

    /**
     * Speichert die letzte Selektion in der Liste.
     */
    private var lastSelection = -1

    private val mutableFileNamesLiveData: MutableLiveData<List<String>> = MutableLiveData()
    val fileNamesLiveData: LiveData<List<String>> = mutableFileNamesLiveData

    /**
     * Liest die Lektionen aus dem Ordner aus und zeigt sie in einer Liste an.
     * @return **True**, wenn Lektionen vorhanden sind und erfolgreich ausgelesen werden konnten.
     * Sonst **false**
     */
    fun readFlashCardFiles(): Boolean {
        return try { // Dateien auslesen
            paukerManager.listFiles(context)?.let {
                // Sortieren
                Arrays.sort(it) { o1, o2 -> o1.name.compareTo(o2.name) }
                files.addAll(it.asIterable())
                // Liste füllen und Endungen abschneiden
                val names: MutableList<String> = mutableListOf()
                if (files.size == 0) {
                    return false
                }
                for (aFile in files) {
                    names.add(aFile.name)
                }
                mutableFileNamesLiveData.postValue(names)
                true
            }
            false
        } catch (e: Exception) {
            Log.d(
                    "ImportFlashCardFile::onCreate",
                    "Unable to read directory from flash card $e"
            )
            false
        }
    }

    fun getNextExpireDate(lessonName: String): String {
        val uri = paukerManager.getFilePath(context, lessonName).toURI()
        val parser = FlashCardXMLPullFeedParser(uri.toURL())
        val map = parser.nextExpireDate

        return if (map[0] > Long.MIN_VALUE) {
            if (map[1, 0] > 0) {
                val numberOfCards = map[1]
                context.getString(R.string.expired_cards) + " " + numberOfCards.toString()
            } else {
                val dateL = map[0]
                val cal =
                        Calendar.getInstance(Locale.getDefault())
                cal.timeInMillis = dateL
                val date =
                        DateFormat.format("dd.MM.yyyy HH:mm", cal)
                                .toString()
                "${context.getString(R.string.next_expire_date)} $date"
            }
        } else {
            "${context.getString(R.string.next_expire_date)} ${context.getString(R.string.nothing_learned_yet)}"
        }
    }

    fun setupNewLesson() {
        paukerAndModelManager.setupNewApplicationLesson()
        paukerManager.isSaveRequired = false
    }

    fun isLessonNotNew(): Boolean = paukerManager.isLessonNotNew

    fun currentFileNameExist(): String =
            if (fileNamesLiveData.value?.contains(paukerManager.currentFileName) == true) {
                getCurrentFileName()
            } else {
                ""
            }

    fun getCurrentFileName(): String = paukerManager.currentFileName ?: ""

    private fun getSelectedFileName(): String? = fileNamesLiveData.value?.get(lastSelection)

    fun lessonSelected(position: Int) {
        if (lastSelection != position) {
            lastSelection = position
        }
    }

    fun resetSelection(): Boolean = if (lastSelection != -1) {
        lastSelection = -1
        true
    } else false

    fun isPositionSelected(position: Int): Boolean = lastSelection == position

    fun startSync(accessToken: String) {
        this.accessToken = accessToken
        val syncIntent = Intent(context, SyncDialog::class.java)
        syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, this.accessToken)
        syncIntent.putExtra(SyncDialog.FILES, files.toTypedArray())
        syncIntent.action = SyncDialog.SYNC_ALL_ACTION
        (context as Activity).startActivityForResult(
                syncIntent,
                Constants.REQUEST_CODE_SYNC_DIALOG
        )
    }

    fun deleteLesson(filename: String) {
        val filePath =
                Environment.getExternalStorageDirectory().toString() +
                        paukerManager.applicationDataDirectory + filename
        val file = File(filePath)
        if (file.isFile) {
            if (modelManager.deleteLesson(context, file)) {
                readFlashCardFiles()
                ShortcutReceiver.deleteShortcut(context, filename)
                if (fileNamesLiveData.value?.contains(paukerManager.currentFileName) == true) {
                    paukerAndModelManager.setupNewApplicationLesson()
                    paukerManager.isSaveRequired = false
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

    fun openLesson(filename: String?) {
        paukerAndModelManager.loadLessonFromFile(paukerManager.getFilePath(context, filename))
        paukerManager.isSaveRequired = false
    }

    fun openSelectedLessonAfterSync() {
        openLesson(getSelectedFileName())
    }

    fun openSelectedLesson(): Boolean = openLesson(lastSelection)

    fun openLesson(position: Int): Boolean {
        val filename = fileNamesLiveData.value?.get(position) as String
        if (SettingsManager.instance().getBoolPreference(context, SettingsManager.Keys.AUTO_DOWNLOAD)) {
            val accessToken =
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .getString(
                                    Constants.DROPBOX_ACCESS_TOKEN,
                                    null
                            )
            val syncIntent = Intent(context, SyncDialog::class.java)
            syncIntent.putExtra(
                    SyncDialog.FILES,
                    paukerManager.getFilePath(context, filename)
            )
            syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken)
            syncIntent.action = SyncDialog.SYNC_FILE_ACTION
            (context as Activity).startActivityForResult(
                    syncIntent,
                    Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN
            )
            Log.d(
                    "LessonImportActivity:openLesson",
                    "Check for newer version on DB"
            )
            return false
        } else {
            PaukerManager.showToast(
                    context as Activity,
                    R.string.open_lesson_hint,
                    Toast.LENGTH_SHORT
            )
            openLesson(filename)
            return true
        }
    }


    fun createShortCut(position: Int) {
        fileNamesLiveData.value?.get(position)?.let {
            ShortcutReceiver.createShortcut(context, it)
        }
    }


    fun deleteShortCut(position: Int) {
        fileNamesLiveData.value?.get(position)?.let {
            ShortcutReceiver.deleteShortcut(context,it)
        }
    }

    fun openBrowserForDownload() {
        val url = "http://pauker.sourceforge.net/pauker.php?page=lessons"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        (context as Activity).startActivity(browserIntent)
    }
}