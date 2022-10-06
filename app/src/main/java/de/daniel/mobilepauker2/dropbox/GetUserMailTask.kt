package de.daniel.mobilepauker2.dropbox

import com.dropbox.core.v2.DbxClientV2

class GetUserMailTask internal constructor(
    private val mDbxClient: DbxClientV2,
    private val mCallback: Callback
) : CoroutinesAsyncTask<Void, Void, String>("GetUserMailTask") {

    interface Callback {
        fun onComplete(result: String)
        fun onError()
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        mCallback.onComplete(result)
    }

    override fun doInBackground(vararg params: Void?): String {
        return try {
            mDbxClient.users().currentAccount.email
        } catch (e: Exception) {
            mCallback.onError()
            ""
        }
    }
}