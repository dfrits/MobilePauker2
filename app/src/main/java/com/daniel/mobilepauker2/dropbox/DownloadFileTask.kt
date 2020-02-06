package com.daniel.mobilepauker2.dropbox

import android.os.AsyncTask
import android.os.Environment
import com.daniel.mobilepauker2.PaukerManager
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Task to download a file from Dropbox and put it in the Downloads folder
 */
class DownloadFileTask internal constructor(
    private val mDbxClient: DbxClientV2?,
    private val mCallback: Callback
) : AsyncTask<FileMetadata, FileMetadata, List<File>>() {

    override fun onPostExecute(result: List<File>) {
        super.onPostExecute(result)
        mCallback.onDownloadComplete(result)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        mCallback.onDownloadStartet()
    }

    override fun onProgressUpdate(vararg values: FileMetadata) {
        super.onProgressUpdate(*values)
        mCallback.onProgressUpdate(values[0])
    }

    override fun doInBackground(vararg params: FileMetadata): List<File>? {
        var downloadFile: File? = null
        try {
            val list: MutableList<File> =
                ArrayList()
            for (metadata in params) {
                val path = File(
                    Environment.getExternalStorageDirectory()
                        .toString() + PaukerManager.instance().applicationDataDirectory
                )
                val file = File(path, metadata.name)
                // Make sure the Downloads directory exists.
                if (!path.exists() && !path.mkdirs()) {
                    mCallback.onError(RuntimeException("Unable to access directory: $path"))
                    return null
                } else if (!path.isDirectory) {
                    mCallback.onError(IllegalStateException("Download path is not a directory: $path"))
                    return null
                }
                // Download the file.
                downloadFile = File(file.parent, "downloadFile.pau.gz")
                FileOutputStream(downloadFile).use { outputStream ->
                    mDbxClient!!.files().download(metadata.pathLower, metadata.rev)
                        .download(outputStream)
                    outputStream.close()
                    downloadFile.renameTo(file)
                }
                list.add(file)
                publishProgress(metadata)
            }
            return list.toList()
        } catch (e: DbxException) {
            mCallback.onError(e)
        } catch (e: IOException) {
            mCallback.onError(e)
        } finally {
            if (downloadFile != null && downloadFile.exists()) {
                downloadFile.delete()
            }
        }
        return null
    }

    interface Callback {
        fun onDownloadStartet()
        fun onProgressUpdate(metadata: FileMetadata)
        fun onDownloadComplete(result: List<File>)
        fun onError(e: Exception?)
    }
}