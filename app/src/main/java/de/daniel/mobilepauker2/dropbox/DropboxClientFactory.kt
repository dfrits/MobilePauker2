package de.daniel.mobilepauker2.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2

/**
 * Singleton instance of [DbxClientV2] and friends
 */
internal object DropboxClientFactory {
    private var sDbxClient: DbxClientV2? = null
    fun init(accessToken: String?) {
        if (sDbxClient == null) {
            val requestConfig = DbxRequestConfig.newBuilder("examples-v2-demo")
                .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                .build()
            sDbxClient = DbxClientV2(requestConfig, accessToken)
        }
    }

    val client: DbxClientV2
        get() {
            checkNotNull(sDbxClient) { "Client not initialized." }
            return sDbxClient!!
        }
}