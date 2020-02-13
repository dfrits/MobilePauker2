package com.daniel.mobilepauker2.dropbox

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.Log
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.dropbox.core.DbxException
import com.dropbox.core.v2.files.*
import java.io.File
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by dfritsch on 21.11.2018.
 * MobilePauker++
 */
@Suppress("UNCHECKED_CAST")
class SyncDialog : Activity() {
    private val context: Context = this
    private val modelManager: ModelManager = ModelManager.instance()
    private val paukerManager: PaukerManager = PaukerManager.instance()
    private var timeout: Timer? = null
    private var timerTask: TimerTask? = null
    private var cancelbutton: Button? = null
    private lateinit var files: Array<File>
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network?) {
            connectionLost()
        }

        override fun onUnavailable() {
            connectionLost()
        }
    }

    // Hintergrundtasks
    private val tasks: MutableList<AsyncTask<*, *, *>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_dialog)
        val intent = intent
        val accessToken = intent.getStringExtra(ACCESS_TOKEN)

        if (accessToken == null) {
            Log.d(
                "SyncDialog::OnCreate",
                "Synchro mit accessToken = null gestartet"
            )
            finishDialog(RESULT_CANCELED)
            return
        }

        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ni = cm.activeNetworkInfo

        if (ni == null || !ni.isConnected) {
            PaukerManager.showToast(
                context as Activity,
                "Internetverbindung prüfen!",
                Toast.LENGTH_LONG
            )
            finishDialog(RESULT_CANCELED)
            return
        }

        cm.registerDefaultNetworkCallback(networkCallback)

        DropboxClientFactory.init(accessToken)

        val serializableExtra =
            intent.getSerializableExtra(FILES)

        startSync(intent, serializableExtra)
    }

    private fun connectionLost() {
        PaukerManager.showToast(
            context as Activity,
            "Internetverbindung prüfen!",
            Toast.LENGTH_LONG
        )
        finishDialog(RESULT_CANCELED)
    }

    private fun startSync(intent: Intent, serializableExtra: Serializable) {
        intent.action?.let { action ->
            if (SYNC_ALL_ACTION == action && serializableExtra is Array<*>) {
                syncAllFiles(serializableExtra as Array<File>)
            } else if (serializableExtra is File) {
                syncFile(serializableExtra, action)
            } else {
                Log.d("SyncDialog::OnCreate", "Synchro mit falschem Extra gestartet")
                finishDialog(RESULT_CANCELED)
            }
        }
    }

    /**
     * Synchronisiert alle Files mit denen auf Dropbox.
     * @param serializableExtra Lokale Files die synchronisiert werden sollen
     */
    private fun syncAllFiles(serializableExtra: Array<File>) {
        showProgressbar()
        files = serializableExtra
        startTimer()
        loadData()
    }

    /**
     * Bei [UPLOAD_ACTION][SyncDialog.UPLOAD_FILE_ACTION] wird das File hochgeladen.
     * Dabei wird nicht überprüft ob das neuer oder älter ist, als das File auf Dropbox. <br></br>
     * Bei [SYNC_FILE_ACTION][SyncDialog.SYNC_FILE_ACTION] wird das File mit dem gleichnamigen
     * auf Dropbox verglichen. Ist das auf Dropbox neuer, wird dieses heruntergeladen.
     * @param serializableExtra File, das synchronisiert werden soll
     * @param action Spezifiziert was mit dem File geschehen soll
     */
    private fun syncFile(serializableExtra: File, action: String) {
        val list: MutableList<File> = ArrayList()
        if (serializableExtra.exists()) {
            Log.d("SyncDialog:syncFile", "File exists")
            Log.d("SyncDialog:syncFile", "Syncaction: $action")
            when (action) {
                UPLOAD_FILE_ACTION -> {
                    Log.d("SyncDialog:syncFile", "Upload just one file")
                    list.add(serializableExtra)
                    uploadFiles(list)
                    finishDialog(RESULT_OK)
                }
                SYNC_FILE_ACTION -> {
                    Log.d(
                        "SyncDialog:syncFile",
                        "Download just one file"
                    )
                    showProgressbar()
                    showCancelButton()
                    val getFileMetadataTask =
                        GetFileMetadataTask(
                            DropboxClientFactory.client!!,
                            object : GetFileMetadataTask.Callback {
                                override fun onDataLoaded(result: Metadata?) {
                                    Log.d(
                                        "SyncDialog:syncFile::onDataLoaded",
                                        "Data loaded"
                                    )
                                    if (result is FileMetadata) {
                                        if (serializableExtra.lastModified() < result.clientModified.time) {
                                            Log.d(
                                                "SyncDialog:syncFile::onDataLoaded",
                                                "File wird runtergeladen"
                                            )
                                            val metadataList: ArrayList<FileMetadata> =
                                                ArrayList()
                                            metadataList.add(result)
                                            downloadFiles(metadataList, result.size.toInt())
                                        } else {
                                            Log.d(
                                                "SyncDialog:syncFile::onDataLoaded",
                                                "File wird NICHT runtergeladen"
                                            )
                                            finishDialog(RESULT_CANCELED)
                                        }
                                    } else if (result is DeletedMetadata) {
                                        PaukerManager.showToast(
                                            context as Activity,
                                            "Datei ist nicht länger verfügbar auf Dropbox",
                                            Toast.LENGTH_LONG
                                        )
                                        finishDialog(RESULT_CANCELED)
                                    }
                                }

                                override fun onError(e: DbxException?) {
                                    PaukerManager.showToast(
                                        context as Activity,
                                        R.string.error_synchronizing,
                                        Toast.LENGTH_LONG
                                    )
                                    finishDialog(RESULT_CANCELED)
                                }
                            }).execute(serializableExtra)
                    tasks.add(getFileMetadataTask)
                }
                else -> {
                    Log.d("SyncDialog:syncFile", "File does not exist")
                    PaukerManager.showToast(
                        context as Activity,
                        R.string.error_synchronizing,
                        Toast.LENGTH_LONG
                    )
                    finishDialog(RESULT_CANCELED)
                }
            }
        } else {
            Log.d("SyncDialog:syncFile", "File does not exist")
            PaukerManager.showToast(
                context as Activity,
                R.string.error_file_not_found,
                Toast.LENGTH_LONG
            )
            finishDialog(RESULT_CANCELED)
        }
    }

    private fun showProgressbar() {
        val progressBar = findViewById<RelativeLayout>(R.id.pFrame)
        progressBar.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        title.setText(R.string.synchronizing)
    }

    private fun showCancelButton() {
        cancelbutton = findViewById(R.id.cancel_button)
        cancelbutton?.visibility = View.VISIBLE
        Log.d(
            "SyncDialog::showCancelButton",
            "Button is enabled: " + cancelbutton?.isEnabled
        )
    }

    /**
     * Sucht nach allen Dateien im Dropboxordner, validiert sie und ruft dann
     * [synWithFiles()][SyncDialog.syncWithFiles] auf.
     */
    private fun loadData() {
        val listFolderTask: AsyncTask<String?, Void?, ListFolderResult?>
        listFolderTask = ListFolderTask(
            DropboxClientFactory.client,
            object : ListFolderTask.Callback {
                override fun onDataLoaded(result: ListFolderResult?) {
                    // Dateien in Dropbox mit der passenden Endung
                    val dbFiles: MutableList<Metadata> = ArrayList()

                    // Dateien, die in Dropbox gelöscht wurden
                    val dbDeletedFiles: MutableList<Metadata> = ArrayList()

                    val lokalAddedFiles = modelManager.getLokalAddedFiles(context)

                    result?.let {
                        while (true) {
                            val entries = result.entries
                            for (entry in entries) {
                                if (paukerManager.validateFilename(entry.name)) {
                                    if (entry is DeletedMetadata) {
                                        if (!lokalAddedFiles.contains(entry.getName())) {
                                            dbDeletedFiles.add(entry)
                                        }
                                    } else {
                                        dbFiles.add(entry)
                                    }
                                }
                            }
                            if (!result.hasMore) break
                            try {
                                DropboxClientFactory.client?.files()
                                    ?.listFolderContinue(result.cursor)
                            } catch (e: DbxException) {
                                e.printStackTrace()
                            }
                        }
                    }
                    deleteFiles(dbDeletedFiles, dbFiles)
                }

                override fun onError(e: DbxException) {
                    Log.d(
                        "LessonImportActivity::loadData::onError"
                        , "Error loading Files: " + e.message
                    )
                    PaukerManager.showToast(
                        context as Activity,
                        R.string.simple_error_message,
                        Toast.LENGTH_SHORT
                    )
                    cancelTasks()
                    if (e.requestId != null && e.requestId == "401") {
                        val builder =
                            AlertDialog.Builder(context)
                        builder.setTitle("Dropbox token is invalid!")
                            .setMessage(
                                "There is something wrong with the dropbox token. Maybe it is " +
                                        "solved by the next try. If this doesn't work" +
                                        "please get in contact."
                            )
                            .setPositiveButton(R.string.ok, null)
                            .setNeutralButton("Send E-Mail") { _, _ ->
                                val reporter: ErrorReporter = ErrorReporter.instance()
                                reporter.init(context)
                                reporter.uncaughtException(null, e)
                                reporter.checkErrorAndSendMail()
                            }
                            .setOnDismissListener {
                                PreferenceManager.getDefaultSharedPreferences(context).edit()
                                    .putString(
                                        Constants.DROPBOX_ACCESS_TOKEN,
                                        null
                                    ).apply()
                                finishDialog(RESULT_CANCELED)
                            }
                            .setCancelable(false)
                        builder.create().show()
                    } else {
                        finishDialog(RESULT_CANCELED)
                    }
                }
            }).execute(Constants.DROPBOX_PATH)
        tasks.add(listFolderTask)
    }

    /**
     * Löscht die Dateien auf Dropbox und Lokal.
     * @param deletedFiles   Liste von Metadaten der Dateien auf Dropbox, welche lokal gelöscht
     * werden sollen
     * @param validatedFiles Files, die anschließend syncronisiert werden sollen
     */
    private fun deleteFiles(deletedFiles: List<Metadata>, validatedFiles: List<Metadata>) {
        val lokalDeletedFiles =
            modelManager.getLokalDeletedFiles(context) // Dateien, die lokal gelöscht wurden
        val filesTMP: List<File> = ArrayList(listOf(*files))

        for (i in deletedFiles.indices) {
            if (deletedFiles[i] is DeletedMetadata) {
                val metadata = deletedFiles[i] as DeletedMetadata
                val index = getFileIndex(File(metadata.name), filesTMP)
                if (index > -1) {
                    filesTMP[index].delete()
                }
            }
        }
        val data = lokalDeletedFiles.keys.toTypedArray()
        val deleteTask: AsyncTask<String, Void, List<DeleteResult>?>
        deleteTask = DeleteFileTask(
            DropboxClientFactory.client,
            object : DeleteFileTask.Callback {
                override fun onDeleteComplete(result: List<DeleteResult?>?) {
                    modelManager.resetDeletedFilesData(context)
                    syncWithFiles(validatedFiles, lokalDeletedFiles)
                }

                override fun onError(e: Exception?) {
                    println()
                }
            }).execute(*data)
        tasks.add(deleteTask)
    }

    /**
     * Aktualisiert die Files und ladet gegebenenfalls runter bzw hoch.
     * @param metadataList      Liste von Metadaten der Dateien auf Dropbox, welche mit den lokalen
     * Dateien verglichen werden sollen
     * @param lokalDeletedFiles Liste von Files, die im lokalen Speicher gelöscht wurden
     */
    private fun syncWithFiles(
        metadataList: List<Metadata>,
        lokalDeletedFiles: Map<String?, String?>
    ) {
        // Lokale Files kopieren, damit nichts verloren geht
        val filesTMP: List<File>
        filesTMP = ArrayList(listOf(*files))
        val lokal: MutableList<File> =
            ArrayList() // Zum Hochladen
        val dropB: ArrayList<FileMetadata> =
            ArrayList() // Zum Runterladen
        var downloadSize = 0 // Die Gesamtgröße zum runterladen
        for (i in metadataList.indices) {
            if (metadataList[i] is FileMetadata) {
                val metadata = metadataList[i] as FileMetadata
                if (paukerManager.validateFilename(metadata.name)) {
                    val fileIndex = getFileIndex(File(metadata.name), filesTMP)
                    if (fileIndex == -1) {
                        if (getFileIndex(metadata.name, lokalDeletedFiles.keys) == -1) {
                            dropB.add(metadata)
                            downloadSize += metadata.size.toInt()
                        }
                    } else {
                        if (metadata.clientModified.time < filesTMP[fileIndex].lastModified()) {
                            lokal.add(filesTMP[fileIndex])
                        } else {
                            dropB.add(metadata)
                            downloadSize += metadata.size.toInt()
                        }
                        filesTMP.removeAt(fileIndex)
                    }
                }
            }
        }
        if (filesTMP.isNotEmpty()) lokal.addAll(filesTMP)
        if (lokal.isNotEmpty()) {
            uploadFiles(lokal)
        }
        if (dropB.isNotEmpty()) {
            downloadFiles(dropB, downloadSize)
        } else {
            setResult(if (modelManager.resetIndexFiles(context)) RESULT_OK else RESULT_CANCELED)
            finish()
        }
    }

    /**
     * Ladet die Dateien in den Ordner herunter und aktualisiert die Liste.
     * @param list Liste mit den Dateien, die heruntergeladen werden sollen
     */
    private fun downloadFiles(
        list: ArrayList<FileMetadata>,
        downloadSize: Int
    ) {
        val data = arrayOfNulls<FileMetadata>(list.size)
        list.toArray(data)
        val progressBar = findViewById<ProgressBar>(R.id.pBar)
        val downloadTask: AsyncTask<FileMetadata, FileMetadata, List<File>>
        downloadTask = DownloadFileTask(DropboxClientFactory.client,
            object : DownloadFileTask.Callback {
                override fun onDownloadStartet() {
                    Log.d(
                        "SyncDialog:downloadFiles",
                        "Download startet"
                    )
                    progressBar.max = downloadSize
                    progressBar.isIndeterminate = false
                }

                override fun onProgressUpdate(metadata: FileMetadata) {
                    Log.d(
                        "SyncDialog:downloadFiles",
                        "Download update: " + progressBar.progress + metadata.size
                    )
                    progressBar.progress = (progressBar.progress + metadata.size).toInt()
                }

                override fun onDownloadComplete(result: List<File>) {
                    Log.d(
                        "SyncDialog:downloadFiles",
                        "Download complete"
                    )
                    progressBar.isIndeterminate = true
                    finishDialog(RESULT_OK)
                }

                override fun onError(e: Exception?) {
                    Log.e(
                        "LessonImportActivity::downloadFiles",
                        "Failed to download file.", e
                    )
                    PaukerManager.showToast(
                        context as Activity,
                        R.string.simple_error_message,
                        Toast.LENGTH_SHORT
                    )
                    finishDialog(RESULT_CANCELED)
                }
            }).execute(*data)
        tasks.add(downloadTask)
    }

    /**
     * Lädt die Dateien in den Dropboxordner hoch.
     * @param list Liste mit Dateien, die hochgeladen werden sollen
     */
    private fun uploadFiles(list: List<File>) {
        val data = arrayOfNulls<File>(list.size)
        for (i in list.indices) {
            val file = list[i]
            if (file.exists()) data[i] = file
        }
        val uploadTask: AsyncTask<File, Void, List<Metadata>?>
        uploadTask = UploadFileTask(
            DropboxClientFactory.client,
            object : UploadFileTask.Callback {
                override fun onUploadComplete(result: List<Metadata>) {
                    modelManager.resetAddedFilesData(context)
                    Log.d("SyncDialog:uploadFiles", "upload success")
                }

                override fun onError(e: Exception) {
                    Log.e(
                        "SyncDialog::uploadFiles",
                        "upload error: ", e
                    )
                    runOnUiThread {
                        PaukerManager.showToast(
                            context as Activity,
                            e.message,
                            Toast.LENGTH_LONG
                        )
                    }
                }
            }).execute(*data)
        tasks.add(uploadTask)
    }

    /**
     * Sucht nach dem ersten Index des Files mit dem übergebenen Namen.
     * @param file Name des Files
     * @param list Liste in der gesucht werden soll
     * @return Index. -1, falls File nicht vorhanden ist
     */
    private fun getFileIndex(file: File, list: Collection<File>): Int {
        for ((i, item) in list.withIndex()) {
            if (item.name == file.name) return i
        }
        return -1
    }

    /**
     * Sucht nach dem ersten Index des Files mit dem übergebenen Namen.
     * @param fileName Name des Files
     * @param list     Liste in der gesucht werden soll
     * @return Index. -1, falls File nicht vorhanden ist
     */
    private fun getFileIndex(fileName: String, list: Collection<String?>): Int {
        for ((i, name) in list.withIndex()) {
            if (name == fileName) return i
        }
        return -1
    }

    // Touchevents und Backbutton blockieren, dass er nicht minimiert werden kann
    override fun onBackPressed() {}

    override fun onResume() {
        super.onResume()
        if (timeout != null && timerTask != null) {
            timeout!!.cancel()
            startTimer()
        }
    }

    override fun onDestroy() {
        if (timeout != null && timerTask != null) {
            timeout!!.cancel()
        }
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .unregisterNetworkCallback(networkCallback)
        cancelTasks()
        super.onDestroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        Log.d("SyncDialog::TouchEvent", "Touched")
        if (cancelbutton != null) {
            val pos = IntArray(2)
            cancelbutton!!.getLocationInWindow(pos)
            if (ev.y <= pos[1] + cancelbutton!!.height && ev.x > pos[0]
                && ev.y > pos[1] && ev.x <= pos[0] + cancelbutton!!.width
                && ev.action == MotionEvent.ACTION_UP
            ) {
                cancelClicked(cancelbutton!!)
            }
        }
        return false
    }

    private fun startTimer() {
        timeout = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    PaukerManager.showToast(
                        context as Activity,
                        R.string.synchro_timeout,
                        Toast.LENGTH_SHORT
                    )
                }
                finishDialog(RESULT_CANCELED)
            }
        }
        timeout!!.schedule(timerTask, 60000)
    }

    fun cancelClicked(view: View) {
        Log.d("SyncDialog::cancelClicked", "Cancel Sync")
        view.isEnabled = false
        PaukerManager.showToast(
            context as Activity,
            R.string.synchro_canceled_by_user,
            Toast.LENGTH_LONG
        )
        finishDialog(RESULT_CANCELED)
    }

    private fun cancelTasks() {
        for (task in tasks) {
            if (task.status != AsyncTask.Status.FINISHED) {
                task.cancel(false)
                tasks.remove(task)
            }
        }
    }

    private fun finishDialog(result: Int) {
        setResult(result)
        finish()
    }

    companion object {
        const val ACCESS_TOKEN = "ACCESS_TOKEN"
        const val FILES = "FILES"
        const val SYNC_ALL_ACTION = "SYNC_ALL_ACTION"
        const val UPLOAD_FILE_ACTION = "UPLOAD_FILE_ACTION"
        const val SYNC_FILE_ACTION = "SYNC_FILE_ACTION"
    }
}