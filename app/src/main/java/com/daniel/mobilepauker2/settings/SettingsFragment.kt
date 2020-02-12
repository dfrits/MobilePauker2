package com.daniel.mobilepauker2.settings

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.RingtoneManager
import android.os.Bundle
import android.preference.*
import android.preference.Preference.OnPreferenceClickListener
import android.provider.Settings
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.dropbox.DropboxAccDialog
import com.daniel.mobilepauker2.settings.SettingsManager.Keys
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.pauker_native.Log

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class SettingsFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {
    private val settingsManager: SettingsManager =
        SettingsManager.instance()

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        val preferenceScreen = preferenceScreen
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        init(preferenceScreen)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent
    ) {
        if (requestCode == Constants.REQUEST_CODE_DB_ACC_DIALOG && resultCode == Activity.RESULT_OK) {
            initSyncPrefs(context)
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        updatePrefSummary(findPreference(key))
    }

    override fun onResume() {
        super.onResume()
        updatePrefSummary(
            findPreference(
                settingsManager!!.getSettingsKey(
                    context,
                    Keys.RING_TONE
                )
            )
        )
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun init(preference: Preference) {
        if (preference is PreferenceGroup) {
            val pGrp = preference
            for (i in 0 until pGrp.preferenceCount) {
                init(pGrp.getPreference(i))
            }
        } else {
            if (preference is EditTextPreference) {
                val editTextP = preference
                editTextP.editText.addTextChangedListener(
                    MinFilter(
                        editTextP
                    )
                )
            }
            updatePrefSummary(preference)
        }
        findPreference(
            settingsManager!!.getSettingsKey(
                context,
                Keys.RING_TONE
            )
        ).onPreferenceClickListener = OnPreferenceClickListener {
            val intent =
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            intent.putExtra(
                Settings.EXTRA_APP_PACKAGE,
                context.packageName
            )
            intent.putExtra(
                Settings.EXTRA_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_ID
            )
            startActivity(intent)
            true
        }
    }

    private fun updatePrefSummary(preference: Preference?) {
        if (preference == null) return
        var summ = preference.summary
        val context = context
        if (summ != null) {
            if (preference is EditTextPreference) {
                val editTextP = preference
                if (editTextP.key == settingsManager!!.getSettingsKey(
                        context,
                        Keys.USTM
                    )
                ) summ =
                    getString(R.string.ustm_summ) else if (editTextP.key == settingsManager.getSettingsKey(
                        context,
                        Keys.STM
                    )
                ) summ = getString(R.string.stm_summ)
                editTextP.summary = String.format(summ.toString(), editTextP.text)
            } else if (preference is ListPreference) {
                val listP = preference
                val s = listP.key
                if (s == settingsManager!!.getSettingsKey(context, Keys.REPEAT_CARDS)) {
                    summ = getString(R.string.repeat_cards_summ)
                } else if (s == settingsManager.getSettingsKey(
                        context,
                        Keys.RETURN_FORGOTTEN_CARDS
                    )
                ) {
                    summ = getString(R.string.return_forgotten_cards_summ)
                } else if (s == settingsManager.getSettingsKey(context, Keys.FLIP_CARD_SIDES)) {
                    summ = getString(R.string.flip_card_sides_summ)
                }
                listP.summary = String.format(summ.toString(), listP.entry)
            }
        }
        val preferenceKey = preference.key
        if (preferenceKey != null) {
            if (preferenceKey == settingsManager!!.getSettingsKey(context, Keys.DB_PREFERENCE)) {
                initSyncPrefs(context)
            } else if (preferenceKey == settingsManager.getSettingsKey(context, Keys.RING_TONE)) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager != null) {
                    val ringtonePath =
                        notificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)
                            .sound
                    val ringtone = RingtoneManager.getRingtone(context, ringtonePath)
                    preference.summary = ringtone.getTitle(context)
                } else {
                    preference.summary = "No Sound"
                }
            }
        }
    }

    private fun removeSyncPrefAndSetAutoSync(enableAutoSync: Boolean) {
        val switchUp = findPreference(
            settingsManager.getSettingsKey(context, Keys.AUTO_UPLOAD)
        ) as SwitchPreference
        val switchDown = findPreference(
            settingsManager.getSettingsKey(context, Keys.AUTO_DOWNLOAD)
        ) as SwitchPreference
        if (enableAutoSync) {
            switchUp.setSummary(R.string.auto_sync_enabled_upload_summ)
            switchDown.setSummary(R.string.auto_sync_enabled_download_summ)
        } else {
            switchUp.setSummary(R.string.auto_sync_disabled_summ)
            switchDown.setSummary(R.string.auto_sync_disabled_summ)
        }
        switchUp.isEnabled = enableAutoSync
        switchDown.isEnabled = enableAutoSync
    }

    private fun initSyncPrefs(context: Context) {
        Log.d("SettingsFragment::initSyncPrefs", "init syncprefs")
        val instance: SettingsManager =
            SettingsManager.instance()
        val dbPref =
            findPreference(instance.getSettingsKey(context, Keys.DB_PREFERENCE))
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val accessToken =
            pref.getString(Constants.DROPBOX_ACCESS_TOKEN, null)
        if (accessToken == null) {
            setPrefAss(context, dbPref)
        } else {
            pref.edit().putString(
                Constants.DROPBOX_ACCESS_TOKEN,
                accessToken
            ).apply()
            setPrefUnlink(context, dbPref)
            Log.d(
                "SettingsFragment::initSyncPrefs",
                "enable autosync"
            )
            removeSyncPrefAndSetAutoSync(true)
        }
    }

    private fun setPrefAss(context: Context, dbPref: Preference) {
        dbPref.setTitle(R.string.associate_dropbox_title)
        val assIntent = Intent(context, DropboxAccDialog::class.java)
        assIntent.putExtra(DropboxAccDialog.Companion.AUTH_MODE, true)
        dbPref.onPreferenceClickListener = OnPreferenceClickListener {
            startActivityForResult(
                assIntent,
                Constants.REQUEST_CODE_DB_ACC_DIALOG
            )
            false
        }
        Log.d("SettingsFragment::initSyncPrefs", "disable autosync")
        removeSyncPrefAndSetAutoSync(false)
    }

    private fun setPrefUnlink(context: Context, dbPref: Preference) {
        dbPref.setTitle(R.string.unlink_dropbox_title)
        val unlIntent = Intent(context, DropboxAccDialog::class.java)
        unlIntent.putExtra(DropboxAccDialog.Companion.UNL_MODE, true)
        dbPref.onPreferenceClickListener = OnPreferenceClickListener {
            Log.d(
                "SettingsFragment::initSyncPrefs",
                "unlinkDB clicked"
            )
            startActivityForResult(
                unlIntent,
                Constants.REQUEST_CODE_DB_ACC_DIALOG
            )
            false
        }
    }
}