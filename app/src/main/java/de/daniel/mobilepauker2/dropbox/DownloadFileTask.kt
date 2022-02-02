package de.daniel.mobilepauker2.dropbox

import android.os.Environment
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import de.daniel.mobilepauker2.utils.Constants
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Task to download a file from Dropbox and put it in the Downloads folder
 */
class DownloadFileTask internal constructor(
    private val mDbxClient: DbxClientV2,
    private val mCallback: Callback
) : CoroutinesAsyncTask<FileMetadata, FileMetadata, List<File>>("DownloadFileTask") {

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

    override fun doInBackground(vararg params: FileMetadata?): List<File> {
        var downloadFile: File? = null
        try {
            val list: MutableList<File> = mutableListOf()
            for (metadata in params) {
                metadata?.let {
                    val path = File("${Environment.getExternalStorageDirectory()}${Constants.DEFAULT_APP_FILE_DIRECTORY}")
                    val file = File(path, metadata.name)

                    // Make sure the Downloads directory exists.
                    if (!path.exists() && !path.mkdirs()) {
                        mCallback.onError(RuntimeException("Unable to access directory: $path"))
                        return emptyList()
                    } else if (!path.isDirectory) {
                        mCallback.onError(IllegalStateException("Download path is not a directory: $path"))
                        return emptyList()
                    }

                    // Download the file.
                    downloadFile = File(file.parent, "downloadFile.pau.gz")
                    FileOutputStream(downloadFile).use { outputStream ->
                        mDbxClient.files().download(metadata.pathLower, metadata.rev)
                            .download(outputStream)
                        outputStream.close()
                        downloadFile!!.renameTo(file)
                    }
                    list.add(file)
                    publishProgress(metadata)
                }
            }
            val result = arrayOfNulls<File>(list.size)
            return list.toList()
        } catch (e: DbxException) {
            mCallback.onError(e)
        } catch (e: IOException) {
            mCallback.onError(e)
        } finally {
            downloadFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
        return emptyList()
    }

    interface Callback {
        fun onDownloadStartet()
        fun onProgressUpdate(metadata: FileMetadata)
        fun onDownloadComplete(result: List<File>)
        fun onError(e: Exception?)
    }
}