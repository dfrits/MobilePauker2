package de.daniel.mobilepauker2.dropbox

import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.Metadata
import java.io.File

class GetFileMetadataTask internal constructor(
    private val mDbxClient: DbxClientV2,
    private val mCallback: Callback
) : CoroutinesAsyncTask<File, Void, Metadata?>("GetFileMetadataTask") {
    private var mException: DbxException? = null

    interface Callback {
        fun onDataLoaded(result: Metadata?)
        fun onError(e: DbxException?)
    }

    override fun onPostExecute(result: Metadata?) {
        super.onPostExecute(result)
        if (mException != null) {
            mCallback.onError(mException)
        } else {
            mCallback.onDataLoaded(result)
        }
    }

    override fun doInBackground(vararg params: File?): Metadata? {
        mException = try {
            return mDbxClient.files().getMetadata("/" + params[0]?.name)
        } catch (e: DbxException) {
            e
        }
        return null
    }
}