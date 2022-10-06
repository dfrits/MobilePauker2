package de.daniel.mobilepauker2.dropbox

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class DropboxAccDialog : AppCompatActivity(R.layout.progress_dialog) {
    private var prefs: SharedPreferences? = null
    private var assStarted = false

    @Inject
    lateinit var toaster: Toaster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val progressBar = findViewById<RelativeLayout>(R.id.pFrame)
        progressBar.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        val credentialPref = prefs?.getString(Constants.DROPBOX_CREDENTIAL, null)

        when (intent.action) {
            Constants.DROPBOX_AUTH_ACTION -> {
                title.setText(R.string.association)
                if (credentialPref == null) {
                    val clientIdentifier = getAppVersion()
                    val requestConfig = DbxRequestConfig(clientIdentifier)
                    Auth.startOAuth2PKCE(this, Constants.DROPBOX_APP_KEY, requestConfig)
                } else {
                    toaster.showToast(this, R.string.already_connected, Toast.LENGTH_SHORT)
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
            Constants.DROPBOX_UNLINK_ACTION -> {
                title.setText(R.string.unlinking)
                if (credentialPref == null) {
                    toaster.showToast(
                        this,
                        getString(R.string.dropbox_link_error),
                        Toast.LENGTH_SHORT
                    )
                    setResult(RESULT_CANCELED)
                } else {
                    prefs?.edit()?.remove(Constants.DROPBOX_CREDENTIAL)?.apply()
                    toaster.showToast(
                        this,
                        getString(R.string.dropbox_unlinked),
                        Toast.LENGTH_SHORT
                    )
                    Log.d("SettingsFragment::initSyncPrefs", "accessTocken = null")
                    setResult(RESULT_OK)
                }
                finish()
            }
            else -> finish()
        }
    }

    override fun onResume() {
        super.onResume()

        val serializedCredential = prefs!!.getString(Constants.DROPBOX_CREDENTIAL, null)

        if (serializedCredential == null && assStarted) {
            val credential = Auth.getDbxCredential()

            if (credential != null) {
                prefs!!.edit()
                    .putString(Constants.DROPBOX_CREDENTIAL, credential.toString())
                    .apply()
                toaster.showToast(this, R.string.connected, Toast.LENGTH_SHORT)

                saveUserMail(credential)

                setResult(RESULT_OK)
            } else {
                toaster.showToast(this, R.string.error_connection, Toast.LENGTH_SHORT)
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        assStarted = true
    }

    fun cancelClicked(view: View?) {}

    private fun getAppVersion(): String {
        val manager = this.packageManager
        val info = manager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        return info.versionName
    }

    private fun saveUserMail(credential: DbxCredential) {
        DropboxClientFactory.init(credential)
        GetUserMailTask(DropboxClientFactory.client, object : GetUserMailTask.Callback {
            override fun onComplete(result: String) {
                prefs?.edit()?.putString(Constants.DROPBOX_USER_MAIL, result)?.apply()
                finish()
            }

            override fun onError() {
                finish()
            }
        }).execute()
    }
}