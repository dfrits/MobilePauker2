package de.daniel.mobilepauker2.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.OkHttp3Requestor

internal object DbxRequestConfigFactory {
    private var sDbxRequestConfig: DbxRequestConfig? = null

    val requestConfig: DbxRequestConfig?
        get() {
            if (sDbxRequestConfig == null) {
                sDbxRequestConfig = DbxRequestConfig.newBuilder("MobilePauker++")
                    .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                    .build();
            }
            return sDbxRequestConfig
        }
}