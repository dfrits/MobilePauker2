package com.daniel.mobilepauker2.dropbox

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.pauker_native.Log
import com.dropbox.core.android.Auth

class DropboxAccDialog : Activity() {
    private var prefs: SharedPreferences? = null
    private var assStarted = false
    private var firstStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_dialog)
        val progressBar = findViewById<RelativeLayout>(R.id.pFrame)
        progressBar.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val accessToken = prefs?.getString(Constants.DROPBOX_ACCESS_TOKEN, null)
        val intent = intent
        if (intent.getBooleanExtra(AUTH_MODE, false)) {
            title.setText(R.string.association)
            if (accessToken == null) {
                Auth.startOAuth2Authentication(
                    this,
                    Constants.DROPBOX_APP_KEY
                )
                assStarted = true
            } else {
                PaukerManager.showToast(this, "Bereits verbunden", Toast.LENGTH_SHORT)
                setResult(RESULT_CANCELED)
                finish()
            }
        } else if (intent.getBooleanExtra(UNL_MODE, false)) {
            title.setText(R.string.unlinking)
            if (accessToken == null) {
                PaukerManager.showToast(this, "Nicht möglich", Toast.LENGTH_SHORT)
                setResult(RESULT_CANCELED)
                finish()
            } else {
                prefs?.edit()?.remove(Constants.DROPBOX_ACCESS_TOKEN)?.apply()
                PaukerManager.showToast(this, "Dropbox getrennt", Toast.LENGTH_SHORT)
                Log.d(
                    "SettingsFragment::initSyncPrefs",
                    "accessTocken = null"
                )
                setResult(RESULT_OK)
                finish()
            }
        } else finish()
    }

    override fun onResume() {
        super.onResume()
        if (!firstStart && assStarted) {
            val accessToken = Auth.getOAuth2Token()
            if (accessToken != null) {
                prefs?.edit()?.putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken)?.apply()
                PaukerManager.showToast(this, "Verbunden", Toast.LENGTH_SHORT)
                setResult(RESULT_OK)
            } else {
                PaukerManager.showToast(this, "Fehler beim Verbinden", Toast.LENGTH_SHORT)
                setResult(RESULT_CANCELED)
            }
            finish()
        }
        firstStart = false
    }

    fun cancelClicked(view: View?) {}

    companion object {
        var AUTH_MODE = "AUTH_MODE"
        var UNL_MODE = "UNL_MODE"
    }
}