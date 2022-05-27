package de.daniel.mobilepauker2.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import de.daniel.mobilepauker2.utils.Constants

/**
 * Singleton instance of [DbxClientV2] and friends
 */
internal object DropboxClientFactory {
    private var sDbxClient: DbxClientV2? = null

    fun init(credential: DbxCredential) {
        val newCredential = DbxCredential(
            credential.accessToken,
            -1L,
            credential.refreshToken,
            credential.appKey
        )
        if (sDbxClient == null) {
            sDbxClient = DbxClientV2(DbxRequestConfigFactory.requestConfig, newCredential)
        }
    }

    fun readCredentialFromString(credential: String): DbxCredential =
        DbxCredential.Reader.readFully(credential)

    val client: DbxClientV2
        get() {
            checkNotNull(sDbxClient) { "Client not initialized." }
            return sDbxClient!!
        }
}