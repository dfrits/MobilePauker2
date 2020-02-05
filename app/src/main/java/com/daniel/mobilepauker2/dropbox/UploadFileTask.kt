package com.daniel.mobilepauker2.dropbox

import android.os.AsyncTask
import com.daniel.mobilepauker2.utils.Constants
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.Metadata
import com.dropbox.core.v2.files.WriteMode
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

/**
 * Async task to upload a file to a directory
 */
class UploadFileTask internal constructor(
    private val mDbxClient: DbxClientV2?,
    private val mCallback: Callback
) : AsyncTask<File?, Void?, List<Metadata?>?>() {

    interface Callback {
        fun onUploadComplete(result: List<Metadata?>?)
        fun onError(e: Exception)
    }

    override fun onPostExecute(result: List<Metadata?>?) {
        super.onPostExecute(result)
        mCallback.onUploadComplete(result)
    }

    protected override fun doInBackground(vararg params: File): List<Metadata?>? {
        val remoteFolderPath =
            File(Constants.DROPBOX_PATH)
        val data: MutableList<Metadata?> =
            ArrayList()
        for (localFile in params) {
            if (localFile != null) {
                if (localFile.exists()) {
                    try {
                        FileInputStream(localFile).use { inputStream ->
                            // Note - this is not ensuring the name is a valid dropbox file name
                            val remoteFileName = localFile.name
                            data.add(
                                mDbxClient!!.files().uploadBuilder("$remoteFolderPath/$remoteFileName")
                                    .withMode(WriteMode.OVERWRITE)
                                    .uploadAndFinish(inputStream)
                            )
                        }
                    } catch (e: DbxException) {
                        mCallback.onError(e)
                    } catch (e: IOException) {
                        mCallback.onError(e)
                    }
                } else {
                    mCallback.onError(Exception("File not found"))
                }
            }
        }
        return data
    }

}