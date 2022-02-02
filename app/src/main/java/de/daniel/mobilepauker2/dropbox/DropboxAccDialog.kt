package de.daniel.mobilepauker2.dropbox

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.dropbox.core.android.Auth
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class DropboxAccDialog : AppCompatActivity(R.layout.progress_dialog) {
    private var prefs: SharedPreferences? = null
    private var assStarted = false
    private var firstStart = true

    @Inject
    lateinit var toaster: Toaster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val progressBar = findViewById<RelativeLayout>(R.id.pFrame)
        progressBar.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        val accessToken = prefs?.getString(Constants.DROPBOX_ACCESS_TOKEN, null)

        when (intent.action) {
            Constants.DROPBOX_AUTH_ACTION -> {
                title.setText(R.string.association)
                if (accessToken == null) {
                    Auth.startOAuth2Authentication(this, Constants.DROPBOX_APP_KEY)
                    assStarted = true
                } else {
                    toaster.showToast(this, R.string.already_connected, Toast.LENGTH_SHORT)
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
            Constants.DROPBOX_UNLINK_ACTION -> {
                title.setText(R.string.unlinking)
                if (accessToken == null) {
                    toaster.showToast(this, "Nicht möglich", Toast.LENGTH_SHORT)
                    setResult(RESULT_CANCELED)
                } else {
                    prefs?.edit()?.remove(Constants.DROPBOX_ACCESS_TOKEN)?.apply()
                    toaster.showToast(this, "Dropbox getrennt", Toast.LENGTH_SHORT)
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
        if (!firstStart && assStarted) {
            val accessToken = Auth.getOAuth2Token()
            if (accessToken != null) {
                prefs?.edit()?.putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken)?.apply()
                toaster.showToast(this, "Verbunden", Toast.LENGTH_SHORT)
                setResult(RESULT_OK)
            } else {
                toaster.showToast(this, "Fehler beim Verbinden", Toast.LENGTH_SHORT)
                setResult(RESULT_CANCELED)
            }
            finish()
        }
        firstStart = false
    }

    fun cancelClicked(view: View?) {}
}