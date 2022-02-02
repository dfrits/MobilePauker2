package de.daniel.mobilepauker2.dropbox

import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.ListFolderResult

/**
 * Async task to list items in a folder
 */
class ListFolderTask internal constructor(
    private val mDbxClient: DbxClientV2,
    private val cachedCursor: String?,
    private val mCallback: Callback
) : CoroutinesAsyncTask<String?, Void?, ListFolderResult?>("ListFolderTask") {
    private var mException: DbxException? = null

    interface Callback {
        fun onDataLoaded(listFolderResult: ListFolderResult?)
        fun onError(e: DbxException?)
    }

    override fun onPostExecute(result: ListFolderResult?) {
        super.onPostExecute(result)
        if (mException != null) {
            mCallback.onError(mException)
        } else {
            mCallback.onDataLoaded(result)
        }
    }

    override fun doInBackground(vararg params: String?): ListFolderResult? {
        mException = try {
            return if (cachedCursor == null) {
                mDbxClient.files()
                    .listFolderBuilder(params[0])
                    .withRecursive(false)
                    .withIncludeDeleted(true)
                    .start()
            } else {
                mDbxClient.files().listFolderContinue(cachedCursor)
            }
        } catch (e: DbxException) {
            e
        }
        return null
    }
}