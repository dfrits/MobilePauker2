package com.daniel.mobilepauker2.lessonimport

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.daniel.mobilepauker2.pauker_native.Log
import com.daniel.mobilepauker2.pauker_native.PaukerAndModelManager
import java.io.File
import java.util.*

class LessonImportViewModel(
        val paukerAndModelManager: PaukerAndModelManager,
        val context: Context
) : ViewModel() {
    private val paukerManager = paukerAndModelManager.paukerManager
    private val modelManager = paukerAndModelManager.modelManager
    private val files: MutableList<File> = mutableListOf()

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
}