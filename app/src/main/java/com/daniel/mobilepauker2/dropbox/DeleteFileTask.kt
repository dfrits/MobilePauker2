package com.daniel.mobilepauker2.dropbox

import android.os.AsyncTask
import com.daniel.mobilepauker2.utils.Constants
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DeleteResult
import java.io.File
import java.util.*

/**
 * Created by Daniel on 14.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 *
 *
 * Löscht die Dateien auf Dropbox.
 */
class DeleteFileTask internal constructor(
    private val mDbxClient: DbxClientV2?,
    private val mCallback: Callback
) : AsyncTask<String, Void, List<DeleteResult>>() {

    interface Callback {
        fun onDeleteComplete(result: List<DeleteResult?>?)
        fun onError(e: Exception?)
    }

    override fun onPostExecute(result: List<DeleteResult>) {
        super.onPostExecute(result)
        mCallback.onDeleteComplete(result)
    }

    override fun doInBackground(vararg params: String): List<DeleteResult> {
        val remoteFolderPath =
            File(Constants.DROPBOX_PATH)
        val data: MutableList<DeleteResult> = ArrayList()
        for (localFile in params) {
            if (!localFile.isEmpty()) {
                try {
                    data.add(mDbxClient!!.files().deleteV2("$remoteFolderPath/$localFile"))
                } catch (e: DbxException) {
                    mCallback.onError(e)
                }
            }
        }
        return data
    }
}