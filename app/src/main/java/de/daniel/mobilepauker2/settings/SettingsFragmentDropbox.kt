package de.daniel.mobilepauker2.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.preference.*
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.dropbox.DropboxAccDialog
import de.daniel.mobilepauker2.settings.SettingsManager.Keys
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class SettingsFragmentDropbox : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var toaster: Toaster

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        (context?.applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
        addPreferencesFromResource(R.xml.preferences_dropbox)
        val preferenceScreen: PreferenceScreen = preferenceScreen
        preferenceScreen.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
        init(preferenceScreen)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePrefSummary(findPreference(key!!))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_DB_ACC_DIALOG && resultCode == Activity.RESULT_OK) {
            initSyncPrefs()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePrefSummary(findPreference(settingsManager.getSettingsKey(Keys.RING_TONE)))
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun init(preference: Preference) {
        if (preference is PreferenceGroup) {
            for (i in 0 until preference.preferenceCount) {
                init(preference.getPreference(i))
            }
        } else {
            updatePrefSummary(preference)
        }
    }

    private fun removeSyncPrefAndSetAutoSync(enableAutoSync: Boolean) {
        val switchUp =
            findPreference(settingsManager.getSettingsKey(Keys.AUTO_UPLOAD)) as SwitchPreference?
        if (enableAutoSync) {
            switchUp?.setSummary(R.string.auto_sync_enabled_upload_summ)
        } else {
            switchUp?.setSummary(R.string.auto_sync_disabled_summ)
        }
        switchUp?.isEnabled = enableAutoSync
    }

    private fun initSyncPrefs() {
        Log.d("SettingsFragment::initSyncPrefs", "init syncprefs")
        val dbPref: Preference? =
            findPreference(settingsManager.getSettingsKey(Keys.DB_PREFERENCE))
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val accessToken = pref.getString(Constants.DROPBOX_ACCESS_TOKEN, null)
        if (accessToken == null) {
            setPrefAss(dbPref)
        } else {
            pref.edit().putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken).apply()
            setPrefUnlink(dbPref)
            Log.d("SettingsFragment::initSyncPrefs", "enable autosync")
            removeSyncPrefAndSetAutoSync(true)

            val syncPref: Preference? =
                findPreference(settingsManager.getSettingsKey(Keys.RESET_SYNC))
            initResetSyncClicked(syncPref)
        }
    }

    private fun setPrefAss(dbPref: Preference?) {
        dbPref?.setTitle(R.string.associate_dropbox_title)
        val assIntent = Intent(context, DropboxAccDialog::class.java)
        assIntent.putExtra(Constants.DROPBOX_AUTH_ACTION, true)
        dbPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivityForResult(assIntent, Constants.REQUEST_CODE_DB_ACC_DIALOG)
            false
        }
        Log.d("SettingsFragment::initSyncPrefs", "disable autosync")
        removeSyncPrefAndSetAutoSync(false)
    }

    private fun initResetSyncClicked(syncPref: Preference?) {
        syncPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            dataManager.cacheCursor(null)
            toaster.showToast(context as Activity, getString(R.string.sync_reset_success), Toast.LENGTH_SHORT)
            false
        }
    }

    private fun setPrefUnlink(dbPref: Preference?) {
        dbPref?.setTitle(R.string.unlink_dropbox_title)
        val unlIntent = Intent(context, DropboxAccDialog::class.java)
        unlIntent.putExtra(Constants.DROPBOX_UNLINK_ACTION, true)
        dbPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Log.d("SettingsFragment::initSyncPrefs", "unlinkDB clicked")
            startActivityForResult(unlIntent, Constants.REQUEST_CODE_DB_ACC_DIALOG)
            false
        }
    }

    private fun updatePrefSummary(preference: Preference?) {
        preference?.key?.let { preferenceKey ->
            if (preferenceKey == settingsManager.getSettingsKey(Keys.DB_PREFERENCE)) {
                initSyncPrefs()
            }
        }
    }
}