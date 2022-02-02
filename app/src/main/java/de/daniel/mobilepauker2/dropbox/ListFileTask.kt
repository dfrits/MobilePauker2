package de.daniel.mobilepauker2.dropbox

import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.Metadata

class ListFileTask(
    private val mDbxClient: DbxClientV2,
    private val mCallback: Callback
) : CoroutinesAsyncTask<String?, Void?, Metadata?>("ListFileTask") {
    private var mException: DbxException? = null

    interface Callback {
        fun onDataLoaded(metadata: Metadata?)
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

    override fun doInBackground(vararg params: String?): Metadata? {
        mException = try {
            return mDbxClient.files().getMetadata(params[0])
        } catch (e: DbxException) {
            e
        }
        return null
    }
}